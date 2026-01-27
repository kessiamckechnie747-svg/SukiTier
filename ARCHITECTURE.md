# SukiTier Architecture Deep Dive

## System Design

### 1. Tiered Verification System

```
┌─────────────────────────────────────────────┐
│          TIER 1: CORE FOUNDATION            │
│  ✓ Kernel patches  ✓ Boot modules          │
│  ✓ Base integrity  ✓ SHA256 checksums      │
│  Status: REQUIRED (always verify)          │
└──────────────────┬──────────────────────────┘
                   │
              ✓ PASS? ✓
                   │
                   ▼
┌─────────────────────────────────────────────┐
│      TIER 2: SYSTEM PATCHES & MODS          │
│  ✓ SELinux policies                         │
│  ✓ System modifications                     │
│  ✓ Depends on Tier 1                        │
└──────────────────┬──────────────────────────┘
                   │
          ✓ T1 VERIFIED?
                   │
                   ▼
┌─────────────────────────────────────────────┐
│   TIER 3: EXPERIMENTAL FEATURES             │
│  ✓ New functionality                        │
│  ✓ Advanced modules                         │
│  ✓ Gate-controlled (user toggle)            │
└──────────────────┬──────────────────────────┘
                   │
     ✓ T1 + T2 VERIFIED?
                   │
                   ▼
┌─────────────────────────────────────────────┐
│       TIER 4: OTA PATCHING (SILENT)         │
│  ✓ Inactive slot detection                  │
│  ✓ Automatic Tier 1 patching                │
│  ✓ Non-blocking background                  │
└─────────────────────────────────────────────┘
```

### 2. Module Mount Sequence

```
User Request (Toggle Tier 3)
    │
    ▼
Check Predecessors
    ├─ Tier 1 verified? → Check
    ├─ Tier 2 verified? → Check
    │
    ▼
Verify Tier 3
    ├─ Module checksums (SHA256)
    ├─ Dependency resolution
    ├─ Corruption detection
    │
    ▼
Mount Modules (Sequential)
    ├─ Acquire tier-level lock
    ├─ Mount each module
    ├─ Record mount state
    │
    ▼
Status Update UI
```

### 3. Fail-Safe Trigger & Rollback

```
Integrity Check Fails
    │
    ▼
Record Failure Event
    ├─ Write to failsafe.log
    ├─ Capture stack trace
    │
    ▼
Create Snapshot
    ├─ Tier states (mounted modules)
    ├─ Module manifests
    ├─ Checksum state
    │
    ▼
Execute rescue_sentry.sh
    ├─ Kill mount operations
    ├─ Unmount modules (reverse order)
    │
    ▼
Restore from Snapshot
    ├─ Restore state.bin
    ├─ Re-verify tiers
    │
    ▼
Boot with Degraded Tier
    └─ Continue with lower tiers
```

## Component Details

### TierVerificationEngine.kt

**Responsibilities:**
- Hierarchical tier verification
- Checksum computation and validation
- Dependency graph resolution
- Corruption detection
- Verification caching

**Key Methods:**
```kotlin
suspend fun verifyTier(tier: TierLevel): VerificationResult
fun verifyModuleChecksum(moduleId: String, expectedChecksum: String): Boolean
fun registerModule(module: ModuleInfo)
fun markTierFailed(tier: TierLevel, reason: String)
```

**Verification Algorithm:**
```
for tier in tierOrder:
    for predecessor in tier.predecessors():
        if not cached[predecessor].passed:
            verify(predecessor)
        if not cached[predecessor].passed:
            return FAILED
    
    for module in tier.modules():
        // Check file existence
        if not file.exists():
            failures += module
        
        // Check checksum
        if compute_sha256(file) != module.checksum:
            failures += module
        
        // Check dependencies
        for dep in module.dependencies:
            if dep.tier not verified:
                failures += module
    
    if failures.empty():
        result.passed = true
    cache[tier] = result
```

### TieredModuleManager.kt

**Responsibilities:**
- Module mounting with tier validation
- Per-tier mount state tracking
- Dependent unmounting on failure
- Thread-safe operations with Mutex

**Mount Flow:**
```
request: MountRequest
    ├─ Check predecessors verified
    ├─ Verify module exists
    ├─ Check permissions
    ├─ Mount module (Linux mount or bind)
    ├─ Update mount state
    └─ Record in tier state
```

### FailSafeManager.kt

**Responsibilities:**
- Trigger fail-safe on verification failure
- Create rollback snapshots
- Execute rescue_sentry.sh
- Manage snapshot lifecycle

**Snapshot Structure:**
```
/data/susystem/snapshots/{timestamp}/
├── metadata.txt        (timestamp, tier, reason)
├── state.bin          (tier states, mounted modules)
├── modules.json       (module manifests)
└── recovery.log       (recovery operations)
```

