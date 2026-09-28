package com.asahioo.moodly.data.model

import kotlinx.serialization.Serializable

/** Ánimos disponibles. El orden define el orden de los chips y de la leyenda. */
@Serializable
enum class Mood { HAPPY, ANGRY, SLEEPY, BORED, CALM, STRESSED }

@Serializable
enum class StressLevel { LOW, MEDIUM, HIGH }

/** Etiquetas predefinidas. Se serializan por nombre: nunca renombrar, solo agregar al final. */
@Serializable
enum class PresetTag { WORK, FAMILY, FRIENDS, EXERCISE, POOR_SLEEP, STUDY, HEALTH, REST }

/** Etiqueta creada por el usuario. El id es estable: renombrarla no pierde el historial. */
@Serializable
data class CustomTag(val id: String, val label: String)

/** Contexto opcional de un día. Se quita del mapa cuando queda vacío. */
@Serializable
data class DayContext(
    val note: String = "",
    val tags: Set<PresetTag> = emptySet(),
    /** Ids de [CustomTag]. */
    val customTags: Set<String> = emptySet(),
    /** Sueño de la noche anterior a este día. */
    val sleepMinutes: Int? = null,
) {
    val hasEntry: Boolean get() = note.isNotEmpty() || tags.isNotEmpty() || customTags.isNotEmpty()
    val isEmpty: Boolean get() = !hasEntry && sleepMinutes == null
}

@Serializable
data class Settings(
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val reminderEnabled: Boolean = false,
    /** Minutos desde medianoche (21:00 por defecto). */
    val reminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
) {
    companion object {
        const val DEFAULT_REMINDER_MINUTES = 21 * 60
    }
}

@Serializable
data class QuizProgress(
    /** true = respondió "Sí". La posición corresponde a la pregunta. */
    val answers: List<Boolean> = emptyList(),
) {
    val index: Int get() = answers.size
}

/**
 * Estado persistido completo de la app. Es pequeño (unos cuantos cientos de entradas como máximo),
 * así que se guarda como un único documento JSON en DataStore.
 */
@Serializable
data class AppData(
    val onboarded: Boolean = false,
    /** Clave: fecha ISO-8601 (yyyy-MM-dd). */
    val moods: Map<String, Mood> = emptyMap(),
    /** Nota, etiquetas y sueño por día. Misma clave que [moods]. */
    val days: Map<String, DayContext> = emptyMap(),
    /** En orden de creación (= orden de los chips). */
    val customTags: List<CustomTag> = emptyList(),
    /** null = aún no completa el quiz. */
    val stress: StressLevel? = null,
    val quiz: QuizProgress = QuizProgress(),
    val settings: Settings = Settings(),
    val userName: String = "",
) {
    companion object {
        /** Valor inicial del selector de sueño cuando no hay registro. */
        const val DEFAULT_SLEEP_MINUTES = 440
        const val MIN_SLEEP_MINUTES = 240
        const val MAX_SLEEP_MINUTES = 720
        const val MAX_NAME_LENGTH = 30
        const val MAX_NOTE_LENGTH = 140
        const val MAX_TAG_LENGTH = 20
        const val MAX_CUSTOM_TAGS = 20
    }
}
