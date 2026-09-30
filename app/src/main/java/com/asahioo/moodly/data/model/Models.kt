package com.asahioo.moodly.data.model

import kotlinx.serialization.Serializable
import java.time.LocalTime

/** Ánimos disponibles. El orden define el orden de los chips y de la leyenda. */
@Serializable
enum class Mood { HAPPY, ANGRY, SLEEPY, BORED, CALM, STRESSED }

@Serializable
enum class StressLevel { LOW, MEDIUM, HIGH }

/** Momento del día de un registro. Se serializa por nombre y el orden es cronológico: solo agregar con cuidado. */
@Serializable
enum class DayPart {
    MORNING, AFTERNOON, NIGHT;

    companion object {
        fun of(time: LocalTime): DayPart = when {
            time.hour < 12 -> MORNING
            time.hour < 19 -> AFTERNOON
            else -> NIGHT
        }

        /** El ánimo "representativo" del día (calendario, racha, widget): el de la parte más tardía. */
        fun latest(parts: Map<DayPart, Mood>): Mood? = parts.maxByOrNull { it.key.ordinal }?.value
    }
}

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
    /** true = [sleepMinutes] vino de Health Connect y otra sincronización puede actualizarlo. */
    val sleepFromHealth: Boolean = false,
    /** Pasos del día según Health Connect. */
    val steps: Int? = null,
) {
    val hasEntry: Boolean get() = note.isNotEmpty() || tags.isNotEmpty() || customTags.isNotEmpty()
    val isEmpty: Boolean get() = !hasEntry && sleepMinutes == null && steps == null

    /** Mezcla lo leído de Health Connect. El sueño manual nunca se pisa: es la corrección del usuario. */
    fun withHealth(health: HealthDay): DayContext {
        val sleep = health.sleepMinutes?.takeIf { it > 0 && (sleepMinutes == null || sleepFromHealth) }
        return copy(
            sleepMinutes = sleep ?: sleepMinutes,
            sleepFromHealth = if (sleep != null) true else sleepFromHealth,
            steps = health.steps?.takeIf { it > 0 } ?: steps,
        )
    }
}

/** Lo que Health Connect reporta para un día; null = sin dato. No se persiste tal cual. */
data class HealthDay(val sleepMinutes: Int?, val steps: Int?)

@Serializable
data class Settings(
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val reminderEnabled: Boolean = false,
    /** Minutos desde medianoche (21:00 por defecto). */
    val reminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
    /** Pide la credencial del dispositivo al abrir la app. */
    val appLock: Boolean = false,
    /** Nombre del JPEG de la foto de perfil en filesDir; null = ilustración por defecto. */
    val avatarFile: String? = null,
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
    /**
     * Ánimo por momento del día. [moods] guarda el de la parte más tardía, así que todo lo que solo
     * necesita "el ánimo del día" sigue leyendo [moods]. Los días viejos no tienen entrada aquí.
     */
    val parts: Map<String, Map<DayPart, Mood>> = emptyMap(),
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
