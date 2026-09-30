package com.asahioo.moodly.ui

import androidx.annotation.StringRes
import com.asahioo.moodly.R
import com.asahioo.moodly.ui.components.AppIcons
import com.asahioo.moodly.ui.components.GlyphSpec
import java.time.LocalDate

enum class Tab(@StringRes val label: Int, val icon: GlyphSpec) {
    Home(R.string.tab_home, AppIcons.Home),
    Insights(R.string.tab_insights, AppIcons.Pie),
    Calendar(R.string.tab_calendar, AppIcons.Calendar),
    Settings(R.string.tab_settings, AppIcons.Grid),
}

/** Hojas inferiores que puede mostrar la app. */
sealed interface SheetRequest {
    data class Day(val date: LocalDate) : SheetRequest
    data object Sleep : SheetRequest
    data object Stress : SheetRequest
    data object MonthPicker : SheetRequest
    data object Reminder : SheetRequest
    data object Reset : SheetRequest
    /** El texto del archivo elegido queda pendiente hasta que el usuario confirme. */
    data class Import(val text: String) : SheetRequest
    data object Tags : SheetRequest
    data object Theme : SheetRequest
}
