package com.asahioo.moodly.ui

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.asahioo.moodly.R
import com.asahioo.moodly.data.model.AppTheme
import com.asahioo.moodly.data.model.DayPart
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.data.model.PresetTag
import com.asahioo.moodly.data.model.StressLevel
import com.asahioo.moodly.domain.Factor
import com.asahioo.moodly.domain.Magnitude
import com.asahioo.moodly.domain.MoodGroup
import com.asahioo.moodly.domain.Pattern
import com.asahioo.moodly.ui.theme.Palette
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/* Mapeos de modelos a recursos de UI: mantienen la capa de datos libre de Android. */

val Mood.color: Color
    get() = when (this) {
        Mood.HAPPY -> Palette.MoodHappy
        Mood.ANGRY -> Palette.MoodAngry
        Mood.SLEEPY -> Palette.MoodSleepy
        Mood.BORED -> Palette.MoodBored
        Mood.CALM -> Palette.MoodCalm
        Mood.STRESSED -> Palette.MoodStressed
    }

@get:StringRes
val Mood.labelRes: Int
    get() = when (this) {
        Mood.HAPPY -> R.string.mood_happy
        Mood.ANGRY -> R.string.mood_angry
        Mood.SLEEPY -> R.string.mood_sleepy
        Mood.BORED -> R.string.mood_bored
        Mood.CALM -> R.string.mood_calm
        Mood.STRESSED -> R.string.mood_stressed
    }

@get:StringRes
val Mood.messageRes: Int
    get() = when (this) {
        Mood.HAPPY -> R.string.mood_msg_happy
        Mood.ANGRY -> R.string.mood_msg_angry
        Mood.SLEEPY -> R.string.mood_msg_sleepy
        Mood.BORED -> R.string.mood_msg_bored
        Mood.CALM -> R.string.mood_msg_calm
        Mood.STRESSED -> R.string.mood_msg_stressed
    }

@get:StringRes
val AppTheme.labelRes: Int
    get() = when (this) {
        AppTheme.Classic -> R.string.theme_classic
        AppTheme.Honey -> R.string.theme_honey
        AppTheme.Clay -> R.string.theme_clay
        AppTheme.Sea -> R.string.theme_sea
        AppTheme.Rose -> R.string.theme_rose
        AppTheme.Oat -> R.string.theme_oat
        AppTheme.Ember -> R.string.theme_ember
        AppTheme.Slate -> R.string.theme_slate
        AppTheme.Moss -> R.string.theme_moss
        AppTheme.Plum -> R.string.theme_plum
    }

@get:StringRes
val DayPart.labelRes: Int
    get() = when (this) {
        DayPart.MORNING -> R.string.part_morning
        DayPart.AFTERNOON -> R.string.part_afternoon
        DayPart.NIGHT -> R.string.part_night
    }

@get:StringRes
val StressLevel.labelRes: Int
    get() = when (this) {
        StressLevel.LOW -> R.string.stress_low
        StressLevel.MEDIUM -> R.string.stress_medium
        StressLevel.HIGH -> R.string.stress_high
    }

@get:StringRes
val StressLevel.textRes: Int
    get() = when (this) {
        StressLevel.LOW -> R.string.stress_text_low
        StressLevel.MEDIUM -> R.string.stress_text_medium
        StressLevel.HIGH -> R.string.stress_text_high
    }

val StressLevel.tipsRes: List<Int>
    get() = when (this) {
        StressLevel.LOW -> listOf(R.string.stress_tip_low_1, R.string.stress_tip_low_2, R.string.stress_tip_low_3)
        StressLevel.MEDIUM -> listOf(R.string.stress_tip_medium_1, R.string.stress_tip_medium_2, R.string.stress_tip_medium_3)
        StressLevel.HIGH -> listOf(R.string.stress_tip_high_1, R.string.stress_tip_high_2, R.string.stress_tip_high_3)
    }

/** Altura relativa de las barras del indicador de estrés. */
val StressLevel.barFactor: Float
    get() = when (this) {
        StressLevel.LOW -> 0.42f
        StressLevel.MEDIUM -> 0.7f
        StressLevel.HIGH -> 1f
    }

