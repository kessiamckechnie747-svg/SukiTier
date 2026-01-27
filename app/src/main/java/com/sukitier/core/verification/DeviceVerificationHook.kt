package com.sukitier.core.verification

import android.content.Context
import android.os.Build
import kotlinx.serialization.Serializable
import java.io.File

/**
 * Device Verification Hook System
 * Pre-flash verification during OTA patching
 */

enum class DeviceProperty {
    KERNEL_VERSION,      // GKI version
    ANDROID_VERSION,     // Android version
    DEVICE_MODEL,        // Device model
    CPU_ABI,             // Architecture
    BOOTLOADER_VERSION,  // Bootloader version
    PARTITION_SCHEME,    // A/B or single
    MANUFACTURER,        // Device manufacturer
    BUILD_FINGERPRINT    // Build fingerprint
}

@Serializable
data class DeviceSpecs(
    val kernelVersion: String,
    val androidVersion: String,
    val deviceModel: String,
    val cpuAbi: String,
    val bootloader: String,
    val partitionScheme: String,
    val manufacturer: String,
    val buildFingerprint: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class DeviceVerificationResult(
    val verified: Boolean,
    val deviceSpecs: DeviceSpecs,
    val compatibilityScore: Float = 0f,  // 0-100%
    val failures: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Device Verification Hook Interface
 * Allows custom verification plugins
 */
interface DeviceVerificationHook {
    suspend fun onPreFlash(context: Context, specs: DeviceSpecs): DeviceVerificationResult
    suspend fun onPostFlash(context: Context, result: DeviceVerificationResult): Boolean
    fun getName(): String
    fun getPriority(): Int  // Higher = runs first
}

/**
 * Device Verifier - Queries device properties
 */
class DeviceVerifier(private val context: Context) {
    
    /**
     * Get current device specifications
     */
    fun getDeviceSpecs(): DeviceSpecs {
        return DeviceSpecs(
            kernelVersion = getKernelVersion(),
            androidVersion = getAndroidVersion(),
            deviceModel = Build.MODEL,
            cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            bootloader = Build.BOOTLOADER,
            partitionScheme = getPartitionScheme(),
            manufacturer = Build.MANUFACTURER,
            buildFingerprint = Build.FINGERPRINT
        )
    }

    /**
     * Verify device is compatible for patching
     */
    suspend fun verifyDevice(): DeviceVerificationResult {
        val specs = getDeviceSpecs()
        val failures = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Check kernel version (GKI 6.1 required)
        if (!specs.kernelVersion.contains("6.1", ignoreCase = true)) {
            failures.add("Kernel version ${specs.kernelVersion} is not GKI 6.1")
        }
        
        // Check Android version (12+)
        val androidMajor = specs.androidVersion.split(".").firstOrNull()?.toIntOrNull() ?: 0
        if (androidMajor < 12) {
            failures.add("Android ${specs.androidVersion} is below minimum (12+)")
        }
        
        // Check CPU ABI (64-bit required)
        if (!specs.cpuAbi.contains("64", ignoreCase = true) && 
            !specs.cpuAbi.contains("arm64", ignoreCase = true)) {
            failures.add("CPU ABI ${specs.cpuAbi} is not 64-bit")
        }
        
        // Warn if bootloader is locked
        if (specs.bootloader.contains("locked", ignoreCase = true)) {
            warnings.add("Bootloader is locked - may prevent flashing")
        }
        
        val compatibilityScore = calculateCompatibilityScore(failures, warnings)
        
        return DeviceVerificationResult(
            verified = failures.isEmpty(),
            deviceSpecs = specs,
            compatibilityScore = compatibilityScore,
            failures = failures,
            warnings = warnings
        )
    }

    /**
     * Verify patch compatibility with device
     */
    fun verifyPatchCompatibility(
        patchTarget: String,
        currentSpecs: DeviceSpecs
    ): Boolean {
        // Parse patch target (e.g., "GKI-6.1-generic-arm64")
        return when {
            patchTarget.contains("GKI-6.1", ignoreCase = true) && 
            currentSpecs.kernelVersion.contains("6.1", ignoreCase = true) -> true
            
            patchTarget.contains("arm64", ignoreCase = true) && 
            currentSpecs.cpuAbi.contains("64", ignoreCase = true) -> true
            
            else -> false
        }
    }

    /**
     * Get kernel version from system properties
     */
    private fun getKernelVersion(): String {
        return try {
            android.os.SystemProperties.get("os.build.version.release", "unknown")
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Get Android version
     */
    private fun getAndroidVersion(): String {
        return "${Build.VERSION.RELEASE}.${Build.VERSION.SDK_INT}"
    }

    /**
     * Determine partition scheme (A/B or single)
     */
    private fun getPartitionScheme(): String {
        val slotSuffix = android.os.SystemProperties.get("ro.boot.slot_suffix", "")
        return if (slotSuffix.isNotEmpty()) "AB" else "Single"
    }

    /**
     * Calculate compatibility score (0-100%)
     */
    private fun calculateCompatibilityScore(
        failures: List<String>,
        warnings: List<String>
    ): Float {
        if (failures.isNotEmpty()) return 0f
        
        val scoreDeduction = warnings.size * 10f
        return maxOf(0f, 100f - scoreDeduction)
    }
}

/**
 * Device Verification Hook Manager
 */
class DeviceVerificationHookManager {
    
    private val hooks = mutableListOf<DeviceVerificationHook>()
    private val results = mutableListOf<DeviceVerificationResult>()

    /**
     * Register a verification hook
     */
    fun registerHook(hook: DeviceVerificationHook) {
        hooks.add(hook)
        hooks.sortByDescending { it.getPriority() }
    }

    /**
     * Run all verification hooks in priority order
     */
    suspend fun runPreFlashHooks(
        context: Context,
        specs: DeviceSpecs
    ): List<DeviceVerificationResult> {
        results.clear()
        
        for (hook in hooks) {
            try {
                val result = hook.onPreFlash(context, specs)
                results.add(result)
                
                // Stop on first failure
                if (!result.verified) break
            } catch (e: Exception) {
                // Log hook error
                results.add(DeviceVerificationResult(
                    verified = false,
                    deviceSpecs = specs,
                    failures = listOf("Hook ${hook.getName()} error: ${e.message}")
                ))
                break
            }
        }
        
        return results.toList()
    }

    /**
     * Get last verification result
     */
    fun getLastResult(): DeviceVerificationResult? = results.lastOrNull()

    /**
     * Get all results
     */
    fun getAllResults(): List<DeviceVerificationResult> = results.toList()
}

/**
 * Default implementation - required compatibility checks
 */
class DefaultDeviceVerificationHook : DeviceVerificationHook {
    
    override suspend fun onPreFlash(
        context: Context,
        specs: DeviceSpecs
    ): DeviceVerificationResult {
        val verifier = DeviceVerifier(context)
        return verifier.verifyDevice()
    }

    override suspend fun onPostFlash(
        context: Context,
        result: DeviceVerificationResult
    ): Boolean {
        // Post-flash validation
        return result.verified
    }

    override fun getName(): String = "DefaultDeviceVerification"

    override fun getPriority(): Int = 100
}

/**
 * Storage and retrieval of device verification state
 */
class DeviceVerificationState(context: Context) {
    
    private val verificationDir = File("/data/susystem/verification")
    
    init {
        verificationDir.mkdirs()
    }

    fun saveVerificationResult(result: DeviceVerificationResult) {
        val resultFile = File(verificationDir, "last_verification.json")
        try {
            // Save result (would use actual JSON serialization in production)
            resultFile.writeText(
                """
                {
                  "verified": ${result.verified},
                  "compatibility": ${result.compatibilityScore},
                  "failures": ${result.failures.size},
                  "timestamp": ${result.timestamp}
                }
                """.trimIndent()
            )
        } catch (e: Exception) {
            // Fail silently
        }
    }

    fun getLastVerificationResult(): File? {
        val resultFile = File(verificationDir, "last_verification.json")
        return if (resultFile.exists()) resultFile else null
    }

    fun clearVerificationHistory() {
        verificationDir.deleteRecursively()
        verificationDir.mkdirs()
    }
}
