package com.asahioo.moodly.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.asahioo.moodly.data.model.AppTheme

private fun colorScheme(c: ThemeColors) = (if (c.paper.luminance() < 0.5f) darkColorScheme() else lightColorScheme()).copy(
    primary = c.ink,
    onPrimary = c.paper,
    secondary = c.lime,
    onSecondary = c.ink,
    background = c.paper,
    onBackground = c.ink,
    surface = c.paper,
    onSurface = c.ink,
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

/**
 * Los temas oscuros usan darkColorScheme. [theme] se publica en [Palette] antes de dibujar el contenido; por
 * defecto queda el vigente (p. ej. otra Activity que no conoce los ajustes).
 */
@Composable
fun MoodlyTheme(theme: AppTheme = Palette.theme, content: @Composable () -> Unit) {
    Palette.theme = theme
    val colors = remember(theme) { colorScheme(colorsFor(theme)) }
    MaterialTheme(colorScheme = colors, typography = typography) {
        CompositionLocalProvider(
            LocalTextStyle provides MoodType.Base,
            LocalContentColor provides Palette.Ink,
            content = content,
        )
    }
}
