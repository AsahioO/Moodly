package com.asahioo.moodly.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asahioo.moodly.data.model.DayContext
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.data.model.StressLevel
import com.asahioo.moodly.data.repository.MoodRepository
import com.asahioo.moodly.domain.DateProvider
import com.asahioo.moodly.domain.StressQuiz
import com.asahioo.moodly.ui.containerViewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val today: LocalDate,
    val userName: String = "",
    val todayMood: Mood? = null,
    val todayContext: DayContext? = null,
    /** Etiquetas propias por id, para mostrar el resumen del contexto de hoy. */
    val customTagLabels: Map<String, String> = emptyMap(),
    val stress: StressLevel? = null,
    val quizIndex: Int = 0,
) {
    val quizTotal: Int get() = StressQuiz.size
    val quizDone: Boolean get() = quizIndex >= quizTotal
    val firstName: String get() = userName.substringBefore(' ')
    val sleepMinutes: Int? get() = todayContext?.sleepMinutes
}

sealed interface HomeEvent {
    data class QuizCompleted(val level: StressLevel) : HomeEvent
}

class HomeViewModel(
    private val repository: MoodRepository,
    private val dates: DateProvider,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.data
        .map { d ->
            val today = dates.today()
            HomeUiState(
                today = today,
                userName = d.userName,
                todayMood = d.moods[today.toString()],
                todayContext = d.days[today.toString()],
                customTagLabels = d.customTags.associate { it.id to it.label },
                stress = d.stress,
                quizIndex = d.quiz.index,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(dates.today()))

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    fun logTodayMood(mood: Mood) {
        viewModelScope.launch { repository.setMood(dates.today(), mood) }
    }

    fun answerQuiz(yes: Boolean) {
        viewModelScope.launch {
            repository.answerQuiz(yes)?.let { _events.send(HomeEvent.QuizCompleted(it)) }
        }
    }

    fun restartQuiz() {
        viewModelScope.launch { repository.restartQuiz() }
    }

    fun setSleep(minutes: Int) {
        viewModelScope.launch { repository.setSleep(dates.today(), minutes) }
    }

    companion object {
        val Factory = containerViewModelFactory<HomeViewModel> { HomeViewModel(it.repository, it.dateProvider) }
    }
}
