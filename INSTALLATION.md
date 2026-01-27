# Installation & Setup Guide

## Prerequisites

- **Device:** Android 12+ (tested on Android 14+, target Android 16)
- **Kernel:** GKI 6.1 or later
- **Architecture:** 64-bit ARM (arm64-v8a)
- **Storage:** 500MB free space in `/data/`
- **Bootloader:** Unlocked for root access
- **ADB:** Android Debug Bridge installed

## Quick Start

### 1. Clone Repository
```bash
git clone https://github.com/yourusername/SukiTier.git
cd SukiTier
```

### 2. Build Project
```bash
# Using build script
chmod +x build.sh
./build.sh release

# Or using Gradle directly
./gradlew assembleRelease
```

### 3. Install APK
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### 4. Initialize Module System
```bash
adb shell mkdir -p /data/susystem
adb push assets/scripts/init_modules.sh /data/susystem/
adb shell sh /data/susystem/init_modules.sh
```

### 5. Verify Installation
```bash
adb shell ls -la /data/susystem/
# Should show: modules/, patches/, snapshots/, logs/, scripts/
```

## Detailed Installation

### Step 1: Environment Setup

#### On Development Machine
```bash
# Install Android SDK/NDK (if not already installed)
export ANDROID_SDK_ROOT=$HOME/Android/sdk
export ANDROID_NDK_ROOT=$HOME/Android/ndk/26.0.10792818

# Verify Gradle installation
./gradlew --version
# Should show: Gradle 8.2
```

#### On Target Device
```bash
adb shell pm grant com.sukitier android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.sukitier android.permission.WRITE_EXTERNAL_STORAGE
```

### Step 2: Build & Deploy

#### Debug Build (Development)
```bash
./gradlew assembleDebug

# Install to device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.sukitier/.MainActivity
```

#### Release Build (Production)
```bash
# Create release keystore (one-time)
keytool -genkey -v -keystore SukiTier.keystore \
  -alias sukitier \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000

# Build signed APK
./gradlew assembleRelease \
  -Pandroid.injected.signing.store.file=./SukiTier.keystore \
  -Pandroid.injected.signing.store.password=<password> \
  -Pandroid.injected.signing.key.alias=sukitier \
  -Pandroid.injected.signing.key.password=<password>

# Install
adb install app/build/outputs/apk/release/app-release-signed.apk
```

### Step 3: Module System Initialization

```bash
# Create directory structure
adb shell mkdir -p /data/susystem/{modules,patches,snapshots,logs,scripts}

# Copy initialization script
adb push assets/scripts/init_modules.sh /data/susystem/

# Run initialization
adb shell "sh /data/susystem/init_modules.sh"

# Verify structure
adb shell "find /data/susystem -type d | sort"
```

### Step 4: Configure Modules

#### Tier 1: Core Modules

```bash
# Create Tier 1 module directory
adb shell mkdir -p /data/susystem/modules/tier1/kernel-patch

# Create module manifest
cat > /tmp/kernel-patch.json << 'EOF'
{
  "id": "kernel-patch",
  "name": "Kernel Patch Module",
  "tier": "TIER1_CORE",
  "version": "1.0.0",
  "checksum": "$(sha256sum kernel-patch.bin | awk '{print $1}')",
  "dependencies": []
}
EOF

# Push manifest
adb push /tmp/kernel-patch.json \
  /data/susystem/modules/tier1/kernel-patch/manifest.json
```

#### Tier 2: System Modules

```bash
# Create Tier 2 module directory
adb shell mkdir -p /data/susystem/modules/tier2/selinux-patch

# Create manifest
cat > /tmp/selinux-patch.json << 'EOF'
{
  "id": "selinux-patch",
  "name": "SELinux Policy Patch",
  "tier": "TIER2_SYSTEM",
  "version": "1.0.0",
  "checksum": "$(sha256sum selinux-patch.bin | awk '{print $1}')",
  "dependencies": ["kernel-patch"]
}
EOF

# Push manifest
adb push /tmp/selinux-patch.json \
  /data/susystem/modules/tier2/selinux-patch/manifest.json
```

### Step 5: First Boot & Verification

```bash
# Reboot device
adb reboot

# Wait for boot to complete
sleep 30

# Check if Tier 1 verified automatically
adb shell tail -20 /data/susystem/logs/verification.log

# Launch app
adb shell am start -n com.sukitier/.MainActivity
```

## Configuration Files

### Main Configuration: `SukiTier.properties`

