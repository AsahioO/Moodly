package com.asahioo.moodly.ui.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asahioo.moodly.R
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.data.model.StressLevel
import com.asahioo.moodly.domain.MonthSummary
import com.asahioo.moodly.domain.MoodPatterns
import com.asahioo.moodly.domain.Pattern
import com.asahioo.moodly.domain.PatternsResult
import com.asahioo.moodly.ui.calendar.DayCell
import com.asahioo.moodly.ui.color
import com.asahioo.moodly.ui.components.AnimatedNumber
import com.asahioo.moodly.ui.components.CardShape
import com.asahioo.moodly.ui.components.MoodIcon
import com.asahioo.moodly.ui.components.NavHeader
import com.asahioo.moodly.ui.components.RollingText
import com.asahioo.moodly.ui.components.Screen
import com.asahioo.moodly.ui.components.SectionTitle
import com.asahioo.moodly.ui.components.Stagger
import com.asahioo.moodly.ui.components.bounceClick
import com.asahioo.moodly.ui.components.rememberStagger
import com.asahioo.moodly.ui.components.staggered
import com.asahioo.moodly.ui.labelRes
import com.asahioo.moodly.ui.monthYear
import com.asahioo.moodly.ui.sentence
import com.asahioo.moodly.ui.sundayIndex
import com.asahioo.moodly.ui.theme.LocalReduceMotion
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.Motion
import com.asahioo.moodly.ui.theme.Palette
import java.text.NumberFormat

