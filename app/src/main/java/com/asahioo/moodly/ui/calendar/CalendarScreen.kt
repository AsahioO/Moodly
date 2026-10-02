package com.asahioo.moodly.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.asahioo.moodly.R
import com.asahioo.moodly.domain.MonthSummary
import com.asahioo.moodly.ui.components.AnimatedNumber
import com.asahioo.moodly.ui.components.AppIcons
import com.asahioo.moodly.ui.components.CircleIconButton
import com.asahioo.moodly.ui.components.CardShape
import com.asahioo.moodly.ui.components.LocalHaptics
import com.asahioo.moodly.ui.components.MoodIcon
import com.asahioo.moodly.ui.components.Screen
import com.asahioo.moodly.ui.components.NavHeader
import com.asahioo.moodly.ui.components.RollingText
import com.asahioo.moodly.ui.components.Stagger
import com.asahioo.moodly.ui.components.bounceClick
import com.asahioo.moodly.ui.components.rememberStagger
import com.asahioo.moodly.ui.components.staggered
import com.asahioo.moodly.ui.labelRes
import com.asahioo.moodly.ui.messageRes
import com.asahioo.moodly.ui.monthName
import com.asahioo.moodly.ui.monthYear
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.Motion
import com.asahioo.moodly.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPickMonth: () -> Unit,
    onSearch: () -> Unit,
    onDayClick: (DayCell) -> Unit,
    onStep: (Int) -> Boolean,
) {
    // Se reinicia cada vez que la pantalla entra: las celdas "brotan" en diagonal.
    val pop = rememberStagger(Unit, totalMs = 1600)

    Screen {
        NavHeader(
            title = stringResource(R.string.calendar_title),
            subtitle = monthYear(state.month),
            actions = {
                CircleIconButton(AppIcons.Search, stringResource(R.string.search_open), onSearch)
                Spacer(Modifier.width(8.dp))
                CircleIconButton(AppIcons.CalendarCheck, stringResource(R.string.cd_pick_month), onPickMonth)
            },
        )
        WeekdayRow(Modifier.padding(top = 20.dp, bottom = 6.dp))
        SwipeableMonth(state, pop, onDayClick, onStep)
        SummaryCard(state.summary, pop)
        StatsRow(state.summary, pop)
    }
}

@Composable
private fun WeekdayRow(modifier: Modifier) {
    val names = stringArrayResource(R.array.weekdays_short)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        names.forEach { name ->
            Text(
                name,
                style = MoodType.Base.copy(fontSize = MoodType.Tiny.fontSize, color = Palette.Grey),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { },
            )
        }
    }
}

/** Cuadrícula del mes; se arrastra horizontalmente para cambiar de mes con rebote en los límites. */
@Composable
private fun SwipeableMonth(
    state: CalendarUiState,
    pop: Stagger,
    onDayClick: (DayCell) -> Unit,
    onStep: (Int) -> Boolean,
) {
    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHaptics.current
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }

    Box(
        Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val dx = drag.value
                        if (abs(dx) > threshold) {
                            val moved = onStep(if (dx < 0) 1 else -1)
                            if (moved) haptics.tick() else haptics.reject()
                        }
                        scope.launch { drag.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 380f)) }
                    },
                    onDragCancel = {
                        scope.launch { drag.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 380f)) }
                    },
                ) { change, amount ->
                    change.consume()
                    scope.launch { drag.snapTo(drag.value + amount * 0.55f) }
                }
            }
            .graphicsLayer { translationX = drag.value },
    ) {
        AnimatedContent(
            targetState = state,
            contentKey = { it.month },
            transitionSpec = {
                val dir = if (targetState.month > initialState.month) 1 else -1
                (slideInHorizontally(tween(440, easing = Motion.EaseOut)) { it * dir / 2 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(220, easing = Motion.EaseIn)) { -it * dir * 6 / 10 } + fadeOut(tween(200)))
            },
            label = "month",
        ) { monthState ->
            MonthGrid(monthState, pop, onDayClick)
        }
    }
}

