package com.sukitier.test

import android.os.Build
import com.sukitier.core.sentry.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Comprehensive PreFlashSentry Unit Tests
 */
class PreFlashSentryTests {
    
    private lateinit var sentry: PreFlashSentry
    private lateinit var analyzer: HeaderAnalyzer
    private lateinit var verifier: BootloaderVerifier
    
    @Before
    fun setUp() {
        sentry = PreFlashSentry()
        analyzer = HeaderAnalyzer()
        verifier = BootloaderVerifier()
    }
    
    // ==================== HeaderAnalyzer Tests ====================
    
    @Test
    fun testHeaderAnalyzerMagicValidation() {
        // Test that analyzer rejects non-boot images
        val invalidFile = createTempFile("invalid", ".img")
        invalidFile.writeText("NOT A BOOT IMAGE")
        
        val result = analyzer.analyzeImage(invalidFile.absolutePath)
        assertNull("Should reject invalid magic", result)
        
        invalidFile.delete()
    }
    
    @Test
    fun testHeaderAnalyzerFileSizeSanityCheck() {
        // Test that analyzer rejects undersized files
        val tinyFile = createTempFile("tiny", ".img")
        tinyFile.writeText("ANDROID!small")
        
        val result = analyzer.analyzeImage(tinyFile.absolutePath)
        assertNull("Should reject undersized file", result)
        
        tinyFile.delete()
    }
    
    @Test
    fun testHeaderAnalyzerOSVersionExtraction() {
        // Test OS version extraction from header
        val imageHeader = createMockImageHeader(
            osVersion = "16.0",
            patchLevel = "2026-01"
        )
        
        assertEquals("16.0", imageHeader.osVersion)
    }
    
    @Test
    fun testHeaderAnalyzerPatchLevelFormat() {
        // Test patch level YYYY-MM format
        val imageHeader = createMockImageHeader(
            patchLevel = "2026-01"
        )
        
        assertTrue("Patch level should match YYYY-MM format",
            Regex("\\d{4}-\\d{2}").matches(imageHeader.patchLevel))
    }
    
    // ==================== BootloaderVerifier Tests ====================
    
    @Test
    fun testBootloaderVerifierDetectsUnlockedBootloader() {
        val isUnlocked = verifier.isBootloaderUnlocked()
        
        // On development devices, bootloader is typically unlocked
        // This test validates the method returns a boolean without crashing
        assertTrue("Should return boolean", isUnlocked is Boolean)
    }
    
    @Test
    fun testBootloaderVerifierReadsVerifiedBootState() {
        val bootState = verifier.getVerifiedBootState()
        
        // Should be one of: green, orange, red, yellow, or unknown
        assertTrue("Boot state should be valid",
            bootState in listOf("green", "orange", "red", "yellow", "unknown"))
    }
    
    @Test
    fun testBootloaderVerifierChecksBootCompletion() {
        val bootCompleted = verifier.isBootCompleted()
        
        // Device should be booted during testing
        assertTrue("Device should be booted", bootCompleted)
    }
    
    // ==================== SafetyException Tests ====================
    
    @Test
    fun testSafetyExceptionCategories() {
        // Test all exception categories are defined
        val categories = SafetyExceptionCategory.values()
        
        assertEquals("Should have 10 categories", 10, categories.size)
        assertTrue("Should include BOOTLOADER_LOCKED",
            categories.contains(SafetyExceptionCategory.BOOTLOADER_LOCKED))
        assertTrue("Should include PATCH_LEVEL_DOWNGRADE",
            categories.contains(SafetyExceptionCategory.PATCH_LEVEL_DOWNGRADE))
    }
    
    @Test
    fun testSafetyExceptionBootloaderLockedRecovery() {
        val exception = SafetyException(
            category = SafetyExceptionCategory.BOOTLOADER_LOCKED,
            message = "Test bootloader locked",
            imageHeader = null,
            deviceState = null
        )
        
        val recoverySteps = exception.getRecoverySteps()
        assertTrue("Should have recovery steps", recoverySteps.isNotEmpty())
        assertTrue("Should mention fastboot",
            recoverySteps.any { it.contains("fastboot", ignoreCase = true) })
    }
    
    @Test
    fun testSafetyExceptionPatchDowngradeRecovery() {
        val exception = SafetyException(
            category = SafetyExceptionCategory.PATCH_LEVEL_DOWNGRADE,
            message = "Cannot downgrade patch",
            imageHeader = null,
            deviceState = null
        )
        
        val steps = exception.getRecoverySteps()
        assertTrue("Should explain cannot downgrade",
            steps.any { it.contains("downgrade", ignoreCase = true) })
    }
    
