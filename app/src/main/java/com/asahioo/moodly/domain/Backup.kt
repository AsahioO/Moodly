package com.asahioo.moodly.domain

import com.asahioo.moodly.data.model.AppData
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Serializable
private data class BackupFile(val format: String, val version: Int, val data: AppData)

/** Respaldo portable: [AppData] envuelto con formato y versión para rechazar archivos ajenos. */
object Backup {
    private const val FORMAT = "moodly-backup"
    private const val VERSION = 1

    fun encode(json: Json, data: AppData): String =
        json.encodeToString(BackupFile.serializer(), BackupFile(FORMAT, VERSION, data))

    /** null si el texto no es un respaldo de Moodly legible. */
    fun decode(json: Json, text: String): AppData? {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            return null
        } catch (e: IllegalArgumentException) {
            return null
        }
        if (file.format != FORMAT || file.version > VERSION) return null
        return sanitize(file.data)
    }

    /** Un archivo editado a mano no debe romper la app: fechas inválidas fuera y textos recortados. */
    private fun sanitize(d: AppData): AppData {
        fun validDate(key: String) = try {
            LocalDate.parse(key)
            true
        } catch (e: DateTimeParseException) {
            false
        }
        return d.copy(
            onboarded = true,
            // El otro teléfono pudo tener bloqueo; este quizá no tenga credencial y quedaría fuera.
            settings = d.settings.copy(appLock = false, avatarFile = null), // la foto vive en filesDir de este teléfono
            userName = d.userName.trim().take(AppData.MAX_NAME_LENGTH),
            moods = d.moods.filterKeys(::validDate),
            parts = d.parts.filterKeys(::validDate),
            days = d.days.filterKeys(::validDate).mapValues { (_, c) ->
                c.copy(
                    note = c.note.trim().take(AppData.MAX_NOTE_LENGTH),
                    sleepMinutes = c.sleepMinutes?.coerceIn(AppData.MIN_SLEEP_MINUTES, AppData.MAX_SLEEP_MINUTES),
                )
            },
            customTags = d.customTags
                .map { it.copy(label = normalizeTagLabel(it.label)) }
                .filter { it.label.isNotEmpty() }
                .take(AppData.MAX_CUSTOM_TAGS),
        )
    }
}
