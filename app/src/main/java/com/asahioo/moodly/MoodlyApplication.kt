package com.asahioo.moodly

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import com.asahioo.moodly.di.AppContainer
import com.asahioo.moodly.quicklog.MoodWidget
import com.asahioo.moodly.quicklog.REMINDER_NOTIFICATION_ID
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MoodlyApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        val data = container.repository.data
        // Los ajustes son la fuente de verdad de la alarma: activar, cambiar la hora o borrar todo
        // se refleja aquí sin que las pantallas hablen con AlarmManager.
        container.appScope.launch {
            data.map { it.settings.reminderEnabled to it.settings.reminderMinutes }
                .distinctUntilChanged()
                .collect { (enabled, minutes) ->
                    if (enabled) container.reminders.schedule(minutes) else container.reminders.cancel()
                }
        }
        // Un solo lugar mantiene al día el widget, venga el registro de Inicio, Calendario o un reset.
        // ponytail: el resaltado cambia al registrar y con el recordatorio diario, no justo a medianoche.
        container.appScope.launch {
            data.map { it.moods[container.dateProvider.today().toString()] }
                .distinctUntilChanged()
                .collect { mood ->
                    MoodWidget.refresh(this@MoodlyApplication, mood)
                    if (mood != null) NotificationManagerCompat.from(this@MoodlyApplication).cancel(REMINDER_NOTIFICATION_ID)
                }
        }
    }
}
