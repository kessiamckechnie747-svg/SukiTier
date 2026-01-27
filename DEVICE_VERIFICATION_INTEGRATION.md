# Device Verification Integration Guide

## Quick Start

### 1. OTA Patching with Device Verification

```kotlin
// In OTA boot receiver or patching service
val otaPatchEngine = OTAPatchEngine(context)

// Run pre-flash verification (automatic)
val success = otaPatchEngine.detectAndPatchInactiveSlot()
// Steps:
// 1. Queries device specs
// 2. Runs hook pipeline
// 3. Shows verification UI
// 4. Proceeds with patching if verified
// 5. Records result
```

### 2. Manual Device Verification

```kotlin
// Query device without patching
val deviceVerifier = DeviceVerifier(context)
val result = deviceVerifier.verifyDevice()

if (result.verified) {
    Log.d("Device", "✓ Compatible (${result.compatibilityScore}%)")
    result.deviceSpecs.apply {
        Log.d("Device", "Kernel: $kernelVersion")
        Log.d("Device", "Android: $androidVersion")
        Log.d("Device", "Model: $deviceModel")
    }
} else {
    Log.e("Device", "✗ Verification failed:")
    result.failures.forEach { Log.e("Device", "  - $it") }
}
```

### 3. Show Verification Dialog

```kotlin
// In Compose
@Composable
fun FlashScreen() {
    var result by remember { mutableStateOf<DeviceVerificationResult?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVerifying = true
        result = deviceVerifier.verifyDevice()
        isVerifying = false
    }
    
    if (result != null) {
        DeviceVerificationDialog(
            result = result!!,
            onProceed = { startFlashing() },
            onCancel = { abortFlashing() }
        )
    } else if (isVerifying) {
        DeviceVerificationProgress()
    }
}
```

### 4. Create Custom Hook

```kotlin
class NetworkConnectivityHook : DeviceVerificationHook {
    override suspend fun onPreFlash(
        context: Context,
        specs: DeviceSpecs
    ): DeviceVerificationResult {
        val failures = mutableListOf<String>()
        
        if (!hasNetworkConnection(context)) {
            failures.add("No network connection - cannot verify remote patches")
        }
        
        return DeviceVerificationResult(
            verified = failures.isEmpty(),
            deviceSpecs = specs,
            failures = failures
        )
    }
    
    override suspend fun onPostFlash(
        context: Context,
        result: DeviceVerificationResult
    ) = true
    
    override fun getName() = "NetworkConnectivity"
    override fun getPriority() = 90
}

// Register in OTA engine
otaPatchEngine.registerVerificationHook(NetworkConnectivityHook())
```

### 5. Access Verification Results

```kotlin
// Get last result
val lastResult = otaPatchEngine.getLastVerificationResult()
if (lastResult != null) {
    Log.d("OTA", "Last verification: ${lastResult.timestamp}")
    Log.d("OTA", "Device: ${lastResult.deviceSpecs.deviceModel}")
}

// Get device specs anytime
val specs = otaPatchEngine.getDeviceSpecs()
Log.d("Device", "Current kernel: ${specs.kernelVersion}")
```

---

## UI Implementation

### Complete Pre-Flash Screen

```kotlin
@Composable
fun OTAFlashScreen(
    onFlashComplete: () -> Unit
) {
    val deviceVerifier = remember { DeviceVerifier(LocalContext.current) }
    var result by remember { mutableStateOf<DeviceVerificationResult?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVerifying = true
        result = deviceVerifier.verifyDevice()
        isVerifying = false
    }
    
    if (isVerifying) {
        DeviceVerificationProgress(
            isVerifying = true,
            message = "Checking device compatibility..."
        )
    } else if (result != null) {
        PreFlashVerificationScreen(
            verificationResult = result,
            isVerifying = false,
            onProceed = {
                // Start flashing
                startOTAFlashing()
                onFlashComplete()
            },
            onCancel = {
                // Abort and return
                navigateBack()
            }
        )
    }
}
```

### Compact Status Display

```kotlin
@Composable
fun OTAStatusBar(result: DeviceVerificationResult?) {
    if (result != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeviceVerificationStatusChip(
                verified = result.verified,
                compatibilityScore = result.compatibilityScore
            )
            
            if (!result.verified) {
                Text(
                    "${result.failures.size} issue(s)",
                    fontSize = 12.sp,
                    color = Color.Red
                )
            }
        }
    }
}
```

