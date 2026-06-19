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

enum class AppThemeColor {
    GREEN, LAVENDER, PASTEL_BLUE, PINK, RED, YELLOW, MINT, PEACH, PURPLE
}

private fun createLightColorScheme(primaryColor: Color) = lightColorScheme(
    primary = primaryColor,
    onPrimary = Color.White,
    primaryContainer = GptLightMsgBg,
    onPrimaryContainer = GptTextLight,
    background = GptLightBg,
    surface = GptLightSurface,
    onBackground = GptTextLight,
    onSurface = GptTextLight,
    surfaceVariant = GptLightMsgBg,
    onSurfaceVariant = GptTextLight,
    secondary = GptTextSecondaryLight,
    outline = GptBorder
)

private fun createDarkColorScheme(primaryColor: Color) = darkColorScheme(
    primary = primaryColor,
    onPrimary = Color.White,
    primaryContainer = GptDarkMsgBg,
    onPrimaryContainer = GptTextDark,
    background = GptDarkBg,
    surface = GptDarkSurface,
    onBackground = GptTextDark,
    onSurface = GptTextDark,
    surfaceVariant = GptDarkMsgBg,
    onSurfaceVariant = GptTextDark,
    secondary = GptTextSecondaryDark,
    outline = GptDarkBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: AppThemeColor = AppThemeColor.GREEN,
    content: @Composable () -> Unit
) {
    val primaryColor = when (themeColor) {
        AppThemeColor.GREEN -> GptGreen
        AppThemeColor.LAVENDER -> LavenderPrimary
        AppThemeColor.PASTEL_BLUE -> PastelBluePrimary
        AppThemeColor.PINK -> PinkPrimary
        AppThemeColor.RED -> RedPrimary
        AppThemeColor.YELLOW -> YellowPrimary
        AppThemeColor.MINT -> MintPrimary
        AppThemeColor.PEACH -> PeachPrimary
        AppThemeColor.PURPLE -> PurplePrimary
    }

    val colorScheme = if (darkTheme) createDarkColorScheme(primaryColor) else createLightColorScheme(primaryColor)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
