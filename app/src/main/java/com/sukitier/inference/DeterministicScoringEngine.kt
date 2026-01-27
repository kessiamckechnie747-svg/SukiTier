package com.sukitier.inference

import android.util.Log
import java.io.File
import java.util.regex.Pattern
import kotlin.math.log2

/**
 * "Sentient Logic" - Deterministic Heuristic Scoring Engine
 * Not ML-blackbox - fully transparent scoring
 */
class DeterministicScoringEngine {
    
    companion object {
        private const val TAG = "SentientLogic"
        
        // Scoring thresholds (Float32 as specified)
        const val SCORE_THRESHOLD_TIER_1 = 90.0f  // Critical System
        const val SCORE_THRESHOLD_TIER_2 = 65.0f  // Standard Mod
        const val SCORE_THRESHOLD_TIER_3 = 30.0f  // Experimental/Unsafe
        
        // Regex Pattern Weights
        private val REGEX_PATTERNS = listOf(
            PatternWeights(
                pattern = Pattern.compile(".*mount -o remount,rw /system.*"),
                weight = 45.0f,
                description = "Filesystem remount (rw)",
                category = "SYSTEM_MODIFICATION"
            ),
            PatternWeights(
                pattern = Pattern.compile(".*setenforce 0.*"),
                weight = 80.0f,
                description = "SELinux disable (IMMEDIATE FLAG)",
                category = "SECURITY_BYPASS"
            ),
            PatternWeights(
                pattern = Pattern.compile(".*insmod .*\\.ko.*"),
                weight = 50.0f,
                description = "Kernel module insertion",
                category = "KERNEL_MODIFICATION"
            ),
            PatternWeights(
                pattern = Pattern.compile(".*chmod 777.*"),
                weight = 40.0f,
                description = "Permission escalation",
                category = "PRIVILEGE_ESCALATION"
            ),
            PatternWeights(
                pattern = Pattern.compile(".*dd if=.*of=/dev/block/.*"),
                weight = 70.0f,
                description = "Raw block device write",
                category = "BLOCK_DEVICE_ACCESS"
            ),
            PatternWeights(
                pattern = Pattern.compile(".*echo .* > /proc/sys/.*"),
                weight = 35.0f,
                description = "Kernel parameter modification",
                category = "KERNEL_TUNING"
            )
        )
    }
    
    data class PatternWeights(
        val pattern: Pattern,
        val weight: Float,
        val description: String,
        val category: String
    )
    
    data class ScoringResult(
        val totalScore: Float,
        val tier: Int,
        val matchedPatterns: List<MatchedPattern>,
        val entropyAnalysis: EntropyResult?,
        val verdict: Verdict
    ) {
        enum class Verdict {
            APPROVED_CRITICAL,
            APPROVED_STANDARD,
            APPROVED_EXPERIMENTAL,
            REJECTED_UNSAFE,
            REJECTED_MALICIOUS
        }
    }
    
    data class MatchedPattern(
        val pattern: PatternWeights,
        val context: String,
        val lineNumber: Int
    )
    
    /**
     * Analyze script with deterministic heuristics
     */
    fun analyzeScript(scriptContent: String): ScoringResult {
        val matchedPatterns = mutableListOf<MatchedPattern>()
        var totalScore = 0.0f
        
        // Line-by-line analysis
        scriptContent.lines().forEachIndexed { lineNumber, line ->
            REGEX_PATTERNS.forEach { patternWeight ->
                if (patternWeight.pattern.matcher(line).find()) {
                    matchedPatterns.add(
                        MatchedPattern(patternWeight, line.trim(), lineNumber + 1)
                    )
                    totalScore += patternWeight.weight
                    
                    // Immediate flag check
                    if (patternWeight.weight >= 70.0f) {
                        Log.w(TAG, "IMMEDIATE FLAG: ${patternWeight.description} at line $lineNumber")
                    }
                }
            }
        }
        
        // Entropy analysis for binary files referenced in script
        val entropyResult = analyzeBinaryEntropy(scriptContent)
        
        // Determine tier
        val tier = when {
            totalScore >= SCORE_THRESHOLD_TIER_1 -> 1
            totalScore >= SCORE_THRESHOLD_TIER_2 -> 2
            totalScore >= SCORE_THRESHOLD_TIER_3 -> 3
            else -> -1  // Unsafe
        }
        
        // Final verdict
        val verdict = determineVerdict(totalScore, tier, entropyResult)
        
        Log.d(TAG, "Script analysis complete: score=$totalScore, tier=$tier, verdict=$verdict")
        
        return ScoringResult(
            totalScore = totalScore,
            tier = tier,
            matchedPatterns = matchedPatterns,
            entropyAnalysis = entropyResult,
            verdict = verdict
        )
    }
    
