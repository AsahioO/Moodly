package com.asahioo.moodly.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.asahioo.moodly.R

/** Inter Tight (OFL) en cuatro pesos estáticos y recortados a latín para pesar poco. */
val InterTight = FontFamily(
    Font(R.font.inter_tight_regular, FontWeight.Normal),
    Font(R.font.inter_tight_medium, FontWeight.Medium),
    Font(R.font.inter_tight_semibold, FontWeight.SemiBold),
    Font(R.font.inter_tight_bold, FontWeight.Bold),
)

private val tightLines = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

/** Escala tipográfica de la app. */
object MoodType {
    // Sin color: lo toma de LocalContentColor, que MoodlyTheme ata al tema vigente.
    val Base = TextStyle(fontFamily = InterTight, fontSize = 14.sp)

    val Display = Base.copy(
        fontSize = 43.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.035).em,
        lineHeight = 45.sp, lineHeightStyle = tightLines,
    )
    val Greeting = Base.copy(
        fontSize = 29.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.025).em,
        lineHeight = 34.sp, lineHeightStyle = tightLines,
    )
    val ScreenTitle = Base.copy(
        fontSize = 27.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.03).em,
        lineHeight = 32.sp, lineHeightStyle = tightLines,
    )
    val Big = Base.copy(fontSize = 34.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.04).em)
    val BigUnit = Base.copy(fontSize = 21.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.02).em)
    val SummaryTitle = Base.copy(fontSize = 36.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.04).em)
    val SheetTitle = Base.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.025).em)
    val Question = Base.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.015).em, lineHeight = 21.sp)
    val Title = Base.copy(fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em)
    val Name = Base.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em)
    val Stat = Base.copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em)
    val Button = Base.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    val Body = Base.copy(fontSize = 13.5.sp, lineHeight = 19.sp)
    val Label = Base.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
    val Chip = Base.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val Section = Base.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    // Getters: el gris cambia con el tema.
    val Caption get() = Base.copy(fontSize = 12.sp, color = Palette.Grey)
    val Small = Base.copy(fontSize = 11.5.sp, lineHeight = 15.sp)
    val Tiny get() = Base.copy(fontSize = 10.5.sp, color = Palette.Grey)
}
