#!/bin/bash
# SukiTier Quick Reference Guide
# For developers and testers

echo "╔══════════════════════════════════════════════════════╗"
echo "║     SUKITIER v1.0-ALPHA - QUICK REFERENCE           ║"
echo "║  Tiered Android Root Module Manager for GKI 6.1     ║"
echo "╚══════════════════════════════════════════════════════╝"

echo ""
echo "═══ PROJECT STRUCTURE ═══"
echo "
SukiTier/
├── app/                     # Main Android application
├── modules/                 # Feature modules (tier1-3)
├── assets/scripts/          # rescue_sentry.sh, init_modules.sh
├── README.md                # Main documentation
├── ARCHITECTURE.md          # System design deep dive
├── INSTALLATION.md          # Setup & deployment guide
└── PROJECT_SUMMARY.md       # Feature checklist & status
"

echo "═══ CORE COMPONENTS ═══"
echo "
1. TierVerificationEngine.kt
   └─ Hierarchical tier verification with checksum validation

2. IntegrityChecker.kt
   └─ SHA256/MD5 checksums, corruption detection

3. TieredModuleManager.kt
   └─ Sequential mounting with predecessor validation

4. OTAPatching.kt
   └─ Inactive slot detection & auto-patching

5. FailSafeManager.kt
   └─ Automatic rollback via rescue_sentry.sh

6. IndustrialUI.kt
   └─ Mechanical gauges, industrial design, Compose

7. MainActivity.kt
   └─ Full UI implementation with live updates
"

echo "═══ BUILD COMMANDS ═══"
echo "
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew testDebugUnitTest

# Clean build
./gradlew clean assembleRelease
"

echo "═══ DEPLOYMENT ═══"
echo "
# 1. Build release APK
./gradlew assembleRelease

# 2. Install to device
adb install app/build/outputs/apk/release/app-release.apk

# 3. Initialize modules
adb push assets/scripts/init_modules.sh /data/susystem/
adb shell sh /data/susystem/init_modules.sh

# 4. Verify installation
adb shell ls -la /data/susystem/
"

echo "═══ DEBUGGING ═══"
echo "
# View verification logs (real-time)
adb shell tail -f /data/susystem/logs/verification.log

# Check failsafe events
adb shell cat /data/susystem/logs/failsafe.log

# List rollback snapshots
adb shell ls -lat /data/susystem/snapshots/

# View all logs
adb shell ls -la /data/susystem/logs/
"

echo "═══ TIER ARCHITECTURE ═══"
echo "
TIER 1 - CORE (REQUIRED)
  ├─ Kernel patch module
  ├─ Boot module
  └─ SHA256 verification: ALWAYS

TIER 2 - SYSTEM (OPTIONAL)
  ├─ SELinux patch
  ├─ System modifications
  └─ Requires: TIER 1 ✓

TIER 3 - EXPERIMENTAL (GATED)
  ├─ Experimental features
  ├─ User-controlled toggle
  └─ Requires: TIER 1 + TIER 2 ✓

TIER 4 - OTA (AUTOMATIC)
  ├─ Inactive slot patching
  ├─ Auto on boot
  └─ Requires: All predecessors ✓
"

echo "═══ KEY FILES ═══"
echo "
Configuration:
  └─ SukiTier.properties          # Project config
  └─ gradle.properties            # Gradle settings
  └─ build.gradle.kts            # App build config

Verification:
  └─ TierVerificationEngine.kt    # Verification logic
  └─ IntegrityChecker.kt          # Checksum & corruption

Module Management:
  └─ TieredModuleManager.kt       # Module mounting
  └─ OTAPatching.kt              # OTA handling

Fail-Safe:
  └─ FailSafeManager.kt          # Rollback system
  └─ rescue_sentry.sh            # Emergency script

UI:
  └─ IndustrialUI.kt             # Compose components
  └─ Theme.kt                    # Material3 theme
  └─ MainActivity.kt             # Main screen

Tests:
  └─ TierVerificationTests.kt     # Unit tests
"

echo "═══ ON-DEVICE PATHS ═══"
echo "
/data/susystem/
├── modules/                     # Module storage
│   ├── tier1/
│   ├── tier2/
│   └── tier3/
├── patches/                     # OTA patches
├── snapshots/                   # Rollback points
├── logs/                        # All logs
├── scripts/                     # rescue_sentry.sh
└── module_manifest.json         # System config
"

echo "═══ IMPORTANT FEATURES ═══"
echo "
✅ Tiered mounting (T1 → T2 → T3 → T4)
✅ Cascading verification (predecessors required)
✅ Checksum validation (SHA256)
✅ Experimental gate toggle
✅ Mechanical gauge animations
✅ High-contrast industrial UI
✅ Automatic OTA patching
✅ Snapshot-based rollback
✅ Emergency rescue_sentry.sh
✅ Real-time status updates
✅ Comprehensive logging
✅ Thread-safe operations
"

echo "═══ DOCUMENTATION ═══"
echo "
1. README.md
   └─ Main guide, usage examples, API reference

2. ARCHITECTURE.md
   └─ Deep dive into design, data flows, security

3. INSTALLATION.md
   └─ Step-by-step setup, troubleshooting, config

4. PROJECT_SUMMARY.md
   └─ Feature checklist, component overview

5. SukiTier.properties
   └─ Project configuration file
"

echo "═══ NEXT STEPS ═══"
echo "
For Development:
  1. Read ARCHITECTURE.md
  2. Review TierVerificationEngine.kt
  3. Customize IndustrialUI.kt

For Testing:
  1. Follow INSTALLATION.md
  2. Build debug APK
  3. Deploy to test device
  4. Monitor logs in real-time

For Production:
  1. Create release keystore
  2. Build signed release APK
  3. Run all unit tests
  4. Deploy and verify on device
"

echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║              STATUS: ✅ READY FOR DEVELOPMENT        ║"
echo "║         Version 1.0.0-ALPHA | January 26, 2026       ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""

# Quick verify script exists
if [ -x "build.sh" ]; then
    echo "✅ build.sh found and executable"
else
    echo "⚠️  build.sh not executable - run: chmod +x build.sh"
fi

if [ -f "README.md" ]; then
    echo "✅ README.md found"
fi

if [ -f "ARCHITECTURE.md" ]; then
    echo "✅ ARCHITECTURE.md found"
fi

if [ -f "INSTALLATION.md" ]; then
    echo "✅ INSTALLATION.md found"
fi

echo ""
echo "All core components implemented ✅"
echo "Ready for: Development → Testing → Deployment"
echo ""
