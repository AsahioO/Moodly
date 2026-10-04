package com.asahioo.moodly.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.asahioo.moodly.data.model.AppTheme

/**
 * Los tokens de [Palette] que cambian con el tema. Ánimos, Danger, Scrim, Night y el trazo de las
 * caras son fijos y no están aquí.
 */
@Immutable
data class ThemeColors(
    val ink: Color,
    val paper: Color,
    val mist: Color,
    val mist2: Color,
    val future: Color,
    val track: Color,
    val grey: Color,
    val grey2: Color,
    val divider: Color,
    val tabIdle: Color,
    val switchOff: Color,
    val peach: Color,
    val peachInk: Color,
    val peachLite: Color,
    val lavender: Color,
    val lavenderInk: Color,
    val lime: Color,
    val limeInk: Color,
)

/** El diseño original, con los hex de siempre. */
private val Classic = ThemeColors(
    ink = Color(0xFF141414),
    paper = Color(0xFFFFFFFF),
    mist = Color(0xFFF3F3F4),
    mist2 = Color(0xFFECECEE),
    future = Color(0xFFF7F7F8),
    track = Color(0xFFE4E4E7),
    grey = Color(0xFF9A9A9F),
    grey2 = Color(0xFF6E6E73),
    divider = Color(0xFFE3E3E6),
    tabIdle = Color(0xFF808085),
    switchOff = Color(0xFFD5D5D9),
    peach = Color(0xFFF7A984),
    peachInk = Color(0xFFE47F52),
    peachLite = Color(0xFFFBC6A9),
    lavender = Color(0xFFDDBDF8),
    lavenderInk = Color(0xFFA477E4),
    lime = Color(0xFFA6EB8C),
    limeInk = Color(0xFF3F6B30),
)

/**
 * Un tema se define con seis colores; grises, pistas y tintas salen de mezclarlos, así todos
 * comparten las mismas proporciones. [a1]/[a2]/[a3] ocupan el lugar de lima, durazno y lavanda.
 */
private fun derived(paper: Long, mist: Long, ink: Long, a1: Long, a2: Long, a3: Long): ThemeColors {
    val p = Color(paper)
    val m = Color(mist)
    val i = Color(ink)
    val c1 = Color(a1)
    val c2 = Color(a2)
    val c3 = Color(a3)
    return ThemeColors(
        ink = i,
        paper = p,
        mist = m,
        mist2 = lerp(m, i, 0.05f),
        future = lerp(p, m, 0.5f),
        track = lerp(m, i, 0.10f),
        grey = lerp(p, i, 0.43f),
        grey2 = lerp(p, i, 0.62f),
        divider = lerp(m, i, 0.09f),
        tabIdle = lerp(p, i, 0.54f),
        switchOff = lerp(m, i, 0.17f),
        peach = c2,
        peachInk = lerp(c2, i, 0.15f),
        peachLite = lerp(c2, p, 0.4f),
        lavender = c3,
        lavenderInk = lerp(c3, i, 0.25f),
        lime = c1,
        limeInk = lerp(c1, i, 0.7f),
    )
}

private val Honey = derived(0xFFFFFBF2, 0xFFF7EFDF, 0xFF2B2118, 0xFFF3CE7C, 0xFFEDB08A, 0xFFD6CDA0)
private val Clay = derived(0xFFFFF8F3, 0xFFF6E8DE, 0xFF2E1D17, 0xFFE7A58E, 0xFFF0C29A, 0xFFC9A9A0)
private val Sea = derived(0xFFF6FBF9, 0xFFE6F1EE, 0xFF182725, 0xFFA9D8CF, 0xFFB8D3E6, 0xFFC8DDB4)
private val Rose = derived(0xFFFFF8F8, 0xFFF7E9EA, 0xFF2D1E22, 0xFFEBB8C3, 0xFFF0C6B4, 0xFFC9BBD3)
private val Oat = derived(0xFFFAF6EF, 0xFFEFE7DA, 0xFF2A2420, 0xFFCDB89A, 0xFFD9A78C, 0xFFB9C2A5)

// Oscuros tenues: fondo carbón medio (no negro), tinta crema (~11:1) y acentos apagados que son
// fondo de tarjeta con texto Ink encima (≥6:1).
private val Ember = derived(0xFF2F2925, 0xFF3A332E, 0xFFF0E6DA, 0xFF47503A, 0xFF684A3C, 0xFF574553)
private val Slate = derived(0xFF2B3038, 0xFF353B44, 0xFFE8ECF1, 0xFF38524A, 0xFF6A4A40, 0xFF4A4766)
private val Moss = derived(0xFF2A2F29, 0xFF343A32, 0xFFE9EBDF, 0xFF3F5032, 0xFF6B5236, 0xFF48504F)
private val Plum = derived(0xFF302A33, 0xFF3A333E, 0xFFF0E6EE, 0xFF414D3A, 0xFF6E4A48, 0xFF5A4870)

fun colorsFor(theme: AppTheme): ThemeColors = when (theme) {
    AppTheme.Classic -> Classic
    AppTheme.Honey -> Honey
    AppTheme.Clay -> Clay
    AppTheme.Sea -> Sea
    AppTheme.Rose -> Rose
    AppTheme.Oat -> Oat
    AppTheme.Ember -> Ember
    AppTheme.Slate -> Slate
    AppTheme.Moss -> Moss
    AppTheme.Plum -> Plum
}
