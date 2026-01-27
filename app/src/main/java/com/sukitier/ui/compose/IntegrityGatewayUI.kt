package com.sukitier.ui.compose

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukitier.core.integrity.SafetyInterlockState
import com.sukitier.core.integrity.SafetyInterlockStatus

/**
 * High-Contrast Critical Warning Block
 * Displayed when device integrity check fails
 */
@Composable
fun IntegrityFailureWarning(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A0000))
            .border(2.dp, Color(0xFFFF0000), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Red flashing header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.WarningAmber,
                contentDescription = null,
                tint = Color(0xFFFF3333),
                modifier = Modifier.size(32.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "CRITICAL",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF0000),
                    fontFamily = FontFamily.Monospace
                )
                
                Text(
                    "Device Integrity Check Failed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF6666)
                )
            }
        }
        
        Divider(color = Color(0xFFFF3333), thickness = 1.dp)
        
        // Warning message
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Flashing may trigger Strong Integrity lockout.",
                fontSize = 11.sp,
                color = Color(0xFFFFAAAA),
                lineHeight = 14.sp
            )
            
            Text(
                "Verify pif.json and try again.",
                fontSize = 11.sp,
                color = Color(0xFFFFAAAA),
                lineHeight = 14.sp
            )
        }
        
        Divider(color = Color(0xFFFF3333), thickness = 1.dp)
        
        // Recovery instructions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF330000))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Recovery Steps:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF8888)
            )
            
            Text(
                "• Check device fingerprints and build info\n" +
                "• Ensure pif.json is valid for your device\n" +
                "• Verify bootloader is unlocked\n" +
                "• Clear Play Services cache and retry",
                fontSize = 9.sp,
                color = Color(0xFFFFBBBB),
                fontFamily = FontFamily.Monospace,
                lineHeight = 12.sp
            )
        }
    }
}

/**
 * Safety Interlock Status Display
 * Shows current state of integrity gate
 */
@Composable
fun SafetyInterlockDisplay(
    status: SafetyInterlockStatus,
    modifier: Modifier = Modifier
) {
    val stateColor = when (status.state) {
        SafetyInterlockState.IDLE -> Color(0xFF4A4A4A)
        SafetyInterlockState.CHECKING_GOOGLE_PLAY -> Color(0xFF2196F3)
        SafetyInterlockState.VERDICT_RECEIVED -> Color(0xFFFFC107)
        SafetyInterlockState.VERIFIED -> Color(0xFF4CAF50)
        SafetyInterlockState.FAILED -> Color(0xFFFF0000)
        SafetyInterlockState.RECOVERABLE_ERROR -> Color(0xFFFFC107)
        SafetyInterlockState.CRITICAL_ERROR -> Color(0xFFD32F2F)
    }
    
    val stateIcon = when (status.state) {
        SafetyInterlockState.IDLE -> Icons.Default.Lock
        SafetyInterlockState.CHECKING_GOOGLE_PLAY -> Icons.Default.Sync
        SafetyInterlockState.VERDICT_RECEIVED -> Icons.Default.CheckCircle
        SafetyInterlockState.VERIFIED -> Icons.Default.Lock
        SafetyInterlockState.FAILED -> Icons.Default.WarningAmber
        SafetyInterlockState.RECOVERABLE_ERROR -> Icons.Default.WarningAmber
        SafetyInterlockState.CRITICAL_ERROR -> Icons.Default.Error
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.5.dp, stateColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status.state == SafetyInterlockState.CHECKING_GOOGLE_PLAY) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = stateColor
                )
            } else {
                Icon(
                    imageVector = stateIcon,
                    contentDescription = null,
                    tint = stateColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    status.state.toString().replace("_", " "),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = stateColor,
                    fontFamily = FontFamily.Monospace
                )
                
                Text(
                    status.message,
                    fontSize = 10.sp,
                    color = Color(0xFFBBBBBB)
                )
            }
            
            // Gate indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (status.gateOpen) Color(0xFF4CAF50) else Color(0xFFFF0000))
            )
        }
    }
}

/**
 * Integrity Audit Progress
 */
