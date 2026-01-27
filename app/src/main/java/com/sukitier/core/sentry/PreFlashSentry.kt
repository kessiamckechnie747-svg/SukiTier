package com.sukitier.core.sentry

import android.os.Build
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Image Header Information extracted from .img file
 */
data class ImageHeader(
    val osVersion: String,          // e.g., "16.0"
    val patchLevel: String,         // e.g., "2026-01"
    val kernelVersion: String,      // e.g., "6.1.25-android"
    val buildId: String,            // Build ID from image
    val timestamp: Long,            // Image creation time
    val productName: String,        // Device product name
    val bootImageSize: Long,        // Boot image size
    val rawData: ByteArray?         // Raw header bytes for verification
)

/**
 * Device State Information
 */
data class DeviceState(
    val sdkVersion: Int,            // Build.VERSION.SDK_INT
    val buildVersion: String,       // Build.VERSION.RELEASE
    val fingerprint: String,        // Build.FINGERPRINT
    val bootloaderUnlocked: Boolean,// Bootloader state
    val verifiedBootState: String,  // ro.boot.verifiedbootstate
    val bootCompleted: Boolean,     // sys.boot_completed
    val currentPatchLevel: String   // Current patch level
)

/**
 * Safety Exception Categories
 */
enum class SafetyExceptionCategory {
    BOOTLOADER_LOCKED,              // Bootloader not unlocked
    OS_VERSION_MISMATCH,            // Android version mismatch
    PATCH_LEVEL_DOWNGRADE,          // Trying to downgrade patch level
    PRODUCT_MISMATCH,               // Device product mismatch
    VERIFIED_BOOT_FAILURE,          // Verified boot state invalid
    KERNEL_VERSION_MISMATCH,        // Kernel version incompatible
    BUILD_FINGERPRINT_MISMATCH,     // Build fingerprint mismatch
    IMAGE_CORRUPTION,               // Header parsing failed
    INVALID_IMAGE_FORMAT,           // Image not recognized
    SYSTEM_STATE_INVALID            // Device in invalid state
}

/**
 * Safety Exception thrown when pre-flash checks fail
 */
class SafetyException(
    val category: SafetyExceptionCategory,
    message: String,
    val imageHeader: ImageHeader?,
    val deviceState: DeviceState?,
    val details: Map<String, String> = emptyMap()
) : Exception(message) {
    
    fun getRecoverySteps(): List<String> {
        return when (category) {
            SafetyExceptionCategory.BOOTLOADER_LOCKED -> listOf(
                "Unlock bootloader via fastboot: fastboot flashing unlock",
                "Confirm unlock on device screen",
                "Re-run pre-flash sentry check"
            )
            
            SafetyExceptionCategory.OS_VERSION_MISMATCH -> listOf(
                "Verify image matches device Android version",
                "Check device: Settings → About → Build number",
                "Ensure pif.json is updated for your device"
            )
            
            SafetyExceptionCategory.PATCH_LEVEL_DOWNGRADE -> listOf(
                "Cannot downgrade security patch level",
                "Use newer image or current patch level",
                "Check image release date"
            )
            
            SafetyExceptionCategory.PRODUCT_MISMATCH -> listOf(
                "Wrong image for this device",
                "Verify device model matches image product",
                "Use device-specific image: getprop ro.product.device"
            )
            
            SafetyExceptionCategory.VERIFIED_BOOT_FAILURE -> listOf(
                "Verified boot integrity check failed",
                "Clear Play Services cache: pm clear com.google.android.gms",
                "Ensure device is in developer mode"
            )
            
            SafetyExceptionCategory.KERNEL_VERSION_MISMATCH -> listOf(
                "Image kernel version incompatible",
                "Verify image is GKI 6.1+ compatible",
                "Check kernel version: uname -r"
            )
            
            SafetyExceptionCategory.BUILD_FINGERPRINT_MISMATCH -> listOf(
                "Build fingerprint does not match device",
                "Update pif.json with correct fingerprint",
                "Run: adb shell getprop ro.build.fingerprint"
            )
            
            SafetyExceptionCategory.IMAGE_CORRUPTION -> listOf(
                "Image header validation failed",
                "Verify image file integrity (checksums)",
                "Re-download image from official source"
            )
            
            SafetyExceptionCategory.INVALID_IMAGE_FORMAT -> listOf(
                "Image format not recognized",
                "Ensure file is valid Android boot.img",
                "Check file size > 4MB"
            )
            
            SafetyExceptionCategory.SYSTEM_STATE_INVALID -> listOf(
                "Device in invalid state for flashing",
                "Reboot device and try again",
                "Check device is not in recovery mode"
            )
        }
    }
}

