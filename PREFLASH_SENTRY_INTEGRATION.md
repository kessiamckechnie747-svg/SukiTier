# PreFlashSentry Integration Guide

## Quick Integration

### Step 1: Initialize in MainActivity

```kotlin
import com.sukitier.core.sentry.PreFlashSentry

class MainActivity : ComponentActivity() {
    private val sentry = PreFlashSentry()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SukiTierApp()
        }
    }
}
```

### Step 2: Add UI State Management

```kotlin
@Composable
fun SukiTierMainScreen() {
    var selectedImagePath by remember { mutableStateOf("") }
    var validationResult by remember { mutableStateOf<ImageHeader?>(null) }
    var validationError by remember { mutableStateOf<SafetyException?>(null) }
    var isValidating by remember { mutableStateOf(false) }
    
    val sentry = remember { PreFlashSentry() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Existing UI sections...
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Pre-Flash Sentry Section
        PreFlashSentryPanel(
            imagePath = selectedImagePath,
            onImageSelected = { selectedImagePath = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Validation Button
        Button(
            onClick = {
                if (selectedImagePath.isNotEmpty()) {
                    isValidating = true
                    try {
                        validationResult = sentry.validatePreFlash(selectedImagePath)
                        validationError = null
                    } catch (e: SafetyException) {
                        validationError = e
                        validationResult = null
                    } finally {
                        isValidating = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = selectedImagePath.isNotEmpty() && !isValidating,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3),
                disabledContainerColor = Color.Gray
            )
        ) {
            if (isValidating) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
            Text(if (isValidating) "VALIDATING..." else "RUN SENTRY CHECKS")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Results Display
        validationResult?.let { imageHeader ->
            ImageAnalysisResult(imageHeader)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            DeviceStateDisplay(getCurrentDeviceState())
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Safe to flash indicator
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                ),
                border = CardDefaults.outlinedCardBorder(
                    borderColor = Color(0xFF4CAF50)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Safe",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 12.dp)
                    )
                    Text(
                        text = "✓ Image is safe to flash. All checks passed.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
        }
        
        // Error Display
        validationError?.let { error ->
            SafetyExceptionAlert(error)
        }
    }
}

// Helper function to get current device state
@Composable
fun getCurrentDeviceState(): DeviceState {
    val bootloaderVerifier = remember { BootloaderVerifier() }
    
    return remember {
        DeviceState(
            sdkVersion = Build.VERSION.SDK_INT,
            buildVersion = Build.VERSION.RELEASE,
            fingerprint = Build.FINGERPRINT,
            bootloaderUnlocked = bootloaderVerifier.isBootloaderUnlocked(),
            verifiedBootState = bootloaderVerifier.getVerifiedBootState(),
            bootCompleted = bootloaderVerifier.isBootCompleted(),
            currentPatchLevel = getPatchLevel()
        )
    }
}

fun getPatchLevel(): String {
    return try {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getDeclaredMethod("get", String::class.java, String::class.java)
        method.invoke(null, "ro.build.version.security_patch", "unknown") as String
    } catch (e: Exception) {
        "unknown"
    }
}
```

### Step 3: Integrate with Flashing Logic

```kotlin
class FlashingManager(private val context: Context) {
    private val sentry = PreFlashSentry()
    
    suspend fun flashWithValidation(
        imagePath: String,
        onProgress: (String) -> Unit,
        onError: (SafetyException) -> Unit,
        onSuccess: (ImageHeader) -> Unit
    ) {
        try {
            // Step 1: Validate image before flashing
            onProgress("Running pre-flash sentry checks...")
            val imageHeader = sentry.validatePreFlash(imagePath)
            
            // Step 2: If validation passes, flash
            onProgress("Validation passed. Starting flash...")
            flashToDevice(imagePath, imageHeader)
            
            onProgress("Flash completed successfully!")
            onSuccess(imageHeader)
            
        } catch (e: SafetyException) {
            onProgress("Validation failed!")
            onError(e)
        }
    }
    
    private fun flashToDevice(imagePath: String, imageHeader: ImageHeader) {
        // Implement actual flashing logic
        val bootSlot = detectInactiveSlot()
        writeImageToSlot(imagePath, bootSlot)
        verifyFlash(bootSlot)
    }
}
```

### Step 4: Handle Specific Error Categories