### OTA Patching System

**Responsibilities:**
- Boot receiver for OTA detection
- Background patch worker
- Inactive slot detection
- Automatic Tier 1 patching

**OTA Detection Path:**
```
Boot receiver
    ├─ Detect OTA on inactive slot
    ├─ Schedule OTAPatchWorker
    │
    OTAPatchWorker (background)
    ├─ Verify Tier 1 on target slot
    ├─ Apply Tier 1 patches
    ├─ Record success/failure
    │
    Boot continues
```

## Performance Metrics

### Verification Times
- **Tier 1:** ~250ms (3 modules)
- **Tier 2:** ~300ms (5 modules)
- **Tier 3:** ~150ms (2 modules)
- **Total:** ~700ms for all tiers

### Mount Times
- **Per module:** 50-100ms
- **All modules:** 200-500ms

### OTA Patch Window
- **Detection:** <100ms
- **Verification:** ~250ms
- **Patching:** 5-30 seconds
- **Total:** <30 seconds before boot

### Storage
- **APK size:** ~4-5MB
- **Snapshot (single):** ~2-5MB
- **Logs (all-time):** ~10MB
- **Module space:** 50-200MB

## Security Considerations

### Checksum Verification
- SHA256 for main verification
- MD5 fallback for compatibility
- Per-file integrity checks
- No checksums in plaintext paths

### Privilege Escalation Protection
- rescue_sentry.sh runs as root only
- Module loading enforces tier hierarchy
- No direct file system access from app
- SELinux context preservation

### Rollback Safety
- Snapshots timestamped and immutable
- Only system can initiate rollback
- Previous tier state verified before restore
- Failed rollback triggers manual review flag

## Integration Points

### System Integration
```
SukiTier ←→ SystemProperties (device info)
         ←→ WorkManager (background tasks)
         ←→ File system (/data/susystem)
         ←→ Mount subsystem (Linux mounts)
         ←→ OTA Update Service (update detection)
```

### UI Framework
```
Jetpack Compose
    ├─ Material3 components
    ├─ Custom gauge rendering
    ├─ Real-time status updates
    └─ Mechanical animations
```

## Extension Points

### Custom Verification
Implement custom tier verification:
```kotlin
class CustomTierVerifier(private val engine: TierVerificationEngine) {
    suspend fun customVerify(tier: TierLevel): VerificationResult {
        // Call parent first
        val result = engine.verifyTier(tier)
        if (!result.passed) return result
        
        // Add custom logic
        return VerificationResult(
            tier = tier,
            passed = true,
            // ...
        )
    }
}
```

### Custom UI Components
Create custom status displays:
```kotlin
@Composable
fun CustomTierDisplay(tier: TierLevel, result: VerificationResult) {
    // Your UI implementation
}
```

## Deployment Architecture

### Device Layout
```
/dev/block/mmcblk0p1  → Boot (Tier 4 patches)
/dev/block/mmcblk0p2  → System (Tier 1/2 modules)
/dev/block/mmcblk0p3  → Data (Module storage)
    └─ /data/susystem/
        ├─ modules/     (Tier 1/2/3 module files)
        ├─ patches/     (OTA patches)
        ├─ snapshots/   (Rollback snapshots)
        ├─ scripts/     (rescue_sentry.sh)
        └─ logs/        (Verification & failsafe logs)
```

### Installation Sequence
1. Install APK via adb
2. Run init_modules.sh to create directory structure
3. Copy module manifests to /data/susystem/modules
4. Boot device and verify Tier 1
5. Optional: Enable Tier 3 via experimental gate
6. OTA system auto-patches inactive slot

## Monitoring & Telemetry

### Logged Events
- **Verification start/completion**
- **Module mount/unmount**
- **Checksum validation results**
- **Fail-safe triggers**
- **Rollback operations**
- **OTA patch operations**

### Accessible Metrics
```bash
# View verification stats
cat /data/susystem/logs/verification.log | grep "COMPLETED"

# Check failsafe triggers
wc -l /data/susystem/logs/failsafe.log

# List available rollback snapshots
ls -la /data/susystem/snapshots/
```

## Troubleshooting Guide

### Issue: Tier 3 Won't Enable
**Solution:** Verify Tier 1 & 2 checksums
```bash
adb shell cat /data/susystem/logs/verification.log | tail -20
```

### Issue: Rollback Triggered
**Solution:** Review fail-safe log
```bash
adb shell cat /data/susystem/logs/failsafe.log
# Check last snapshot
adb shell ls -lat /data/susystem/snapshots/ | head -1
```

### Issue: OTA Patch Failure
**Solution:** Check OTA status
```bash
adb shell getprop ro.boot.slot_suffix
adb shell cat /data/susystem/logs/ota.log
```

---

For implementation details, see [README.md](README.md)
