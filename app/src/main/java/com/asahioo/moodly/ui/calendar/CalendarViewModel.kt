package com.asahioo.moodly.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asahioo.moodly.data.model.DayContext
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.data.model.PresetTag
import com.asahioo.moodly.data.repository.MoodRepository
import com.asahioo.moodly.domain.DateProvider
import com.asahioo.moodly.domain.MonthSummary
import com.asahioo.moodly.domain.MoodStats
import com.asahioo.moodly.ui.containerViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class DayCell(
    val date: LocalDate,
    val mood: Mood?,
    val isFuture: Boolean,
    val isToday: Boolean,
    /** Tiene nota o etiquetas. */
    val hasContext: Boolean = false,
)

data class MonthOption(val month: YearMonth, val topMoods: List<Mood>)

data class CalendarUiState(
    val month: YearMonth,
    /** Celdas vacías antes del día 1 (semana inicia en domingo). */
    val leadingBlanks: Int,
    val days: List<DayCell>,
    val summary: MonthSummary,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
)

class CalendarViewModel(
    private val repository: MoodRepository,
    private val dates: DateProvider,
) : ViewModel() {

    private val currentMonth: YearMonth get() = YearMonth.from(dates.today())
    private val firstMonth: YearMonth get() = currentMonth.minusMonths(MONTHS_BACK)

    private val visible = MutableStateFlow(currentMonth)

    val uiState: StateFlow<CalendarUiState> = combine(repository.data, visible) { data, month ->
        build(data.moods, data.days, month)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), build(emptyMap(), emptyMap(), visible.value))

    val monthOptions: StateFlow<List<MonthOption>> = repository.data
        .map { data ->
            val today = dates.today()
            (0..MONTHS_BACK).map { back ->
                val month = currentMonth.minusMonths(back)
                val summary = MoodStats.month(data.moods, month, today)
                MonthOption(
                    month = month,
                    topMoods = summary.counts.filterValues { it > 0 }
                        .entries.sortedByDescending { it.value }.take(3).map { it.key },
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Cambia de mes; devuelve false si ya está en el límite. */
    fun step(delta: Int): Boolean {
        val target = visible.value.plusMonths(delta.toLong())
        if (target.isBefore(firstMonth) || target.isAfter(currentMonth)) return false
        visible.value = target
        return true
    }

    fun show(month: YearMonth) {
        if (!month.isBefore(firstMonth) && !month.isAfter(currentMonth)) visible.value = month
    }

    fun setMood(date: LocalDate, mood: Mood?) {
        if (date.isAfter(dates.today())) return
        viewModelScope.launch { repository.setMood(date, mood) }
    }

    fun saveDay(date: LocalDate, mood: Mood, note: String, tags: Set<PresetTag>, customTags: Set<String>) {
        if (date.isAfter(dates.today())) return
        viewModelScope.launch { repository.saveDay(date, mood, note, tags, customTags) }
    }

    private fun build(moods: Map<String, Mood>, contexts: Map<String, DayContext>, month: YearMonth): CalendarUiState {
        val today = dates.today()
        val days = (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day)
            DayCell(
                date = date,
                mood = moods[date.toString()],
                isFuture = date.isAfter(today),
                isToday = date == today,
                hasContext = contexts[date.toString()]?.hasEntry == true,
            )
        }
        return CalendarUiState(
            month = month,
            leadingBlanks = month.atDay(1).dayOfWeek.value % 7,
            days = days,
            summary = MoodStats.month(moods, month, today),
            canGoBack = month.isAfter(firstMonth),
            canGoForward = month.isBefore(currentMonth),
        )
    }

    companion object {
        const val MONTHS_BACK = 3L
        val Factory = containerViewModelFactory<CalendarViewModel> { CalendarViewModel(it.repository, it.dateProvider) }
    }
}
