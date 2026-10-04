package com.asahioo.moodly.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.asahioo.moodly.ui.components.EmptyValue
import com.asahioo.moodly.ui.components.Glyph
import com.asahioo.moodly.ui.components.RollingText
import com.asahioo.moodly.ui.components.Screen
import com.asahioo.moodly.ui.components.Stagger
import com.asahioo.moodly.ui.components.bounceClick
import com.asahioo.moodly.ui.components.staggered
import com.asahioo.moodly.ui.labelRes
import com.asahioo.moodly.ui.shortDate
import com.asahioo.moodly.ui.theme.LocalReduceMotion
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.Motion
import com.asahioo.moodly.ui.theme.Palette
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import androidx.compose.ui.text.font.FontWeight
import com.asahioo.moodly.ui.sundayIndex

@Composable
fun HomeScreen(
    state: HomeUiState,
    avatarFile: String?,
    stagger: Stagger?,
    onOpenSettings: () -> Unit,
    onPickMood: (Mood) -> Unit,
    onAnswer: (Boolean) -> Unit,
    onRestartQuiz: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenStress: () -> Unit,
    onOpenContext: () -> Unit,
    /** Pasos en lugar del quiz: permiso de pasos concedido y el quiz no fue pedido desde estrés. */
    showSteps: Boolean,
    scrollState: ScrollState = rememberScrollState(),
) {
    Screen(scrollState = scrollState, fillViewport = true) {
        HomeHeader(state, avatarFile, onOpenSettings, stagger)

        MoodHero(
            state.todayMood,
            // El héroe se queda con el alto sobrante: así todo Inicio cabe sin scroll.
            Modifier
                .padding(top = 20.dp)
                .weight(1f)
                .heightIn(min = 190.dp)
                .staggered(stagger, 120),
        ) {
            // Solo después de registrar: el toque rápido sigue siendo el camino principal.
            AnimatedVisibility(
                visible = state.todayMood != null,
                enter = expandVertically(tween(300, easing = Motion.EaseOut)) + fadeIn(tween(300)),
                exit = shrinkVertically(tween(250)) + fadeOut(tween(200)),
            ) {
                ContextRow(state, onOpenContext)
            }
        }
        MoodStrip(state.todayMood, stagger, onPickMood, Modifier.padding(top = 6.dp))

        Row(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val cardModifier = Modifier
                .weight(1f)
                .height(150.dp)
            SleepCard(
                minutes = state.sleepMinutes,
                onClick = onOpenSleep,
                modifier = cardModifier.staggered(stagger, 340, 800, 40.dp, 0.94f, Motion.Navigation),
            )
            StressCard(
                level = state.stress,
                onClick = onOpenStress,
                modifier = cardModifier.staggered(stagger, 400, 800, 40.dp, 0.94f, Motion.Navigation),
            )
        }
        // Con pasos de Health Connect, el quiz vive detrás del indicador de estrés.
        if (showSteps) {
            StepsCard(
                state.steps,
                state.week,
                Modifier
                    .padding(top = 10.dp)
                    .staggered(stagger, 460, 800, 40.dp, 0.94f, Motion.Navigation),
            )
        } else {
            QuizCard(
                state = state,
                onAnswer = onAnswer,
                onRestart = onRestartQuiz,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .staggered(stagger, 460, 800, 40.dp, 0.94f, Motion.Navigation),
            )
        }
    }
}

/** Fecha y saludo a la izquierda; la foto de perfil lleva a Ajustes. */
@Composable
private fun HomeHeader(state: HomeUiState, avatarFile: String?, onOpenSettings: () -> Unit, stagger: Stagger?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                shortDate(state.today),
                style = MoodType.Caption,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .staggered(stagger, 0),
            )
            Greeting(state.firstName, stagger)
        }
        Avatar(
            avatarFile,
            Modifier
                .padding(start = 12.dp)
                .size(46.dp)
                .staggered(stagger, 60)
                .bounceClick(onClick = onOpenSettings, pressedScale = 0.9f, onClickLabel = stringResource(R.string.cd_profile)),
        )
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
            .clearAndSetSemantics {
                contentDescription = text
                heading()
            },
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        words.forEachIndexed { i, word ->
            Text(word, style = MoodType.Greeting, modifier = Modifier.staggered(stagger, 60 + i * 32))
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
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .bounceClick(onClick = onClick, pressedScale = 0.97f, onClickLabel = stringResource(R.string.cd_edit_context))
            .clip(CircleShape)
            .background(Palette.Paper)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    )
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
                .padding(top = 10.dp, bottom = 10.dp)
                .fillMaxWidth()
                .height(36.dp)
                .alpha(if (minutes == null) EMPTY_ALPHA else 1f),
        )
        Spacer(Modifier.weight(1f))
        if (minutes == null) {
            EmptyValue(stringResource(R.string.tap_to_log), CardValue)
        } else {
            Row {
                AnimatedNumber(minutes / 60, CardValue, Modifier.alignByBaseline())
                Text(stringResource(R.string.unit_h), style = CardUnit, modifier = Modifier.alignByBaseline())
                Spacer(Modifier.width(5.dp))
                AnimatedNumber(minutes % 60, CardValue, Modifier.alignByBaseline(), durationMs = 1100)
                Text(stringResource(R.string.unit_min), style = CardUnit, modifier = Modifier.alignByBaseline())
            }
        }
    }
}