```kotlin
@Composable
fun ErrorRecoveryGuide(exception: SafetyException) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (exception.category) {
            SafetyExceptionCategory.BOOTLOADER_LOCKED -> {
                BootloaderUnlockGuide(exception)
            }
            
            SafetyExceptionCategory.PATCH_LEVEL_DOWNGRADE -> {
                PatchDowngradeWarning(exception)
            }
            
            SafetyExceptionCategory.PRODUCT_MISMATCH -> {
                DeviceMismatchGuide(exception)
            }
            
            SafetyExceptionCategory.OS_VERSION_MISMATCH -> {
                VersionMismatchGuide(exception)
            }
            
            else -> {
                GenericErrorGuide(exception)
            }
        }
    }
}

@Composable
fun BootloaderUnlockGuide(exception: SafetyException) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "How to Unlock Bootloader",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            exception.getRecoverySteps().forEachIndexed { i, step ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${i + 1}.",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(text = step)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { /* Show fastboot terminal */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Fastboot Commands")
            }
        }
    }
}

@Composable
fun PatchDowngradeWarning(exception: SafetyException) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        ),
        border = CardDefaults.outlinedCardBorder(
            borderColor = Color(0xFFD32F2F)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Security Patch Downgrade Blocked",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFFD32F2F)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = exception.details["current_patch"] ?: "Current" +
                      " ➜ " +
                      (exception.details["image_patch"] ?: "Image"),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "This is a security restriction. You cannot downgrade your security patch level. " +
                      "Please use a newer image or the current patch level.",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun DeviceMismatchGuide(exception: SafetyException) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEDEDED)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Wrong Device Image",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Image For:", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        exception.imageHeader?.productName ?: "Unknown",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column {
                    Text("Your Device:", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        Build.PRODUCT,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
```

### Step 5: Save Validation Report

```kotlin
fun saveValidationReport(
    imageHeader: ImageHeader,
    deviceState: DeviceState,
    success: Boolean
) {
    try {
        val report = """
        === PRE-FLASH SENTRY REPORT ===
        Timestamp: ${System.currentTimeMillis()}
        Status: ${if (success) "PASSED" else "FAILED"}
        
        IMAGE HEADER:
        - OS Version: ${imageHeader.osVersion}
        - Patch Level: ${imageHeader.patchLevel}
        - Kernel Version: ${imageHeader.kernelVersion}
        - Product: ${imageHeader.productName}
        - Size: ${imageHeader.bootImageSize / (1024 * 1024)}MB
        
        DEVICE STATE:
        - SDK: ${deviceState.sdkVersion}
        - Build: ${deviceState.buildVersion}
        - Bootloader: ${if (deviceState.bootloaderUnlocked) "UNLOCKED" else "LOCKED"}
        - Verified Boot: ${deviceState.verifiedBootState}
        - Patch Level: ${deviceState.currentPatchLevel}
        """.trimIndent()
        
        val file = File("/data/susystem/logs/sentry_report_${System.currentTimeMillis()}.txt")
        file.parentFile?.mkdirs()
        file.writeText(report)
        
    } catch (e: Exception) {
        Log.e("Sentry", "Failed to save report", e)
    }
}
```

---

## Error Handling Strategy

### 1. Catch-All Pattern

```kotlin
try {
    sentry.validatePreFlash(imagePath)
    // Flash device
} catch (e: SafetyException) {
    // Categorized handling
}
```

### 2. Category-Based Flow Control

```kotlin
when (exception.category) {
    SafetyExceptionCategory.BOOTLOADER_LOCKED -> {
        // Direct user to fastboot unlock
    }
    SafetyExceptionCategory.PATCH_LEVEL_DOWNGRADE -> {
        // Prevent operation, show security notice
    }
    SafetyExceptionCategory.PRODUCT_MISMATCH -> {
        // Show device mismatch alert
    }
    else -> {
        // Show generic error with recovery steps
    }
}
```

### 3. Recovery Step Automation

```kotlin
exception.getRecoverySteps().forEach { step ->
    println(step)
    // Can be shown to user or automated
}
```

---

## Testing Integration

```kotlin
@RunWith(AndroidJUnit4::class)
class PreFlashSentryIntegrationTest {
    
    @Test
    fun testValidationIntegration() {
        val sentry = PreFlashSentry()
        
        // Test with real device state
        try {
            sentry.validatePreFlash("/sdcard/boot.img")
            // Should pass on compatible device
        } catch (e: SafetyException) {
            // Expected on incompatible device
        }
    }
}
```

---

## Production Considerations

1. **Always Run Pre-Flash Sentry** before any partition write
2. **Never Ignore Exceptions** - they are there for safety
3. **Show Recovery Steps** to users in clear, actionable format
4. **Log All Validations** for diagnostics
5. **Test on Real Devices** before deployment