@Composable
fun IntegrityAuditProgress(
    state: SafetyInterlockState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state == SafetyInterlockState.CHECKING_GOOGLE_PLAY,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A))
                .border(1.dp, Color(0xFF2196F3), RoundedCornerShape(8.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp,
                color = Color(0xFF2196F3)
            )
            
            Text(
                "Gate Closed: Checking Google Play Reputation...",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64B5F6),
                textAlign = TextAlign.Center
            )
            
            // Progress steps
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0D0D0D))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IntegrityStepIndicator("01", "Patch boot.img", completed = true)
                IntegrityStepIndicator("02", "Integrity Audit", completed = false, active = true)
                IntegrityStepIndicator("03", "Verdict Received", completed = false)
                IntegrityStepIndicator("04", "Gate Opens", completed = false)
            }
        }
    }
}

@Composable
fun IntegrityStepIndicator(
    step: String,
    description: String,
    completed: Boolean = false,
    active: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when {
                        completed -> Color(0xFF4CAF50)
                        active -> Color(0xFF2196F3)
                        else -> Color(0xFF333333)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                completed -> Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                active -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 1.5.dp,
                    color = Color(0xFF64B5F6)
                )
                else -> Text(
                    step,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF999999)
                )
            }
        }
        
        Text(
            description,
            fontSize = 11.sp,
            color = when {
                completed -> Color(0xFF4CAF50)
                active -> Color(0xFF64B5F6)
                else -> Color(0xFF666666)
            },
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Flash Button with Integrity Gate
 * Enabled only when gate is open
 */
@Composable
fun IntegrityGatedFlashButton(
    gateOpen: Boolean,
    isFlashing: Boolean,
    onFlash: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onFlash,
            enabled = gateOpen && !isFlashing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (gateOpen) Color(0xFF4CAF50) else Color(0xFF666666),
                disabledContainerColor = Color(0xFF444444)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFlashing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                
                Text(
                    if (isFlashing) "FLASHING..." else "PROCEED TO FLASH",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        
        // Status text
        if (!gateOpen) {
            Text(
                "Run integrity audit before flashing",
                fontSize = 10.sp,
                color = Color(0xFFFF6666),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Complete Integrity Check Panel
 */
@Composable
fun IntegrityCheckPanel(
    status: SafetyInterlockStatus,
    onRunAudit: () -> Unit,
    onFlash: () -> Unit,
    isFlashing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            "DEVICE INTEGRITY GATE",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3),
            fontFamily = FontFamily.Monospace
        )
        
        // Interlock status
        SafetyInterlockDisplay(status)
        
        // Audit progress
        IntegrityAuditProgress(status.state)
        
        // Failure warning
        if (status.state == SafetyInterlockState.FAILED) {
            IntegrityFailureWarning()
        }
        
        // Run Audit button
        AnimatedVisibility(
            visible = status.state == SafetyInterlockState.IDLE || 
                      status.state == SafetyInterlockState.FAILED ||
                      status.state == SafetyInterlockState.RECOVERABLE_ERROR,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Button(
                onClick = onRunAudit,
                enabled = !isFlashing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "RUN INTEGRITY AUDIT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Flash button (gated by integrity)
        IntegrityGatedFlashButton(
            gateOpen = status.gateOpen,
            isFlashing = isFlashing,
            onFlash = onFlash
        )
    }
}

/**
 * Compact integrity status for minimal UI
 */
@Composable
fun IntegrityStatusIndicator(
    state: SafetyInterlockState,
    gateOpen: Boolean
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (gateOpen) Color(0xFF1B5E20) else Color(0xFF4D0000)
            )
            .border(
                1.dp,
                if (gateOpen) Color(0xFF4CAF50) else Color(0xFFFF0000),
                RoundedCornerShape(4.dp)
            )
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (gateOpen) Icons.Default.Lock else Icons.Default.WarningAmber,
            contentDescription = null,
            tint = if (gateOpen) Color(0xFF4CAF50) else Color(0xFFFF0000),
            modifier = Modifier.size(14.dp)
        )
        
        Text(
            if (gateOpen) "Gate Open" else "Gate Closed",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (gateOpen) Color(0xFF4CAF50) else Color(0xFFFF0000),
            fontFamily = FontFamily.Monospace
        )
    }
}
