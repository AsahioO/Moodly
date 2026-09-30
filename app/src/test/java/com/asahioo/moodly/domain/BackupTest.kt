package com.asahioo.moodly.domain

import com.asahioo.moodly.data.model.AppData
import com.asahioo.moodly.data.model.DayContext
import com.asahioo.moodly.data.model.Mood
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val sample = AppData(
        onboarded = true,
        userName = "Ana",
        moods = mapOf("2026-09-01" to Mood.HAPPY),
        days = mapOf("2026-09-01" to DayContext(note = "hola", sleepMinutes = 450)),
    )

    @Test
    fun roundTrip_keepsData() {
        assertEquals(sample, Backup.decode(json, Backup.encode(json, sample)))
    }

    @Test
    fun garbage_isRejected() {
        assertNull(Backup.decode(json, "no es json"))
        assertNull(Backup.decode(json, """{"onboarded":true}"""))
    }

    @Test
    fun unknownVersion_isRejected() {
        val future = Backup.encode(json, sample).replace("\"version\":1", "\"version\":99")
        assertNull(Backup.decode(json, future))
    }

    @Test
    fun invalidDates_areDropped() {
        val bad = sample.copy(moods = sample.moods + ("mañana" to Mood.CALM))
        assertEquals(sample.moods, Backup.decode(json, Backup.encode(json, bad))?.moods)
    }

    @Test
    fun avatarFile_isDropped() {
        val withPhoto = sample.copy(settings = sample.settings.copy(avatarFile = "avatar_1.jpg"))
        assertNull(Backup.decode(json, Backup.encode(json, withPhoto))?.settings?.avatarFile)
    }
}
