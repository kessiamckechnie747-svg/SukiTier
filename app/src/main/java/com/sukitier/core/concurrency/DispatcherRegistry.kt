package com.sukitier.core.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import android.util.Log

/**
 * "Neurolink" Concurrency Controller
 * Hard-scifi industrial with fail-secure supervision
 */
object DispatcherRegistry {
    
    private const val TAG = "Neurolink"
    
    // CPU Core detection with safety margin
    private val CPU_CORES = maxOf(1, Runtime.getRuntime().availableProcessors() - 1)
    
    // IO Dispatcher: 64 thread maximum for flash operations
    val IO_DISPATCHER: CoroutineDispatcher = Executors.newFixedThreadPool(
        minOf(64, CPU_CORES * 4),
        NamedThreadFactory("Suki-IO")
    ).asCoroutineDispatcher()
    
    // Compute Dispatcher: CPU-bound operations only
    val COMPUTE_DISPATCHER: CoroutineDispatcher = Executors.newFixedThreadPool(
        maxOf(1, CPU_CORES),
        NamedThreadFactory("Suki-Compute")
    ).asCoroutineDispatcher()
    
    // Main Dispatcher (UI-bound)
    val MAIN_DISPATCHER: CoroutineDispatcher = Dispatchers.Main.immediate
    
    // Emergency Recovery Dispatcher (Single-threaded, highest priority)
    val RECOVERY_DISPATCHER: CoroutineDispatcher = Executors.newSingleThreadExecutor(
        NamedThreadFactory("Suki-Recovery")
    ).asCoroutineDispatcher()
    
    // Timeout Constants (Industrial-grade)
    object Timeouts {
        const val VALIDATION_TIMEOUT_MS = 15000L      // 15 seconds
        const val FLASH_WRITE_TIMEOUT_MS = 300000L    // 5 minutes
        const val ATOMIC_OPERATION_TIMEOUT_MS = 5000L // 5 seconds
        const val HARDWARE_POLL_INTERVAL_MS = 250L    // 250ms
    }
    
    // Fail-Secure Exception Handler
    val FAIL_SECURE_HANDLER = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "Coroutine failure detected: ${exception.message}", exception)
        // Emergency state transition to safe mode
        // SafetyManager.enterFailSecureState() - Will be initialized after SafetyManager
    }
    
    // Root Supervisor Scope
    val ROOT_SCOPE = CoroutineScope(
        SupervisorJob() + MAIN_DISPATCHER + FAIL_SECURE_HANDLER
    )
    
    // Sub-scopes for different safety tiers
    val TIER0_SCOPE = CoroutineScope(
        SupervisorJob() + RECOVERY_DISPATCHER + FAIL_SECURE_HANDLER
    )
    
    val TIER1_SCOPE = CoroutineScope(
        SupervisorJob() + COMPUTE_DISPATCHER + FAIL_SECURE_HANDLER
    )
    
    val TIER2_SCOPE = CoroutineScope(
        SupervisorJob() + IO_DISPATCHER + FAIL_SECURE_HANDLER
    )
    
    init {
        Log.i(TAG, "Neurolink initialized: CPU_CORES=$CPU_CORES, IO_THREADS=${minOf(64, CPU_CORES * 4)}")
    }
    
    /**
     * Idempotent operation wrapper - ensures operation runs once only
     */
    class IdempotentOperation<T>(
        private val operationName: String,
        private val block: suspend () -> T
    ) {
        private val executionFlag = AtomicInteger(0)
        private val operationLock = Any()
        
        suspend fun execute(): Result<T> = synchronized(operationLock) {
            if (executionFlag.compareAndSet(0, 1)) {
                try {
                    Result.success(block())
                } catch (e: Exception) {
                    Result.failure(e)
                } finally {
                    executionFlag.set(2)
                }
            } else {
                Result.failure(IllegalStateException(
                    "Operation '$operationName' already executed (state: ${executionFlag.get()})"
                ))
            }
        }
        
        val isExecuted: Boolean get() = executionFlag.get() > 0
        val isCompleted: Boolean get() = executionFlag.get() == 2
    }
}

/**
 * Named thread factory with anime-hacker aesthetics
 */
class NamedThreadFactory(
    private val baseName: String,
    private val counter: AtomicInteger = AtomicInteger(1)
) : ThreadFactory {
    
    override fun newThread(runnable: Runnable): Thread {
        return Thread(runnable, "[${baseName}-${counter.getAndIncrement()}]").apply {
            priority = when {
                baseName.contains("Recovery") -> Thread.MAX_PRIORITY
                baseName.contains("Compute") -> Thread.NORM_PRIORITY + 1
                else -> Thread.NORM_PRIORITY
            }
            
            // Set uncaught exception handler for industrial reliability
            setUncaughtExceptionHandler { thread, exception ->
                Log.e("NamedThreadFactory", "Thread ${thread.name} crashed: ${exception.message}", exception)
                // Trigger emergency protocol (will be integrated with EmergencyProtocolHandler)
            }
        }
    }
}
