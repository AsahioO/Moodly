package com.asahioo.moodly.di

import android.content.Context
import com.asahioo.moodly.data.health.HealthConnect
import com.asahioo.moodly.data.local.MoodLocalDataSource
import com.asahioo.moodly.data.repository.DefaultMoodRepository
import com.asahioo.moodly.data.repository.MoodRepository
import com.asahioo.moodly.domain.DateProvider
import com.asahioo.moodly.domain.SystemDateProvider
import com.asahioo.moodly.quicklog.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

/**
 * Inyección de dependencias manual. Para una app de este tamaño evita el costo de Hilt;
 * si el proyecto crece, este es el único lugar que hay que migrar.
 */
class AppContainer(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val dateProvider: DateProvider = SystemDateProvider

    val repository: MoodRepository = DefaultMoodRepository(MoodLocalDataSource(context, json), json)

    /** Vive lo que vive el proceso: receptores, widget y sincronización del recordatorio. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val reminders = ReminderScheduler(context)

    val health = HealthConnect(context, repository, dateProvider)
}
