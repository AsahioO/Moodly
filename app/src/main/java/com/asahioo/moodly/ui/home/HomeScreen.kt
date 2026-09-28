package com.asahioo.moodly.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.asahioo.moodly.R
import com.asahioo.moodly.data.model.AppData
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.data.model.StressLevel
import com.asahioo.moodly.domain.SeededRandom
import com.asahioo.moodly.ui.QuizQuestions
import com.asahioo.moodly.ui.barFactor
import com.asahioo.moodly.ui.color
import com.asahioo.moodly.ui.components.AnimatedNumber
import com.asahioo.moodly.ui.components.AppIcons
import com.asahioo.moodly.ui.components.Avatar
import com.asahioo.moodly.ui.components.CardHeader
import com.asahioo.moodly.ui.components.CardShape
import com.asahioo.moodly.ui.components.CircleIconButton
import com.asahioo.moodly.ui.components.EmptyValue
import com.asahioo.moodly.ui.components.Glyph
import com.asahioo.moodly.ui.components.MoodIcon
import com.asahioo.moodly.ui.components.PanelShape
import com.asahioo.moodly.ui.components.RollingText
import com.asahioo.moodly.ui.components.Stagger
import com.asahioo.moodly.ui.components.bounceClick
import com.asahioo.moodly.ui.components.staggered
import com.asahioo.moodly.ui.labelRes
import com.asahioo.moodly.ui.shortDate
import com.asahioo.moodly.ui.theme.LocalReduceMotion
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.Motion
import com.asahioo.moodly.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    state: HomeUiState,
    stagger: Stagger?,
    onOpenSettings: () -> Unit,
    onPickMood: (Mood) -> Unit,
    onAnswer: (Boolean) -> Unit,
    onRestartQuiz: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenStress: () -> Unit,
    onOpenContext: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Palette.Night),
    ) {
        val viewport = maxHeight
        var panelHeight by remember { mutableStateOf(0.dp) }
        var quizHeight by remember { mutableStateOf(0.dp) }
        // Las tarjetas superiores crecen para llenar la pantalla (mínimo 190dp).
        val topCardsHeight = max(190.dp, viewport - panelHeight - quizHeight - 18.dp)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { panelHeight = with(density) { it.height.toDp() } }
                    .clip(PanelShape)
                    .background(Palette.Paper)
                    .statusBarsPadding()
                    .padding(top = 8.dp, bottom = 18.dp),
            ) {
                HomeHeader(state.userName, onOpenSettings, Modifier.staggered(stagger, 0))
                Text(
                    shortDate(state.today),
                    style = MoodType.Base.copy(color = Palette.Grey, fontSize = MoodType.Chip.fontSize),
                    modifier = Modifier
                        .padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
                        .staggered(stagger, 40),
                )
                Greeting(state.firstName, stagger)
                MoodChips(state.todayMood, stagger, onPickMood)
                // Solo después de registrar: el toque rápido sigue siendo el camino principal.
                AnimatedVisibility(
                    visible = state.todayMood != null,
                    enter = expandVertically(tween(300, easing = Motion.EaseOut)) + fadeIn(tween(300)),
                    exit = shrinkVertically(tween(250)) + fadeOut(tween(200)),
                ) {
                    ContextRow(state, onOpenContext)
                }
            }

            Column(
                Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(topCardsHeight),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SleepCard(
                        minutes = state.sleepMinutes,
                        onClick = onOpenSleep,
                        modifier = Modifier
                            .weight(1f)
                            .staggered(stagger, 180, 800, 60.dp, 0.94f, Motion.Navigation),
                    )
                    StressCard(
                        level = state.stress,
                        onClick = onOpenStress,
                        modifier = Modifier
                            .weight(1f)
                            .staggered(stagger, 270, 800, 60.dp, 0.94f, Motion.Navigation),
                    )
                }
                QuizCard(
                    state = state,
                    onAnswer = onAnswer,
                    onRestart = onRestartQuiz,
                    modifier = Modifier
                        .onSizeChanged { quizHeight = with(density) { it.height.toDp() } }
                        .staggered(stagger, 360, 800, 60.dp, 0.94f, Motion.Navigation),
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(name: String, onOpenSettings: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val cd = stringResource(R.string.cd_profile)
        Row(
            Modifier
                .weight(1f)
                .bounceClick(onClick = onOpenSettings, pressedScale = 0.96f, onClickLabel = cd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(Modifier.size(40.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(stringResource(R.string.welcome_back), style = MoodType.Small.copy(color = Palette.Grey))
                if (name.isNotBlank()) Text(name, style = MoodType.Name)
            }
        }
        CircleIconButton(AppIcons.Menu, stringResource(R.string.cd_menu), onOpenSettings)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Greeting(firstName: String, stagger: Stagger?) {
    val text = if (firstName.isBlank()) stringResource(R.string.greeting_anon) else stringResource(R.string.greeting, firstName)
    val words = remember(text) { text.split(' ') }
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clearAndSetSemantics {
                contentDescription = text
                heading()
            },
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        words.forEachIndexed { i, word ->
            Text(word, style = MoodType.Greeting, modifier = Modifier.staggered(stagger, 80 + i * 32))
        }
    }
}

/** "＋ Agregar nota o etiquetas", o el resumen de lo que ya se agregó hoy. */
@Composable
private fun ContextRow(state: HomeUiState, onClick: () -> Unit) {
    val context = state.todayContext?.takeIf { it.hasEntry }
    val summary = if (context == null) {
        "+ " + stringResource(R.string.home_add_context)
    } else {
        val tags = context.tags.sortedBy { it.ordinal }.map { stringResource(it.labelRes) } +
            context.customTags.mapNotNull { state.customTagLabels[it] }
        val note = context.note.takeIf { it.isNotEmpty() }?.let { "“$it”" }
        (tags + listOfNotNull(note)).joinToString(" · ")
    }
    Text(
        summary,
        style = MoodType.Label.copy(color = if (context == null) Palette.Grey2 else Palette.Ink),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .bounceClick(onClick = onClick, pressedScale = 0.97f, onClickLabel = stringResource(R.string.cd_edit_context))
            .clip(CircleShape)
            .background(Palette.Mist)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun MoodChips(selected: Mood?, stagger: Stagger?, onPick: (Mood) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 8.dp, top = 22.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Mood.entries.forEachIndexed { i, mood ->
            MoodChip(
                mood = mood,
                selected = mood == selected,
                onClick = { onPick(mood) },
                modifier = Modifier.staggered(stagger, 260 + i * 45, 650, 14.dp, 0.85f, Motion.Back),
            )
        }
    }
}

@Composable
private fun MoodChip(mood: Mood, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val bg by animateColorAsState(if (selected) Palette.Ink else Palette.Mist, tween(300), label = "chipBg")
    val fg by animateColorAsState(if (selected) Palette.Paper else Palette.Ink, tween(300), label = "chipFg")
    val pop = remember { Animatable(0f) }
    val burst = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val reduce = LocalReduceMotion.current
    val label = stringResource(mood.labelRes)
    val color = mood.color

    Box(modifier.padding(end = 8.dp)) {
        Box(
            Modifier
                .bounceClick(
                    onClick = {
                        if (!reduce) {
                            scope.launch {
                                pop.snapTo(1f)
                                pop.animateTo(0f, spring(dampingRatio = 0.38f, stiffness = 260f))
                            }
                            scope.launch {
                                burst.snapTo(0f)
                                burst.animateTo(1f, tween(620, easing = Motion.EaseOut))
                            }
                        }
                        onClick()
                    },
                    pressedScale = 0.93f,
                    haptic = false,
                    onClickLabel = stringResource(R.string.cd_log_mood, label),
                )
                .height(34.dp)
                .clip(CircleShape)
                .background(bg)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MoodType.Chip.copy(color = fg))
        }
        MoodIcon(
            mood,
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-15).dp)
                .size(27.dp)
                .drawBehind {
                    val b = burst.value
                    if (b < 1f) {
                        val c = center
                        for (k in 0 until 8) {
                            val angle = (k / 8f) * 2f * PI.toFloat() + 0.2f
                            val dist = (22.dp.toPx() + (k % 3) * 5.dp.toPx()) * b
                            drawCircle(
                                color = if (k % 2 == 0) color else Palette.Ink,
                                radius = 4.5.dp.toPx() * (1f - 0.8f * b),
                                center = Offset(c.x + cos(angle) * dist, c.y + sin(angle) * dist),
                                alpha = 1f - b,
                            )
                        }
                    }
                }
                .graphicsLayer {
                    val p = pop.value
                    translationY = -12.dp.toPx() * p
                    scaleX = 1f + 0.3f * p
                    scaleY = 1f + 0.3f * p
                    rotationZ = -12f * p
                },
        )
    }
}

@Composable
private fun SleepCard(minutes: Int?, onClick: () -> Unit, modifier: Modifier) {
    val cd = if (minutes == null) {
        stringResource(R.string.cd_sleep_card_empty)
    } else {
        stringResource(R.string.cd_sleep_card, minutes / 60, minutes % 60)
    }
    Column(
        modifier
            .fillMaxSize()
            .bounceClick(onClick = onClick, pressedScale = 0.97f)
            .clip(CardShape)
            .background(Palette.Peach)
            .clearAndSetSemantics { contentDescription = cd }
            .padding(14.dp),
    ) {
        CardHeader(AppIcons.Bed, stringResource(R.string.sleep_duration))
        SleepBars(
            minutes ?: AppData.DEFAULT_SLEEP_MINUTES,
            Modifier
                .padding(top = 18.dp, bottom = 14.dp)
                .fillMaxWidth()
                .height(62.dp)
                .alpha(if (minutes == null) EMPTY_ALPHA else 1f),
        )
        Spacer(Modifier.weight(1f))
        if (minutes == null) {
            EmptyValue(stringResource(R.string.tap_to_log), MoodType.Big)
        } else {
            Row {
                AnimatedNumber(minutes / 60, MoodType.Big, Modifier.alignByBaseline())
                Text(stringResource(R.string.unit_h), style = MoodType.BigUnit, modifier = Modifier.alignByBaseline())
                Spacer(Modifier.width(6.dp))
                AnimatedNumber(minutes % 60, MoodType.Big, Modifier.alignByBaseline(), durationMs = 1100)
                Text(stringResource(R.string.unit_min), style = MoodType.BigUnit, modifier = Modifier.alignByBaseline())
            }
        }
    }
}

/** Barras del sueño: alturas deterministas según los minutos, crecen con rebote. */
@Composable
private fun SleepBars(minutes: Int, modifier: Modifier) {
    val bars = remember(minutes) {
        List(14) { i ->
            val h = 0.30f + SeededRandom.unit("sb$minutes-$i") * 0.70f
            val cap = 0.12f + SeededRandom.unit("st$minutes-$i") * 0.45f
            h to cap
        }
    }
    val reduce = LocalReduceMotion.current
    val grow = remember(minutes) { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(grow) { grow.animateTo(1f, tween(1300, delayMillis = 200)) }
    Canvas(modifier) {
        val n = bars.size
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        val radius = CornerRadius(4.dp.toPx())
        val t = grow.value * 1300f
        bars.forEachIndexed { i, (h, cap) ->
            val p = Motion.Back.transform(((t - i * 28f) / 700f).coerceIn(0f, 1f))
            val barH = size.height * h * p
            if (barH > 0.5f) {
                val left = i * (w + gap)
                val top = size.height - barH
                drawRoundRect(Palette.PeachInk, Offset(left, top), Size(w, barH), radius)
                drawRoundRect(Palette.PeachLite.copy(alpha = 0.8f), Offset(left, top), Size(w, barH * cap), radius)
            }
        }
    }
}

/** Opacidad de las barras decorativas mientras la tarjeta no tiene datos. */
private const val EMPTY_ALPHA = 0.35f

private val StressBase = floatArrayOf(0.09f, 0.09f, 0.09f, 0.22f, 0.34f, 0.46f, 0.60f, 0.78f, 1f)

@Composable
private fun StressCard(level: StressLevel?, onClick: () -> Unit, modifier: Modifier) {
    val cd = if (level == null) {
        stringResource(R.string.cd_stress_card_empty)
    } else {
        stringResource(R.string.cd_stress_card, stringResource(level.labelRes))
    }
    Column(
        modifier
            .fillMaxSize()
            .bounceClick(onClick = onClick, pressedScale = 0.97f)
            .clip(CardShape)
            .background(Palette.Lavender)
            .clearAndSetSemantics { contentDescription = cd }
            .padding(14.dp),
    ) {
        CardHeader(AppIcons.StressFace, stringResource(R.string.stress_indicator))
        StressBars(
            level ?: StressLevel.LOW,
            Modifier
                .padding(top = 18.dp, bottom = 14.dp)
                .fillMaxWidth()
                .height(62.dp)
                .alpha(if (level == null) EMPTY_ALPHA else 1f),
        )
        Spacer(Modifier.weight(1f))
        if (level == null) {
            EmptyValue(stringResource(R.string.take_quiz), MoodType.Big)
        } else {
            RollingText(level, style = MoodType.Big) { stringResource(it.labelRes) }
        }
    }
}

@Composable
private fun StressBars(level: StressLevel, modifier: Modifier) {
    val factor by animateFloatAsState(
        level.barFactor,
        spring(dampingRatio = 0.55f, stiffness = 180f),
        label = "stress",
    )
    val reduce = LocalReduceMotion.current
    val grow = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(1100, delayMillis = 260)) }
    Canvas(modifier) {
        val n = StressBase.size
        val gap = 5.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        val minH = 6.dp.toPx()
        val radius = CornerRadius(3.dp.toPx())
        val t = grow.value * 1100f
        for (i in 0 until n) {
            val p = Motion.Back.transform(((t - i * 45f) / 700f).coerceIn(0f, 1f))
            val rel = if (i < 3) StressBase[i] else StressBase[i] * factor
            val barH = maxOf(minH, size.height * rel) * p
            if (barH > 0.5f) {
                drawRoundRect(
                    Palette.LavenderInk,
                    Offset(i * (w + gap), size.height - barH),
                    Size(w, barH),
                    radius,
                )
            }
        }
    }
}

