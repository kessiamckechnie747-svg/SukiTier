package com.sukitier.ui.compose

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukitier.core.verification.DeviceVerificationResult
import com.sukitier.core.verification.DeviceSpecs

/**
 * Device Verification Pre-Flash UI Components
 */

@Composable
fun DeviceVerificationDialog(
    result: DeviceVerificationResult,
    onProceed: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (result.verified) Icons.Default.CheckCircle 
                                  else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (result.verified) Color(0xFF4CAF50) else Color(0xFFFFC107),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Device Verification",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Compatibility Score
                CompatibilityScoreDisplay(result.compatibilityScore)
                
                Divider()
                
                // Device Info
                DeviceInfoSection(result.deviceSpecs)
                
                // Failures/Warnings
                if (result.failures.isNotEmpty()) {
                    VerificationFailuresSection(result.failures)
                }
                
                if (result.warnings.isNotEmpty()) {
                    VerificationWarningsSection(result.warnings)
                }
                
                if (result.verified) {
                    VerificationSuccessSection()
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onProceed,
                enabled = result.verified,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (result.verified) Color(0xFF4CAF50) else Color.Gray
                )
            ) {
                Text(if (result.verified) "Proceed with Flash" else "Cannot Flash")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CompatibilityScoreDisplay(score: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = when {
                    score >= 90f -> Color(0xFFE8F5E9)
                    score >= 70f -> Color(0xFFFFF3E0)
                    else -> Color(0xFFFFEBEE)
                }
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Compatibility Score", fontSize = 12.sp, color = Color.Gray)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${score.toInt()}%",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    score >= 90f -> Color(0xFF4CAF50)
                    score >= 70f -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }
            )
        }
        
        // Progress bar
        LinearProgressIndicator(
            progress = score / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = when {
                score >= 90f -> Color(0xFF4CAF50)
                score >= 70f -> Color(0xFFFFC107)
                else -> Color(0xFFF44336)
            }
        )
    }
}

@Composable
fun DeviceInfoSection(specs: DeviceSpecs) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Device Information",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        
        DeviceInfoRow("Device Model", specs.deviceModel)
        DeviceInfoRow("Manufacturer", specs.manufacturer)
        DeviceInfoRow("Android", specs.androidVersion)
        DeviceInfoRow("Kernel", specs.kernelVersion)
        DeviceInfoRow("CPU Architecture", specs.cpuAbi)
        DeviceInfoRow("Partition Scheme", specs.partitionScheme)
        DeviceInfoRow("Bootloader", specs.bootloader)
    }
}

@Composable
fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFF5F5F5))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun VerificationFailuresSection(failures: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFEBEE))
            .border(1.dp, Color(0xFFFB8C8D), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = Color(0xFFF44336),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Verification Failures (${failures.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC62828)
            )
        }
        
        failures.forEach { failure ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 26.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    "•",
                    fontSize = 12.sp,
                    color = Color(0xFFF44336),
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    failure,
                    fontSize = 11.sp,
                    color = Color(0xFF5E3D3D),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun VerificationWarningsSection(warnings: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFF3E0))
            .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Warnings (${warnings.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100)
            )
        }
        
        warnings.forEach { warning ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 26.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    "•",
                    fontSize = 12.sp,
                    color = Color(0xFFFFC107),
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    warning,
                    fontSize = 11.sp,
                    color = Color(0xFF6D4C41),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun VerificationSuccessSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE8F5E9))
            .border(1.dp, Color(0xFF81C784), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
        )
        Text(
            "Device is compatible for patching",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
        Text(
            "All system requirements are met",
            fontSize = 11.sp,
            color = Color(0xFF558B2F)
        )
    }
}

/**
 * Device Verification Progress Indicator
 */
@Composable
fun DeviceVerificationProgress(
    isVerifying: Boolean,
    message: String = "Verifying device..."
) {
    AnimatedVisibility(
        visible = isVerifying,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F5F5))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
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
                message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }
    }
}

/**
 * Device Verification Status Chip
 */
@Composable
fun DeviceVerificationStatusChip(
    verified: Boolean,
    compatibilityScore: Float,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = {},
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (verified) Icons.Default.CheckCircle 
                                  else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    if (verified) "Verified (${compatibilityScore.toInt()}%)" 
                    else "Failed (${compatibilityScore.toInt()}%)",
                    fontSize = 12.sp
                )
            }
        },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (verified) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
            labelColor = if (verified) Color(0xFF2E7D32) else Color(0xFFC62828)
        )
    )
}

/**
 * Pre-Flash Verification Screen
 */
@Composable
fun PreFlashVerificationScreen(
    verificationResult: DeviceVerificationResult?,
    isVerifying: Boolean,
    onProceed: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isVerifying) {
            DeviceVerificationProgress()
        } else if (verificationResult != null) {
            DeviceVerificationDialog(
                result = verificationResult,
                onProceed = onProceed,
                onCancel = onCancel
            )
        }
    }
}
