package com.asahioo.moodly.data

import com.asahioo.moodly.data.model.AppData
import com.asahioo.moodly.data.model.DayPart
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.data.repository.withMood
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class DayPartTest {

    @Test
    fun of_splitsTheDay() {
        assertEquals(DayPart.MORNING, DayPart.of(LocalTime.of(0, 0)))
        assertEquals(DayPart.MORNING, DayPart.of(LocalTime.of(11, 59)))
        assertEquals(DayPart.AFTERNOON, DayPart.of(LocalTime.of(12, 0)))
        assertEquals(DayPart.AFTERNOON, DayPart.of(LocalTime.of(18, 59)))
        assertEquals(DayPart.NIGHT, DayPart.of(LocalTime.of(19, 0)))
    }

    @Test
    fun representative_isLatestPart_evenIfLoggedOutOfOrder() {
        val d = AppData()
            .withMood("2026-09-01", DayPart.NIGHT, Mood.CALM)
            .withMood("2026-09-01", DayPart.MORNING, Mood.STRESSED)
        assertEquals(Mood.CALM, d.moods["2026-09-01"])
        assertEquals(mapOf(DayPart.NIGHT to Mood.CALM, DayPart.MORNING to Mood.STRESSED), d.parts["2026-09-01"])
    }

    @Test
    fun samePart_isReplaced() {
        val d = AppData()
            .withMood("2026-09-01", DayPart.MORNING, Mood.ANGRY)
            .withMood("2026-09-01", DayPart.MORNING, Mood.HAPPY)
        assertEquals(Mood.HAPPY, d.moods["2026-09-01"])
    }

    @Test
    fun legacyDay_withoutParts_isReplacedByFirstPart() {
        val legacy = AppData(moods = mapOf("2026-09-01" to Mood.BORED))
        val d = legacy.withMood("2026-09-01", DayPart.MORNING, Mood.HAPPY)
        assertEquals(Mood.HAPPY, d.moods["2026-09-01"])
    }
}
