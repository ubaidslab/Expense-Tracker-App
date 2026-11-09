package com.example.kharcha.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette: black, white, green (paisa/control), blue (info)
val GreenAccent = Color(0xFF22C55E)
val BlueAccent = Color(0xFF3B82F6)
val CardBg = Color(0xFF111111)
val Muted = Color(0xFF9CA3AF)
val DangerRed = Color(0xFFEF4444)

private val KharchaColors = darkColorScheme(
    primary = GreenAccent,
    onPrimary = Color.Black,
    secondary = BlueAccent,
    onSecondary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = CardBg,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Muted,
    error = DangerRed
)

@Composable
fun KharchaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KharchaColors, content = content)
}
