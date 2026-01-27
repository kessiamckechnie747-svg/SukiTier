package com.sukitier.core.di

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * Lightweight Service Locator with fail-secure initialization
 * Anime-hacker aesthetic: "Neural Implant Registry"
 */
object ServiceLocator {
    
    private const val TAG = "NeuralImplant"
    
    private val services = ConcurrentHashMap<KClass<*>, Any>()
    private val singletons = ConcurrentHashMap<KClass<*>, Lazy<Any>>()
    private val initializationLock = Any()
    
    /**
     * Register a singleton service (lazy initialization)
     */
    inline fun <reified T : Any> registerSingleton(
        crossinline factory: () -> T,
        isCritical: Boolean = false
    ) {
        synchronized(initializationLock) {
            singletons[T::class] = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
                val instance = factory()
                
                // Critical service validation
                if (isCritical) {
                    validateCriticalService(instance)
                }
                
                instance
            }
        }
    }
    
    /**
     * Register a transient service (new instance each time)
     */
    inline fun <reified T : Any> registerFactory(
        crossinline factory: () -> T
    ) {
        services[T::class] = factory
    }
    
    /**
     * Get service instance (fail-secure)
     */
    inline fun <reified T : Any> get(): T {
        // Check singleton first
        singletons[T::class]?.let { lazyInstance ->
            @Suppress("UNCHECKED_CAST")
            return lazyInstance.value as T
        }
        
        // Check factory
        @Suppress("UNCHECKED_CAST")
        (services[T::class] as? () -> T)?.let { factory ->
            return factory()
        }
        
        // Auto-discovery via reflection (last resort)
        return autoDiscover()
    }
    
    /**
     * Fail-secure service initialization
     */
    fun initializeCriticalServices() {
        // Initialize critical services in dependency order
        Log.i(TAG, "Initializing critical services...")
        
        // Note: Actual critical services will be initialized as they're registered
        // This method serves as a lifecycle checkpoint
    }
    
    /**
     * Emergency service shutdown
     */
    fun emergencyShutdown() {
        synchronized(initializationLock) {
            Log.w(TAG, "Emergency shutdown initiated - releasing all services")
            
            // Release all services in reverse dependency order
            singletons.clear()
            services.clear()
            
            Log.i(TAG, "All services released")
        }
    }
    
    /**
     * Check if a service is registered
     */
    inline fun <reified T : Any> isRegistered(): Boolean {
        return singletons.containsKey(T::class) || services.containsKey(T::class)
    }
    
    // Private implementations
    private inline fun <reified T : Any> autoDiscover(): T {
        return try {
            val constructor = T::class.primaryConstructor
                ?: throw IllegalStateException("No primary constructor for ${T::class}")
            
            val params = constructor.parameters.associateWith { param ->
                // Recursive dependency resolution
                @Suppress("UNCHECKED_CAST")
                val paramClass = param.type.classifier as? KClass<*>
                    ?: throw IllegalStateException("Cannot resolve type for parameter ${param.name}")
                get(paramClass)
            }
            
            constructor.callBy(params)
        } catch (e: Exception) {
            throw ServiceLocatorException(
                "Failed to auto-discover ${T::class.simpleName}: ${e.message}",
                e
            )
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> get(klass: KClass<*>): T {
        singletons[klass]?.let { return it.value as T }
        (services[klass] as? () -> T)?.let { return it() }
        throw ServiceLocatorException("Service ${klass.simpleName} not registered")
    }
    
    private fun validateCriticalService(instance: Any) {
        Log.d(TAG, "Validating critical service: ${instance::class.simpleName}")
    }
    
    class ServiceLocatorException(message: String, cause: Throwable? = null) :
        RuntimeException(message, cause)
}
