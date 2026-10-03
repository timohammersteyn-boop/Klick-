package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonOrange,
    onPrimary = Color.Black,
    primaryContainer = ChassisElevated,
    onPrimaryContainer = NeonOrange,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = ChassisSurface,
    onSecondaryContainer = NeonCyan,
    tertiary = NeonPurple,
    onTertiary = Color.White,
    background = ChassisBlack,
    onBackground = Color(0xFFF1F5F9),
    surface = ChassisDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = ChassisSurface,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = ChassisBorder,
    outlineVariant = ChassisHighlight,
    error = NeonRed,
    onError = Color.White
)

@Composable
fun SchwungLiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // For professional audio groovebox instruments, a dedicated precision dark theme is standard
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
