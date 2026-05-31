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

private val LightColorScheme =
  lightColorScheme(
    primary = DeepMaroon,
    secondary = BloodCrimson,
    tertiary = PaavaiGold,
    background = WarmSlate, // Pure White
    surface = DarkCharcoal,   // Light surface
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextDark,  // Deep Coal Text
    onSurface = TextDark
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Default to clean Light Theme as requested
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve our branding
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