### Device Info Display

```kotlin
@Composable
fun DeviceInfoPanel(specs: DeviceSpecs) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Device Information", fontWeight = FontWeight.Bold)
        
        InfoRow("Model", specs.deviceModel)
        InfoRow("Kernel", specs.kernelVersion)
        InfoRow("Android", specs.androidVersion)
        InfoRow("Architecture", specs.cpuAbi)
        InfoRow("Partition", specs.partitionScheme)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}
```

---

## Flow Diagrams

### Pre-Flash Verification Flow

```
User initiates OTA
    ↓
[isVerifying = true]
DeviceVerificationProgress shows
    ↓
QueryDeviceSpecs()
    ├─ System properties
    ├─ Build info
    └─ Kernel version
    ↓
RunHookPipeline()
    ├─ DefaultDeviceVerificationHook
    ├─ [Custom hooks if registered]
    └─ Collect results
    ↓
CalculateScore()
    ├─ Base: 100%
    ├─ -10% per warning
    ├─ 0% if failures
    └─ Min: 70% to proceed
    ↓
ShowDialog()
    ├─ Device info
    ├─ Compatibility score
    ├─ Failures (if any)
    └─ Proceed/Cancel buttons
    ↓
[User clicks Proceed]
    ↓
[Verified = true?]
    ├─ YES: Tier1Verification → Patching
    └─ NO: Abort, show error
    ↓
RecordResult()
SaveVerificationState()
```

### Hook Execution Order

```
Hook 1 (Priority: 100)
  onPreFlash() → Result 1
    ↓
[Result 1.verified?]
    ├─ NO: STOP, return failures
    └─ YES: continue
    ↓
Hook 2 (Priority: 80)
  onPreFlash() → Result 2
    ↓
[Result 2.verified?]
    ├─ NO: STOP, return failures
    └─ YES: continue
    ↓
Hook 3 (Priority: 50)
  onPreFlash() → Result 3
    ↓
Return [Result1, Result2, Result3]
```

---

## Data Flow

### Request: getDeviceSpecs()

```
getDeviceSpecs()
├─ SystemProperties.get("ro.build.version.release")
├─ Build.MODEL
├─ Build.MANUFACTURER
├─ Build.VERSION.RELEASE
├─ Build.SUPPORTED_ABIS[0]
├─ Build.BOOTLOADER
├─ Build.FINGERPRINT
└─ SystemProperties.get("ro.boot.slot_suffix")
    ↓
DeviceSpecs {
    kernelVersion,
    androidVersion,
    deviceModel,
    cpuAbi,
    bootloader,
    partitionScheme,
    manufacturer,
    buildFingerprint,
    timestamp
}
```

### Request: verifyDevice()

```
verifyDevice()
├─ getDeviceSpecs()
├─ Check: kernel contains "6.1"?
├─ Check: androidVersion >= 12?
├─ Check: cpuAbi contains "64"?
├─ Check: bootloader locked?
├─ Calculate compatibility score
└─ DeviceVerificationResult {
    verified: Boolean,
    deviceSpecs,
    compatibilityScore: Float,
    failures: List<String>,
    warnings: List<String>,
    timestamp
}
```

### Request: runPreFlashHooks()

```
runPreFlashHooks(context, specs)
├─ hookList.sortBy(-priority)
├─ for each hook:
│   ├─ hook.onPreFlash(context, specs)
│   ├─ [if !verified: BREAK]
│   └─ collect result
└─ return List<DeviceVerificationResult>
```

---

## Error Handling

### Common Failures

| Failure Message | Cause | Solution |
|---|---|---|
| Kernel version X.X is not GKI 6.1 | Wrong kernel | Update to GKI 6.1 kernel |
| Android X.X is below minimum (12+) | Old Android version | Upgrade device to Android 12+ |
| CPU ABI X is not 64-bit | 32-bit device | Use 64-bit compatible device |
| Bootloader is locked | Security feature | Unlock bootloader (advanced users) |

### Handling Failures in Code

```kotlin
val result = deviceVerifier.verifyDevice()

when {
    result.verified -> {
        // Proceed with flashing
        startFlashing()
    }
    result.failures.any { it.contains("Kernel") } -> {
        // Kernel issue - guide user to update
        showDialog("Update Kernel to GKI 6.1")
    }
    result.failures.any { it.contains("Android") } -> {
        // Android version issue - guide user to upgrade
        showDialog("Update Android to 12 or higher")
    }
    else -> {
        // Generic failure
        showDialog("Device is not compatible:\n" + 
            result.failures.joinToString("\n"))
    }
}
```

