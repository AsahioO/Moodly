package com.asahioo.moodly.quicklog

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.asahioo.moodly.MainActivity
import com.asahioo.moodly.R
import java.time.LocalDateTime
import java.time.ZoneId

internal const val REMINDER_NOTIFICATION_ID = 1
private const val CHANNEL_ID = "daily_reminder"
private const val REQUEST_REMINDER = 100
private const val WINDOW_MS = 10 * 60 * 1000L

/** Programa la alarma diaria del recordatorio. Cada disparo programa el siguiente. */
class ReminderScheduler(context: Context) {

    private val app = context.applicationContext
    private val alarms = app.getSystemService(AlarmManager::class.java)

    fun schedule(minutesOfDay: Int) {
        val at = nextTrigger(LocalDateTime.now(), minutesOfDay).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // setAndAllowWhileIdle deja al sistema una ventana de hasta 1 h; setWindow la acota a 10 min
        // (el mínimo) sin pedir el permiso de alarmas exactas.
        // ponytail: en Doze profundo puede diferirse a la siguiente ventana de mantenimiento;
        // setExactAndAllowWhileIdle + SCHEDULE_EXACT_ALARM si hace falta puntualidad al minuto.
        alarms.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MS, pendingIntent())
    }

    fun cancel() = alarms.cancel(pendingIntent())

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        app,
        REQUEST_REMINDER,
        Intent(app, QuickLogReceiver::class.java).setAction(ACTION_REMINDER),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** Próximo disparo: hoy a esa hora si aún no llega; si ya pasó (o es justo ahora), mañana. */
internal fun nextTrigger(now: LocalDateTime, minutesOfDay: Int): LocalDateTime {
    val todayAt = now.toLocalDate().atTime(minutesOfDay / 60, minutesOfDay % 60)
    return if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
}

// areNotificationsEnabled() ya refleja el permiso POST_NOTIFICATIONS en Android 13+.
@SuppressLint("MissingPermission")
internal fun showReminder(context: Context, userName: String) {
    val manager = NotificationManagerCompat.from(context)
    if (!manager.areNotificationsEnabled()) return
    manager.createNotificationChannel(
        NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.reminder_channel))
            .build(),
    )
    val firstName = userName.substringBefore(' ')
    val title = if (firstName.isBlank()) {
        context.getString(R.string.reminder_title_anon)
    } else {
        context.getString(R.string.reminder_title, firstName)
    }
    // Las acciones estándar solo muestran texto y caben 3; por eso las 6 caras van en una vista propia.
    val faces = viewsWithFaces(context, R.layout.notification_mood_big, null)
        .apply { setTextViewText(R.id.title, title) }
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_moodly)
        .setContentTitle(title)
        .setContentText(context.getString(R.string.reminder_text))
        .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        .setCustomContentView(viewsWithFaces(context, R.layout.notification_mood_compact, null))
        .setCustomBigContentView(faces)
        .setCustomHeadsUpContentView(faces)
        .setContentIntent(openAppIntent(context))
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .build()
    manager.notify(REMINDER_NOTIFICATION_ID, notification)
}

/** Abre la app; lo usan la notificación y el widget. */
internal fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    0,
    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
    PendingIntent.FLAG_IMMUTABLE,
)
