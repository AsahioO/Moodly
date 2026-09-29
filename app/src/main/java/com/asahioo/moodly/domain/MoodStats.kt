package com.asahioo.moodly.domain

import com.asahioo.moodly.data.model.Mood
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

data class MonthSummary(
    val month: YearMonth,
    val counts: Map<Mood, Int>,
    val logged: Int,
    val elapsedDays: Int,
    val topMood: Mood?,
    val discipline: Int,
) {
    companion object {
        fun empty(month: YearMonth) = MonthSummary(
            month = month,
            counts = Mood.entries.associateWith { 0 },
            logged = 0,
            elapsedDays = 0,
            topMood = null,
            discipline = 0,
        )
    }
}

object MoodStats {

    fun month(moods: Map<String, Mood>, month: YearMonth, today: LocalDate): MonthSummary {
        val counts = Mood.entries.associateWithTo(LinkedHashMap()) { 0 }
        var logged = 0
        var elapsed = 0
        for (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            if (date.isAfter(today)) break
            elapsed++
            moods[date.toString()]?.let { mood ->
                counts[mood] = counts.getValue(mood) + 1
                logged++
            }
        }
        // En empate gana el primero en el orden de Mood.entries.
        val top = counts.entries.filter { it.value > 0 }.maxByOrNull { it.value }?.key
        return MonthSummary(
            month = month,
            counts = counts,
            logged = logged,
            elapsedDays = elapsed,
            topMood = top,
            discipline = if (elapsed == 0) 0 else (logged * 100f / elapsed).roundToInt(),
        )
    }

    /**
     * Días consecutivos con registro que terminan hoy. Si hoy aún no hay registro cuenta desde
     * ayer, para que la racha no caiga a 0 mientras el día sigue abierto.
     */
    fun streak(moods: Map<String, Mood>, today: LocalDate): Int {
        var day = if (moods.containsKey(today.toString())) today else today.minusDays(1)
        var count = 0
        while (moods.containsKey(day.toString())) {
            count++
            day = day.minusDays(1)
        }
        return count
    }
}
