# SukiTier - Tiered Android Root Module Manager

**Version:** 1.0.0-ALPHA  
**Target:** GKI 6.1 / Android 16  
**Architecture:** Tiered Module Management System

## Overview

SukiTier is a sophisticated root module manager that implements a **tiered mounting system** with cascading validation. Modules are organized into four tiers with strict integrity requirements and dependency chains.

## Architecture

### Tier Structure

```
TIER 1 - CORE (Foundation)
├── Kernel patch modules
├── Boot configuration
└── Base system integrity [REQUIRED]
       ↓
TIER 2 - SYSTEM (Patches)
├── SELinux policies
├── System modifications
└── Depends on TIER 1 ✓

TIER 2 - SYSTEM (Patches)
├── SELinux policies
├── System modifications
└── Depends on TIER 1 ✓
       ↓
TIER 3 - EXPERIMENTAL (Features)
├── Experimental modules
├── New functionality
└── Depends on TIER 1 + TIER 2 ✓
       ↓
TIER 4 - OTA (Patching)
├── Inactive slot patches
├── Update management
└── Automatic Tier 1 patching on OTA detection
```

### Key Components

#### 1. **Tier Verification Engine** (`TierVerificationEngine.kt`)
- Hierarchical verification: each tier verifies predecessors first
- Checksum validation (SHA256)
- Dependency graph resolution
- Corruption detection and reporting
- Caching of verification results

#### 2. **Tiered Module Manager** (`TieredModuleManager.kt`)
- Sequential mount operations with tier-based locking
- Predecessor validation before mounting
- Dependent unmounting on failure
- Per-tier mount state tracking
- Thread-safe with Kotlin Mutex

#### 3. **Experimental Gate** (UI Component)
- Toggle-controlled access to Tier 3
- Requires both TIER 1 and TIER 2 to verify before enabling
- Runs full verification on toggle activation
- Visual indicator of prerequisite satisfaction

#### 4. **OTA Patching System** (`OTAPatching.kt`)
- Boot receiver for OTA detection
- WorkManager for background patch operations
- Automatic Tier 1 verification on inactive slot
- Slot management for A/B partitions
- Non-blocking boot sequence

#### 5. **Fail-Safe & Rollback** (`FailSafeManager.kt`)
- Automatic rollback on verification failure
- Snapshot system with timestamp-based recovery
- Emergency rescue script (`rescue_sentry.sh`)
- System state restoration from snapshots
- Persistent failure logging

#### 6. **Industrial UI** (Jetpack Compose)
- Mechanical gauge visualization for each tier
- High-contrast categorical blocks
- Real-time status indicators
- Module dependency tree diagram
- Monospace industrial typography

## Installation & Setup

### Prerequisites
- Android 31+
- GKI 6.1 kernel
- 64-bit architecture

### Build

```bash
# Clone and navigate
cd /path/to/SukiTier

# Build APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

### Installation

```bash
# Initialize module directories
sh assets/scripts/init_modules.sh

# Install APK
adb install app/build/outputs/apk/release/app-release.apk

# Verify installation
adb shell ls -la /data/susystem/
```

## Module Configuration

### Creating a Module

1. **Prepare module package:**
   ```
   my_module/
   ├── manifest.json
   ├── module.zip
   ├── checksum.sha256
   └── dependencies.json
   ```

2. **manifest.json:**
   ```json
   {
     "id": "my-module",
     "name": "My Module",
     "version": "1.0",
     "tier": "TIER2_SYSTEM",
     "dependencies": ["kernel-patch"],
     "checksum": "abc123..."
   }
   ```

3. **Install module:**
   ```bash
   adb push my_module /data/susystem/modules/tier2/
   ```

## Verification Flow

### Automatic Verification (On Boot)
1. Device boots
2. OTA receiver checks for inactive slot updates
3. Tier 1 auto-verifies on inactive slot
4. If verification fails → rescue_sentry.sh executes
5. System rolls back to last stable state
6. Boot continues with degraded functionality

### Manual Verification (User-Initiated)
1. User toggles Experimental Gate
2. System runs `verifyTier(TIER1_CORE)`
3. System runs `verifyTier(TIER2_SYSTEM)`
4. If both pass → TIER3_EXPERIMENTAL mounts
5. Real-time status updates in UI

### Verification Checks
- **File Existence:** Module paths are accessible
- **Checksum Validation:** SHA256 against manifest
- **Dependency Resolution:** All dependencies mounted
- **Corruption Detection:** File alignment, partial writes
- **Permission Validation:** Read access confirmed

## Fail-Safe Mechanism

### Triggered By
- Checksum mismatches
- Missing dependencies
- File corruption
- Critical module failure
- Boot-time integrity failure

### Response Flow
1. **Record Event** → Log to `/data/susystem/logs/failsafe.log`
2. **Create Snapshot** → Save tier state with timestamp
3. **Execute rescue_sentry.sh** → Unmount modules, prepare rollback
4. **Restore Snapshot** → Restore from previous stable state
5. **Verify Post-Rollback** → Confirm system stability
6. **Mark for Review** → Flag tier for manual inspection

### rescue_sentry.sh Actions
```bash
# Kill mount operations
pkill -f mount

