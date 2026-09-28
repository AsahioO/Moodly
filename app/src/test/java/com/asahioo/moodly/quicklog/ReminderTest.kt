package com.asahioo.moodly.quicklog

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class ReminderTest {

    private val nine = 21 * 60

    @Test
    fun beforeTheHour_firesToday() {
        assertEquals(
            LocalDateTime.of(2026, 9, 27, 21, 0),
            nextTrigger(LocalDateTime.of(2026, 9, 27, 8, 30), nine),
        )
    }

    @Test
    fun afterTheHour_firesTomorrow() {
        assertEquals(
            LocalDateTime.of(2026, 9, 28, 21, 0),
            nextTrigger(LocalDateTime.of(2026, 9, 27, 21, 0, 1), nine),
        )
    }

    @Test
    fun exactlyAtTheHour_firesTomorrow() {
        // La alarma que acaba de sonar no debe reprogramarse para el mismo instante.
        assertEquals(
            LocalDateTime.of(2026, 9, 28, 21, 0),
            nextTrigger(LocalDateTime.of(2026, 9, 27, 21, 0), nine),
        )
    }

    @Test
    fun rollsOverMonthAndYear() {
        assertEquals(
            LocalDateTime.of(2027, 1, 1, 7, 15),
            nextTrigger(LocalDateTime.of(2026, 12, 31, 22, 0), 7 * 60 + 15),
        )
    }
}