### Post-Failure Recovery

```kotlin
// If flashing fails despite verification:
val lastResult = otaPatchEngine.getLastVerificationResult()
Log.e("OTA", "Flashing failed. Last verification: $lastResult")

// Retry verification
val retryResult = otaPatchEngine.verifyDeviceForPatching()
if (!retryResult) {
    Log.e("OTA", "Device state changed - abort patching")
    triggerFailSafe()
}
```

---

## Testing Checklist

### Manual Testing

- [ ] Verify on GKI 6.1 device → Should pass
- [ ] Verify on wrong kernel → Should fail with message
- [ ] Verify on Android 11 → Should fail with message
- [ ] Verify on 32-bit device → Should fail with message
- [ ] Check UI dialog displays correctly
- [ ] Test Proceed button when verified
- [ ] Test Cancel button functionality
- [ ] Check compatibility score calculation
- [ ] Verify failure messages are clear

### Automated Testing

```bash
# Build and run tests
./gradlew testDebug

# Run device verification tests
./gradlew testDebug -k DeviceVerification

# Check UI rendering
./gradlew testDebugUiTest
```

---

## Performance Optimization

### Caching Results

```kotlin
// Cache last verification for 60 seconds
var cachedResult: DeviceVerificationResult? = null
var cacheTime = 0L

fun verifyDeviceCached(): DeviceVerificationResult {
    val now = System.currentTimeMillis()
    if (cachedResult != null && (now - cacheTime) < 60_000) {
        return cachedResult!!
    }
    cachedResult = runBlocking { deviceVerifier.verifyDevice() }
    cacheTime = now
    return cachedResult!!
}
```

### Async Verification

```kotlin
// Don't block UI
viewModelScope.launch {
    val result = withContext(Dispatchers.Default) {
        deviceVerifier.verifyDevice()
    }
    
    // Update UI on main thread
    uiState.value = uiState.value.copy(
        verificationResult = result
    )
}
```

---

## Logging & Debugging

### Enable Verbose Logging

```kotlin
// Set log level to DEBUG
Log.d("DeviceVerification", "Starting verification...")

// In manifest (logcat)
adb logcat com.sukitier.verification:D
```

### View Verification Results

```bash
# On device
adb shell cat /data/susystem/verification/last_verification.json

# In logcat
adb logcat | grep "DeviceVerification"
```

### Simulate Verification Failure

```kotlin
// For testing - mock device verifier
val mockVerifier = mockk<DeviceVerifier>()
every { mockVerifier.verifyDevice() } returns 
    DeviceVerificationResult(
        verified = false,
        deviceSpecs = testSpecs,
        failures = listOf("Test failure")
    )
```

---

## Production Deployment

### Before Release

1. ✅ Test on multiple devices
2. ✅ Test with locked bootloader
3. ✅ Verify error messages
4. ✅ Check compatibility scoring
5. ✅ Monitor logs for issues

### Release Checklist

```
[ ] Device verification hook system tested
[ ] UI components render correctly
[ ] OTA integration verified
[ ] Error handling complete
[ ] Logging configured
[ ] Documentation updated
[ ] Custom hooks working
[ ] Performance acceptable (<200ms total)
[ ] No blocking UI operations
[ ] All tests passing
```

### Post-Release Monitoring

```kotlin
// Track verification metrics
class VerificationMetrics {
    var totalVerifications = 0
    var successCount = 0
    var failureCount = 0
    var avgTimeMs = 0f
    
    fun recordVerification(result: DeviceVerificationResult, timeMs: Long) {
        totalVerifications++
        if (result.verified) successCount++ else failureCount++
        avgTimeMs = ((avgTimeMs * (totalVerifications - 1)) + timeMs) / totalVerifications
    }
}
```

---

## Summary

✅ **Device verification is fully integrated** with:
- Pre-flash compatibility checking
- Material3 UI components
- OTA patching pipeline
- Extensible hook system
- Comprehensive error handling

**Key Features:**
- Kernel, Android, CPU, bootloader validation
- Compatibility scoring (0-100%)
- Detailed failure/warning reporting
- Custom verification hooks
- Professional UI dialogs
- Result persistence
- Audit logging

**Ready for:**
- Device testing and verification
- Custom hook development
- Production deployment
- Performance monitoring
