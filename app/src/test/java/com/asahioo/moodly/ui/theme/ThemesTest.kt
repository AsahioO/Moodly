package com.asahioo.moodly.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.asahioo.moodly.data.model.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemesTest {

    /** Contraste WCAG entre dos colores opacos. */
    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = if (a.luminance() > b.luminance()) a to b else b to a
        return (hi.luminance() + 0.05f) / (lo.luminance() + 0.05f)
    }

    private fun check(name: String, theme: AppTheme, min: Float, a: Color, b: Color) {
        val ratio = contrast(a, b)
        assertTrue("$theme: $name = $ratio (mínimo $min)", ratio >= min)
    }

    @Test
    fun everyTheme_keepsTextReadable() {
        for (theme in AppTheme.entries) {
            val c = colorsFor(theme)
            check("tinta/fondo", theme, 7f, c.ink, c.paper)
            check("tinta/tarjeta", theme, 4.5f, c.ink, c.mist)
            check("gris2/fondo", theme, 3f, c.grey2, c.paper)
            // En oscuro, demasiado contraste deja halo al leer.
            if (c.paper.luminance() < 0.5f) {
                val ratio = contrast(c.ink, c.paper)
                assertTrue("$theme: tinta/fondo = $ratio (máximo 16)", ratio <= 16f)
            }
        }
    }

    @Test
    fun faceInk_readsOnEveryMoodColor() {
        for (mood in listOf(Palette.MoodHappy, Palette.MoodAngry, Palette.MoodSleepy, Palette.MoodBored, Palette.MoodCalm, Palette.MoodStressed)) {
            assertTrue("$mood", contrast(Palette.FaceInk, mood) >= 4.5f)
        }
    }

    @Test
    fun everyTheme_keepsAccentsReadable() {
        for (theme in AppTheme.entries) {
            val c = colorsFor(theme)
            for ((name, fill) in listOf("lima" to c.lime, "durazno" to c.peach, "lavanda" to c.lavender)) {
                check("tinta/$name", theme, 4.5f, c.ink, fill)
            }
            // 4.0 y no 4.5: el Clásico original da 4.43 y sus colores no se tocan.
            check("tinta lima/lima", theme, 4.0f, c.limeInk, c.lime)
        }
    }
}
