package com.asahioo.moodly.domain

import com.asahioo.moodly.data.model.AppData
import com.asahioo.moodly.data.model.CustomTag
import com.asahioo.moodly.data.model.DayContext
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.data.model.PresetTag
import com.asahioo.moodly.data.model.StressLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class StressQuizTest {

    @Test
    fun allHealthyAnswers_isLow() {
        assertEquals(StressLevel.LOW, StressQuiz.evaluate(StressQuiz.healthyAnswers))
    }

    @Test
    fun allUnhealthyAnswers_isHigh() {
        assertEquals(StressLevel.HIGH, StressQuiz.evaluate(StressQuiz.healthyAnswers.map { !it }))
    }

    @Test
    fun threeOrFourUnhealthy_isMedium() {
        val answers = StressQuiz.healthyAnswers.mapIndexed { i, a -> if (i < 3) !a else a }
        assertEquals(StressLevel.MEDIUM, StressQuiz.evaluate(answers))
    }
}

class MoodStatsTest {

    private val today = LocalDate.of(2026, 9, 26)

    @Test
    fun countsOnlyDaysUpToToday() {
        val moods = mapOf(
            "2026-09-01" to Mood.HAPPY,
            "2026-09-02" to Mood.HAPPY,
            "2026-09-03" to Mood.ANGRY,
            "2026-09-30" to Mood.CALM, // futuro: se ignora
        )
        val s = MoodStats.month(moods, YearMonth.of(2026, 9), today)
        assertEquals(3, s.logged)
        assertEquals(26, s.elapsedDays)
        assertEquals(Mood.HAPPY, s.topMood)
        assertEquals(0, s.counts.getValue(Mood.CALM))
    }

    @Test
    fun emptyMonth_hasNoTopMood() {
        val s = MoodStats.month(emptyMap(), YearMonth.of(2026, 9), today)
        assertNull(s.topMood)
        assertEquals(0, s.discipline)
    }
}

class MoodPatternsTest {

    private val today = LocalDate.of(2026, 9, 26)

    /** Construye [AppData] con un día por entrada, contando hacia atrás desde hoy. */
    private fun data(vararg days: Pair<Mood, DayContext>, customTags: List<CustomTag> = emptyList()): AppData {
        val keys = days.indices.map { today.minusDays(it.toLong()).toString() }
        return AppData(
            moods = keys.zip(days.map { it.first }).toMap(),
            days = keys.zip(days.map { it.second }).toMap().filterValues { !it.isEmpty },
            customTags = customTags,
        )
    }

    private fun slept(minutes: Int) = DayContext(sleepMinutes = minutes)

    @Test
    fun shortSleep_withTenseMoods_isDetected() {
        val short = List(4) { Mood.ANGRY to slept(300) } + (Mood.HAPPY to slept(330))
        val long = List(4) { Mood.STRESSED to slept(480) } + List(16) { Mood.CALM to slept(480) }
        val result = MoodPatterns.find(data(*(short + long).toTypedArray()), today)
        val pattern = result.patterns.single { it.factor == Factor.ShortSleep }
        assertEquals(MoodGroup.TENSE, pattern.group)
        assertEquals(Magnitude.TRIPLE, pattern.magnitude)
        assertEquals(5, pattern.daysWith)
    }

    @Test
    fun daysWithoutSleep_doNotCountAsLongSleep() {
        // 5 noches cortas tensas + 3 largas tensas: sin diferencia. Los 20 días sin dato de sueño no deben
        // entrar al grupo "sin", o parecería que dormir poco triplica lo tenso.
        val days = List(5) { Mood.ANGRY to slept(300) } + List(3) { Mood.ANGRY to slept(480) } +
            List(20) { Mood.CALM to DayContext(note = "x") }
        val result = MoodPatterns.find(data(*days.toTypedArray()), today)
        assertTrue(result.patterns.none { it.factor == Factor.ShortSleep })
    }

    @Test
    fun exercise_withFewerTenseDays_isHalf() {
        val exercise = DayContext(tags = setOf(PresetTag.EXERCISE))
        val days = List(8) { Mood.CALM to exercise } + List(8) { Mood.STRESSED to DayContext(note = "x") } +
            List(4) { Mood.HAPPY to DayContext(note = "x") }
        val pattern = MoodPatterns.find(data(*days.toTypedArray()), today).patterns
            .single { it.factor == Factor.Preset(PresetTag.EXERCISE) }
        assertEquals(MoodGroup.TENSE, pattern.group)
        assertEquals(Magnitude.HALF, pattern.magnitude)
    }

    @Test
    fun customTag_isAFactor() {
        val gym = CustomTag("g1", "Gym")
        val tagged = DayContext(customTags = setOf(gym.id))
        val days = List(6) { Mood.ANGRY to tagged } + List(14) { Mood.HAPPY to DayContext(note = "x") }
        val result = MoodPatterns.find(data(*days.toTypedArray(), customTags = listOf(gym)), today)
        assertEquals(Factor.Custom(gym), result.patterns.first().factor)
    }

    @Test
    fun smallSample_hasNoPatterns() {
        val days = List(9) { Mood.ANGRY to slept(300) }
        val result = MoodPatterns.find(data(*days.toTypedArray()), today)
        assertTrue(result.patterns.isEmpty())
        assertEquals(9, result.sampleDays)
    }

    @Test
    fun factorOnFewDays_isIgnored() {
        val work = DayContext(tags = setOf(PresetTag.WORK))
        val days = List(2) { Mood.ANGRY to work } + List(18) { Mood.CALM to DayContext(note = "x") }
        assertTrue(MoodPatterns.find(data(*days.toTypedArray()), today).patterns.isEmpty())
    }

    @Test
    fun atMostOnePatternPerFactor_andThreeTotal() {
        val all = DayContext(tags = setOf(PresetTag.WORK, PresetTag.STUDY, PresetTag.HEALTH, PresetTag.FAMILY))
        val days = List(6) { Mood.ANGRY to all } + List(14) { Mood.HAPPY to DayContext(note = "x") }
        val patterns = MoodPatterns.find(data(*days.toTypedArray()), today).patterns
        assertEquals(3, patterns.size)
        assertEquals(3, patterns.map { it.factor }.distinct().size)
    }

    @Test
    fun noContext_isEmpty() {
        val moods = (0 until 30).associate { today.minusDays(it.toLong()).toString() to Mood.HAPPY }
        assertEquals(PatternsResult.Empty, MoodPatterns.find(AppData(moods = moods), today))
    }
}

class TagsTest {

    @Test
    fun normalize_trimsAndCollapsesSpaces() {
        assertEquals("mi gym", normalizeTagLabel("  mi   gym "))
        assertEquals("", normalizeTagLabel("   "))
        assertEquals(AppData.MAX_TAG_LENGTH, normalizeTagLabel("a".repeat(50)).length)
    }
}
