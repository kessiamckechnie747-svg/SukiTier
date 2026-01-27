package com.sukitier.core.failsafe

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sukitier.core.modules.TieredModuleManager
import com.sukitier.core.verification.TierLevel
import com.sukitier.core.verification.TierVerificationEngine
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Fail-Safe and Rollback Management System
 */

/**
 * Fail-safe trigger types
 */
enum class FailSafeEvent {
    TIER1_VERIFICATION_FAILED,
    TIER2_VERIFICATION_FAILED,
    TIER3_VERIFICATION_FAILED,
    CRITICAL_FILE_CORRUPTION,
    BOOT_FAILURE,
    DEPENDENCY_MISSING,
    CHECKSUM_MISMATCH
}

/**
 * Rollback state information
 */
data class RollbackSnapshot(
    val timestamp: Long,
    val tierSnapshot: Map<TierLevel, TierSnapshot>,
    val reason: String,
    val lastStableTier: TierLevel
)

data class TierSnapshot(
    val tier: TierLevel,
    val mountedModules: List<String>,
    val verified: Boolean
)

/**
 * Fail-Safe Manager - triggers rescue operations
 */
class FailSafeManager(private val context: Context) {
    
    private val verificationEngine = TierVerificationEngine()
    private val moduleManager = TieredModuleManager()
    private val snapshotManager = SnapshotManager(context)

    /**
     * Trigger fail-safe mechanism
     * Executes rescue_sentry.sh and rolls back to last stable tier
     */
    suspend fun triggerFailSafe(event: FailSafeEvent, affectedTier: TierLevel) {
        // Record the failure
        recordFailureEvent(event, affectedTier)

        // Create snapshot for debugging
        val snapshot = snapshotManager.createSnapshot(affectedTier, event.toString())

        // Execute rescue_sentry.sh
        executeRescueSentry(affectedTier, snapshot)

        // Rollback to last stable state
        rollbackToLastStable(affectedTier)
    }

    /**
     * Find and rollback to last stable tier
     */
    private suspend fun rollbackToLastStable(failedTier: TierLevel) {
        // Unmount all tiers above failed one
        val tiersToUnmount = TierLevel.values()
            .filter { it.ordinal >= failedTier.ordinal }
        
        for (tier in tiersToUnmount.reversed()) {
            moduleManager.unmountAll()
        }

        // Retrieve last stable snapshot and restore
        val lastStableSnapshot = snapshotManager.getLastStableSnapshot(failedTier)
        if (lastStableSnapshot != null) {
            restoreFromSnapshot(lastStableSnapshot)
        }
    }

    /**
     * Restore system from a saved snapshot
     */
    private suspend fun restoreFromSnapshot(snapshot: RollbackSnapshot) {
        // Clear current verification state
        verificationEngine.clearVerifications()

        // Re-mount modules from snapshot in correct order
        for ((tier, tierSnapshot) in snapshot.tierSnapshot) {
            if (tierSnapshot.verified) {
                verificationEngine.verifyTier(tier)
                moduleManager.markTierVerified(tier)
            }
        }
    }

    /**
     * Execute rescue_sentry.sh with fail-safe parameters
     */
    private suspend fun executeRescueSentry(failedTier: TierLevel, snapshot: RollbackSnapshot) {
        val rescueScriptPath = "/data/susystem/scripts/rescue_sentry.sh"
        val rescueScript = File(rescueScriptPath)

        if (!rescueScript.exists()) {
            // Create emergency rescue script
            createEmergencyRescueScript(rescueScript)
        }

        try {
            // Execute with parameters
            val process = Runtime.getRuntime().exec(arrayOf(
                "sh",
                rescueScriptPath,
                "--tier=${failedTier.name}",
                "--snapshot=${snapshot.timestamp}",
                "--rollback"
            ))
            process.waitFor(30, TimeUnit.SECONDS)
        } catch (e: Exception) {
            // Log error but continue with manual rollback
        }
    }

    /**
     * Create emergency rescue script if missing
     */
    private fun createEmergencyRescueScript(script: File) {
        script.parent?.let { File(it).mkdirs() }
        
        val scriptContent = """
            #!/system/bin/sh
            # SukiTier Emergency Rescue Script
            
            TIER=${'$'}{1##*=}
            SNAPSHOT=${'$'}{2##*=}
            
            # Log rescue event
            echo "[SukiTier Rescue] Triggered for $TIER at $SNAPSHOT" >> /data/susystem/logs/rescue.log
            
            # Unmount all problematic modules
            for module in /mnt/sumodules/*; do
                if [ -d "${'$'}module" ]; then
                    umount -l "${'$'}module" 2>/dev/null || true
                fi
            done
            
            # Restore previous tier state
            if [ -f "/data/susystem/snapshots/$SNAPSHOT/state.bin" ]; then
                # Restore from backup
                cp /data/susystem/snapshots/$SNAPSHOT/state.bin /data/susystem/state.bin
            fi
            
            echo "Rollback complete" >> /data/susystem/logs/rescue.log
        """.trimIndent()

        script.writeText(scriptContent)
        script.setExecutable(true)
    }