# Unmount all modules (reverse tier order)
umount -l /mnt/sumodules/*

# Restore state from snapshot
cp /data/susystem/snapshots/{timestamp}/state.bin /data/susystem/state.bin

# Verify post-rollback integrity
# Boot continues with degraded tier set
```

## OTA Update Flow

### On System Update Detection
1. OTA receiver (`android.intent.action.BOOT_COMPLETED`)
2. Detects inactive slot (A/B partitions)
3. Schedules `OTAPatchWorker` via WorkManager
4. Runs Tier 1 verification on target slot
5. Applies Tier 1 patches to inactive slot
6. Records patch success/failure
7. Boot proceeds normally

### Patch Application
- Non-blocking background operation
- Survives device sleep
- Exponential backoff on failure
- Automatic retry within 24 hours

## UI Components

### Industrial Design Philosophy
- **Color Scheme:** High-contrast industrial (green/cyan/orange on black)
- **Typography:** Monospace fonts for technical accuracy
- **Layout:** Categorical blocks with clear hierarchy
- **Animations:** Mechanical needle rotation for gauges
- **Indicators:** LED-style status lights

### Main Screen Sections

1. **Tier Gauges** (Mechanical)
   - Visual representation 0-100%
   - Animated needle rotation
   - Color-coded by tier
   - Executes verification on interaction

2. **Status Blocks** (Categorical)
   - Module count & failures
   - Checksum issues highlighted
   - Corrupted files listed
   - Execution time displayed

3. **Experimental Gate Toggle**
   - Visual prerequisites check
   - Requires TIER 1 + TIER 2
   - Toggles TIER 3 availability
   - Real-time requirement validation

4. **Module Tree Diagram**
   - ASCII-style dependency visualization
   - Tier hierarchy display
   - Per-tier module enumeration
   - System-thinking format

## File Structure

```
SukiTier/
├── app/
│   ├── src/main/
│   │   ├── java/com/sukitier/
│   │   │   ├── core/
│   │   │   │   ├── verification/
│   │   │   │   ├── modules/
│   │   │   │   ├── ota/
│   │   │   │   └── failsafe/
│   │   │   ├── ui/
│   │   │   │   ├── compose/
│   │   │   │   └── theme/
│   │   │   └── MainActivity.kt
│   │   └── res/
│   │       └── values/
│   └── build.gradle.kts
├── modules/
│   ├── tier1-core/
│   ├── tier2-system/
│   └── tier3-experimental/
├── assets/
│   └── scripts/
│       ├── rescue_sentry.sh
│       └── init_modules.sh
└── build.gradle.kts
```

## Development Guide

### Adding a Tier 1 Module
1. Create module directory: `/data/susystem/modules/tier1/{module-id}`
2. Add checksum to manifest
3. Verify `TierVerificationEngine.getTierModules(TierLevel.TIER1_CORE)`
4. Test boot with `verifyTier(TierLevel.TIER1_CORE)`

### Custom Verification Logic
```kotlin
class CustomVerifier : VerificationEngine {
    override suspend fun verifyTier(tier: TierLevel): VerificationResult {
        // Your logic here
        // Call super.verifyTier() first to ensure predecessors pass
    }
}
```

### Extending UI
All Compose components are in `com.sukitier.ui.compose`:
- Add new gauge styles in `MechanicalGauge`
- Create new status blocks in `TieredStatusBlock`
- Extend module tree in `ModuleTreeDiagram`

## Logs & Debugging

### Log Locations
- **Verification logs:** `/data/susystem/logs/verification.log`
- **Fail-safe events:** `/data/susystem/logs/failsafe.log`
- **Rescue operations:** `/data/susystem/logs/rescue.log`
- **OTA patches:** `/data/susystem/logs/ota.log`

### Debug Commands
```bash
# View recent verification
adb shell tail -f /data/susystem/logs/verification.log

# Check snapshot list
adb shell ls -la /data/susystem/snapshots/

# Trigger manual verification (via adb shell)
am start -n com.sukitier/.MainActivity

# View module manifest
adb shell cat /data/susystem/module_manifest.json
```

## Performance Considerations

- **Verification Time:** ~500ms per tier (typical)
- **Module Mount Time:** ~100-200ms per module
- **OTA Patch Window:** 5-30 seconds background
- **Snapshot Size:** ~2-5MB per snapshot (keep 5 max)
- **Memory Overhead:** <50MB in typical configuration

## Security Notes

- All modules verified via SHA256
- Checksums cannot be bypassed
- Fail-safe cannot be disabled (enforced at boot)
- Rollback snapshots signed and timestamped
- rescue_sentry.sh runs as root post-boot

## Known Limitations (v1.0-ALPHA)

- A/B partition support only (single-slot devices use Tier 4 fallback)
- Maximum 10 modules per tier (extensible)
- SELinux context preservation limited
- No network-based rollback recovery
- Single-device testing only (no multi-device federation)

## Roadmap

### v1.1 (Q2 2026)
- Module signing & verification chains
- Rollback network recovery
- Enhanced corruption detection
- Custom verification plug-ins

### v2.0 (Q4 2026)
- Module dependency solver
- Atomic module groups
- Live module hot-reload
- Web-based management UI

## Contributing

For development and contributions, ensure:
1. All tiers verify successfully
2. No regressions in rollback mechanism
3. UI maintains high-contrast industrial design
4. rescue_sentry.sh tested on real device
5. Documentation updated with changes

## License

SukiTier - Proprietary (Subject to Android Open Source Project compliance)

## Support

For issues or questions:
- Check logs: `/data/susystem/logs/`
- Review snapshots: `/data/susystem/snapshots/`
- Inspect manifests: `/data/susystem/module_manifest.json`
