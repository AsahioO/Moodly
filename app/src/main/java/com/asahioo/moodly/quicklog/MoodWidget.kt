package com.asahioo.moodly.quicklog

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import com.asahioo.moodly.MoodlyApplication
import com.asahioo.moodly.R
import com.asahioo.moodly.data.model.Mood
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Widget de pantalla de inicio con las 6 caras: un toque registra el ánimo de hoy. */
class MoodWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val container = (context.applicationContext as MoodlyApplication).container
        val pending = goAsync()
        container.appScope.launch {
            try {
                val today = container.dateProvider.today().toString()
                refresh(context, container.repository.data.first().moods[today])
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        fun refresh(context: Context, todayMood: Mood?) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MoodWidget::class.java))
            if (ids.isEmpty()) return
            manager.updateAppWidget(ids, viewsWithFaces(context, R.layout.widget_mood, todayMood))
        }
    }
}
