package com.john.youlan.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F7DF3), onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F1FF), surface = Color(0xFFF7F9FC),
    surfaceVariant = Color(0xFFEFF3F8), onSurface = Color(0xFF18202A)
)
private val DarkColors = darkColorScheme()

@Composable
fun YouLanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
