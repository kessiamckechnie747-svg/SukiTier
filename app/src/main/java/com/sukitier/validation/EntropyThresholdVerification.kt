package com.sukitier.validation

import kotlin.math.log2

/**
 * Mathematical validation of the 7.8 entropy threshold
 * For detecting packed/obfuscated binaries
 */
object EntropyThresholdVerification {
    
    /**
     * Theoretical maximum entropy for 8-bit bytes:
     * H_max = -Σ p(x) * log2(p(x)) where p(x) = 1/256 for all x
     * H_max = -256 * (1/256 * log2(1/256))
     *       = -log2(1/256) 
     *       = log2(256)
     *       = 8 bits per byte
     */
    const val MAX_THEORETICAL_ENTROPY = 8.0f
    const val ENTROPY_THRESHOLD = 7.8f
    
    /**
     * Justification for 7.8 threshold:
     * - Normal compiled code: ~6.0-7.5
     * - Compressed data: ~7.8-7.95
     * - Encrypted/random: ~7.95-8.0
     * 
     * Threshold at 7.8 catches:
     * 1. UPX/MPress packed executables
     * 2. Encrypted malware payloads
     * 3. Obfuscated .ko modules
     */
    fun validateThreshold(sampleData: ByteArray): ValidationResult {
        val actualEntropy = calculateShannonEntropy(sampleData)
        val isHighEntropy = actualEntropy > ENTROPY_THRESHOLD
        
        return ValidationResult(
            actualEntropy = actualEntropy,
            isHighEntropy = isHighEntropy,
            distanceFromThreshold = actualEntropy - ENTROPY_THRESHOLD,
            confidence = calculateConfidence(actualEntropy)
        )
    }
    
    fun calculateShannonEntropy(data: ByteArray): Float {
        if (data.isEmpty()) return 0.0f
        
        val byteCounts = IntArray(256)
        data.forEach { byte ->
            byteCounts[byte.toInt() and 0xFF]++
        }
        
        var entropy = 0.0f
        val dataSize = data.size.toFloat()
        
        for (count in byteCounts) {
            if (count > 0) {
                val probability = count / dataSize
                entropy -= probability * log2(probability)
            }
        }
        
        return entropy
    }
    
    private fun calculateConfidence(entropy: Float): Float {
        return when {
            entropy >= 7.9f -> 0.95f  // Very likely packed
            entropy >= 7.8f -> 0.85f  // Likely packed
            entropy >= 7.5f -> 0.50f  // Possibly packed
            else -> 0.10f              // Unlikely packed
        }
    }
    
    data class ValidationResult(
        val actualEntropy: Float,
        val isHighEntropy: Boolean,
        val distanceFromThreshold: Float,
        val confidence: Float
    )
}
