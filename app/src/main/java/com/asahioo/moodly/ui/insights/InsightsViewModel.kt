package com.asahioo.moodly.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asahioo.moodly.data.model.StressLevel
import com.asahioo.moodly.data.repository.MoodRepository
import com.asahioo.moodly.domain.DateProvider
import com.asahioo.moodly.domain.MonthSummary
import com.asahioo.moodly.domain.MoodPatterns
import com.asahioo.moodly.domain.MoodStats
import com.asahioo.moodly.domain.PatternsResult
import com.asahioo.moodly.ui.calendar.DayCell
import com.asahioo.moodly.ui.containerViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class InsightsUiState(
    val summary: MonthSummary,
    val lastWeek: List<DayCell>,
    val sleepMinutes: Int?,
    val stress: StressLevel?,
    val patterns: PatternsResult = PatternsResult.Empty,
)

class InsightsViewModel(
    repository: MoodRepository,
    private val dates: DateProvider,
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = repository.data
        .map { d ->
            val today = dates.today()
            InsightsUiState(
                summary = MoodStats.month(d.moods, YearMonth.from(today), today),
                lastWeek = (6 downTo 0).map { back ->
                    val date = today.minusDays(back.toLong())
                    DayCell(date, d.moods[date.toString()], isFuture = false, isToday = back == 0)
                },
                sleepMinutes = d.days[today.toString()]?.sleepMinutes,
                stress = d.stress,
                patterns = MoodPatterns.find(d, today),
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            InsightsUiState(
                summary = MonthSummary.empty(YearMonth.from(dates.today())),
                lastWeek = emptyList(),
                sleepMinutes = null,
                stress = null,
            ),
        )

    companion object {
        val Factory = containerViewModelFactory<InsightsViewModel> { InsightsViewModel(it.repository, it.dateProvider) }
    }
}
