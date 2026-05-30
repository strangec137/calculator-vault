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
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
  )

@Composable
fun MyApplicationTheme(
  appTheme: String = "Default",
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = when (appTheme) {
    "Bakery Cozy" -> {
      if (darkTheme) {
        darkColorScheme(
          primary = Color(0xFFCD853F),
          onPrimary = Color.Black,
          secondary = Color(0xFFD2691E),
          background = Color(0xFF1E1C1A),
          surface = Color(0xFF2B2521),
          onBackground = Color(0xFFFFF0E5),
          onSurface = Color(0xFFFFF0E5),
          surfaceVariant = Color(0xFF3D322B)
        )
      } else {
        lightColorScheme(
          primary = Color(0xFF8B4513),
          onPrimary = Color.White,
          secondary = Color(0xFFCD853F),
          background = Color(0xFFFDF0E6),
          surface = Color(0xFFFFF7F0),
          onBackground = Color(0xFF3E2723),
          onSurface = Color(0xFF3E2723),
          surfaceVariant = Color(0xFFF5E1D3)
        )
      }
    }
    "Pastel Green Mint" -> {
      lightColorScheme(
        primary = Color(0xFF006D5B), // Elegant Dark Teal / Forest Green accent
        onPrimary = Color.White,
        secondary = Color(0xFF40916C),
        background = Color(0xFFFAFCFA), // Soft off-white/mint background
        surface = Color(0xFFFAFCFA),    // Matching surface color for unified design
        onBackground = Color(0xFF004D40), // Dark green background elements
        onSurface = Color(0xFF212121), // Dark grey or black standard text
        surfaceVariant = Color(0xFFE8F5E9), // Soft, low-contrast sage green background for container cards
        onSurfaceVariant = Color(0xFF0D533A), // Dark-green typography for container cards
        outline = Color(0xFFC8E6C9)
      )
    }
    else -> {
      when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          val context = LocalContext.current
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
      }
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