    private fun recordFailureEvent(event: FailSafeEvent, tier: TierLevel) {
        // Write to persistent log
        val logFile = File("/data/susystem/logs/failsafe.log")
        logFile.parentFile?.mkdirs()
        logFile.appendText("${System.currentTimeMillis()} - $event on $tier\n")
    }
}

/**
 * Snapshot management for rollback
 */
class SnapshotManager(private val context: Context) {
    
    private val snapshotDir = File("/data/susystem/snapshots")

    init {
        snapshotDir.mkdirs()
    }

    /**
     * Create a snapshot of current tier state
     */
    suspend fun createSnapshot(
        tier: TierLevel,
        reason: String
    ): RollbackSnapshot {
        val timestamp = System.currentTimeMillis()
        val snapshotPath = File(snapshotDir, timestamp.toString())
        snapshotPath.mkdirs()

        val tierSnapshots = mutableMapOf<TierLevel, TierSnapshot>()
        
        // Capture current tier states
        TierLevel.values().forEach { t ->
            val tierState = tierSnapshots[t]?.let {
                TierSnapshot(
                    tier = t,
                    mountedModules = tierState?.getMountedModules() ?: emptyList(),
                    verified = tierState?.isVerified ?: false
                )
            }
            tierState?.let { tierSnapshots[t] = it }
        }

        val snapshot = RollbackSnapshot(
            timestamp = timestamp,
            tierSnapshot = tierSnapshots,
            reason = reason,
            lastStableTier = tier
        )

        // Save snapshot metadata
        val metadataFile = File(snapshotPath, "metadata.txt")
        metadataFile.writeText(
            """
            timestamp=$timestamp
            tier=$tier
            reason=$reason
            created=${System.currentTimeMillis()}
            """.trimIndent()
        )

        return snapshot
    }

    /**
     * Get last stable snapshot for a tier
     */
    fun getLastStableSnapshot(tier: TierLevel): RollbackSnapshot? {
        if (!snapshotDir.exists()) return null

        return snapshotDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.name.toLongOrNull() ?: 0 }
            ?.firstNotNullOfOrNull { snapshotPath ->
                val metadataFile = File(snapshotPath, "metadata.txt")
                if (metadataFile.exists()) {
                    parseSnapshot(snapshotPath)
                } else {
                    null
                }
            }
    }

    /**
     * List all available snapshots
     */
    fun listSnapshots(): List<RollbackSnapshot> {
        if (!snapshotDir.exists()) return emptyList()

        return snapshotDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { snapshotPath ->
                val metadataFile = File(snapshotPath, "metadata.txt")
                if (metadataFile.exists()) {
                    parseSnapshot(snapshotPath)
                } else {
                    null
                }
            }
            ?: emptyList()
    }

    /**
     * Clean old snapshots (keep last 5)
     */
    fun cleanOldSnapshots(keepCount: Int = 5) {
        snapshotDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.name.toLongOrNull() ?: 0 }
            ?.drop(keepCount)
            ?.forEach { it.deleteRecursively() }
    }

    private fun parseSnapshot(snapshotPath: File): RollbackSnapshot? {
        val metadataFile = File(snapshotPath, "metadata.txt")
        val metadata = metadataFile.readLines().associate {
            val parts = it.split("=", limit = 2)
            parts[0] to (parts.getOrNull(1) ?: "")
        }

        return RollbackSnapshot(
            timestamp = metadata["timestamp"]?.toLongOrNull() ?: 0,
            tierSnapshot = emptyMap(),
            reason = metadata["reason"] ?: "Unknown",
            lastStableTier = TierLevel.valueOf(metadata["tier"] ?: "TIER1_CORE")
        )
    }
}

/**
 * Rollback worker for async rollback operations
 */
class RollbackWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val failedTierName = inputData.getString("failed_tier") ?: "TIER1_CORE"
            val failedTier = TierLevel.valueOf(failedTierName)
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

/**
 * Schedule async rollback operation
 */
fun scheduleRollback(context: Context, failedTier: TierLevel) {
    val rollbackRequest = OneTimeWorkRequestBuilder<RollbackWorker>()
        .setInitialDelay(1, TimeUnit.SECONDS)
        .setInputData(
            androidx.work.Data.Builder()
                .putString("failed_tier", failedTier.name)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "rollback_${failedTier.name}",
        androidx.work.ExistingWorkPolicy.KEEP,
        rollbackRequest
    )
}
