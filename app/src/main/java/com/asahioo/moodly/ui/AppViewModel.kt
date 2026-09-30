package com.asahioo.moodly.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asahioo.moodly.data.health.HealthConnect
import com.asahioo.moodly.data.health.HealthStatus
import com.asahioo.moodly.data.model.AppTheme
import com.asahioo.moodly.data.model.CustomTag
import com.asahioo.moodly.data.model.DayContext
import com.asahioo.moodly.data.model.DayPart
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.data.model.Settings
import com.asahioo.moodly.data.repository.MoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AppUiState(
    val isReady: Boolean = false,
    val onboarded: Boolean = false,
    val settings: Settings = Settings(),
    val userName: String = "",
    val trackingSince: LocalDate? = null,
    /** Registros completos: la hoja del día puede abrirse para cualquier fecha (Inicio o Calendario). */
    val moods: Map<String, Mood> = emptyMap(),
    val parts: Map<String, Map<DayPart, Mood>> = emptyMap(),
    val days: Map<String, DayContext> = emptyMap(),
    val customTags: List<CustomTag> = emptyList(),
    /** Bloqueo activo y sin desbloquear: la UI debe tapar todo. */
    val locked: Boolean = false,
)

/**
 * Estado global: arranque, onboarding y ajustes. La alarma del recordatorio se sincroniza sola
 * a partir de [Settings] (ver MoodlyApplication), así que aquí solo se guardan preferencias.
 */
class AppViewModel(
    private val repository: MoodRepository,
    private val health: HealthConnect,
) : ViewModel() {

    /** Se pierde con el proceso (arranque en frío = bloqueado) y al volver de segundo plano tras la gracia. */
    private val unlocked = MutableStateFlow(false)

    fun unlock() { unlocked.value = true }
    fun lock() { unlocked.value = false }
    fun setAvatar(file: String?) = launchSet { repository.updateSettings { it.copy(avatarFile = file) } }
    fun setAppLock(enabled: Boolean) = launchSet { repository.updateSettings { it.copy(appLock = enabled) } }

    val uiState: StateFlow<AppUiState> = combine(repository.data, unlocked) { d, isUnlocked ->
            AppUiState(
                isReady = true,
                onboarded = d.onboarded,
                settings = d.settings,
                userName = d.userName,
                // Las fechas ISO-8601 ordenan igual como texto que como fecha.
                trackingSince = d.moods.keys.minOrNull()?.let(LocalDate::parse),
                moods = d.moods,
                parts = d.parts,
                days = d.days,
                customTags = d.customTags,
                locked = d.settings.appLock && !isUnlocked,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    /** [reminderMinutes] null = el usuario eligió "Ahora no". */
    fun completeOnboarding(name: String, reminderMinutes: Int?) = launchSet {
        repository.completeOnboarding(name) { settings ->
            if (reminderMinutes == null) settings.copy(reminderEnabled = false)
            else settings.copy(reminderEnabled = true, reminderMinutes = reminderMinutes)
        }
    }

    fun replayOnboarding() = launchSet { repository.setOnboarded(false) }
    fun setHaptics(enabled: Boolean) = launchSet { repository.updateSettings { it.copy(haptics = enabled) } }
    fun setTheme(theme: AppTheme) = launchSet { repository.updateSettings { it.copy(theme = theme) } }
    fun setReduceMotion(enabled: Boolean) = launchSet { repository.updateSettings { it.copy(reduceMotion = enabled) } }
    fun setReminderEnabled(enabled: Boolean) = launchSet { repository.updateSettings { it.copy(reminderEnabled = enabled) } }
    fun setReminderTime(minutes: Int) = launchSet {
        repository.updateSettings { it.copy(reminderEnabled = true, reminderMinutes = minutes) }
    }
    /** Si Health Connect siguiera conectado, el siguiente resume reimportaría días en la app vacía. */
    fun resetAll() = launchSet {
        health.disconnect()
        repository.resetAll()
    }

    suspend fun exportBackup(): String = repository.exportBackup()
    suspend fun importBackup(text: String): Boolean = repository.importBackup(text)

    val healthStatus: StateFlow<HealthStatus> = health.status

    /** Tras el diálogo de permisos: si concedió algo, rellena el último mes de una vez. */
    fun onHealthPermissionsResult() = launchSet {
        health.refresh()
        health.sync(HealthConnect.BACKFILL_DAYS)
    }
    fun disconnectHealth() = launchSet { health.disconnect() }

    /** Suspende para que quien la crea pueda seleccionarla al momento. */
    suspend fun addTag(label: String): String? = repository.addCustomTag(label)
    fun renameTag(id: String, label: String) = launchSet { repository.renameCustomTag(id, label) }
    fun deleteTag(id: String) = launchSet { repository.deleteCustomTag(id) }

    private fun launchSet(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory = containerViewModelFactory<AppViewModel> { AppViewModel(it.repository, it.health) }
    }
}
