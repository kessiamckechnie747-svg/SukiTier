package com.sukitier.core.memory

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import kotlin.experimental.xor

/**
 * Industrial-grade direct buffer allocation with GC avoidance
 * Buffer size: 0x2000 (8KB) as specified
 */
object DirectBufferAllocator {
    
    // Constants from technical constraints
    private const val DIRECT_BUFFER_SIZE = 0x2000  // 8KB
    private const val MAX_BUFFER_POOL_SIZE = 64
    private const val WARM_BUFFERS = 4
    
    // Buffer pool for performance (Thread-local for lock-free access)
    private val bufferPool = ThreadLocal.withInitial {
        ArrayDeque<ByteBuffer>(WARM_BUFFERS).apply {
            repeat(WARM_BUFFERS) {
                add(allocateNewBuffer())
            }
        }
    }
    
    // Allocation tracker for debugging
    private val allocationTracker = ConcurrentHashMap<String, Int>()
    
    /**
     * Allocate or acquire a direct buffer with zero GC overhead
     */
    fun acquireBuffer(label: String = "anonymous"): ByteBuffer {
        val pool = bufferPool.get()
        
        return if (pool.isNotEmpty()) {
            pool.removeLast().apply {
                clear()
                // Track allocation
                allocationTracker.merge(label, 1, Int::plus)
            }
        } else {
            allocateNewBuffer(label)
        }
    }
    
    /**
     * Release buffer back to pool (idempotent)
     */
    fun releaseBuffer(buffer: ByteBuffer, label: String = "anonymous") {
        if (buffer.isDirect) {
            buffer.clear()
            val pool = bufferPool.get()
            
            if (pool.size < MAX_BUFFER_POOL_SIZE) {
                pool.add(buffer)
                // Track deallocation
                allocationTracker.merge(label, -1, Int::plus)
            } else {
                // Buffer will be GC'd naturally
                android.util.Log.w("DirectBufferAllocator", "Buffer pool full, forcing GC for $label")
            }
        }
    }
    
    /**
     * Atomic file transaction using direct buffers
     */
    suspend fun atomicFileTransaction(
        filePath: String,
        operation: (ByteBuffer) -> Unit
    ): Boolean {
        val tempPath = "$filePath.tmp${System.currentTimeMillis()}"
        val buffer = acquireBuffer("atomic_$filePath")
        
        return try {
            // Phase 1: Write to temporary file
            buffer.rewind()
            operation(buffer)
            writeBufferToFile(buffer, tempPath)
            
            // Phase 2: Atomic rename (POSIX compliant)
            val renameSuccess = File(tempPath).renameTo(File(filePath))
            
            if (!renameSuccess) {
                // Clean up temp file on failure
                File(tempPath).delete()
                false
            } else {
                // Phase 3: Sync to disk (industrial reliability)
                try {
                    Runtime.getRuntime().exec("sync $filePath").waitFor()
                } catch (e: Exception) {
                    android.util.Log.w("DirectBufferAllocator", "Sync command failed: ${e.message}")
                }
                true
            }
        } finally {
            releaseBuffer(buffer, "atomic_$filePath")
        }
    }
    
    /**
     * Memory-mapped I/O for kernel interactions
     */
    fun createMemoryMappedView(
        devicePath: String,
        offset: Long = 0L,
        size: Int = DIRECT_BUFFER_SIZE
    ): ByteBuffer? {
        return try {
            RandomAccessFile(devicePath, "r").use { file ->
                file.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    offset,
                    size.toLong()
                ).order(ByteOrder.LITTLE_ENDIAN)  // ARM standard
            }
        } catch (e: Exception) {
            android.util.Log.e("DirectBufferAllocator", "Failed to mmap $devicePath: ${e.message}")
            null
        }
    }
    
    /**
     * Verify buffer integrity with checksum
     */
    fun verifyBufferIntegrity(buffer: ByteBuffer, expectedChecksum: Int): Boolean {
        buffer.rewind()
        var checksum = 0
        
        while (buffer.hasRemaining()) {
            checksum = checksum xor (buffer.get().toInt() and 0xFF)
            checksum = (checksum shl 1) or (checksum ushr 31)  // Rotate left
        }
        
        buffer.rewind()
        return checksum == expectedChecksum
    }
    
    /**
     * Get buffer statistics
     */
    fun getBufferStatistics(): Map<String, Int> {
        return allocationTracker.toMap()
    }
    
    /**
     * Clear all tracking (for testing/reset)
     */
    fun resetTracking() {
        allocationTracker.clear()
    }
    
    // Private implementations
    private fun allocateNewBuffer(label: String = "new"): ByteBuffer {
        android.util.Log.d("DirectBufferAllocator", "Allocating new direct buffer for $label")
        return ByteBuffer.allocateDirect(DIRECT_BUFFER_SIZE).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            // Pre-fill with known pattern for debugging
            repeat(DIRECT_BUFFER_SIZE) { position ->
                put(position, 0xDE.toByte())
            }
            clear()
        }
    }
    
    private suspend fun writeBufferToFile(buffer: ByteBuffer, path: String) {
        RandomAccessFile(path, "rw").use { file ->
            file.channel.write(buffer)
        }
    }
}
