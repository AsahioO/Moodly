package com.asahioo.moodly.quicklog

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.core.app.NotificationManagerCompat
import com.asahioo.moodly.MoodlyApplication
import com.asahioo.moodly.R
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.ui.labelRes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/*
 * Registro rápido: la fila de 6 caras que comparten la notificación del recordatorio y el widget.
 * Tocar una cara guarda el ánimo de hoy sin abrir la app.
 */

private const val ACTION_LOG_MOOD = "com.asahioo.moodly.action.LOG_MOOD"
internal const val ACTION_REMINDER = "com.asahioo.moodly.action.REMINDER"
private const val ACTION_EDIT_MOOD = "com.asahioo.moodly.action.EDIT_MOOD"
private const val EXTRA_MOOD = "mood"
private const val REQUEST_EDIT = 200

/** Opacidad de las caras no elegidas cuando hoy ya hay registro (0–255). */
private const val DIMMED_ALPHA = 90

@get:IdRes
private val Mood.faceViewId: Int
    get() = when (this) {
        Mood.HAPPY -> R.id.face_happy
        Mood.ANGRY -> R.id.face_angry
        Mood.SLEEPY -> R.id.face_sleepy
        Mood.BORED -> R.id.face_bored
        Mood.CALM -> R.id.face_calm
        Mood.STRESSED -> R.id.face_stressed
    }

@get:DrawableRes
internal val Mood.iconRes: Int
    get() = when (this) {
        Mood.HAPPY -> R.drawable.ic_mood_happy
        Mood.ANGRY -> R.drawable.ic_mood_angry
        Mood.SLEEPY -> R.drawable.ic_mood_sleepy
        Mood.BORED -> R.drawable.ic_mood_bored
        Mood.CALM -> R.drawable.ic_mood_calm
        Mood.STRESSED -> R.drawable.ic_mood_stressed
    }

/** Fondo pastel del widget cuando hoy ya hay este ánimo. */
@get:DrawableRes
internal val Mood.widgetBgRes: Int
    get() = when (this) {
        Mood.HAPPY -> R.drawable.widget_bg_happy
        Mood.ANGRY -> R.drawable.widget_bg_angry
        Mood.SLEEPY -> R.drawable.widget_bg_sleepy
        Mood.BORED -> R.drawable.widget_bg_bored
        Mood.CALM -> R.drawable.widget_bg_calm
        Mood.STRESSED -> R.drawable.widget_bg_stressed
    }

/** Crea la fila de caras; si hoy ya hay ánimo, las demás caras se atenúan. */
private fun moodFacesViews(context: Context, todayMood: Mood?, facesLayout: Int): RemoteViews =
    RemoteViews(context.packageName, facesLayout).apply {
        Mood.entries.forEach { mood ->
            val id = mood.faceViewId
            setOnClickPendingIntent(id, logMoodIntent(context, mood))
            setContentDescription(id, context.getString(R.string.cd_quick_log, context.getString(mood.labelRes)))
            setInt(id, "setImageAlpha", if (todayMood == null || todayMood == mood) 255 else DIMMED_ALPHA)
        }
    }

/**
 * Coloca las caras ([facesLayout]: la fila fija de la notificación, o las del widget) dentro de
 * @id/faces de [layout]. Se vacía primero porque el launcher puede
 * reaplicar las acciones sobre la vista existente y duplicaría la fila.
 */
internal fun viewsWithFaces(
    context: Context,
    layout: Int,
    todayMood: Mood?,
    facesLayout: Int = R.layout.mood_faces,
): RemoteViews =
    RemoteViews(context.packageName, layout).apply {
        removeAllViews(R.id.faces)
        addView(R.id.faces, moodFacesViews(context, todayMood, facesLayout))
    }

/** "Cambiar" en el panel de confirmación del widget: vuelve a mostrar las caras. */
internal fun editMoodIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
    context,
    REQUEST_EDIT,
    Intent(context, QuickLogReceiver::class.java).setAction(ACTION_EDIT_MOOD),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

private fun logMoodIntent(context: Context, mood: Mood): PendingIntent = PendingIntent.getBroadcast(
    context,
    mood.ordinal, // requestCode distinto por cara: los extras no distinguen PendingIntents.
    Intent(context, QuickLogReceiver::class.java).setAction(ACTION_LOG_MOOD).putExtra(EXTRA_MOOD, mood.name),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

/**
 * Recibe los toques en las caras, la alarma diaria y los eventos del sistema que borran
 * alarmas (reinicio, cambio de hora o zona horaria, actualización de la app).
 */
class QuickLogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as MoodlyApplication).container
        val pending = goAsync()
        container.appScope.launch {
            try {
                val today = container.dateProvider.today()
                if (intent.action == ACTION_LOG_MOOD) {
                    val mood = Mood.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_MOOD) } ?: return@launch
                    container.repository.setMood(today, mood)
                    NotificationManagerCompat.from(context).cancel(REMINDER_NOTIFICATION_ID)
                    MoodWidget.refresh(context)
                    return@launch
                }
                if (intent.action == ACTION_EDIT_MOOD) {
                    MoodWidget.refresh(context, editing = true)
                    return@launch
                }
                val data = container.repository.data.first()
                val todayMood = data.moods[today.toString()]
                if (data.settings.reminderEnabled) {
                    container.reminders.schedule(data.settings.reminderMinutes)
                    if (intent.action == ACTION_REMINDER && todayMood == null) showReminder(context, data.userName)
                }
                // También limpia el resaltado del día anterior en el widget.
                MoodWidget.refresh(context)
            } finally {
                pending.finish()
            }
        }
    }
}
