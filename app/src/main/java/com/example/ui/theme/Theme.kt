package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FitnessColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    secondary = SecondaryNeonGreen,
    tertiary = TertiaryElectricBlue,
    background = DarkBackground,
    surface = CardSurface,
    surfaceVariant = HighlightSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite,
    outline = BorderSlate
)

private val LightBackupColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    secondary = SecondaryNeonGreen,
    tertiary = TertiaryElectricBlue,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFECEFF1),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFF121212),
    onSurface = Color(0xFF121212),
    outline = Color(0xFFCFD8DC)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We enforce our highly customized Dark Theme to give a continuous, premium fitness tracker experience
    forceDark: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (forceDark || darkTheme) {
        FitnessColorScheme
    } else {
        LightBackupColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