Located at project root:
```properties
[Paths]
sukisystem=/data/susystem
modules=/data/susystem/modules
scripts=/data/susystem/scripts
```

### Module Manifest: `manifest.json`

Required for each module:
```json
{
  "id": "module-id",
  "name": "Module Name",
  "tier": "TIER1_CORE",
  "version": "1.0.0",
  "checksum": "sha256_hash_here",
  "dependencies": ["parent-module-id"],
  "size_bytes": 1024000,
  "requires_reboot": false
}
```

### System Configuration: `module_manifest.json`

Located at `/data/susystem/module_manifest.json`:
```json
{
  "version": "1.0.0",
  "device": "generic",
  "gki_version": "6.1",
  "android_version": "16",
  "tiers": {
    "tier1": {
      "name": "Core Foundation",
      "required": true,
      "modules": []
    }
  }
}
```

## Verification & Testing

### Manual Tier Verification

```bash
# Connect to device shell
adb shell

# Test Tier 1 verification (in app)
# Navigate to app → observe gauge animation

# Manually trigger Tier 3 gate
# Toggle "Experimental Gate" → observe Tier 2 validation first

# Check verification log
tail -f /data/susystem/logs/verification.log
```

### Automated Tests

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests
./gradlew connectedAndroidTest

# View test results
cat app/build/reports/tests/testDebugUnitTest/index.html
```

### Integrity Checks

```bash
# Verify module checksums
adb shell "
for module in /data/susystem/modules/tier1/*; do
  echo \"Checking \$(basename \$module)...\"
  sha256sum \$module/module.bin
done
"

# Check system logs
adb shell tail -50 /data/susystem/logs/verification.log
adb shell tail -20 /data/susystem/logs/failsafe.log
```

## Troubleshooting

### APK Won't Install

```bash
# Check if app already exists
adb shell pm list packages | grep sukitier

# Uninstall first
adb uninstall com.sukitier

# Verify APK validity
aapt dump badging app/build/outputs/apk/release/app-release.apk

# Reinstall
adb install app/build/outputs/apk/release/app-release.apk
```

### Module Directory Issues

```bash
# Check directory permissions
adb shell "ls -la /data/susystem/ && stat /data/susystem/"

# Fix permissions if needed
adb shell "chmod 755 /data/susystem && chmod -R 755 /data/susystem/*"

# Verify mount points
adb shell "mount | grep mnt/sumodules"
```

### Verification Failures

```bash
# Check detailed verification logs
adb shell "cat /data/susystem/logs/verification.log | grep ERROR"

# View fail-safe events
adb shell "cat /data/susystem/logs/failsafe.log"

# List available snapshots for rollback
adb shell "ls -lat /data/susystem/snapshots/"
```

### OTA Patching Issues

```bash
# Check current slot
adb shell "getprop ro.boot.slot_suffix"

# Check OTA logs
adb shell "cat /data/susystem/logs/ota.log"

# Manually trigger OTA check
adb shell "am startservice -n com.sukitier/.core.ota.OTAPatchService"
```

## Performance Optimization

### Build Optimization

```bash
# Use local Gradle wrapper
export GRADLE_USER_HOME=~/.gradle

# Parallel build
./gradlew assembleRelease --parallel --max-workers=4

# Build cache
./gradlew build --build-cache
```

### Device Optimization

```bash
# Disable unnecessary services during testing
adb shell "settings put global air_plane_mode_on 1"

# Verify minimal load
adb shell "top -n 1 | head -10"

# Check available memory
adb shell "free -m"
```

## Uninstallation

```bash
# Remove APK
adb uninstall com.sukitier

# Clean module directories
adb shell "rm -rf /data/susystem"

# Clear app data
adb shell "rm -rf /data/data/com.sukitier"
```

## Advanced Configuration

### Custom Verification Engine

Edit `TierVerificationEngine.kt` to extend verification logic:
```kotlin
suspend fun verifyTier(tier: TierLevel): VerificationResult {
    // Add custom checks here
    super.verifyTier(tier)
}
```

### Module Hot-Reload (v2.0 feature)

Currently not supported in v1.0-ALPHA. Full module reload requires device reboot.

## Support & Resources

- **Documentation:** [README.md](README.md), [ARCHITECTURE.md](ARCHITECTURE.md)
- **Logs:** `/data/susystem/logs/`
- **Snapshots:** `/data/susystem/snapshots/`
- **ADB Shell:** `adb shell`

---

For troubleshooting, always check the logs first:
```bash
adb shell tail -100 /data/susystem/logs/verification.log
```
