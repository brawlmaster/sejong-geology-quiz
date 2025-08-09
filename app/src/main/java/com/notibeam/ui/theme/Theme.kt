package com.notibeam.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NotiBeamColorScheme: ColorScheme = darkColorScheme(
    primary = NavyPrimary,
    onPrimary = NavyOnPrimary,
    secondary = NavySecondary,
    tertiary = NavyTertiary,
    background = Background,
    surface = SurfaceColor,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun NotiBeamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NotiBeamColorScheme,
        typography = Typography,
        content = content
    )
}