package com.sukitier.kernel

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.lang.reflect.Method
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * "The Metal" - Low-level kernel interfacing
 * Direct hardware manipulation with safety checks
 */
object KernelInterface {
    
    private const val TAG = "TheMetal"
    
    // Magic Numbers (from Pixel 9 architecture)
    companion object {
        // Android Boot Image Magic
        const val ANDROID_BOOT_MAGIC = 0x414E44524F494321L  // "ANDROID!"
        const val ANDROID_BOOT_MAGIC_OFFSET = 0x000
        const val ANDROID_BOOT_MAGIC_SIZE = 8
        
        // Compression Headers
        const val LZ4_MAGIC = 0x184D2204L
        const val GZIP_MAGIC = 0x1F8B0800L
        const val ZLIB_MAGIC = 0x789C5E72L
        
        // Partition Magic
        const val EXT4_MAGIC = 0xEF53L
        const val F2FS_MAGIC = 0xF2F52010L
        
        // IOCTL Constants (Linux standard)
        const val BLKGETSIZE64 = 0x80081272L
        const val BLKBSZGET = 0x80081270L
        const val BLKBSZSET = 0x40081271L
        const val BLKFLSBUF = 0x1261L
        const val BLKROSET = 0x125EL
        const val BLKROGET = 0x125FL
        
        // Kernel Structure Offsets (Android 16+)
        const val PAGE_SIZE = 4096
        const val BOOT_HEADER_SIZE = 4096
    }
    
    /**
     * HiddenAPI 36 Reflection Bypass
     */
    object HiddenAPIBypass {
        
        private var systemPropertiesClass: Class<*>? = null
        private var getMethod: Method? = null
        
        /**
         * Get system property bypassing hiddenapi restrictions
         */
        fun getSystemProperty(key: String): String? {
            return try {
                // Method 1: Standard reflection (may be blocked)
                val value = tryStandardReflection(key)
                if (value != null) return value
                
                // Method 2: getprop shell command
                tryShellCommand(key)
            } catch (e: Exception) {
                Log.w(TAG, "HiddenAPI bypass failed for key '$key': ${e.message}")
                null
            }
        }
        
        /**
         * Standard reflection approach
         */
        private fun tryStandardReflection(key: String): String? {
            return try {
                if (systemPropertiesClass == null) {
                    systemPropertiesClass = Class.forName("android.os.SystemProperties")
                }
                
                if (getMethod == null) {
                    getMethod = systemPropertiesClass!!.getDeclaredMethod("get", String::class.java)
                    getMethod!!.isAccessible = true
                }
                
                getMethod!!.invoke(null, key) as? String
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * Shell command fallback
         */
        private fun tryShellCommand(key: String): String? {
            return try {
                val process = Runtime.getRuntime().exec("getprop $key")
                val reader = process.inputStream.bufferedReader()
                val result = reader.readLine()?.trim() ?: return null
                reader.close()
                
                if (result.isNotEmpty()) result else null
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Verify boot image magic numbers
     */
    fun verifyBootImage(imageBuffer: ByteBuffer): Boolean {
        imageBuffer.order(ByteOrder.LITTLE_ENDIAN)
        imageBuffer.position(ANDROID_BOOT_MAGIC_OFFSET)
        
        // Check Android boot magic
        val magic = imageBuffer.long
        if (magic != ANDROID_BOOT_MAGIC) {
            Log.e(TAG, "Invalid boot magic: 0x${magic.toString(16)}")
            return false
        }
        
        // Verify kernel size and addresses
        imageBuffer.position(0x0008)
        val kernelSize = imageBuffer.int
        val kernelAddr = imageBuffer.int
        
        if (kernelSize <= 0 || kernelSize > 0x10000000) {  // 256MB max
            Log.e(TAG, "Invalid kernel size: $kernelSize")
            return false
        }
        
        if (kernelAddr < 0x8000 || kernelAddr > 0xFFFFFFFFL) {
            Log.e(TAG, "Invalid kernel address: 0x${kernelAddr.toString(16)}")
            return false
        }
        
        // Check for known compression
        imageBuffer.position(ANDROID_BOOT_MAGIC_SIZE)
        val compressionMagic = imageBuffer.int.toLong() and 0xFFFFFFFFL
        
        return when (compressionMagic) {
            LZ4_MAGIC -> {
                Log.i(TAG, "LZ4 compression detected")
                true
            }
            GZIP_MAGIC -> {
                Log.i(TAG, "GZIP compression detected")
                true
            }
            0L -> {
                Log.i(TAG, "Uncompressed kernel")
                true
            }
            else -> {
                Log.w(TAG, "Unknown compression: 0x${compressionMagic.toString(16)}")
                false  // Unknown compression = unsafe
            }
        }
    }
    
    /**
     * Get partition size (generic ARM64/Android 16 support)
     */
    fun getPartitionSize(devicePath: String): Long {
        return try {
            val output = executeShellCommand("blockdev --getsize64 $devicePath")
            output.trim().toLongOrNull() ?: -1L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get partition size for $devicePath: ${e.message}")
            -1L
        }
    }
    
    /**
     * Find partition by name (generic approach)
     * Searches /dev/block/by-name/ or /dev/block/bootdevice/by-name/
     */
    fun findPartitionByName(partitionName: String): String? {
        return try {
            // Try modern path first
            var partFile = File("/dev/block/by-name/$partitionName")
            if (partFile.exists()) return partFile.absolutePath
            
            // Try bootdevice path
            partFile = File("/dev/block/bootdevice/by-name/$partitionName")
            if (partFile.exists()) return partFile.absolutePath
            
            // Fallback: search /dev/block/ recursively
            val devBlockDir = File("/dev/block")
            devBlockDir.listFiles()?.find { 
                it.name.contains(partitionName, ignoreCase = true)
            }?.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "Failed to find partition $partitionName: ${e.message}")
            null
        }
    }
    
    /**
     * Verify partition boundaries before dd
     */
    fun verifyPartitionBoundaries(
        sourceFile: String,
        targetDeviceOrPartition: String
    ): BoundaryCheckResult {
        val sourceSize = File(sourceFile).length()
        val targetDevice = if (targetDeviceOrPartition.startsWith("/dev/")) {
            targetDeviceOrPartition
        } else {
            findPartitionByName(targetDeviceOrPartition) ?: return BoundaryCheckResult(
                sourceSize = sourceSize,
                targetSize = 0,
                isValid = false,
                safetyMargin = 0.0,
                errorMessage = "Could not find partition: $targetDeviceOrPartition"
            )
        }
        
        val targetSize = getPartitionSize(targetDevice)
        
        return BoundaryCheckResult(
            sourceSize = sourceSize,
            targetSize = targetSize,
            isValid = sourceSize <= targetSize && targetSize > 0,
            safetyMargin = if (targetSize > 0) {
                (targetSize - sourceSize).toDouble() / targetSize * 100.0
            } else 0.0
        )
    }
    
    data class BoundaryCheckResult(
        val sourceSize: Long,
        val targetSize: Long,
        val isValid: Boolean,
        val safetyMargin: Double,  // Percentage
        val errorMessage: String = ""
    )
    
    class KernelInterfaceException(message: String, cause: Throwable? = null) :
        RuntimeException(message, cause)
    
    private fun executeShellCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val reader = process.inputStream.bufferedReader()
            val output = reader.use { it.readText() }
            process.waitFor()
            output
        } catch (e: Exception) {
            Log.e(TAG, "Shell command failed: $command - ${e.message}")
            ""
        }
    }
    
    init {
        Log.i(TAG, "KernelInterface initialized - Android 16+ support")
    }
}
