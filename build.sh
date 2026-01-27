#!/usr/bin/env bash

# SukiTier - Tiered Android Root Manager Build Script
# Builds and deploys the application

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_TYPE="${1:-release}"
DEPLOY="${2:-false}"

echo "================================================"
echo "SukiTier Build System"
echo "================================================"
echo "Build Type: $BUILD_TYPE"
echo "Deploy: $DEPLOY"
echo ""

# Clean
echo "[1/4] Cleaning previous builds..."
./gradlew clean --quiet

# Build
echo "[2/4] Building APK ($BUILD_TYPE)..."
if [ "$BUILD_TYPE" = "debug" ]; then
    ./gradlew assembleDebug --quiet
    APK="app/build/outputs/apk/debug/app-debug.apk"
else
    ./gradlew assembleRelease --quiet
    APK="app/build/outputs/apk/release/app-release.apk"
fi

echo "[3/4] Build complete: $APK"

# Deploy
if [ "$DEPLOY" = "true" ]; then
    echo "[4/4] Deploying to device..."
    adb install -r "$APK"
    echo "Installation complete!"
else
    echo "[4/4] Deploy skipped (use 'true' as second argument to deploy)"
fi

echo ""
echo "================================================"
echo "Build finished successfully!"
echo "================================================"
