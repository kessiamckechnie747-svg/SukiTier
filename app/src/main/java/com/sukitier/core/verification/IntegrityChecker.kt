package com.sukitier.core.verification

import java.io.File
import java.security.MessageDigest
import kotlin.math.abs

/**
 * Advanced integrity checking utilities
 */
object IntegrityChecker {

    /**
     * Compute SHA256 checksum of a file
     */
    fun computeSHA256(file: File): String {
        if (!file.exists()) return ""
        
        val digest = MessageDigest.getInstance("SHA-256")
        return file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * Compute MD5 checksum of a file
     */
    fun computeMD5(file: File): String {
        if (!file.exists()) return ""
        
        val digest = MessageDigest.getInstance("MD5")
        return file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * Verify file integrity against expected checksum
     */
    fun verifyIntegrity(file: File, expectedChecksum: String, algorithm: String = "SHA-256"): Boolean {
        val computed = when (algorithm.uppercase()) {
            "SHA-256" -> computeSHA256(file)
            "MD5" -> computeMD5(file)
            else -> return false
        }
        return computed.equals(expectedChecksum, ignoreCase = true)
    }

    /**
     * Detect file corruption patterns
     */
    fun detectCorruption(file: File): CorruptionReport {
        val report = CorruptionReport(filePath = file.absolutePath)

        if (!file.exists()) {
            report.addIssue("File not found", CorruptionType.MISSING)
            return report
        }

        // Check file permissions
        if (!file.canRead()) {
            report.addIssue("Cannot read file (permission denied)", CorruptionType.PERMISSION)
        }

        // Check file size anomalies
        if (file.isFile && file.length() == 0L) {
            report.addIssue("File is empty", CorruptionType.SIZE_ANOMALY)
        }

        // Check for partial writes (common corruption pattern)
        if (file.isFile && file.length() % 4096 != 0L) {
            report.addIssue("File size not 4K-aligned (possible partial write)", CorruptionType.ALIGNMENT)
        }

        return report
    }

    /**
     * Verify directory integrity
     */
    fun verifyDirectoryIntegrity(directory: File, manifest: Map<String, String>): DirectoryIntegrityReport {
        val report = DirectoryIntegrityReport(directory.absolutePath)

        if (!directory.isDirectory) {
            report.addError("Not a directory")
            return report
        }

        val files = directory.walkTopDown().filter { it.isFile }
        
        for (file in files) {
            val relativePath = file.relativeTo(directory).path
            val expectedChecksum = manifest[relativePath]

            if (expectedChecksum == null) {
                report.addUntracked(relativePath)
            } else if (!verifyIntegrity(file, expectedChecksum)) {
                report.addCorrupted(relativePath, computeSHA256(file))
            } else {
                report.addVerified(relativePath)
            }
        }

        return report
    }
}

enum class CorruptionType {
    MISSING,
    PERMISSION,
    SIZE_ANOMALY,
    ALIGNMENT,
    CHECKSUM_MISMATCH,
    UNKNOWN
}

data class CorruptionReport(
    val filePath: String,
    private val issues: MutableList<Pair<String, CorruptionType>> = mutableListOf()
) {
    val isCorrupted: Boolean get() = issues.isNotEmpty()

    fun addIssue(message: String, type: CorruptionType) {
        issues.add(message to type)
    }

    fun getIssues(): List<Pair<String, CorruptionType>> = issues.toList()
}

data class DirectoryIntegrityReport(
    val directory: String,
    private val verified: MutableList<String> = mutableListOf(),
    private val corrupted: MutableMap<String, String> = mutableMapOf(),
    private val untracked: MutableList<String> = mutableListOf(),
    private val errors: MutableList<String> = mutableListOf()
) {
    val integrityScore: Double
        get() {
            val total = verified.size + corrupted.size
            return if (total == 0) 100.0 else (verified.size.toDouble() / total.toDouble()) * 100.0
        }

    fun addVerified(path: String) = verified.add(path)
    fun addCorrupted(path: String, actualChecksum: String) = corrupted.put(path, actualChecksum)
    fun addUntracked(path: String) = untracked.add(path)
    fun addError(message: String) = errors.add(message)

    fun getVerifiedCount(): Int = verified.size
    fun getCorruptedCount(): Int = corrupted.size
    fun getUntrackedCount(): Int = untracked.size
    fun getErrors(): List<String> = errors.toList()
    fun getCorruptedFiles(): Map<String, String> = corrupted.toMap()
}

/**
 * Industrial Integrity Audit - Comprehensive tier verification
 * Checks corruption, kernel compatibility, and module integrity
 */
data class IntegrityAuditResult(
    val tier: Int,
    val passed: Boolean,
    val modulesChecked: Int = 0,
    val checksumFailures: List<String> = emptyList(),
    val vermagicMismatches: List<String> = emptyList(),
    val kernelVersion: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Industrial Integrity Audit Engine
 * GKI 6.1 kernel compatibility validation
 */
class IntegrityAuditor {
    
    companion object {
        const val REQUIRED_KERNEL_VERSION = "6.1"
        const val AUDIT_LOG_PATH = "/data/susystem/logs/integrity_audit.log"
    }

    private val auditLog = mutableListOf<IntegrityAuditResult>()

    /**
     * Run integrity audit on tier modules
     * Check 1: Corruption (Checksum validation)
     * Check 2: Vermagic (Kernel compatibility)
     * 
     * Returns true if all checks pass (bulkhead is watertight)
     */
    fun runIntegrityAudit(
        tier: Int,
        moduleRegistry: Map<String, ModuleInfo>
    ): IntegrityAuditResult {
        val startTime = System.currentTimeMillis()
        val modules = getModulesInTier(tier, moduleRegistry)
        
        val checksumFailures = mutableListOf<String>()
        val vermagicMismatches = mutableListOf<String>()
        
        for (module in modules) {
            // Check 1: Corruption (Checksum)
            if (!verifyChecksum(module)) {
                checksumFailures.add("${module.id}: Checksum mismatch")
                continue
            }
            
            // Check 2: Vermagic (Kernel Compatibility)
            val moduleVermagic = getModuleVermagic(module)
            if (!isKernelCompatible(moduleVermagic)) {
                vermagicMismatches.add(
                    "${module.id}: Expected GKI 6.1, got $moduleVermagic"
                )
                logError("Mismatched Hardware: Module ${module.id} is not 6.1 compatible.")
                continue
            }
        }

        val passed = checksumFailures.isEmpty() && vermagicMismatches.isEmpty()
        val result = IntegrityAuditResult(
            tier = tier,
            passed = passed,
            modulesChecked = modules.size,
            checksumFailures = checksumFailures,
            vermagicMismatches = vermagicMismatches,
            kernelVersion = REQUIRED_KERNEL_VERSION,
            details = if (passed) "Bulkhead is watertight ✓" else "Integrity failures detected",
            timestamp = System.currentTimeMillis()
        )
        
        auditLog.add(result)
        return result
    }

    /**
     * Get all modules in a tier
     */
    private fun getModulesInTier(tier: Int, moduleRegistry: Map<String, ModuleInfo>): List<ModuleInfo> {
        val tierLevel = when (tier) {
            1 -> TierLevel.TIER1_CORE
            2 -> TierLevel.TIER2_SYSTEM
            3 -> TierLevel.TIER3_EXPERIMENTAL
            4 -> TierLevel.TIER4_OTA
            else -> return emptyList()
        }
        return moduleRegistry.values.filter { it.tier == tierLevel }
    }

    /**
     * Verify module checksum integrity
     */
    private fun verifyChecksum(module: ModuleInfo): Boolean {
        val moduleFile = File("/data/susystem/modules/${module.id}")
        if (!moduleFile.exists()) return false
        
        val actualChecksum = computeSHA256(moduleFile)
        return actualChecksum.equals(module.checksum, ignoreCase = true)
    }

    /**
     * Get module vermagic string
     * Vermagic format: "6.1.0 GKI (kernel ABI version)"
     */
    private fun getModuleVermagic(module: ModuleInfo): String {
        val vermagicFile = File("/data/susystem/modules/${module.id}/vermagic.txt")
        return if (vermagicFile.exists()) {
            vermagicFile.readText().trim()
        } else {
            "unknown"
        }
    }

    /**
     * Check if vermagic is compatible with GKI 6.1
     */
    private fun isKernelCompatible(vermagic: String): Boolean {
        // Check for GKI 6.1 compatibility
        return vermagic.contains("6.1", ignoreCase = true) ||
               vermagic.contains("GKI", ignoreCase = true)
    }

    /**
     * Get current kernel version from system
     */
    fun getCurrentKernelVersion(): String {
        return try {
            android.os.SystemProperties.get("os.build.version.release", "unknown")
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Log error with timestamp
     */
    private fun logError(message: String) {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
            java.util.Date()
        )
        val logEntry = "[$timestamp] ERROR: $message"
        
        try {
            val logFile = File(AUDIT_LOG_PATH)
            logFile.parentFile?.mkdirs()
            logFile.appendText("$logEntry\n")
        } catch (e: Exception) {
            // Fail silently if logging unavailable
        }
    }

    /**
     * Get audit history
     */
    fun getAuditHistory(): List<IntegrityAuditResult> = auditLog.toList()

    /**
     * Clear audit logs
     */
    fun clearAuditLogs() = auditLog.clear()

    private fun computeSHA256(file: File): String {
        if (!file.exists()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        return file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
