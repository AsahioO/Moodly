package com.asahioo.moodly.domain

import com.asahioo.moodly.data.model.AppData
import com.asahioo.moodly.data.model.CustomTag
import com.asahioo.moodly.data.model.DayContext
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.data.model.PresetTag
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ln

/** Ánimos que se leen juntos en las frases ("Enojado o Estresado"). */
enum class MoodGroup(val moods: Set<Mood>) {
    TENSE(setOf(Mood.ANGRY, Mood.STRESSED)),
    GOOD(setOf(Mood.HAPPY, Mood.CALM)),
    LOW(setOf(Mood.SLEEPY, Mood.BORED)),
}

sealed interface Factor {
    data class Preset(val tag: PresetTag) : Factor
    data class Custom(val tag: CustomTag) : Factor
    data object ShortSleep : Factor
}

/** Cuántas veces más (o menos) aparece el grupo los días con el factor. */
enum class Magnitude { TRIPLE, DOUBLE, MORE, HALF, LESS }

data class Pattern(val factor: Factor, val group: MoodGroup, val magnitude: Magnitude, val daysWith: Int)

data class PatternsResult(val patterns: List<Pattern>, val sampleDays: Int) {
    companion object {
        val Empty = PatternsResult(emptyList(), 0)
    }
}

/**
 * Busca coincidencias entre el contexto de los días y los ánimos registrados. Compara la
 * frecuencia de cada grupo de ánimo los días con y sin cada factor; son correlaciones, no causas.
 */
object MoodPatterns {

    const val WINDOW_DAYS = 90L
    const val MIN_SAMPLE = 10
    const val SHORT_SLEEP_MINUTES = 360
    private const val MIN_SIDE = 3
    private const val MIN_HITS = 2
    private const val MIN_RATIO = 1.5
    private const val MAX_RESULTS = 3

    private class Day(val mood: Mood, val context: DayContext?)

    fun find(data: AppData, today: LocalDate): PatternsResult {
        // Solo desde el primer día con contexto: el historial previo diluiría todo. Los vacíos ya se podaron.
        val firstWithContext = data.days.keys.minOrNull() ?: return PatternsResult.Empty
        val from = maxOf(today.minusDays(WINDOW_DAYS - 1).toString(), firstWithContext)
        val to = today.toString()
        // Las fechas ISO-8601 ordenan igual como texto que como fecha.
        val sample = data.moods.filterKeys { it in from..to }.map { (key, mood) -> Day(mood, data.days[key]) }
        if (sample.size < MIN_SAMPLE) return PatternsResult(emptyList(), sample.size)

        val splits = buildList {
            PresetTag.entries.forEach { tag ->
                add(Factor.Preset(tag) to sample.partition { it.context?.tags?.contains(tag) == true })
            }
            data.customTags.forEach { tag ->
                add(Factor.Custom(tag) to sample.partition { it.context?.customTags?.contains(tag.id) == true })
            }
            // Los días sin sueño registrado no cuentan como "durmió bien".
            val slept = sample.filter { it.context?.sleepMinutes != null }
            add(Factor.ShortSleep to slept.partition { it.context!!.sleepMinutes!! < SHORT_SLEEP_MINUTES })
        }

        val patterns = splits.mapNotNull { (factor, split) ->
            val (with, without) = split
            if (with.size < MIN_SIDE || without.size < MIN_SIDE) return@mapNotNull null
            MoodGroup.entries.mapNotNull { group ->
                val hitsWith = with.count { it.mood in group.moods }
                val hitsWithout = without.count { it.mood in group.moods }
                // Suavizado de Laplace: evita dividir entre cero y encoge los ratios de muestras chicas.
                val ratio = ((hitsWith + 1.0) / (with.size + 2)) / ((hitsWithout + 1.0) / (without.size + 2))
                val strong = (ratio >= MIN_RATIO && hitsWith >= MIN_HITS) ||
                    (ratio <= 1 / MIN_RATIO && hitsWithout >= MIN_HITS)
                if (strong) Pattern(factor, group, magnitude(ratio), with.size) to abs(ln(ratio)) else null
            }.maxByOrNull { it.second }
        }
            .sortedByDescending { it.second }
            .take(MAX_RESULTS)
            .map { it.first }

        return PatternsResult(patterns, sample.size)
    }

    internal fun magnitude(ratio: Double): Magnitude = when {
        ratio >= 2.75 -> Magnitude.TRIPLE
        ratio >= 1.75 -> Magnitude.DOUBLE
        ratio >= 1.0 -> Magnitude.MORE
        ratio <= 0.5 -> Magnitude.HALF
        else -> Magnitude.LESS
    }
}
