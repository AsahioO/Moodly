package com.asahioo.moodly.domain

import com.asahioo.moodly.data.model.CustomTag
import com.asahioo.moodly.data.model.DayContext
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.data.model.PresetTag
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HistorySearchTest {

    private val gym = CustomTag("g", "Gimnasio")
    private val moods = mapOf(
        "2026-09-01" to Mood.HAPPY,
        "2026-09-02" to Mood.STRESSED,
        "2026-09-03" to Mood.HAPPY,
    )
    private val days = mapOf(
        "2026-09-01" to DayContext(note = "Buen día en la playa", tags = setOf(PresetTag.FRIENDS)),
        "2026-09-02" to DayContext(note = "Entrega", tags = setOf(PresetTag.WORK)),
        "2026-09-03" to DayContext(customTags = setOf("g")),
    )

    private fun search(query: String = "", mood: Mood? = null, tags: Set<PresetTag> = emptySet(), custom: Set<String> = emptySet()) =
        HistorySearch.search(moods, days, listOf(gym), query, mood, tags, custom).map { it.date.toString() }

    @Test
    fun noFilters_allNewestFirst() = assertEquals(listOf("2026-09-03", "2026-09-02", "2026-09-01"), search())

    @Test
    fun text_matchesNoteIgnoringCase_andCustomTagLabel() {
        assertEquals(listOf("2026-09-01"), search("PLAYA"))
        assertEquals(listOf("2026-09-03"), search("gimna"))
    }

    @Test
    fun filters_combineWithAnd() {
        assertEquals(listOf("2026-09-03", "2026-09-01"), search(mood = Mood.HAPPY))
        assertEquals(listOf("2026-09-01"), search(mood = Mood.HAPPY, tags = setOf(PresetTag.FRIENDS)))
        assertEquals(listOf("2026-09-03"), search(custom = setOf("g")))
        assertEquals(emptyList<String>(), search("Entrega", mood = Mood.HAPPY))
    }

    @Test
    fun dayWithoutContext_onlyMatchesEmptyText() {
        val only = HistorySearch.search(mapOf("2026-09-05" to Mood.CALM), emptyMap(), emptyList(), "algo")
        assertEquals(emptyList<Any>(), only)
        assertEquals(LocalDate.of(2026, 9, 5), HistorySearch.search(mapOf("2026-09-05" to Mood.CALM), emptyMap(), emptyList(), "").single().date)
    }
}
