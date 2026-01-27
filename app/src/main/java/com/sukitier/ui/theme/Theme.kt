package com.sukitier.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// High-contrast industrial colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00FF00),      // Bright green
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFF00AAFF),    // Bright cyan
    onSecondary = Color(0xFF000000),
    tertiary = Color(0xFFFFAA00),     // Bright orange
    onTertiary = Color(0xFF000000),
    background = Color(0xFF0a0a0a),   // Almost black
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1a1a1a),
    onSurface = Color(0xFFFFFFFF),
    error = Color(0xFFFF0000),
    onError = Color(0xFF000000)
)

@Composable
fun SukiTierTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
