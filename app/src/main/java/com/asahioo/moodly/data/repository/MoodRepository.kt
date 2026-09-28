package com.asahioo.moodly.data.repository

import com.asahioo.moodly.data.local.MoodLocalDataSource
import com.asahioo.moodly.data.model.AppData
import com.asahioo.moodly.data.model.CustomTag
import com.asahioo.moodly.data.model.DayContext
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.data.model.PresetTag
import com.asahioo.moodly.data.model.QuizProgress
import com.asahioo.moodly.data.model.Settings
import com.asahioo.moodly.data.model.StressLevel
import com.asahioo.moodly.domain.StressQuiz
import com.asahioo.moodly.domain.normalizeTagLabel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.util.UUID

/** Única fuente de verdad para la UI. Los ViewModels solo hablan con esta interfaz. */
interface MoodRepository {
    val data: Flow<AppData>

    /** Con [mood] null borra el registro del día (ánimo, nota y etiquetas; conserva el sueño). */
    suspend fun setMood(date: LocalDate, mood: Mood?)

    /** Guarda ánimo, nota y etiquetas del día en una sola escritura. */
    suspend fun saveDay(date: LocalDate, mood: Mood, note: String, tags: Set<PresetTag>, customTags: Set<String>)
    suspend fun setSleep(date: LocalDate, minutes: Int)

    /** Devuelve el id de la etiqueta (la existente si ya hay una igual) o null si no es válida o no cabe. */
    suspend fun addCustomTag(label: String): String?
    suspend fun renameCustomTag(id: String, label: String)

    /** Borra la etiqueta y la quita de todos los días. */
    suspend fun deleteCustomTag(id: String)

    /** Registra una respuesta; si fue la última, devuelve el nuevo nivel de estrés. */
    suspend fun answerQuiz(answer: Boolean): StressLevel?
    suspend fun restartQuiz()
    suspend fun setOnboarded(value: Boolean)

    /** Guarda el nombre (recortado) y los ajustes elegidos en el onboarding, y lo marca como visto. */
    suspend fun completeOnboarding(name: String, settings: (Settings) -> Settings)
    suspend fun updateSettings(transform: (Settings) -> Settings)

    /** Borra todo: registros, nombre y ajustes. La app vuelve al onboarding. */
    suspend fun resetAll()
}

class DefaultMoodRepository(private val local: MoodLocalDataSource) : MoodRepository {

    override val data: Flow<AppData> = local.data.distinctUntilChanged()

    override suspend fun setMood(date: LocalDate, mood: Mood?) {
        val key = date.toString()
        local.update { d ->
            if (mood != null) return@update d.copy(moods = d.moods + (key to mood))
            val sleep = d.days[key]?.sleepMinutes
            d.copy(moods = d.moods - key, days = d.days.with(key, DayContext(sleepMinutes = sleep)))
        }
    }

    override suspend fun saveDay(
        date: LocalDate,
        mood: Mood,
        note: String,
        tags: Set<PresetTag>,
        customTags: Set<String>,
    ) {
        val key = date.toString()
        local.update { d ->
            val known = d.customTags.mapTo(HashSet()) { it.id }
            val context = (d.days[key] ?: DayContext()).copy(
                note = note.trim().take(AppData.MAX_NOTE_LENGTH),
                tags = tags,
                customTags = customTags intersect known,
            )
            d.copy(moods = d.moods + (key to mood), days = d.days.with(key, context))
        }
    }

    override suspend fun setSleep(date: LocalDate, minutes: Int) {
        val key = date.toString()
        val clamped = minutes.coerceIn(AppData.MIN_SLEEP_MINUTES, AppData.MAX_SLEEP_MINUTES)
        local.update { d -> d.copy(days = d.days.with(key, (d.days[key] ?: DayContext()).copy(sleepMinutes = clamped))) }
    }

    override suspend fun addCustomTag(label: String): String? {
        val clean = normalizeTagLabel(label)
        if (clean.isEmpty()) return null
        var id: String? = null
        local.update { d ->
            val existing = d.customTags.firstOrNull { it.label.equals(clean, ignoreCase = true) }
            when {
                existing != null -> d.also { id = existing.id }
                d.customTags.size >= AppData.MAX_CUSTOM_TAGS -> d
                else -> {
                    val tag = CustomTag(UUID.randomUUID().toString(), clean)
                    id = tag.id
                    d.copy(customTags = d.customTags + tag)
                }
            }
        }
        return id
    }

    override suspend fun renameCustomTag(id: String, label: String) {
        val clean = normalizeTagLabel(label)
        if (clean.isEmpty()) return
        local.update { d ->
            if (d.customTags.any { it.id != id && it.label.equals(clean, ignoreCase = true) }) return@update d
            d.copy(customTags = d.customTags.map { if (it.id == id) it.copy(label = clean) else it })
        }
    }

    override suspend fun deleteCustomTag(id: String) {
        local.update { d ->
            d.copy(
                customTags = d.customTags.filterNot { it.id == id },
                days = d.days.mapValues { (_, c) -> c.copy(customTags = c.customTags - id) }.filterValues { !it.isEmpty },
            )
        }
    }

    override suspend fun answerQuiz(answer: Boolean): StressLevel? {
        var completed: StressLevel? = null
        local.update { d ->
            if (d.quiz.index >= StressQuiz.size) return@update d
            val answers = d.quiz.answers + answer
            if (answers.size == StressQuiz.size) {
                val level = StressQuiz.evaluate(answers)
                completed = level
                d.copy(quiz = QuizProgress(answers), stress = level)
            } else {
                d.copy(quiz = QuizProgress(answers))
            }
        }
        return completed
    }

    override suspend fun restartQuiz() {
        local.update { it.copy(quiz = QuizProgress()) }
    }

    override suspend fun setOnboarded(value: Boolean) {
        local.update { it.copy(onboarded = value) }
    }

    override suspend fun completeOnboarding(name: String, settings: (Settings) -> Settings) {
        local.update {
            it.copy(
                onboarded = true,
                userName = name.trim().take(AppData.MAX_NAME_LENGTH),
                settings = settings(it.settings),
            )
        }
    }

    override suspend fun updateSettings(transform: (Settings) -> Settings) {
        local.update { it.copy(settings = transform(it.settings)) }
    }

    override suspend fun resetAll() {
        local.update { AppData() }
    }
}

/** Agrega o reemplaza el contexto de un día; lo quita si quedó vacío para no acumular basura. */
private fun Map<String, DayContext>.with(key: String, context: DayContext): Map<String, DayContext> =
    if (context.isEmpty) this - key else this + (key to context)
