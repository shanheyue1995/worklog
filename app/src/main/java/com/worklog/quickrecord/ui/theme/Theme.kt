package com.worklog.quickrecord.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF136A52),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3EFE9),
    onPrimaryContainer = Color(0xFF0B4433),
    background = Color(0xFFF1F3F2),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFF1F3F2),
    onSurfaceVariant = Color(0xFF9AA0A6),
    outline = Color(0xFFE4E7E5),
    outlineVariant = Color(0xFFF0F1F0),
    error = Color(0xFFB3261E),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF5FD3AA),
    onPrimary = Color(0xFF06251C),
    primaryContainer = Color(0xFF1C332B),
    onPrimaryContainer = Color(0xFFB4ECD8),
    background = Color(0xFF0F1211),
    onBackground = Color(0xFFE9EEEC),
    surface = Color(0xFF1A1E1D),
    onSurface = Color(0xFFE9EEEC),
    surfaceVariant = Color(0xFF141817),
    onSurfaceVariant = Color(0xFF9AA5A0),
    outline = Color(0xFF323836),
    outlineVariant = Color(0xFF2A302E),
    error = Color(0xFFF09A94),
)

/**
 * 深色模式跟随系统，不提供单独开关。
 */
@Composable
fun QuickRecordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppColors provides appColors(darkTheme)) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            content = content,
        )
    }
}