@Composable
private fun StepsCard(steps: Int?, week: List<StepDay>, modifier: Modifier) {
    val fmt = remember { NumberFormat.getIntegerInstance() }
    val days = stringArrayResource(R.array.weekdays_short)
    // Sin meta: la referencia es el mejor día propio de la semana.
    val best = week.filter { it.steps != null }.takeIf { it.size >= 2 }?.maxBy { it.steps!! }
    val cd = buildString {
        append(stringResource(R.string.steps_today)).append(": ")
        append(steps?.let(fmt::format) ?: stringResource(R.string.steps_no_data))
        if (best != null) append(". ").append(stringResource(R.string.cd_steps_best, fmt.format(best.steps)))
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Palette.Lime)
            .clearAndSetSemantics { contentDescription = cd }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardHeader(AppIcons.Steps, stringResource(R.string.steps_today), Modifier.weight(1f))
            if (best != null) {
                Text(
                    stringResource(R.string.steps_best, fmt.format(best.steps), days[best.date.sundayIndex()]),
                    style = MoodType.Label.copy(color = Palette.LimeInk),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(Modifier.weight(1f)) {
                if (steps == null) {
                    EmptyValue(stringResource(R.string.steps_no_data), MoodType.Big)
                } else {
                    Row {
                        AnimatedNumber(steps, MoodType.Big, Modifier.alignByBaseline(), format = fmt::format)
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.unit_steps), style = MoodType.BigUnit, modifier = Modifier.alignByBaseline())
                    }
                }
            }
            WeekSteps(week, days, Modifier.width(150.dp))
        }
    }
}

/** Últimos 7 días a escala del mejor día; cada barra lleva el color del ánimo de ese día. */
@Composable
private fun WeekSteps(week: List<StepDay>, days: Array<String>, modifier: Modifier) {
    val reduce = LocalReduceMotion.current
    val grow = remember(week) { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(grow) { grow.animateTo(1f, tween(1100, delayMillis = 240)) }
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            val n = week.size
            val gap = 7.dp.toPx()
            val w = (size.width - gap * (n - 1)) / n
            val radius = CornerRadius(w / 2)
            val top = (week.maxOfOrNull { it.steps ?: 0 } ?: 0).coerceAtLeast(1).toFloat()
            val t = grow.value * 1100f
            week.forEachIndexed { i, day ->
                val p = Motion.Back.transform(((t - i * 50f) / 700f).coerceIn(0f, 1f))
                val left = i * (w + gap)
                val barH = if (day.steps == null) w else maxOf(w, size.height * day.steps / top)
                val color = when {
                    day.steps == null -> Palette.LimeInk.copy(alpha = 0.18f)
                    // El verde de "Feliz" se perdería sobre el fondo lima: aquí va en verde oscuro.
                    day.mood == Mood.HAPPY -> Palette.LimeInk
                    day.mood != null -> day.mood.color
                    else -> Palette.Ink.copy(alpha = 0.25f)
                }
                val h = barH * p
                if (h > 0.5f) drawRoundRect(color, Offset(left, size.height - h), Size(w, h), radius)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            week.forEachIndexed { i, day ->
                val isToday = i == week.lastIndex
                Text(
                    days[day.date.sundayIndex()].take(1),
                    style = MoodType.Small.copy(
                        color = if (isToday) Palette.Ink else Palette.LimeInk,
                        fontWeight = if (isToday) FontWeight.SemiBold else null,
                        fontSize = 10.sp,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
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

/** Valor grande de las tarjetas a media pantalla: "6h 25min" debe caber en una línea a 360dp. */
private val CardValue = MoodType.Big.copy(fontSize = 28.sp)
private val CardUnit = MoodType.BigUnit.copy(fontSize = 17.sp)

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
                .padding(top = 10.dp, bottom = 10.dp)
                .fillMaxWidth()
                .height(36.dp)
                .alpha(if (level == null) EMPTY_ALPHA else 1f),
        )
        Spacer(Modifier.weight(1f))
        if (level == null) {
            EmptyValue(stringResource(R.string.take_quiz), CardValue)
        } else {
            RollingText(level, style = CardValue) { stringResource(it.labelRes) }
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
                .padding(vertical = 10.dp),
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
    }
}

@Composable
private fun QuizButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .heightIn(min = 36.dp)
            .bounceClick(onClick = onClick, pressedScale = 0.95f)
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Ink)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MoodType.Base.copy(color = Palette.Paper, fontWeight = MoodType.Chip.fontWeight), textAlign = TextAlign.Center)
    }
}

