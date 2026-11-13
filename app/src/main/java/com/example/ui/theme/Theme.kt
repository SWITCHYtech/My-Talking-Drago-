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

private val DarkColorScheme = darkColorScheme(
    primary = SkyAccent,
    onPrimary = Color.White,
    primaryContainer = CobaltDark,
    onPrimaryContainer = Color.White,
    secondary = AmberGold,
    onSecondary = DeepSlate,
    tertiary = MintGreen,
    background = NightSurface,
    surface = NightSurface,
    surfaceVariant = SoftCardDark,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = CobaltDark,
    secondary = AmberGold,
    onSecondary = DeepSlate,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = MintGreen,
    onTertiary = Color.White,
    background = CreamSurface,
    surface = CreamSurface,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = DeepSlate,
    onSurface = DeepSlate,
    onSurfaceVariant = SlateMuted
)

@Composable
fun DragosPetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
