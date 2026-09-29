package com.asahioo.moodly.quicklog

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.asahioo.moodly.MoodlyApplication
import com.asahioo.moodly.R
import com.asahioo.moodly.data.model.AppData
import com.asahioo.moodly.domain.MoodStats
import com.asahioo.moodly.ui.labelRes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Alto (dp) desde el que el launcher da cabida al layout mediano, al grande y a la cuadrícula de caras. */
private const val MEDIUM_MIN_DP = 90
private const val LARGE_MIN_DP = 160
private const val GRID_MIN_DP = 200
private const val WEEK_DAYS = 7

internal enum class WidgetSize { SMALL, MEDIUM, LARGE }

internal fun widgetSize(heightDp: Int): WidgetSize = when {
    heightDp >= LARGE_MIN_DP -> WidgetSize.LARGE
    heightDp >= MEDIUM_MIN_DP -> WidgetSize.MEDIUM
    else -> WidgetSize.SMALL
}

/**
 * Widget de pantalla de inicio: un toque en una cara registra el ánimo de hoy. Según el alto
 * cambia de layout: pequeño (caras), mediano (+ saludo, racha y confirmación) y grande (+ semana).
 */
class MoodWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refreshAsync(context)

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) =
        refreshAsync(context)

    private fun refreshAsync(context: Context) {
        val pending = goAsync()
        (context.applicationContext as MoodlyApplication).container.appScope.launch {
            try {
                refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /**
         * Redibuja todas las instancias con los datos actuales. [editing] muestra las caras aunque
         * hoy ya haya registro ("Cambiar"); no se guarda, el siguiente refresh vuelve al panel.
         */
        suspend fun refresh(context: Context, editing: Boolean = false) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MoodWidget::class.java))
            if (ids.isEmpty()) return
            val container = (context.applicationContext as MoodlyApplication).container
            val data = container.repository.data.first()
            val today = container.dateProvider.today()
            for (id in ids) {
                val heightDp = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                manager.updateAppWidget(id, widgetViews(context, heightDp, data, today, editing))
            }
        }
    }
}

private fun widgetViews(context: Context, heightDp: Int, data: AppData, today: LocalDate, editing: Boolean): RemoteViews {
    val todayMood = data.moods[today.toString()]
    val size = widgetSize(heightDp)
    val showLogged = todayMood != null && !editing && size != WidgetSize.SMALL
    val layout = when (size) {
        WidgetSize.LARGE -> R.layout.widget_mood_large
        WidgetSize.MEDIUM -> R.layout.widget_mood_medium
        WidgetSize.SMALL -> R.layout.widget_mood
    }
    val facesLayout = if (size != WidgetSize.SMALL && heightDp >= GRID_MIN_DP) R.layout.widget_faces_grid else R.layout.widget_faces_row
    val views = viewsWithFaces(context, layout, todayMood, facesLayout)
    // Pastel del ánimo de hoy; blanco sin registro o mientras se cambia. Se pone siempre: el
    // launcher puede reaplicar sobre la vista existente y conservaría el fondo anterior.
    val tint = todayMood?.takeIf { !editing }
    views.setInt(android.R.id.background, "setBackgroundResource", tint?.widgetBgRes ?: R.drawable.widget_bg)
    if (size == WidgetSize.SMALL) return views

    val large = size == WidgetSize.LARGE
    val firstName = data.userName.substringBefore(' ')
    views.setTextViewText(
        R.id.title,
        if (firstName.isBlank()) context.getString(R.string.reminder_title_anon) else context.getString(R.string.reminder_title, firstName),
    )
    views.setTextViewTextSize(R.id.title, TypedValue.COMPLEX_UNIT_SP, if (large) 17f else 15f)
    views.setOnClickPendingIntent(R.id.title, openAppIntent(context))

    val streak = MoodStats.streak(data.moods, today)
    views.setViewVisibility(R.id.streak, if (streak > 0) View.VISIBLE else View.GONE)
    if (streak > 0) {
        views.setTextViewText(R.id.streak, context.resources.getQuantityString(R.plurals.days_count, streak, streak))
    }

    // Visibilidad explícita en ambos sentidos, por lo mismo que el fondo.
    views.setViewVisibility(R.id.faces, if (showLogged) View.GONE else View.VISIBLE)
    views.setViewVisibility(R.id.logged, if (showLogged) View.VISIBLE else View.GONE)
    if (todayMood != null) {
        views.setImageViewResource(R.id.logged_face, todayMood.iconRes)
        views.setTextViewText(R.id.logged_mood, context.getString(todayMood.labelRes))
        views.setTextViewTextSize(R.id.logged_mood, TypedValue.COMPLEX_UNIT_SP, if (large) 28f else 20f)
        // En el mediano no cabe la etiqueta sobre el nombre junto a los botones.
        views.setViewVisibility(R.id.logged_label, if (large) View.VISIBLE else View.GONE)
        views.setOnClickPendingIntent(R.id.btn_change, editMoodIntent(context))
        views.setOnClickPendingIntent(R.id.btn_note, openAppIntent(context))
    }

    if (large) addWeek(context, views, data, today)
    return views
}

/** Últimos [WEEK_DAYS] días terminando hoy, con la cara de cada uno. */
private fun addWeek(context: Context, views: RemoteViews, data: AppData, today: LocalDate) {
    val labels = context.resources.getStringArray(R.array.weekdays_short)
    views.removeAllViews(R.id.week)
    for (ago in WEEK_DAYS - 1 downTo 0) {
        val date = today.minusDays(ago.toLong())
        val mood = data.moods[date.toString()]
        val label = labels[date.dayOfWeek.value % 7] // el arreglo empieza en domingo
        val moodName = mood?.let { context.getString(it.labelRes) } ?: context.getString(R.string.widget_day_empty)
        val day = RemoteViews(context.packageName, R.layout.widget_week_day).apply {
            setTextViewText(R.id.day_label, label)
            setTextColor(R.id.day_label, context.getColor(if (ago == 0) R.color.ink else R.color.ink_muted))
            setInt(R.id.day_root, "setBackgroundResource", if (ago == 0) R.drawable.widget_today else 0)
            setImageViewResource(R.id.day_face, mood?.iconRes ?: R.drawable.ic_day_empty)
            setContentDescription(R.id.day_face, context.getString(R.string.cd_widget_day, label, moodName))
        }
        views.addView(R.id.week, day)
    }
}
