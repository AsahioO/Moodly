package com.asahioo.moodly.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asahioo.moodly.R
import com.asahioo.moodly.data.model.CustomTag
import com.asahioo.moodly.data.model.DayContext
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.data.model.PresetTag
import com.asahioo.moodly.domain.HistoryEntry
import com.asahioo.moodly.domain.HistorySearch
import com.asahioo.moodly.ui.components.MoodIcon
import com.asahioo.moodly.ui.components.NavHeader
import com.asahioo.moodly.ui.components.PanelShape
import com.asahioo.moodly.ui.components.SectionTitle
import com.asahioo.moodly.ui.components.bounceClick
import com.asahioo.moodly.ui.labelRes
import com.asahioo.moodly.ui.shortDate
import com.asahioo.moodly.ui.sheets.SheetTextField
import com.asahioo.moodly.ui.sheets.TagChip
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.Palette
import java.time.LocalDate

/** ponytail: tope de filas para no componer cientos de golpe; paginar (LazyColumn) si el historial crece mucho. */
private const val MAX_RESULTS = 100
private const val QUERY_MAX = 60

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    moods: Map<String, Mood>,
    days: Map<String, DayContext>,
    customTags: List<CustomTag>,
    onBack: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var mood by rememberSaveable { mutableStateOf<Mood?>(null) }
    var presetTags by remember { mutableStateOf(emptySet<PresetTag>()) }
    var customIds by remember { mutableStateOf(emptySet<String>()) }

    val results = remember(moods, days, customTags, query, mood, presetTags, customIds) {
        HistorySearch.search(moods, days, customTags, query, mood, presetTags, customIds)
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Palette.Night),
    ) {
        val viewport = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewport)
                    .clip(PanelShape)
                    .background(Palette.Paper)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 22.dp),
            ) {
                NavHeader(
                    title = stringResource(R.string.search_title),
                    subtitle = stringResource(R.string.search_count, results.size),
                    onBack = onBack,
                )
                SheetTextField(
                    value = query,
                    onValueChange = { query = it.take(QUERY_MAX) },
                    hint = stringResource(R.string.search_hint),
                    singleLine = true,
                    modifier = Modifier.padding(top = 22.dp),
                )

                SectionTitle(stringResource(R.string.search_mood))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Mood.entries.forEach { m ->
                        TagChip(stringResource(m.labelRes), selected = mood == m) { mood = if (mood == m) null else m }
                    }
                }

                SectionTitle(stringResource(R.string.day_tags))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PresetTag.entries.forEach { tag ->
                        TagChip(stringResource(tag.labelRes), selected = tag in presetTags) {
                            presetTags = if (tag in presetTags) presetTags - tag else presetTags + tag
                        }
                    }
                    customTags.forEach { tag ->
                        TagChip(tag.label, selected = tag.id in customIds) {
                            customIds = if (tag.id in customIds) customIds - tag.id else customIds + tag.id
                        }
                    }
                }

                SectionTitle(stringResource(R.string.search_results))
                if (results.isEmpty()) {
                    Text(stringResource(R.string.search_empty), style = MoodType.Caption)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        results.take(MAX_RESULTS).forEach { entry -> ResultRow(entry, customTags, onOpenDay) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(entry: HistoryEntry, customTags: List<CustomTag>, onOpenDay: (LocalDate) -> Unit) {
    val context = entry.context
    val tagLabels = context?.tags.orEmpty().map { stringResource(it.labelRes) } +
        customTags.filter { it.id in context?.customTags.orEmpty() }.map { it.label }
    Row(
        Modifier
            .fillMaxWidth()
            .bounceClick(onClick = { onOpenDay(entry.date) }, pressedScale = 0.98f)
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Mist)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MoodIcon(entry.mood, Modifier.size(40.dp))
        Column(Modifier.weight(1f)) {
            Text(
                shortDate(entry.date) + " · " + stringResource(entry.mood.labelRes),
                style = MoodType.Chip,
            )
            val note = context?.note.orEmpty()
            if (note.isNotEmpty()) Text(note, style = MoodType.Body, maxLines = 2)
            if (tagLabels.isNotEmpty()) {
                Text(tagLabels.joinToString(" · "), style = MoodType.Caption, maxLines = 1)
            }
        }
    }
}