@Composable
private fun MonthGrid(state: CalendarUiState, pop: Stagger, onDayClick: (DayCell) -> Unit) {
    val cells: List<DayCell?> = remember(state) { List(state.leadingBlanks) { null } + state.days }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cells.chunked(7).forEachIndexed { row, week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 7) {
                    val cell = week.getOrNull(col)
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(0.78f),
                    ) {
                        if (cell != null) {
                            DayCellView(
                                cell = cell,
                                onClick = { onDayClick(cell) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .staggered(pop, 120 + (row + col) * 28, 520, 0.dp, 0.5f, Motion.Back),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Día del mes: el blob del ánimo (o un punto vacío) y el número debajo; hoy lleva el número en cápsula. */
@Composable
private fun DayCellView(cell: DayCell, onClick: () -> Unit, modifier: Modifier) {
    val monthName = monthName(cell.date.monthValue)
    val cd = when {
        cell.mood != null -> stringResource(R.string.cd_day_logged, cell.date.dayOfMonth, monthName, stringResource(cell.mood.labelRes)) +
            (if (cell.hasContext) stringResource(R.string.cd_day_has_context) else "")
        cell.isFuture -> stringResource(R.string.cd_day_future, cell.date.dayOfMonth, monthName)
        else -> stringResource(R.string.cd_day_empty, cell.date.dayOfMonth, monthName)
    }
    Column(
        modifier
            .bounceClick(onClick = onClick, pressedScale = 0.86f, haptic = !cell.isFuture)
            .clearAndSetSemantics { contentDescription = cd },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = cell.mood,
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = 0.45f, stiffness = 420f), initialScale = 0.3f) + fadeIn(tween(150))) togetherWith
                        fadeOut(tween(120))
                },
                label = "face",
            ) { mood ->
                if (mood != null) {
                    MoodIcon(mood, Modifier.fillMaxSize())
                } else {
                    Box(
                        Modifier
                            .fillMaxSize(0.42f)
                            .clip(CircleShape)
                            .background(if (cell.isFuture) Palette.Future else Palette.Mist2),
                    )
                }
            }
        }
        Row(
            Modifier
                .padding(top = 2.dp)
                .clip(CircleShape)
                .background(if (cell.isToday) Palette.Ink else Color.Transparent)
                .padding(horizontal = 6.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                cell.date.dayOfMonth.toString(),
                style = MoodType.Tiny.copy(
                    color = if (cell.isToday) Palette.Paper else Palette.Grey2,
                    fontWeight = if (cell.isToday) FontWeight.SemiBold else FontWeight.Normal,
                ),
            )
            if (cell.hasContext) {
                Box(
                    Modifier
                        .padding(start = 3.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (cell.isToday) Palette.Paper else Palette.Grey2),
                )
            }
        }
    }
}

/** Ánimo principal del mes: su blob a la izquierda y el mensaje al lado, sobre una tarjeta neutra. */
@Composable
private fun SummaryCard(summary: MonthSummary, pop: Stagger) {
    val top = summary.topMood
    Row(
        Modifier
            .padding(top = 18.dp)
            .staggered(pop, 260, 700, 30.dp, easing = Motion.Navigation)
            .fillMaxWidth()
            .clip(CardShape)
            .background(Palette.Mist)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = top,
            modifier = Modifier.size(72.dp),
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.55f, stiffness = 260f), initialScale = 0.6f) + fadeIn(tween(200))) togetherWith
                    fadeOut(tween(150))
            },
            label = "topMood",
        ) { mood ->
            if (mood != null) {
                MoodIcon(mood, Modifier.fillMaxSize())
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Palette.Mist2),
                )
            }
        }
        Column(
            Modifier
                .padding(start = 14.dp)
                .weight(1f),
        ) {
            Text(stringResource(R.string.month_summary), style = MoodType.Caption)
            RollingText(
                top,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                style = MoodType.ScreenTitle,
            ) { mood -> if (mood == null) stringResource(R.string.no_entries) else stringResource(mood.labelRes) }
            Text(
                if (top == null) stringResource(R.string.no_entries_msg) else stringResource(top.messageRes),
                style = MoodType.Small.copy(color = Palette.Grey2),
            )
        }
    }
}

@Composable
private fun StatsRow(summary: MonthSummary, pop: Stagger) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatPill(
            label = stringResource(R.string.logged),
            sub = stringResource(R.string.days_unit),
            modifier = Modifier
                .weight(1f)
                .staggered(pop, 340, 650, 24.dp, easing = Motion.Navigation),
        ) {
            AnimatedNumber(summary.logged, MoodType.Stat, delayMs = 250)
        }
        StatPill(
            label = stringResource(R.string.discipline),
            sub = stringResource(R.string.focus_score),
            modifier = Modifier
                .weight(1f)
                .staggered(pop, 410, 650, 24.dp, easing = Motion.Navigation),
        ) {
            Row {
                AnimatedNumber(summary.discipline, MoodType.Stat, Modifier.alignByBaseline(), delayMs = 250)
                Text("%", style = MoodType.Stat, modifier = Modifier.alignByBaseline())
            }
        }
    }
}

/** Cifra del mes en una píldora: valor a la izquierda, qué mide a la derecha. */
@Composable
private fun StatPill(label: String, sub: String, modifier: Modifier, value: @Composable () -> Unit) {
    Row(
        modifier
            .clip(CircleShape)
            .border(1.5.dp, Palette.Mist2, CircleShape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        value()
        Column(Modifier.padding(start = 10.dp)) {
            Text(label, style = MoodType.Label, maxLines = 1)
            Text(sub, style = MoodType.Tiny, maxLines = 1)
        }
    }
}
