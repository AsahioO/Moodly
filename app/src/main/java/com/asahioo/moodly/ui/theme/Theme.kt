package com.asahioo.moodly.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val colors = lightColorScheme(
    primary = Palette.Ink,
    onPrimary = Palette.Paper,
    secondary = Palette.Lime,
    onSecondary = Palette.Ink,
    background = Palette.Paper,
    onBackground = Palette.Ink,
    surface = Palette.Paper,
    onSurface = Palette.Ink,
)

private val typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = InterTight),
        headlineLarge = headlineLarge.copy(fontFamily = InterTight),
        headlineMedium = headlineMedium.copy(fontFamily = InterTight),
        titleLarge = titleLarge.copy(fontFamily = InterTight),
        titleMedium = titleMedium.copy(fontFamily = InterTight),
        bodyLarge = bodyLarge.copy(fontFamily = InterTight),
        bodyMedium = bodyMedium.copy(fontFamily = InterTight),
        bodySmall = bodySmall.copy(fontFamily = InterTight),
        labelLarge = labelLarge.copy(fontFamily = InterTight),
        labelMedium = labelMedium.copy(fontFamily = InterTight),
        labelSmall = labelSmall.copy(fontFamily = InterTight),
    )
}

/** La app replica un diseño claro fijo, así que solo existe el tema claro. */
@Composable
fun MoodlyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography) {
        CompositionLocalProvider(
            LocalTextStyle provides MoodType.Base,
            LocalContentColor provides Palette.Ink,
            content = content,
        )
    }
}
