package com.sukitier.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukitier.core.verification.TierLevel
import com.sukitier.core.verification.VerificationResult

/**
 * Industrial Mechanical Gauge Component
 * Displays verification progress with mechanical aesthetics
 */
@Composable
fun MechanicalGauge(
    value: Float,
    maxValue: Float = 100f,
    tierLevel: TierLevel,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val normalizedValue = (value / maxValue).coerceIn(0f, 1f)
    val rotation by animateFloatAsState(
        targetValue = normalizedValue * 270f - 135f,
        animationSpec = tween(1500)
    )
    
    val gaugeColor = getTierColor(tierLevel)
    val backgroundColor = Color(0xFF1a1a1a)

    Box(
        modifier = modifier
            .size(size)
            .background(color = backgroundColor, shape = CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Gauge background
        Box(
            modifier = Modifier
                .size(size * 0.9f)
                .background(color = Color(0xFF0a0a0a), shape = CircleShape)
        )

        // Needle
        Box(
            modifier = Modifier
                .size(size * 0.05f, size * 0.35f)
                .background(color = Color(0xFFFFAA00))
                .rotate(rotation)
                .align(Alignment.Center),
        )

        // Center cap
        Box(
            modifier = Modifier
                .size(size * 0.08f)
                .background(color = gaugeColor, shape = CircleShape)
                .align(Alignment.Center)
        )

        // Tier label
        Text(
            text = tierLevel.name.replace("_", " "),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Tiered Status Block - Categorical display for each tier
 */
@Composable
fun TieredStatusBlock(
    tier: TierLevel,
    verificationResult: VerificationResult?,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            verificationResult == null -> Color(0xFF2a2a2a)
            verificationResult.passed -> Color(0xFF1a3a1a)
            else -> Color(0xFF3a1a1a)
        }
    )

    val borderColor = getTierColor(tier)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = backgroundColor)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (verificationResult?.passed == true) Color(0xFF00FF00) else Color(0xFFFF0000)
                    )
            )
            
            Spacer(modifier = Modifier.size(8.dp))
            
            Text(
                text = "${tier.name} | ${if (verificationResult?.passed == true) "PASS" else "PENDING"}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stats
        verificationResult?.let {
            Text(
                text = "Modules: ${it.modulesChecked} | Failed: ${it.modulesFailed}",
                color = Color(0xFF888888),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            if (it.checksumMismatches.isNotEmpty()) {
                Text(
                    text = "⚠ Checksum issues: ${it.checksumMismatches.size}",
                    color = Color(0xFFFFAA00),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (it.corruptedFiles.isNotEmpty()) {
                Text(
                    text = "✗ Corrupted: ${it.corruptedFiles.take(2).joinToString(", ")}",
                    color = Color(0xFFFF0000),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = "Time: ${it.executionTimeMs}ms",
                color = Color(0xFF666666),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )
        } ?: run {
            Text(
                text = "Awaiting verification...",
                color = Color(0xFF888888),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Experimental Gate Toggle
 */
@Composable
fun ExperimentalGateToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    tier1Status: VerificationResult?,
    tier2Status: VerificationResult?,
    modifier: Modifier = Modifier,
    onVerification: (suspend () -> Unit)? = null
) {
    val canEnable = tier1Status?.passed == true && tier2Status?.passed == true

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Color(0xFF1a1a1a))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ EXPERIMENTAL GATE",
                color = Color(0xFFFFAA00),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(40.dp, 24.dp)
                    .background(
                        color = if (enabled && canEnable) Color(0xFF00FF00) else Color(0xFF333333),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (enabled && canEnable) "ON" else "OFF",
                    color = if (enabled && canEnable) Color.Black else Color(0xFF666666),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Requirements
        Text(
            text = "Requires: Tier 1 ✓ + Tier 2 ✓",
            color = if (canEnable) Color(0xFF00FF00) else Color(0xFFFF0000),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )

        if (!canEnable) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Status: Missing prerequisites",
                color = Color(0xFFFFAA00),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * System Thinking Diagram - Module Dependency Tree
 */
@Composable
fun ModuleTreeDiagram(
    modifier: Modifier = Modifier,
    modules: Map<TierLevel, List<String>> = emptyMap()
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Color(0xFF0a0a0a))
            .padding(16.dp)
    ) {
        Text(
            text = "MODULE HIERARCHY",
            color = Color(0xFF888888),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(12.dp))

        TierLevel.values().forEach { tier ->
            val tierModules = modules[tier] ?: emptyList()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "├─ ",
                    color = Color(0xFF666666),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Column {
                    Text(
                        text = tier.name,
                        color = getTierColor(tier),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    tierModules.forEach { module ->
                        Text(
                            text = "  └─ $module",
                            color = Color(0xFF999999),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

/**
 * Get tier-specific color
 */
fun getTierColor(tier: TierLevel): Color {
    return when (tier) {
        TierLevel.TIER1_CORE -> Color(0xFF00FF00) // Bright green
        TierLevel.TIER2_SYSTEM -> Color(0xFF00AAFF) // Bright cyan
        TierLevel.TIER3_EXPERIMENTAL -> Color(0xFFFFAA00) // Bright orange
        TierLevel.TIER4_OTA -> Color(0xFFFF6600) // Bright red-orange
    }
}
