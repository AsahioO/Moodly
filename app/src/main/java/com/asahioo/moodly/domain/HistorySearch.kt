package com.asahioo.moodly.domain

import com.asahioo.moodly.data.model.CustomTag
import com.asahioo.moodly.data.model.DayContext
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.data.model.PresetTag
import java.time.LocalDate

data class HistoryEntry(val date: LocalDate, val mood: Mood, val context: DayContext?)

object HistorySearch {

    /**
     * Días con registro, del más reciente al más antiguo. Todos los filtros se combinan con AND:
     * [query] busca en la nota y en las etiquetas propias del día; [mood] y las etiquetas exigen coincidencia exacta.
     */
    fun search(
        moods: Map<String, Mood>,
        days: Map<String, DayContext>,
        customTags: List<CustomTag>,
        query: String,
        mood: Mood? = null,
        presetTags: Set<PresetTag> = emptySet(),
        customTagIds: Set<String> = emptySet(),
    ): List<HistoryEntry> {
        val text = query.trim()
        val labels = customTags.associate { it.id to it.label }
        return moods.entries
            .sortedByDescending { it.key }
            .mapNotNull { (key, dayMood) ->
                val context = days[key]
                val matches = (mood == null || dayMood == mood) &&
                    (context?.tags.orEmpty().containsAll(presetTags)) &&
                    (context?.customTags.orEmpty().containsAll(customTagIds)) &&
                    (text.isEmpty() || context != null && (
                        context.note.contains(text, ignoreCase = true) ||
                            context.customTags.any { labels[it]?.contains(text, ignoreCase = true) == true }
                        ))
                if (matches) HistoryEntry(LocalDate.parse(key), dayMood, context) else null
            }
    }
}
