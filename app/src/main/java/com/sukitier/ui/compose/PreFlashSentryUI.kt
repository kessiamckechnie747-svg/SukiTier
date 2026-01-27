package com.sukitier.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukitier.core.sentry.*

/**
 * Pre-Flash Sentry UI Component
 * Displays image analysis, device state, and safety checks
 */

/**
 * Main sentry check panel
 */
@Composable
fun PreFlashSentryPanel(
    imagePath: String = "",
    onImageSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Sentry",
                tint = Color(0xFFFFC107),
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp)
            )
            Text(
                text = "PRE-FLASH SENTRY",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Safety verification before partition write",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Image path input
        OutlinedTextField(
            value = imagePath,
            onValueChange = onImageSelected,
            label = { Text("Boot Image Path") },
            placeholder = { Text("/path/to/boot.img") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Analysis button
        Button(
            onClick = { /* Analysis triggered */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            )
        ) {
            Text(
                text = "RUN SENTRY CHECKS",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Image analysis result display
 */
@Composable
fun ImageAnalysisResult(
    imageHeader: ImageHeader?,
    modifier: Modifier = Modifier
) {
    if (imageHeader == null) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFAFAFA)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Image Analyzed",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp)
                )
                Text(
                    text = "IMAGE HEADER ANALYSIS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info grid
            AnalysisInfoRow("OS Version", imageHeader.osVersion)
            AnalysisInfoRow("Patch Level", imageHeader.patchLevel)
            AnalysisInfoRow("Kernel Version", imageHeader.kernelVersion)
            AnalysisInfoRow("Build ID", imageHeader.buildId)
            AnalysisInfoRow("Product Name", imageHeader.productName)
            AnalysisInfoRow("Boot Image Size", "${imageHeader.bootImageSize / (1024 * 1024)}MB")
        }
    }
}

/**
 * Device state information
 */
@Composable
fun DeviceStateDisplay(
    deviceState: DeviceState?,
    modifier: Modifier = Modifier
) {
    if (deviceState == null) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFAFAFA)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Device State",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp)
                )
                Text(
                    text = "DEVICE STATE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Device info grid
            AnalysisInfoRow("SDK Version", deviceState.sdkVersion.toString())
            AnalysisInfoRow("Build Version", deviceState.buildVersion)
            AnalysisInfoRow("Fingerprint", deviceState.fingerprint)
            AnalysisInfoRow("Patch Level", deviceState.currentPatchLevel)
            
            Spacer(modifier = Modifier.height(8.dp))

            // Bootloader state
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Bootloader",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                BootloaderStatusChip(deviceState.bootloaderUnlocked)
            }

            // Boot state
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Verified Boot State",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = deviceState.verifiedBootState.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        deviceState.verifiedBootState.equals("green", ignoreCase = true) ->
                            Color(0xFF4CAF50)
                        deviceState.verifiedBootState.equals("orange", ignoreCase = true) ->
                            Color(0xFFFFC107)
                        else -> Color(0xFFD32F2F)
                    }
                )
            }
        }
    }
}

/**
 * Bootloader status indicator
 */
@Composable
fun BootloaderStatusChip(
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isUnlocked) Color(0xFFC8E6C9) else Color(0xFFFFCDD2)
    val borderColor = if (isUnlocked) Color(0xFF4CAF50) else Color(0xFFD32F2F)
    val textColor = if (isUnlocked) Color(0xFF2E7D32) else Color(0xFFC62828)
    val status = if (isUnlocked) "UNLOCKED" else "LOCKED"

    AssistChip(
        onClick = { },
        label = {
            Text(
                text = status,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = backgroundColor,
            labelColor = textColor
        ),
        border = AssistChipDefaults.assistChipBorder(
            borderColor = borderColor
        ),
        leadingIcon = {
            Icon(
                imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = status,
                modifier = Modifier.size(16.dp),
                tint = textColor
            )
        }
    )
}

/**
 * Safety exception alert display
 */
@Composable
fun SafetyExceptionAlert(
    exception: SafetyException?,
    modifier: Modifier = Modifier
) {
    if (exception == null) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        ),
        border = CardDefaults.outlinedCardBorder(
            borderColor = Color(0xFFD32F2F)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Error header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Safety Exception",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = "FLASH BLOCKED",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFFD32F2F)
                    )
                    Text(
                        text = exception.category.name.replace("_", " "),
                        fontSize = 12.sp,
                        color = Color(0xFFB71C1C)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Exception message
            Text(
                text = exception.message,
                fontSize = 13.sp,
                color = Color.Black,
                fontWeight = FontWeight.W500,
                modifier = Modifier.fillMaxWidth()
            )

            // Details section
            if (exception.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Details:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                        
                        exception.details.forEach { (key, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = key.replace("_", " ").uppercase(),
                                    fontSize = 11.sp,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.W600
                                )
                                Text(
                                    text = value,
                                    fontSize = 11.sp,
                                    color = Color.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Recovery steps
            Spacer(modifier = Modifier.height(12.dp))

            Column {
                Text(
                    text = "Recovery Steps:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Black
                )
                
                exception.getRecoverySteps().forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = step,
                            fontSize = 12.sp,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sentry check results summary
 */
@Composable
fun SentryChecksSummary(
    checksStatus: Map<String, Boolean> = emptyMap(),
    modifier: Modifier = Modifier
) {
    if (checksStatus.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFAFAFA)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Checks",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp)
                )
                Text(
                    text = "SAFETY CHECKS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Checks list
            checksStatus.forEach { (checkName, passed) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = checkName,
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )

                    val (icon, color, text) = if (passed) {
                        Triple(Icons.Default.CheckCircle, Color(0xFF4CAF50), "✓ PASS")
                    } else {
                        Triple(Icons.Default.Error, Color(0xFFD32F2F), "✗ FAIL")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = text,
                            tint = color,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp)
                        )
                        Text(
                            text = text,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper composable for info rows
 */
@Composable
private fun AnalysisInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.DarkGray
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = Color.Black,
            fontWeight = FontWeight.W500,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