/**
 * Image Header Analyzer
 * Extracts critical information from .img files
 */
class HeaderAnalyzer {
    
    companion object {
        // Android boot image magic
        private val BOOT_IMAGE_MAGIC = "ANDROID!".toByteArray()
        private const val BOOT_IMAGE_MAGIC_SIZE = 8
        private const val BOOT_IMAGE_HEADER_VERSION_ZERO = 0
        private const val BOOT_IMAGE_HEADER_VERSION_THREE = 3
        private const val BOOT_IMAGE_HEADER_VERSION_FOUR = 4
    }

    /**
     * Parse boot.img and extract header information
     */
    fun analyzeImage(imagePath: String): ImageHeader? {
        return try {
            val file = File(imagePath)
            if (!file.exists() || file.length() < 256) {
                return null
            }

            RandomAccessFile(file, "r").use { raf ->
                val headerBytes = ByteArray(256)
                raf.read(headerBytes)
                
                // Verify magic
                val magic = headerBytes.sliceArray(0 until BOOT_IMAGE_MAGIC_SIZE)
                if (!magic.contentEquals(BOOT_IMAGE_MAGIC)) {
                    return null
                }

                // Parse header based on version
                val buffer = ByteBuffer.wrap(headerBytes).apply {
                    order(ByteOrder.LITTLE_ENDIAN)
                }

                // Skip magic
                buffer.position(BOOT_IMAGE_MAGIC_SIZE)
                
                val kernelSize = buffer.int
                val kernelAddr = buffer.int
                val ramdiskSize = buffer.int
                val ramdiskAddr = buffer.int
                val secondSize = buffer.int
                val secondAddr = buffer.int
                val tagsAddr = buffer.int
                val pageSize = buffer.int
                val headerVersion = buffer.int
                val osVersion = buffer.int
                val osVersionByte = (osVersion shr 11) and 0xFF
                val osPatchLevel = osVersion and 0x7FF

                // Parse product name (from kernel command line, fallback extraction)
                val productName = extractProductName(imagePath)
                
                // Create version strings
                val osVersionString = "${6 + osVersionByte}.${(osVersion shr 4) and 0x7F}"
                val patchLevelString = formatPatchLevel(osPatchLevel)
                
                // Read kernel version from image
                val kernelVersion = extractKernelVersion(imagePath)

                ImageHeader(
                    osVersion = osVersionString,
                    patchLevel = patchLevelString,
                    kernelVersion = kernelVersion,
                    buildId = extractBuildId(imagePath),
                    timestamp = file.lastModified(),
                    productName = productName,
                    bootImageSize = file.length(),
                    rawData = headerBytes
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract product name from image
     */
    private fun extractProductName(imagePath: String): String {
        return try {
            RandomAccessFile(imagePath, "r").use { raf ->
                val buffer = ByteArray(1024)
                raf.seek(512)  // Typical location for product info
                raf.read(buffer)
                
                // Look for product= string
                val productStr = String(buffer, Charsets.UTF_8)
                val productMatch = Regex("product=([a-zA-Z0-9_-]+)").find(productStr)
                productMatch?.groupValues?.get(1) ?: "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Extract kernel version from image
     */
    private fun extractKernelVersion(imagePath: String): String {
        return try {
            RandomAccessFile(imagePath, "r").use { raf ->
                val buffer = ByteArray(2048)
                raf.seek(256)  // Kernel starts after header
                raf.read(buffer)
                
                // Look for kernel version string (Linux version X.X.X)
                val kernelStr = String(buffer, Charsets.UTF_8)
                val versionMatch = Regex("Linux version ([0-9.]+)").find(kernelStr)
                versionMatch?.groupValues?.get(1) ?: "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Extract build ID from image
     */
    private fun extractBuildId(imagePath: String): String {
        return try {
            RandomAccessFile(imagePath, "r").use { raf ->
                val buffer = ByteArray(512)
                raf.seek(1024)  // Build ID location
                raf.read(buffer)
                
                val buildStr = String(buffer, Charsets.UTF_8)
                val buildMatch = Regex("BUILD_ID=([a-zA-Z0-9.]+)").find(buildStr)
                buildMatch?.groupValues?.get(1) ?: "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Format patch level (year and month)
     */
    private fun formatPatchLevel(encodedLevel: Int): String {
        val year = 2000 + (encodedLevel shr 4)
        val month = encodedLevel and 0xF
        return String.format("%04d-%02d", year, month)
    }
}

/**
 * Bootloader Verifier using HiddenAPI
 */
class BootloaderVerifier {
    
    /**
     * Check if bootloader is unlocked using system properties
     */
    fun isBootloaderUnlocked(): Boolean {
        return try {
            // Method 1: Check ro.boot.verifiedbootstate
            val verifiedBootState = getSystemProperty("ro.boot.verifiedbootstate")
            val unlocked = verifiedBootState.equals("green", ignoreCase = true) ||
                           verifiedBootState.equals("orange", ignoreCase = true)
            
            // Also check sys.boot_completed
            val bootCompleted = getSystemProperty("sys.boot_completed")
            
            unlocked && bootCompleted.equals("1")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get verified boot state
     */
    fun getVerifiedBootState(): String {
        return try {
            getSystemProperty("ro.boot.verifiedbootstate")
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Check if system is fully booted
     */
    fun isBootCompleted(): Boolean {
        return try {
            getSystemProperty("sys.boot_completed").equals("1")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get system property using reflection (HiddenAPI)
     */
    private fun getSystemProperty(propName: String): String {
        return try {
            // Using Android's SystemProperties via reflection
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getDeclaredMethod("get", String::class.java, String::class.java)
            method.invoke(null, propName, "unknown") as String
        } catch (e: Exception) {
            "unknown"
        }
    }
}

/**
 * Pre-Flash Sentry - Comprehensive safety checks
 */
class PreFlashSentry {
    
    private val headerAnalyzer = HeaderAnalyzer()
    private val bootloaderVerifier = BootloaderVerifier()

    /**
     * Run comprehensive pre-flash safety checks
     * Throws SafetyException if any check fails
     */
    fun validatePreFlash(imagePath: String): ImageHeader {
        // Get current device state
        val deviceState = getCurrentDeviceState()
        
        // Analyze image header
        val imageHeader = headerAnalyzer.analyzeImage(imagePath)
            ?: throw SafetyException(
                category = SafetyExceptionCategory.INVALID_IMAGE_FORMAT,
                message = "Image format not recognized or file not found",
                imageHeader = null,
                deviceState = deviceState
            )

        // Check 1: Bootloader must be unlocked
        if (!deviceState.bootloaderUnlocked) {
            throw SafetyException(
                category = SafetyExceptionCategory.BOOTLOADER_LOCKED,
                message = "CRITICAL: Bootloader is locked. Cannot flash image.",
                imageHeader = imageHeader,
                deviceState = deviceState,
                details = mapOf(
                    "current_state" to deviceState.verifiedBootState,
                    "required_state" to "green or orange"
                )
            )
        }

        // Check 2: Verify boot completion
        if (!deviceState.bootCompleted) {
            throw SafetyException(
                category = SafetyExceptionCategory.SYSTEM_STATE_INVALID,
                message = "CRITICAL: System not fully booted. Reboot device before flashing.",
                imageHeader = imageHeader,
                deviceState = deviceState
            )
        }

        // Check 3: OS Version compatibility
        val imageOsVersion = imageHeader.osVersion.split(".")[0].toIntOrNull() ?: 0
        if (imageOsVersion != deviceState.sdkVersion && 
            imageOsVersion != deviceState.sdkVersion + 1) {  // Allow next version
            throw SafetyException(
                category = SafetyExceptionCategory.OS_VERSION_MISMATCH,
                message = "OS Version mismatch: Image is Android ${imageHeader.osVersion}, " +
                         "device is ${deviceState.buildVersion}",
                imageHeader = imageHeader,
                deviceState = deviceState,
                details = mapOf(
                    "image_version" to imageHeader.osVersion,
                    "device_version" to deviceState.buildVersion,
                    "device_sdk" to deviceState.sdkVersion.toString()
                )
            )
        }

        // Check 4: Patch Level validation (no downgrade)
        if (isDowngradeAttempt(imageHeader.patchLevel, deviceState.currentPatchLevel)) {
            throw SafetyException(
                category = SafetyExceptionCategory.PATCH_LEVEL_DOWNGRADE,
                message = "SECURITY: Cannot downgrade patch level from " +
                         "${deviceState.currentPatchLevel} to ${imageHeader.patchLevel}",
                imageHeader = imageHeader,
                deviceState = deviceState,
                details = mapOf(
                    "current_patch" to deviceState.currentPatchLevel,
                    "image_patch" to imageHeader.patchLevel
                )
            )
        }

        // Check 5: Product name compatibility
        val deviceProduct = Build.PRODUCT
        if (!imageHeader.productName.equals(deviceProduct, ignoreCase = true) &&
            imageHeader.productName != "unknown") {
            throw SafetyException(
                category = SafetyExceptionCategory.PRODUCT_MISMATCH,
                message = "DEVICE MISMATCH: Image is for ${imageHeader.productName}, " +
                         "device is $deviceProduct",
                imageHeader = imageHeader,
                deviceState = deviceState,
                details = mapOf(
                    "image_product" to imageHeader.productName,
                    "device_product" to deviceProduct
                )
            )
        }

        // Check 6: Verified boot state validation
        val bootState = deviceState.verifiedBootState
        if (!bootState.equals("green", ignoreCase = true) && 
            !bootState.equals("orange", ignoreCase = true)) {
            throw SafetyException(
                category = SafetyExceptionCategory.VERIFIED_BOOT_FAILURE,
                message = "VERIFIED BOOT FAILURE: Invalid boot state: $bootState",
                imageHeader = imageHeader,
                deviceState = deviceState,
                details = mapOf(
                    "boot_state" to bootState,
                    "expected" to "green or orange"
                )
            )
        }

        // Check 7: Kernel version compatibility
        if (!imageHeader.kernelVersion.contains("6.1", ignoreCase = true) &&
            imageHeader.kernelVersion != "unknown") {
            throw SafetyException(
                category = SafetyExceptionCategory.KERNEL_VERSION_MISMATCH,
                message = "KERNEL MISMATCH: Image requires kernel ${imageHeader.kernelVersion}, " +
                         "device needs GKI 6.1+",
                imageHeader = imageHeader,
                deviceState = deviceState,
                details = mapOf(
                    "image_kernel" to imageHeader.kernelVersion,
                    "required_kernel" to "6.1.x-android"
                )
            )
        }

        // Check 8: Image corruption check
        if (imageHeader.rawData == null || imageHeader.bootImageSize < 4 * 1024 * 1024) {
            throw SafetyException(
                category = SafetyExceptionCategory.IMAGE_CORRUPTION,
                message = "IMAGE CORRUPTION: Header validation failed or file too small",
                imageHeader = imageHeader,
                deviceState = deviceState,
                details = mapOf(
                    "boot_image_size" to "${imageHeader.bootImageSize / (1024 * 1024)}MB",
                    "minimum_size" to "4MB"
                )
            )
        }

        // All checks passed
        return imageHeader
    }

    /**
     * Check if flash is a downgrade attempt
     */
    private fun isDowngradeAttempt(imagePatch: String, currentPatch: String): Boolean {
        return try {
            val imageParts = imagePatch.split("-")
            val currentParts = currentPatch.split("-")
            
            if (imageParts.size != 2 || currentParts.size != 2) return false
            
            val imageYear = imageParts[0].toInt()
            val imageMonth = imageParts[1].toInt()
            
            val currentYear = currentParts[0].toInt()
            val currentMonth = currentParts[1].toInt()
            
            if (imageYear < currentYear) return true
            if (imageYear == currentYear && imageMonth < currentMonth) return true
            
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get current device state
     */
    private fun getCurrentDeviceState(): DeviceState {
        val bootloaderUnlocked = bootloaderVerifier.isBootloaderUnlocked()
        val verifiedBootState = bootloaderVerifier.getVerifiedBootState()
        val bootCompleted = bootloaderVerifier.isBootCompleted()

        return DeviceState(
            sdkVersion = Build.VERSION.SDK_INT,
            buildVersion = Build.VERSION.RELEASE,
            fingerprint = Build.FINGERPRINT,
            bootloaderUnlocked = bootloaderUnlocked,
            verifiedBootState = verifiedBootState,
            bootCompleted = bootCompleted,
            currentPatchLevel = getCurrentPatchLevel()
        )
    }

    /**
     * Get current system patch level
     */
    private fun getCurrentPatchLevel(): String {
        return try {
            // Try to get security patch level from system properties
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getDeclaredMethod("get", String::class.java, String::class.java)
            method.invoke(null, "ro.build.version.security_patch", "2024-01") as String
        } catch (e: Exception) {
            "unknown"
        }
    }
}