@Composable
fun InsightsScreen(
    state: InsightsUiState,
    onOpenSleep: () -> Unit,
    /** Solo con el permiso de pasos concedido en Health Connect. */
    showSteps: Boolean,
    onOpenSettings: () -> Unit,
    onOpenStress: () -> Unit,
) {
    val pop = rememberStagger(Unit, totalMs = 1500)
    Screen {
        NavHeader(
            title = stringResource(R.string.insights_title),
            subtitle = monthYear(state.summary.month),
        )
        SectionTitle(stringResource(R.string.last_7_days))
        WeekStrip(state.lastWeek, pop)
        SectionTitle(stringResource(R.string.mood_distribution))
        DistributionCard(state.summary, pop)
        SectionTitle(stringResource(R.string.patterns_title))
        PatternsSection(state.patterns, pop)
        SectionTitle(stringResource(R.string.body_mind))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricRow(
                title = stringResource(R.string.sleep_last_night),
                color = Palette.Peach,
                onClick = onOpenSleep,
                modifier = Modifier.staggered(pop, 620, 700, 30.dp, easing = Motion.Navigation),
            ) {
                val sleep = state.sleepMinutes
                if (sleep == null) {
                    Text(stringResource(R.string.tap_to_log), style = MoodType.Label.copy(color = Palette.Ink.copy(alpha = 0.6f)))
                } else {
                    Row {
                        AnimatedNumber(sleep / 60, MoodType.Big.copy(fontSize = 24.sp), Modifier.alignByBaseline())
                        Text(stringResource(R.string.unit_h), style = MoodType.BigUnit.copy(fontSize = 15.sp), modifier = Modifier.alignByBaseline())
                        Spacer(Modifier.width(4.dp))
                        AnimatedNumber(sleep % 60, MoodType.Big.copy(fontSize = 24.sp), Modifier.alignByBaseline())
                        Text(stringResource(R.string.unit_min), style = MoodType.BigUnit.copy(fontSize = 15.sp), modifier = Modifier.alignByBaseline())
                    }
                }
            }
            MetricRow(
                title = stringResource(R.string.stress_level),
                color = Palette.Lavender,
                onClick = onOpenStress,
                modifier = Modifier.staggered(pop, 680, 700, 30.dp, easing = Motion.Navigation),
            ) {
                val stress = state.stress
                if (stress == null) {
                    Text(stringResource(R.string.take_quiz), style = MoodType.Label.copy(color = Palette.Ink.copy(alpha = 0.6f)))
                } else {
                    RollingText(stress, style = MoodType.Big.copy(fontSize = 24.sp), contentAlignment = Alignment.CenterEnd) { level: StressLevel ->
                        stringResource(level.labelRes)
                    }
                }
            }
            if (showSteps) {
                val steps = state.steps
                MetricRow(
                    title = stringResource(R.string.steps_today),
                    color = Palette.Lime,
                    onClick = onOpenSettings,
                    modifier = Modifier.staggered(pop, 740, 700, 30.dp, easing = Motion.Navigation),
                ) {
                    if (steps == null) {
                        Text(stringResource(R.string.steps_no_data), style = MoodType.Label.copy(color = Palette.Ink.copy(alpha = 0.6f)))
                    } else {
                        Row {
                            AnimatedNumber(
                                steps,
                                MoodType.Big.copy(fontSize = 24.sp),
                                Modifier.alignByBaseline(),
                                format = { NumberFormat.getIntegerInstance().format(it) },
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.unit_steps), style = MoodType.BigUnit.copy(fontSize = 15.sp), modifier = Modifier.alignByBaseline())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatternsSection(result: PatternsResult, pop: Stagger) {
    if (result.patterns.isEmpty()) {
        val missing = MoodPatterns.MIN_SAMPLE - result.sampleDays
        Column(
            Modifier
                .fillMaxWidth()
                .staggered(pop, 580, 700, 24.dp, easing = Motion.Navigation)
                .clip(CardShape)
                .background(Palette.Mist)
                .padding(16.dp),
        ) {
            Text(
                if (missing > 0) pluralStringResource(R.plurals.patterns_need_days, missing, missing)
                else stringResource(R.string.patterns_none_yet),
                style = MoodType.Body.copy(color = Palette.Grey2),
            )
            if (missing > 0) {
                val progress by animateFloatAsState(
                    result.sampleDays / MoodPatterns.MIN_SAMPLE.toFloat(),
                    tween(900, 300, Motion.EaseOut),
                    label = "patternsProgress",
                )
                Box(
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Palette.Track),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Palette.Ink),
                    )
                }
            }
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        result.patterns.forEachIndexed { i, pattern ->
            PatternCard(pattern, Modifier.staggered(pop, 580 + i * 70, 700, 24.dp, easing = Motion.Navigation))
        }
        Text(
            stringResource(R.string.patterns_disclaimer),
            style = MoodType.Base.copy(fontSize = 11.5.sp, color = Palette.Grey),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
    }
}

@Composable
private fun PatternCard(pattern: Pattern, modifier: Modifier) {
    val (first, second) = pattern.group.moods.toList()
    Row(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Palette.Mist)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Las dos caras del grupo, encimadas.
        Box(Modifier.size(width = 50.dp, height = 36.dp).clearAndSetSemantics { }) {
            MoodIcon(second, Modifier.size(32.dp).align(Alignment.CenterEnd))
            MoodIcon(first, Modifier.size(32.dp).align(Alignment.CenterStart))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(pattern.sentence(), style = MoodType.Body)
            Text(
                pluralStringResource(R.plurals.pattern_based_on, pattern.daysWith, pattern.daysWith),
                style = MoodType.Label.copy(fontWeight = FontWeight.Normal, color = Palette.Grey2),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Distribución del mes: total, una barra segmentada que crece de izquierda a derecha y el conteo por ánimo. */
@Composable
private fun DistributionCard(summary: MonthSummary, pop: Stagger) {
    val entries = summary.counts.filterValues { it > 0 }.entries.sortedByDescending { it.value }
    Column(
        Modifier
            .fillMaxWidth()
            .staggered(pop, 300, 700, 24.dp, easing = Motion.Navigation)
            .clip(CardShape)
            .background(Palette.Mist)
            .padding(16.dp),
    ) {
        Row {
            AnimatedNumber(
                summary.logged,
                MoodType.Base.copy(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
                Modifier.alignByBaseline(),
            )
            Text(
                stringResource(R.string.days_logged),
                style = MoodType.Caption,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .alignByBaseline(),
            )
        }
        SegmentBar(
            entries.map { it.key to it.value },
            summary.logged,
            Modifier
                .padding(top = 12.dp, bottom = 14.dp)
                .fillMaxWidth()
                .height(14.dp),
        )
        if (entries.isEmpty()) {
            Text(stringResource(R.string.no_entries_month), style = MoodType.Body.copy(color = Palette.Grey2))
        }
        // Dos columnas: los ánimos ya van ordenados por frecuencia.
        entries.chunked(2).forEachIndexed { row, pair ->
            Row(Modifier.padding(top = if (row == 0) 0.dp else 10.dp)) {
                pair.forEachIndexed { col, (mood, count) ->
                    Row(
                        Modifier
                            .weight(1f)
                            .staggered(pop, 420 + (row * 2 + col) * 50, 500, 0.dp, 0.9f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MoodIcon(mood, Modifier.size(24.dp))
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(stringResource(mood.labelRes), style = MoodType.Label, maxLines = 1)
                            Text(pluralStringResource(R.plurals.days_count, count, count), style = MoodType.Tiny)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Barra de proporciones: un segmento redondeado por ánimo, crece de izquierda a derecha al entrar. */
@Composable
private fun SegmentBar(segments: List<Pair<Mood, Int>>, total: Int, modifier: Modifier) {
    val reduce = LocalReduceMotion.current
    val grow = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(900, 200, Motion.EaseOut)) }
    Canvas(modifier.clearAndSetSemantics { }) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(Palette.Track, cornerRadius = radius)
        if (total == 0) return@Canvas
        val gap = 3.dp.toPx()
        val usable = size.width - gap * (segments.size - 1)
        var x = 0f
        segments.forEach { (mood, count) ->
            val w = usable * count / total * grow.value
            if (w > 0.5f) drawRoundRect(mood.color, Offset(x, 0f), Size(w, size.height), radius)
            x += w + gap * grow.value
        }
    }
}

/** Semana en blobs: el día sin registro es un punto; hoy lleva su inicial en cápsula. */
@Composable
private fun WeekStrip(days: List<DayCell>, pop: Stagger) {
    val letters = stringArrayResource(R.array.weekdays_short)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        days.forEachIndexed { i, day ->
            Column(
                Modifier
                    .weight(1f)
                    .staggered(pop, 60 + i * 45, 520, 0.dp, 0.5f, Motion.Back),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    val mood = day.mood
                    if (mood != null) {
                        MoodIcon(mood, Modifier.fillMaxSize())
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize(0.42f)
                                .clip(CircleShape)
                                .background(Palette.Mist2),
                        )
                    }
                }
                Text(
                    letters[day.date.sundayIndex()].take(1),
                    style = MoodType.Base.copy(
                        fontSize = 11.sp,
                        color = if (day.isToday) Palette.Paper else Palette.Grey,
                        fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(CircleShape)
                        .background(if (day.isToday) Palette.Ink else Color.Transparent)
                        .padding(horizontal = 7.dp, vertical = 1.dp),
                )
            }
        }
    }
}

/** Fila de "Cuerpo y mente": título a la izquierda y el valor a la derecha, sobre el color de su tarjeta. */
@Composable
private fun MetricRow(
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier,
    value: @Composable () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .bounceClick(onClick = onClick, pressedScale = 0.97f)
            .clip(CircleShape)
            .background(color)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MoodType.Label, modifier = Modifier.weight(1f))
        value()
    }
}