@get:StringRes
val PresetTag.labelRes: Int
    get() = when (this) {
        PresetTag.WORK -> R.string.tag_work
        PresetTag.FAMILY -> R.string.tag_family
        PresetTag.FRIENDS -> R.string.tag_friends
        PresetTag.EXERCISE -> R.string.tag_exercise
        PresetTag.POOR_SLEEP -> R.string.tag_poor_sleep
        PresetTag.STUDY -> R.string.tag_study
        PresetTag.HEALTH -> R.string.tag_health
        PresetTag.REST -> R.string.tag_rest
    }

/** Inicio de la frase de un patrón: "Los días que haces ejercicio". */
@get:StringRes
private val PresetTag.whenRes: Int
    get() = when (this) {
        PresetTag.WORK -> R.string.pattern_when_work
        PresetTag.FAMILY -> R.string.pattern_when_family
        PresetTag.FRIENDS -> R.string.pattern_when_friends
        PresetTag.EXERCISE -> R.string.pattern_when_exercise
        PresetTag.POOR_SLEEP -> R.string.pattern_when_poor_sleep
        PresetTag.STUDY -> R.string.pattern_when_study
        PresetTag.HEALTH -> R.string.pattern_when_health
        PresetTag.REST -> R.string.pattern_when_rest
    }

@get:StringRes
private val MoodGroup.labelRes: Int
    get() = when (this) {
        MoodGroup.TENSE -> R.string.mood_group_tense
        MoodGroup.GOOD -> R.string.mood_group_good
        MoodGroup.LOW -> R.string.mood_group_low
    }

@get:StringRes
private val Magnitude.labelRes: Int
    get() = when (this) {
        Magnitude.TRIPLE -> R.string.pattern_mag_triple
        Magnitude.DOUBLE -> R.string.pattern_mag_double
        Magnitude.MORE -> R.string.pattern_mag_more
        Magnitude.HALF -> R.string.pattern_mag_half
        Magnitude.LESS -> R.string.pattern_mag_less
    }

/** "Los días que duermes menos de 6 h registras Enojado o Estresado el doble de veces." */
@Composable
fun Pattern.sentence(): String {
    val condition = when (factor) {
        is Factor.Preset -> stringResource(factor.tag.whenRes)
        is Factor.Custom -> stringResource(R.string.pattern_when_custom, factor.tag.label)
        Factor.ShortSleep -> stringResource(R.string.pattern_when_short_sleep)
        Factor.FewSteps -> stringResource(R.string.pattern_when_few_steps)
    }
    return stringResource(R.string.pattern_sentence, condition, stringResource(group.labelRes), stringResource(magnitude.labelRes))
}

val QuizQuestions: List<Int> = listOf(
    R.string.quiz_q1, R.string.quiz_q2, R.string.quiz_q3, R.string.quiz_q4,
    R.string.quiz_q5, R.string.quiz_q6, R.string.quiz_q7, R.string.quiz_q8,
)

val SpanishLocale: Locale = Locale.forLanguageTag("es-MX")

fun String.capitalizedEs(): String = replaceFirstChar { it.titlecase(SpanishLocale) }

/** Índice 0 = domingo. */
fun LocalDate.sundayIndex(): Int = dayOfWeek.value % 7

@Composable
@ReadOnlyComposable
fun monthName(month: Int): String = stringArrayResource(R.array.months)[month - 1]

@Composable
@ReadOnlyComposable
fun monthShort(month: Int): String = stringArrayResource(R.array.months_short)[month - 1]

/** "26 sep 2026" */
@Composable
@ReadOnlyComposable
fun shortDate(date: LocalDate): String = "${date.dayOfMonth} ${monthShort(date.monthValue)} ${date.year}"

/** "Septiembre, 2026" */
@Composable
@ReadOnlyComposable
fun monthYear(month: YearMonth): String = "${monthName(month.monthValue).capitalizedEs()}, ${month.year}"

@Composable
@ReadOnlyComposable
fun weekdayFull(date: LocalDate): String = stringArrayResource(R.array.weekdays_full)[date.sundayIndex()]

/** "9:00 p.m." o "21:00" según el formato de hora del sistema. */
fun formatTimeOfDay(minutesOfDay: Int, is24Hour: Boolean): String =
    LocalTime.of(minutesOfDay / 60, minutesOfDay % 60)
        .format(DateTimeFormatter.ofPattern(if (is24Hour) "H:mm" else "h:mm a", SpanishLocale))

@Composable
@ReadOnlyComposable
fun timeOfDay(minutesOfDay: Int): String =
    formatTimeOfDay(minutesOfDay, DateFormat.is24HourFormat(LocalContext.current))
