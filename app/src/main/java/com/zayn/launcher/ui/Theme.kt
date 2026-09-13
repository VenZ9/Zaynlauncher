package com.zayn.launcher.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFF7CCB6A),
    onPrimary = Color(0xFF0A1F0A),
    secondary = Color(0xFF57B0FF),
    background = Color(0xFF0E1116),
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF1F2630),
    onBackground = Color(0xFFE6EDF3),
    onSurface = Color(0xFFE6EDF3)
)

private val Light = lightColorScheme(
    primary = Color(0xFF2E7D32),
    secondary = Color(0xFF1565C0)
)

@Composable
fun ZaynTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