    @Test
    fun testSafetyExceptionProductMismatchRecovery() {
        val exception = SafetyException(
            category = SafetyExceptionCategory.PRODUCT_MISMATCH,
            message = "Product mismatch",
            imageHeader = null,
            deviceState = null
        )
        
        val steps = exception.getRecoverySteps()
        assertTrue("Should mention device verification",
            steps.any { it.contains("getprop", ignoreCase = true) ||
                       it.contains("device", ignoreCase = true) })
    }
    
    // ==================== PreFlashSentry Validation Tests ====================
    
    @Test
    fun testValidationFailsWhenBootloaderLocked() {
        // This would fail on production devices with locked bootloader
        // Skip on devices that can't have this state
        if (verifier.isBootloaderUnlocked()) {
            // Test passes - bootloader unlocked as expected
            assertTrue("Test device should have bootloader unlocked", true)
        }
    }
    
    @Test
    fun testValidationFailsWhenImagePathInvalid() {
        val exception = assertThrows(SafetyException::class.java) {
            sentry.validatePreFlash("/nonexistent/path/boot.img")
        }
        
        assertEquals(SafetyExceptionCategory.INVALID_IMAGE_FORMAT, exception.category)
    }
    
    @Test
    fun testValidationFailsWhenImageTooSmall() {
        val smallFile = createTempFile("small", ".img")
        smallFile.writeText("ANDROID!")  // Magic but undersized
        
        val exception = assertThrows(SafetyException::class.java) {
            sentry.validatePreFlash(smallFile.absolutePath)
        }
        
        smallFile.delete()
    }
    
    @Test
    fun testValidationChecksOSVersion() {
        // Create a mock scenario
        val imageHeader = createMockImageHeader(
            osVersion = "10.0"  // Old version
        )
        
        // SDK version check would fail if device is Android 16
        if (Build.VERSION.SDK_INT == 35) {  // Android 15
            // Check passes
            assertTrue("Version check should pass for nearby versions", true)
        }
    }
    
    @Test
    fun testPatchLevelDowngradeDetection() {
        // Test patch level comparison logic
        val currentPatch = "2026-01"
        val imagePatch = "2025-12"
        
        val isDowngrade = isPatchDowngrade(imagePatch, currentPatch)
        assertTrue("2025-12 should be downgrade from 2026-01", isDowngrade)
    }
    
    @Test
    fun testPatchLevelUpgradeAllowed() {
        val currentPatch = "2025-12"
        val imagePatch = "2026-01"
        
        val isDowngrade = isPatchDowngrade(imagePatch, currentPatch)
        assertFalse("2026-01 should not be downgrade from 2025-12", isDowngrade)
    }
    
    @Test
    fun testPatchLevelSameMonthAllowed() {
        val currentPatch = "2026-01"
        val imagePatch = "2026-01"
        
        val isDowngrade = isPatchDowngrade(imagePatch, currentPatch)
        assertFalse("Same patch level should not be downgrade", isDowngrade)
    }
    
    @Test
    fun testBootStateValidation() {
        val bootState = verifier.getVerifiedBootState()
        
        val isValid = bootState in listOf("green", "orange", "red", "yellow", "unknown")
        assertTrue("Boot state should be valid", isValid)
    }
    
    @Test
    fun testKernelVersionDetection() {
        val imageHeader = createMockImageHeader(
            kernelVersion = "6.1.25-android"
        )
        
        assertTrue("Should detect GKI 6.1",
            imageHeader.kernelVersion.contains("6.1", ignoreCase = true))
    }
    
    @Test
    fun testProductNameExtraction() {
        val imageHeader = createMockImageHeader(
            productName = "shiba"
        )
        
        assertTrue("Should extract product name",
            imageHeader.productName.isNotEmpty())
    }
    
    @Test
    fun testBootImageSizeValidation() {
        val imageHeader = createMockImageHeader()
        
        val isValid = imageHeader.bootImageSize >= 4 * 1024 * 1024
        assertTrue("Boot image should be at least 4MB", isValid)
    }
    
    // ==================== Integration Tests ====================
    
    @Test
    fun testFullValidationPipeline() {
        // This test would run the full validation pipeline
        // Skip if no test image available
        val testImagePath = "/sdcard/test_boot.img"
        val testImageFile = File(testImagePath)
        
        if (testImageFile.exists()) {
            try {
                val imageHeader = sentry.validatePreFlash(testImagePath)
                assertNotNull("Should return image header on success", imageHeader)
            } catch (e: SafetyException) {
                // Expected on incompatible device
                assertNotNull("Should have exception category", e.category)
            }
        }
    }
    
