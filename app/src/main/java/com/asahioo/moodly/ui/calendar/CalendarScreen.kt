package com.asahioo.moodly.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.asahioo.moodly.R
import com.asahioo.moodly.domain.MonthSummary
import com.asahioo.moodly.ui.color
import com.asahioo.moodly.ui.components.AnimatedNumber
import com.asahioo.moodly.ui.components.AppIcons
import com.asahioo.moodly.ui.components.CircleIconButton
import com.asahioo.moodly.ui.components.Glyph
import com.asahioo.moodly.ui.components.LocalHaptics
import com.asahioo.moodly.ui.components.MoodFace
import com.asahioo.moodly.ui.components.MoodGlyphs
import com.asahioo.moodly.ui.components.NavHeader
import com.asahioo.moodly.ui.components.PanelShape
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
    onBack: () -> Unit,
    onPickMonth: () -> Unit,
    onSearch: () -> Unit,
    onDayClick: (DayCell) -> Unit,
    onStep: (Int) -> Boolean,
) {
    // Se reinicia cada vez que la pantalla entra: las celdas "brotan" en diagonal.
    val pop = rememberStagger(Unit, totalMs = 1600)

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
                    title = stringResource(R.string.calendar_title),
                    subtitle = monthYear(state.month),
                    onBack = onBack,
                    action = {
                        CircleIconButton(
                            AppIcons.CalendarCheck,
                            stringResource(R.string.cd_pick_month),
                            onPickMonth,
                            bordered = false,
                        )
                    },
                )
                Row(
                    Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .bounceClick(onClick = onSearch, pressedScale = 0.98f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.Mist)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Glyph(AppIcons.Search, Modifier.size(18.dp))
                    Text(stringResource(R.string.search_open), style = MoodType.Body.copy(color = Palette.Grey2))
                }
                WeekdayRow(Modifier.padding(top = 16.dp, bottom = 8.dp))
                SwipeableMonth(state, pop, onDayClick, onStep)
                SummaryCard(state.summary, pop)
                StatsRow(state.summary, pop)
            }
        }
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
                            .aspectRatio(1.2f),
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

@Composable
private fun DayCellView(cell: DayCell, onClick: () -> Unit, modifier: Modifier) {
    val target = cell.mood?.color ?: if (cell.isFuture) Palette.Future else Palette.Mist
    val bg by animateColorAsState(target, tween(350), label = "cell")
    val shape = RoundedCornerShape(9.dp)
    val monthName = monthName(cell.date.monthValue)
    val cd = when {
        cell.mood != null -> stringResource(R.string.cd_day_logged, cell.date.dayOfMonth, monthName, stringResource(cell.mood.labelRes)) +
            (if (cell.hasContext) stringResource(R.string.cd_day_has_context) else "")
        cell.isFuture -> stringResource(R.string.cd_day_future, cell.date.dayOfMonth, monthName)
        else -> stringResource(R.string.cd_day_empty, cell.date.dayOfMonth, monthName)
    }
    Box(
        modifier
            .bounceClick(onClick = onClick, pressedScale = 0.86f, haptic = !cell.isFuture)
            .clip(shape)
            .background(bg)
            .then(if (cell.isToday) Modifier.border(2.dp, Palette.Ink, shape) else Modifier)
            .clearAndSetSemantics { contentDescription = cd },
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
                MoodFace(
                    mood,
                    Modifier
                        .fillMaxWidth(0.66f)
                        .aspectRatio(1f),
                )
            } else {
                Box(Modifier.size(1.dp))
            }
        }
        if (cell.hasContext) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background((if (cell.mood != null) Palette.FaceInk else Palette.Ink).copy(alpha = 0.4f)),
            )
        }
    }
}

@Composable
private fun SummaryCard(summary: MonthSummary, pop: Stagger) {
    val top = summary.topMood
    val bg by animateColorAsState(top?.color ?: Palette.Mist, tween(500), label = "summary")
    Box(
        Modifier
            .padding(top = 16.dp)
            .staggered(pop, 260, 700, 30.dp, easing = Motion.Navigation)
            .fillMaxWidth()
            .heightIn(min = 132.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bg),
    ) {
        // Los colores de ánimo son claros en todos los temas: encima va la tinta fija de las caras.
        val ink = if (top != null) Palette.FaceInk else Palette.Ink
        CompositionLocalProvider(LocalContentColor provides ink) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 18.dp)) {
            Text(stringResource(R.string.month_summary), style = MoodType.Label)
            RollingText(
                top,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                style = MoodType.SummaryTitle,
            ) { mood -> if (mood == null) stringResource(R.string.no_entries) else stringResource(mood.labelRes) }
            Text(
                if (top == null) stringResource(R.string.no_entries_msg) else stringResource(top.messageRes),
                style = MoodType.Small.copy(color = ink.copy(alpha = 0.58f)),
                modifier = Modifier.fillMaxWidth(0.58f),
            )
        }
        }
        AnimatedContent(
            targetState = top,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 26.dp, y = 18.dp)
                .size(150.dp),
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.55f, stiffness = 260f), initialScale = 0.6f) + fadeIn(tween(200))) togetherWith
                    fadeOut(tween(150))
            },
            label = "bigFace",
        ) { mood ->
            if (mood != null) {
                Glyph(
                    MoodGlyphs.bigFace(mood),
                    tint = Palette.FaceInk,
                    modifier = Modifier
                        .fillMaxSize()
                        .staggered(pop, 380, 900, 0.dp, 0.7f, Motion.Back),
                )
            } else {
                Box(Modifier.size(1.dp))
            }
        }
    }
}

@Composable
private fun StatsRow(summary: MonthSummary, pop: Stagger) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatCard(
            label = stringResource(R.string.logged),
            sub = stringResource(R.string.days_unit),
            modifier = Modifier
                .weight(1f)
                .staggered(pop, 340, 650, 24.dp, easing = Motion.Navigation),
        ) {
            AnimatedNumber(summary.logged, MoodType.Stat, delayMs = 250)
        }
        StatCard(
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

@Composable
private fun StatCard(label: String, sub: String, modifier: Modifier, value: @Composable () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Mist)
            .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 13.dp),
    ) {
        Text(label, style = MoodType.Label, maxLines = 1)
        Box(Modifier.padding(top = 20.dp)) { value() }
        Text(sub, style = MoodType.Tiny, maxLines = 1)
    }
}
