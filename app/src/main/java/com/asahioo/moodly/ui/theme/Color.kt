package com.asahioo.moodly.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.asahioo.moodly.data.model.AppTheme

/*
 * Paleta de la app. Los tokens de tema leen [theme], un estado de Compose:
 * quien los use en composición o en dibujo se actualiza solo al cambiar de tema.
 * No los guardes en un `val` de archivo ni en un `remember` sin clave: se quedarían con el tema viejo.
 */
object Palette {
    // ponytail: estado global; cambiar de tema recompone la pantalla visible. Pasar a CompositionLocal si estorba.
    var theme by mutableStateOf(AppTheme.Classic)

    private val c get() = colorsFor(theme)

    val Ink get() = c.ink
    val Paper get() = c.paper
    val Mist get() = c.mist
    val Mist2 get() = c.mist2
    val Future get() = c.future
    val Track get() = c.track
    val Grey get() = c.grey
    val Grey2 get() = c.grey2
    val Divider get() = c.divider
    val IsDark get() = c.paper.luminance() < 0.5f
    val TabIdle get() = c.tabIdle
    val SwitchOff get() = c.switchOff

    val Peach get() = c.peach
    val PeachInk get() = c.peachInk
    val PeachLite get() = c.peachLite
    val Lavender get() = c.lavender
    val LavenderInk get() = c.lavenderInk
    val Lime get() = c.lime
    val LimeInk get() = c.limeInk

    // Fijos en todos los temas.
    /** Trazo de las caras: debe coincidir con `res/drawable/ic_mood_*.xml`. */
    val FaceInk = Color(0xFF141414)
    val Night = Color(0xFF0D0D0D)
    val Night2 = Color(0xFF262626)
    val Danger = Color(0xFFE0443A)
    val DangerFill = Color(0xFFE6453B)

    // Deben coincidir con res/drawable/ic_mood_*.xml y widget_bg_*.xml.
    val MoodHappy = Color(0xFFFFB84D)
    val MoodAngry = Color(0xFFF2766B)
    val MoodSleepy = Color(0xFFA3A8F5)
    val MoodBored = Color(0xFFD4BFA0)
    val MoodCalm = Color(0xFF7FD1B9)
    val MoodStressed = Color(0xFFE39AC9)

    val Scrim = Color(0x6B08080C)
}