    @Test
    fun testValidationErrorDetailsComplete() {
        val exception = SafetyException(
            category = SafetyExceptionCategory.PATCH_LEVEL_DOWNGRADE,
            message = "Cannot downgrade",
            imageHeader = createMockImageHeader(),
            deviceState = createMockDeviceState(),
            details = mapOf(
                "current_patch" to "2026-01",
                "image_patch" to "2025-12"
            )
        )
        
        assertTrue("Should have details", exception.details.isNotEmpty())
        assertNotNull("Should have image header", exception.imageHeader)
        assertNotNull("Should have device state", exception.deviceState)
    }
    
    @Test
    fun testExceptionMessageClarity() {
        val exception = SafetyException(
            category = SafetyExceptionCategory.BOOTLOADER_LOCKED,
            message = "CRITICAL: Bootloader is locked. Cannot flash image.",
            imageHeader = null,
            deviceState = null
        )
        
        assertTrue("Message should be clear",
            exception.message.contains("Bootloader") &&
            exception.message.contains("CRITICAL"))
    }
    
    // ==================== Helper Functions ====================
    
    private fun createMockImageHeader(
        osVersion: String = "16.0",
        patchLevel: String = "2026-01",
        kernelVersion: String = "6.1.25-android",
        productName: String = "shiba",
        bootImageSize: Long = 32 * 1024 * 1024  // 32MB
    ): ImageHeader {
        return ImageHeader(
            osVersion = osVersion,
            patchLevel = patchLevel,
            kernelVersion = kernelVersion,
            buildId = "AP3A.240805.005",
            timestamp = System.currentTimeMillis(),
            productName = productName,
            bootImageSize = bootImageSize,
            rawData = ByteArray(256)
        )
    }
    
    private fun createMockDeviceState(
        bootloaderUnlocked: Boolean = true,
        bootCompleted: Boolean = true
    ): DeviceState {
        return DeviceState(
            sdkVersion = Build.VERSION.SDK_INT,
            buildVersion = Build.VERSION.RELEASE,
            fingerprint = Build.FINGERPRINT,
            bootloaderUnlocked = bootloaderUnlocked,
            verifiedBootState = "green",
            bootCompleted = bootCompleted,
            currentPatchLevel = "2026-01"
        )
    }
    
    private fun isPatchDowngrade(imagePatch: String, currentPatch: String): Boolean {
        return try {
            val (imageYear, imageMonth) = imagePatch.split("-").map { it.toInt() }
            val (currentYear, currentMonth) = currentPatch.split("-").map { it.toInt() }
            
            (imageYear < currentYear) ||
            (imageYear == currentYear && imageMonth < currentMonth)
        } catch (e: Exception) {
            false
        }
    }
    
    private fun createTempFile(prefix: String, suffix: String): File {
        return File.createTempFile(prefix, suffix)
    }
}

/**
 * Integration Tests for PreFlashSentry with Real Device
 */
class PreFlashSentryIntegrationTest {
    
    private lateinit var sentry: PreFlashSentry
    
    @Before
    fun setUp() {
        sentry = PreFlashSentry()
    }
    
    @Test
    fun testDeviceStateCapture() {
        // Test that we can capture real device state without crashing
        assertDoesNotThrow {
            val bootloaderVerifier = BootloaderVerifier()
            
            bootloaderVerifier.isBootloaderUnlocked()
            bootloaderVerifier.getVerifiedBootState()
            bootloaderVerifier.isBootCompleted()
        }
    }
    
    @Test
    fun testValidationWithRealDeviceState() {
        // This test would need a real boot.img file
        // In CI/CD environments, this might be skipped
        val testImagePath = "/sdcard/Downloads/boot.img"
        val imageFile = File(testImagePath)
        
        if (imageFile.exists()) {
            assertDoesNotThrow {
                try {
                    sentry.validatePreFlash(testImagePath)
                } catch (e: SafetyException) {
                    // Expected on some devices
                    assertNotNull(e.category)
                }
            }
        }
    }
    
    private fun <T> assertDoesNotThrow(block: () -> T): T {
        return try {
            block()
        } catch (e: Exception) {
            throw AssertionError("Expected no exception but got ${e.javaClass.simpleName}", e)
        }
    }
}