@Composable
private fun QuizCard(
    state: HomeUiState,
    onAnswer: (Boolean) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier,
) {
    // Evita respuestas dobles por toques muy rápidos mientras cambia la pregunta.
    var lastTap by remember { mutableLongStateOf(0L) }
    fun guarded(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastTap > 380) {
            lastTap = now
            action()
        }
    }
    val levelText = state.stress?.let { stringResource(it.labelRes).lowercase() }.orEmpty()

    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Palette.Lime)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardHeader(AppIcons.Puzzle, stringResource(R.string.quiz_title), Modifier.weight(1f))
            RollingText(
                state.quizIndex,
                style = MoodType.Label.copy(color = Palette.LimeInk),
            ) { idx ->
                if (idx >= state.quizTotal) stringResource(R.string.quiz_done_label)
                else stringResource(R.string.quiz_progress, idx + 1, state.quizTotal)
            }
        }
        AnimatedContent(
            targetState = state.quizIndex,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            transitionSpec = {
                (slideInHorizontally(tween(420, easing = Motion.EaseOut)) { it / 6 } + fadeIn(tween(300, 80))) togetherWith
                    (slideOutHorizontally(tween(200, easing = Motion.EaseIn)) { -it / 6 } + fadeOut(tween(160)))
            },
            label = "question",
        ) { idx ->
            Text(
                text = if (idx >= state.quizTotal) stringResource(R.string.quiz_done_text, levelText)
                else stringResource(QuizQuestions[idx]),
                style = MoodType.Question,
                modifier = Modifier.semantics { heading() },
            )
        }
        AnimatedContent(
            targetState = state.quizDone,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "quizActions",
        ) { done ->
            if (done) {
                QuizButton(stringResource(R.string.quiz_retake), Modifier.fillMaxWidth()) { guarded(onRestart) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuizButton(stringResource(R.string.yes), Modifier.weight(1f)) { guarded { onAnswer(true) } }
                    QuizButton(stringResource(R.string.no), Modifier.weight(1f)) { guarded { onAnswer(false) } }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(state.quizTotal) { i ->
                val c by animateColorAsState(
                    if (i < state.quizIndex) Palette.Ink else Palette.Ink.copy(alpha = 0.14f),
                    tween(400),
                    label = "dot",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(c),
                )
            }
        }
    }
}

@Composable
private fun QuizButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .heightIn(min = 40.dp)
            .bounceClick(onClick = onClick, pressedScale = 0.95f)
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Ink)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MoodType.Base.copy(color = Palette.Paper, fontWeight = MoodType.Chip.fontWeight), textAlign = TextAlign.Center)
    }
}

