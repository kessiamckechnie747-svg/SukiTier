package com.sukitier.core.failsafe

import android.util.Log
import com.sukitier.inference.DeterministicScoringEngine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * "Hybrid Marine Controller" SafetyManager
 * Implements "fuel rack lock" when malicious verdict detected
 * Chief Directive: Lock the drive motors until manual override confirmed
 */
class SafetyManager {
    
    companion object {
        private const val TAG = "HybridMarineController"
        private val instance = SafetyManager()
        
        fun getInstance(): SafetyManager = instance
    }
    
    // Fuel rack lock state (Marine metaphor)
    private var fuelRackLocked = false
    private val lockMutex = Mutex()
    
    // Override state (manual valve)
    private var manualOverrideActive = false
    
    // System authorization tier
    private var currentAuthorizationTier = -1
    
    // Safety state enumeration
    enum class SafetyState {
        OPERATIONAL,
        CAUTION,
        WARNING,
        CRITICAL,
        FUEL_RACK_LOCKED,  // Manual valve override required
        FAIL_SECURE
    }
    
    private var currentState = SafetyState.OPERATIONAL
    
    /**
     * Evaluate verdict and potentially lock fuel rack
     * Returns true if operations can proceed, false if locked
     */
    suspend fun evaluateVerdict(verdict: DeterministicScoringEngine.ScoringResult.Verdict): Boolean {
        return lockMutex.withLock {
            Log.i(TAG, "Evaluating verdict: $verdict")
            
            return@withLock when (verdict) {
                DeterministicScoringEngine.ScoringResult.Verdict.APPROVED_CRITICAL -> {
                    currentAuthorizationTier = 1
                    currentState = SafetyState.OPERATIONAL
                    Log.i(TAG, "APPROVED_CRITICAL - Authorization Tier 1")
                    true
                }
                
                DeterministicScoringEngine.ScoringResult.Verdict.APPROVED_STANDARD -> {
                    currentAuthorizationTier = 2
                    currentState = SafetyState.CAUTION
                    Log.i(TAG, "APPROVED_STANDARD - Authorization Tier 2 (CAUTION)")
                    true
                }
                
                DeterministicScoringEngine.ScoringResult.Verdict.APPROVED_EXPERIMENTAL -> {
                    currentAuthorizationTier = 3
                    currentState = SafetyState.WARNING
                    Log.w(TAG, "APPROVED_EXPERIMENTAL - Authorization Tier 3 (WARNING)")
                    true
                }
                
                DeterministicScoringEngine.ScoringResult.Verdict.REJECTED_UNSAFE -> {
                    currentState = SafetyState.CRITICAL
                    Log.e(TAG, "REJECTED_UNSAFE - Operations restricted")
                    false
                }
                
                DeterministicScoringEngine.ScoringResult.Verdict.REJECTED_MALICIOUS -> {
                    // CHIEF DIRECTIVE: Lock the fuel rack
                    lockFuelRack()
                    currentState = SafetyState.FUEL_RACK_LOCKED
                    Log.e(TAG, "REJECTED_MALICIOUS - FUEL RACK LOCKED. Manual override required.")
                    false
                }
            }
        }
    }
    
    /**
     * Lock fuel rack (engine kill switch)
     * Ceases all root-level write operations until manual override
     */
    private suspend fun lockFuelRack() {
        lockMutex.withLock {
            fuelRackLocked = true
            currentState = SafetyState.FUEL_RACK_LOCKED
            
            Log.e(TAG, "╔════════════════════════════════════════╗")
            Log.e(TAG, "║  HULL BREACH! - FUEL RACK LOCKED      ║")
            Log.e(TAG, "║  Drive motors disabled                ║")
            Log.e(TAG, "║  Fix your code, Captain!              ║")
            Log.e(TAG, "║  (Kernel panic simulation)            ║")
            Log.e(TAG, "╚════════════════════════════════════════╝")
            
            // Could trigger emergency protocol here
            // EmergencyProtocolHandler.enterSafeMode()
        }
    }
    
    /**
     * Manual override via Konami Code
     * Re-enables operations after malicious verdict
     * User explicitly accepts risk
     */
    suspend fun activateManualOverride(): Boolean {
        return lockMutex.withLock {
            if (fuelRackLocked) {
                manualOverrideActive = true
                Log.w(TAG, "╔════════════════════════════════════════╗")
                Log.w(TAG, "║  MANUAL OVERRIDE ACTIVATED             ║")
                Log.w(TAG, "║  Fuel rack disengaged                 ║")
                Log.w(TAG, "║  Proceed with extreme caution, Captain ║")
                Log.w(TAG, "╚════════════════════════════════════════╝")
                
                return@withLock true
            } else {
                Log.i(TAG, "Manual override requested but fuel rack not locked")
                return@withLock false
            }
        }
    }
    
    /**
     * Check if fuel rack is locked
     */
    suspend fun isFuelRackLocked(): Boolean {
        return lockMutex.withLock {
            fuelRackLocked
        }
    }
    
    /**
     * Check if manual override is active
     */
    suspend fun isManualOverrideActive(): Boolean {
        return lockMutex.withLock {
            manualOverrideActive
        }
    }
    
    /**
     * Get current authorization tier
     * Tier 1: Critical/Approved (Tier 0 hardware level)
     * Tier 2: Standard/Moderate Risk
     * Tier 3: Experimental/High Risk
     * -1: Not authorized
     */
    suspend fun getCurrentAuthorizationTier(): Int {
        return lockMutex.withLock {
            currentAuthorizationTier
        }
    }
    
    /**
     * Check if authorization meets minimum tier requirement
     */
    suspend fun hasAuthorization(requiredTier: Int): Boolean {
        return lockMutex.withLock {
            if (fuelRackLocked && !manualOverrideActive) {
                return@withLock false
            }
            
            return@withLock currentAuthorizationTier >= 1 && currentAuthorizationTier <= requiredTier
        }
    }
    
    /**
     * Get current safety state
     */
    suspend fun getCurrentState(): SafetyState {
        return lockMutex.withLock {
            currentState
        }
    }
    
    /**
     * Enter fail-secure mode (complete shutdown)
     */
    suspend fun enterFailSecureState() {
        lockMutex.withLock {
            currentState = SafetyState.FAIL_SECURE
            fuelRackLocked = true
            manualOverrideActive = false
            currentAuthorizationTier = -1
            
            Log.e(TAG, "ENTERING FAIL-SECURE STATE - All operations disabled")
        }
    }
    
    /**
     * Reset to operational state (requires explicit call)
     */
    suspend fun resetToOperational() {
        lockMutex.withLock {
            fuelRackLocked = false
            manualOverrideActive = false
            currentAuthorizationTier = -1
            currentState = SafetyState.OPERATIONAL
            
            Log.i(TAG, "Safety system reset to operational state")
        }
    }
    
    /**
     * Get status report for DCS display
     */
    suspend fun getStatusReport(): String {
        return lockMutex.withLock {
            buildString {
                append("=== MARINE CONTROLLER STATUS ===\n")
                append("State: $currentState\n")
                append("Fuel Rack: ${if (fuelRackLocked) "LOCKED" else "UNLOCKED"}\n")
                append("Manual Override: ${if (manualOverrideActive) "ACTIVE" else "INACTIVE"}\n")
                append("Authorization Tier: $currentAuthorizationTier\n")
                append("================================")
            }
        }
    }
}