    /**
     * Entropy analysis for binary detection (Shannon Entropy)
     */
    private fun analyzeBinaryEntropy(scriptContent: String): EntropyResult? {
        // Extract potential binary file references
        val binaryRegex = Pattern.compile("""(?:\./|/)?([\w\-_]+\.(?:ko|so|bin|elf))""")
        val matcher = binaryRegex.matcher(scriptContent)
        val binaries = mutableListOf<String>()
        
        while (matcher.find()) {
            binaries.add(matcher.group(1))
        }
        
        if (binaries.isEmpty()) return null
        
        // Analyze each binary
        val entropyResults = binaries.mapNotNull { binaryName ->
            calculateFileEntropy(binaryName)?.let { entropy ->
                BinaryEntropy(binaryName, entropy, entropy > 7.8f)
            }
        }
        
        return EntropyResult(
            binaries = entropyResults,
            hasHighEntropy = entropyResults.any { it.isHighEntropy },
            averageEntropy = entropyResults.map { it.entropy }.average().toFloat()
        )
    }
    
    /**
     * Calculate Shannon Entropy of a file
     * H(x) = -Σ p(x) * log2(p(x))
     */
    private fun calculateFileEntropy(filePath: String): Float? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            
            val byteCounts = LongArray(256)
            var totalBytes = 0L
            
            // Count byte frequencies
            file.inputStream().buffered().use { stream ->
                var byte = stream.read()
                while (byte != -1) {
                    byteCounts[byte]++
                    totalBytes++
                    byte = stream.read()
                }
            }
            
            // Calculate entropy
            var entropy = 0.0f
            for (count in byteCounts) {
                if (count > 0) {
                    val probability = count.toFloat() / totalBytes
                    entropy -= probability * log2(probability)
                }
            }
            
            entropy
        } catch (e: Exception) {
            Log.w(TAG, "Failed to calculate entropy for $filePath: ${e.message}")
            null
        }
    }
    
    /**
     * Final verdict determination
     */
    private fun determineVerdict(
        score: Float,
        tier: Int,
        entropyResult: EntropyResult?
    ): ScoringResult.Verdict {
        return when {
            tier == 1 && (entropyResult?.hasHighEntropy != true) -> 
                ScoringResult.Verdict.APPROVED_CRITICAL
                
            tier == 2 && (entropyResult?.hasHighEntropy != true) -> 
                ScoringResult.Verdict.APPROVED_STANDARD
                
            tier == 3 && (entropyResult?.hasHighEntropy != true) -> 
                ScoringResult.Verdict.APPROVED_EXPERIMENTAL
                
            entropyResult?.hasHighEntropy == true -> 
                ScoringResult.Verdict.REJECTED_MALICIOUS  // Packed/obfuscated
                
            else -> 
                ScoringResult.Verdict.REJECTED_UNSAFE
        }
    }
    
    // Data classes
    data class BinaryEntropy(
        val fileName: String,
        val entropy: Float,
        val isHighEntropy: Boolean  // H(x) > 7.8
    )
    
    data class EntropyResult(
        val binaries: List<BinaryEntropy>,
        val hasHighEntropy: Boolean,
        val averageEntropy: Float
    )
}
