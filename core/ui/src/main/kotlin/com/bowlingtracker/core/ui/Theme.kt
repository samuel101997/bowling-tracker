package com.bowlingtracker.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Pitch = Color(0xFF2E7D32)
private val Ball = Color(0xFFB71C1C)

private val LightColors = lightColorScheme(primary = Pitch, secondary = Ball)
private val DarkColors = darkColorScheme(primary = Pitch, secondary = Ball)

@Composable
fun BowlingTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
