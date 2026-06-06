package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFA8C7FA),
    onSecondary = Color(0xFF062E6F),
    secondaryContainer = Color(0xFF004494),
    onSecondaryContainer = Color(0xFFD3E3FD),
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF381E72),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF0F1115),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF1D2028),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF44474F),
    outlineVariant = Color(0xFF44474F),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC2E7FF),
    onPrimaryContainer = Color(0xFF001D35),
    secondary = Color(0xFF0B57D0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E3FD),
    onSecondaryContainer = Color(0xFF001D35),
    tertiary = Color(0xFFEADDFF),
    onTertiary = Color(0xFF21005D),
    background = Color(0xFFF3F6FA),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFF3F6FA),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7EFFB),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFFE0E2E6),
    outlineVariant = Color(0xFFE0E2E6),
    errorContainer = Color(0xFFFFDADA),
    onErrorContainer = Color(0xFF410002)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
