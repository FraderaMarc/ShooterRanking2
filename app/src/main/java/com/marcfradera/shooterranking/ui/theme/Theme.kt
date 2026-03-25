package com.marcfradera.shooterranking.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SrDarkColors = darkColorScheme(
    primary = Color(0xFF3BB54A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2C8F39),
    onPrimaryContainer = Color.White,
    background = Color(0xFF0F1223),
    onBackground = Color(0xFFEDEFF6),
    surface = Color(0xFF141834),
    onSurface = Color(0xFFEDEFF6),
    surfaceVariant = Color(0xFF1B2145),
    onSurfaceVariant = Color(0xFFC9CDE0),
    error = Color(0xFFFF4D4D),
    onError = Color.White
)

@Composable
fun ShooterRankingTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SrDarkColors, content = content)
}
