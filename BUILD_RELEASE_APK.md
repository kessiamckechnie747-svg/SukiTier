# SukiTier Release APK Build Guide

**Version:** 1.0.0-ALPHA  
**Generated:** 2026-05-30  
**Target:** Android 34 (API Level 34) / GKI 6.1

## 📋 Prerequisites

### System Requirements
- **Java:** JDK 17 or higher
- **Android SDK:** API 34 (Android 14)
- **Gradle:** 8.x (bundled in project)
- **Memory:** Minimum 8GB RAM (16GB recommended)
- **Disk Space:** 10GB available

### Verify Environment

```bash
# Check Java version (must be 17+)
java -version

# Output should show:
# openjdk version "17.0.x"
```

### Install JDK 17 (if needed)

**macOS (using Homebrew):**
```bash
brew install openjdk@17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install openjdk-17-jdk
```

**Windows:**
- Download from [Oracle](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- Add to PATH

---

## 🔑 Signing Configuration

### Step 1: Create a Keystore (First Time Only)

```bash
# Navigate to project root
cd ~/SukiTier

# Generate keystore (replace YOUR_PASSWORD with a secure password)
keytool -genkey -v -keystore release.keystore \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias sukitier-key \
  -storepass YOUR_PASSWORD \
  -keypass YOUR_PASSWORD

# When prompted, fill in:
# What is your first and last name? [Unknown]:  Your Name
# What is the name of your organizational unit? [Unknown]:  Development
# What is the name of your organization? [Unknown]:  SukiTier
# What is the name of your City or Locality? [Unknown]:  Your City
# What is the name of your State or Province? [Unknown]:  Your State
# What is the two-letter country code for this unit? [Unknown]:  US
```

**Output:** `release.keystore` file in project root

### Step 2: Add to .gitignore

```bash
echo "release.keystore" >> .gitignore
```

### Step 3: Create Signing Configuration

Create `app/keystore.properties`:

```properties
storeFile=../release.keystore
storePassword=YOUR_PASSWORD
keyAlias=sukitier-key
keyPassword=YOUR_PASSWORD
```

**⚠️ Security Note:** Never commit `release.keystore` or `keystore.properties` to version control!

---

## 🔧 Configure Build Signing

Update `app/build.gradle.kts` with signing config:

```kotlin
android {
    // ... existing config ...
    
    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("release.keystore")
            val keystoreProperties = Properties()
            
            if (keystoreFile.exists()) {
                keystoreProperties.load(FileInputStream(rootProject.file("app/keystore.properties")))
            }
            
            storeFile = keystoreFile
            storePassword = keystoreProperties.getProperty("storePassword", "")
            keyAlias = keystoreProperties.getProperty("keyAlias", "")
            keyPassword = keystoreProperties.getProperty("keyPassword", "")
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

Add to top of `app/build.gradle.kts`:

```kotlin
import java.io.FileInputStream
import java.util.Properties
```

---

## 🏗️ Build Release APK

### Quick Build (Recommended)

```bash
# Navigate to project root
cd ~/SukiTier

# Build release APK with Gradle wrapper
./gradlew assembleRelease

# On Windows:
# gradlew.bat assembleRelease
```

### Output Location

```
app/build/outputs/apk/release/app-release.apk
```

### Verify Build Success

```bash
# Check file exists and size
ls -lh app/build/outputs/apk/release/app-release.apk

# Expected output:
# -rw-r--r-- 1 user group 15M May 30 12:34 app/build/outputs/apk/release/app-release.apk
```

---

## 🚀 Advanced Build Options

### Build with Verbose Output

```bash
./gradlew assembleRelease --info
```

### Clean Build (if previous build had issues)

```bash
./gradlew clean assembleRelease
```

### Build and Run Tests

```bash
./gradlew assembleRelease testReleaseUnitTest
```

### Build with Dependency Report

```bash
./gradlew assembleRelease dependencyReport
```

### Profile Build Time

```bash
./gradlew assembleRelease --profile
# Report generated: build/reports/profile/profile-2026-05-30-12-34-00.html
```

---

## 📦 Bundle for Google Play (AAB Format)

For Google Play Store submission, use Android App Bundle format:

```bash
# Build AAB (Android App Bundle)
./gradlew bundleRelease

# Output
app/build/outputs/bundle/release/app-release.aab
```

---

## 🔍 Verify APK Integrity

### Check Signing Certificate

```bash
# Verify APK signature
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# Expected output:
# sm  1234 Fri May 30 12:34:00 EDT 2026 AndroidManifest.xml
# ...
# jar verified.
```

### Extract APK Info

```bash
# Using aapt (Android Asset Packaging Tool)
aapt dump badging app/build/outputs/apk/release/app-release.apk
```

---

## 📱 Install on Device/Emulator

### Prerequisites
- Device or emulator connected via ADB
- USB debugging enabled (for physical devices)

### Install APK

```bash
# Install on connected device
adb install app/build/outputs/apk/release/app-release.apk

# Verify installation
adb shell pm list packages | grep sukitier

# Launch app
adb shell am start -n com.sukitier/.MainActivity

# View app logs
adb logcat | grep sukitier
```

### Uninstall

```bash
adb uninstall com.sukitier
```

---

## 🐛 Troubleshooting

### Issue: "JAVA_HOME not set"

```bash
# Set JAVA_HOME temporarily
export JAVA_HOME=/path/to/jdk17

# Or permanently (add to ~/.bashrc or ~/.zshrc)
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
source ~/.zshrc
```

### Issue: "Build failed: keystore not found"

```bash
# Regenerate keystore
keytool -genkey -v -keystore release.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias sukitier-key
```

### Issue: "Out of memory during build"

```bash
# Increase Gradle memory
export ORG_GRADLE_PROJECT_org_gradle_jvmargs=-Xmx4096m

# Or edit gradle.properties
echo "org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m" >> gradle.properties
```

### Issue: "Compilation errors in modules"

```bash
# Full clean and rebuild
./gradlew clean
./gradlew build --refresh-dependencies
./gradlew assembleRelease
```

### Issue: "Minification errors"

Check `app/proguard-rules.pro` and review build logs:

```bash
./gradlew assembleRelease --info 2>&1 | grep -i "minifi\|proguard"
```

---

## 📊 Build Performance Tips

### Enable Gradle Build Cache

```bash
# Already enabled in gradle.properties
# org.gradle.caching=true
```

### Run Parallel Builds

```bash
# Already enabled in gradle.properties
# org.gradle.parallel=true

# Or pass as flag
./gradlew assembleRelease --parallel
```

### Use Gradle Daemon

```bash
# Already enabled by default
# Keep JVM running between builds
./gradlew assembleRelease --daemon
```

---

## 📋 Build Checklist

Before releasing to production:

- [ ] All unit tests passing: `./gradlew testReleaseUnitTest`
- [ ] All instrumented tests passing: `./gradlew connectedAndroidTest`
- [ ] No lint warnings: `./gradlew lintRelease`
- [ ] ProGuard rules validated
- [ ] Version code incremented in `app/build.gradle.kts`
- [ ] Release notes prepared
- [ ] APK signature verified with `jarsigner`
- [ ] APK installed on real device and tested
- [ ] Performance benchmarks acceptable
- [ ] Logs cleaned (no DEBUG level in production)

---

## 📝 Version Management

### Update Version Before Release

Edit `app/build.gradle.kts`:

```kotlin
defaultConfig {
    applicationId = "com.sukitier"
    minSdk = 31
    targetSdk = 34
    versionCode = 2              // ← Increment
    versionName = "1.0.1-BETA"   // ← Update
}
```

**Version scheme:** `MAJOR.MINOR.PATCH-PHASE`
- Example: `1.0.0-ALPHA` → `1.0.1-BETA` → `1.1.0-RC1` → `1.1.0`

---

## 🔐 Security Best Practices

### Never Commit Sensitive Files

```bash
# Ensure in .gitignore
release.keystore
app/keystore.properties
*.jks
*.keystore
```

### Rotate Keystore Regularly

```bash
# Backup current keystore
cp release.keystore release.keystore.backup

# Create new keystore with new password annually
keytool -genkey -v -keystore release.keystore.new \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias sukitier-key
```

### CI/CD Keystore Management

For GitHub Actions, store base64-encoded keystore:

```bash
# Encode keystore
base64 -i release.keystore > release.keystore.b64

# Add to GitHub Secrets as ANDROID_KEYSTORE_B64
# Then decode in workflow:
echo "${{ secrets.ANDROID_KEYSTORE_B64 }}" | base64 --decode > release.keystore
```

---

## 📈 Next Steps

### After Successful Build

1. **Test on Real Device**
   ```bash
   adb install app/build/outputs/apk/release/app-release.apk
   ```

2. **Submit to Google Play**
   - Create Google Play Developer account
   - Upload `app-release.aab` to Play Console
   - Configure app listing and release notes

3. **Tag Release in Git**
   ```bash
   git tag -a v1.0.0-ALPHA -m "Release version 1.0.0-ALPHA"
   git push origin v1.0.0-ALPHA
   ```

4. **Create GitHub Release**
   - Attach APK/AAB to release
   - Include changelog and known issues

---

## 📚 Reference Links

- [Android Build Documentation](https://developer.android.com/build)
- [Android App Signing](https://developer.android.com/training/articles/signing)
- [ProGuard Rules](https://www.guardsquare.com/proguard/manual/usage)
- [Gradle Android Plugin](https://developer.android.com/reference/tools/gradle-api)
- [Google Play Console](https://play.google.com/console)

---

## 🆘 Support

For build issues:
1. Check logs: `./gradlew assembleRelease --info`
2. Review [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)
3. Check Android Studio Logcat (if using IDE)
4. Run `./gradlew clean` and rebuild

---

**Last Updated:** 2026-05-30  
**SukiTier Project**
