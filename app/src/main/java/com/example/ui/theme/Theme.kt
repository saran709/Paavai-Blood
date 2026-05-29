package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = DeepMaroon,
    secondary = BloodCrimson,
    tertiary = PaavaiGold,
    background = WarmSlate, // Pitch black: 0xFF050505
    surface = DarkCharcoal,   // Slate surface: 0xFF121214
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextDark,  // White-slate: 0xFFF8FAFC
    onSurface = TextDark
  )

private val LightColorScheme = DarkColorScheme // Enforce dark theme even for light mode for complete "Sophisticated Dark" aesthetic

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark mode as requested
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve our branding
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
