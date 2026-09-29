package com.asahioo.moodly.ui.sheets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asahioo.moodly.R
import com.asahioo.moodly.data.model.AppData
import com.asahioo.moodly.data.model.CustomTag
import com.asahioo.moodly.data.model.DayContext
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.data.model.PresetTag
import com.asahioo.moodly.data.model.StressLevel
import com.asahioo.moodly.ui.calendar.MonthOption
import com.asahioo.moodly.ui.color
import com.asahioo.moodly.ui.components.LocalHaptics
import com.asahioo.moodly.ui.components.AnimatedMoodIcon
import com.asahioo.moodly.ui.components.MoodTimePicker
import com.asahioo.moodly.ui.components.PrimaryButton
import com.asahioo.moodly.ui.components.RollingText
import com.asahioo.moodly.ui.components.bounceClick
import com.asahioo.moodly.ui.components.rememberStagger
import com.asahioo.moodly.ui.components.staggered
import com.asahioo.moodly.ui.labelRes
import com.asahioo.moodly.ui.monthName
import com.asahioo.moodly.ui.monthYear
import com.asahioo.moodly.ui.textRes
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.Motion
import com.asahioo.moodly.ui.theme.Palette
import com.asahioo.moodly.ui.tipsRes
import com.asahioo.moodly.ui.weekdayFull
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
private fun SheetHeader(eyebrow: String, title: String) {
    Text(eyebrow, style = MoodType.Label.copy(color = Palette.Grey))
    Text(
        title,
        style = MoodType.SheetTitle,
        modifier = Modifier
            .padding(top = 2.dp)
            .semantics { heading() },
    )
}

/* ---------------------------------- Día del calendario ---------------------------------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaySheet(
    date: LocalDate,
    isToday: Boolean,
    current: Mood?,
    context: DayContext?,
    customTags: List<CustomTag>,
    onSave: (mood: Mood, note: String, tags: Set<PresetTag>, customTags: Set<String>) -> Unit,
    onClear: () -> Unit,
    onCreateTag: suspend (String) -> String?,
) {
    val pop = rememberStagger(date, totalMs = 900)
    var selected by remember(date) { mutableStateOf(current) }
    var note by remember(date) { mutableStateOf(context?.note.orEmpty()) }
    var tags by remember(date) { mutableStateOf(context?.tags.orEmpty()) }
    var custom by remember(date) { mutableStateOf(context?.customTags.orEmpty()) }
    var adding by remember(date) { mutableStateOf(false) }
    var draft by remember(date) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val haptics = LocalHaptics.current
    val weekday = weekdayFull(date)
    val scroll = rememberScrollState()

    fun createTag() {
        scope.launch {
            val id = onCreateTag(draft)
            if (id == null) {
                haptics.reject()
            } else {
                custom = custom + id
                draft = ""
                adding = false
            }
        }
    }

    // Solo se desplaza si no cabe (teclado abierto); así el arrastre para cerrar sigue funcionando.
    Column(Modifier.verticalScroll(scroll, enabled = scroll.maxValue > 0)) {
        SheetHeader(
            eyebrow = if (isToday) stringResource(R.string.weekday_today, weekday) else weekday,
            title = stringResource(R.string.day_month, date.dayOfMonth, monthName(date.monthValue)),
        )
        Text(
            stringResource(R.string.day_sheet_question),
            style = MoodType.Body.copy(color = Palette.Grey2),
            modifier = Modifier.padding(top = 8.dp),
        )
        Column(
            Modifier.padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Mood.entries.chunked(3).forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEachIndexed { c, mood ->
                        MoodTile(
                            mood = mood,
                            selected = selected == mood,
                            modifier = Modifier
                                .weight(1f)
                                .staggered(pop, 80 + (r * 3 + c) * 40, 480, 14.dp, 0.9f, Motion.Back),
                        ) {
                            selected = mood
                            haptics.confirm()
                        }
                    }
                }
            }
        }

        SectionLabel(stringResource(R.string.day_tags), Modifier.staggered(pop, 320, 480, 10.dp))
        FlowRow(
            Modifier.staggered(pop, 360, 480, 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PresetTag.entries.forEach { tag ->
                TagChip(stringResource(tag.labelRes), selected = tag in tags) {
                    tags = if (tag in tags) tags - tag else tags + tag
                }
            }
            customTags.forEach { tag ->
                TagChip(tag.label, selected = tag.id in custom) {
                    custom = if (tag.id in custom) custom - tag.id else custom + tag.id
                }
            }
            if (!adding && customTags.size < AppData.MAX_CUSTOM_TAGS) {
                AddTagChip { adding = true }
            }
        }
        if (adding) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { focus.requestFocus() }
            SheetTextField(
                value = draft,
                onValueChange = { draft = it.take(AppData.MAX_TAG_LENGTH) },
                hint = stringResource(R.string.day_new_tag_hint),
                singleLine = true,
                onDone = ::createTag,
                trailing = { ConfirmButton(stringResource(R.string.cd_confirm_tag), ::createTag) },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .focusRequester(focus),
            )
        }

        SectionLabel(stringResource(R.string.day_note), Modifier.staggered(pop, 400, 480, 10.dp))
        SheetTextField(
            value = note,
            onValueChange = { note = it.take(AppData.MAX_NOTE_LENGTH) },
            hint = stringResource(R.string.day_note_hint),
            singleLine = false,
            modifier = Modifier.staggered(pop, 440, 480, 10.dp),
        )
        val remaining = AppData.MAX_NOTE_LENGTH - note.length
        if (remaining < 20) {
            Text(
                stringResource(R.string.day_note_counter, note.length, AppData.MAX_NOTE_LENGTH),
                style = MoodType.Label.copy(color = if (remaining == 0) Palette.Danger else Palette.Grey),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp),
            )
        }

        PrimaryButton(
            text = stringResource(R.string.save),
            enabled = selected != null,
            onClick = { selected?.let { onSave(it, note, tags, custom) } },
            modifier = Modifier.padding(top = 18.dp),
        )
        if (current != null) {
            PrimaryButton(
                text = stringResource(R.string.clear_entry),
                onClick = onClear,
                container = Palette.Mist,
                content = Palette.Ink,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MoodType.Label.copy(color = Palette.Grey),
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun TagChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Palette.Ink else Palette.Mist, tween(200), label = "tagBg")
    val fg by animateColorAsState(if (selected) Palette.Paper else Palette.Ink, tween(200), label = "tagFg")
    val state = stringResource(if (selected) R.string.tag_selected else R.string.tag_not_selected)
    Box(
        Modifier
            .bounceClick(onClick = onClick, pressedScale = 0.92f, role = Role.Checkbox)
            .semantics { stateDescription = state }
            .height(36.dp)
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MoodType.Chip.copy(color = fg), maxLines = 1)
    }
}

@Composable
private fun AddTagChip(onClick: () -> Unit) {
    val cd = stringResource(R.string.cd_add_tag)
    Box(
        Modifier
            .bounceClick(onClick = onClick, pressedScale = 0.92f)
            .clearAndSetSemantics { contentDescription = cd }
            .height(36.dp)
            .clip(CircleShape)
            .border(1.dp, Palette.Mist2, CircleShape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("+ " + stringResource(R.string.day_add_tag), style = MoodType.Chip.copy(color = Palette.Grey2))
    }
}

@Composable
private fun ConfirmButton(contentDescription: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .bounceClick(onClick = onClick, pressedScale = 0.86f)
            .clip(CircleShape)
            .background(Palette.Ink)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text("✓", style = MoodType.Chip.copy(color = Palette.Paper))
    }
}

/** Campo de texto de las hojas: fondo Mist y placeholder gris, igual que el del onboarding. */
@Composable
private fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val style = MoodType.Body
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        maxLines = if (singleLine) 1 else 4,
        textStyle = style,
        cursorBrush = SolidColor(Palette.Ink),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { field ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.Mist)
                    .padding(start = 14.dp, end = if (trailing != null) 8.dp else 14.dp, top = 8.dp, bottom = 8.dp)
                    .heightIn(min = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(hint, style = style.copy(color = Palette.Grey))
                    field()
                }
                trailing?.invoke()
            }
        },
    )
}

@Composable
private fun MoodTile(mood: Mood, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Palette.Ink else Palette.Mist, tween(250), label = "tileBg")
    val fg by animateColorAsState(if (selected) Palette.Paper else Palette.Ink, tween(250), label = "tileFg")
    val label = stringResource(mood.labelRes)
    Column(
        modifier
            .bounceClick(onClick = onClick, pressedScale = 0.92f, haptic = false)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clearAndSetSemantics { contentDescription = label }
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedMoodIcon(mood, active = selected, modifier = Modifier.size(46.dp))
        Text(
            label,
            style = MoodType.Chip.copy(color = fg),
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/* ---------------------------------------- Sueño ---------------------------------------- */

@Composable
fun SleepSheet(initialMinutes: Int, fromHealth: Boolean, onSave: (Int) -> Unit) {
    var minutes by rememberSaveable { mutableIntStateOf(initialMinutes) }
    val bump = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHaptics.current

    fun change(next: Int) {
        val clamped = next.coerceIn(AppData.MIN_SLEEP_MINUTES, AppData.MAX_SLEEP_MINUTES)
        if (clamped / 30 != minutes / 30) haptics.tick()
        minutes = clamped
    }

    Column {
        SheetHeader(stringResource(R.string.last_night), stringResource(R.string.sleep_duration))
        if (fromHealth) {
            Text(
                stringResource(R.string.sleep_from_health),
                style = MoodType.Caption,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 10.dp)
                .graphicsLayer {
                    scaleX = bump.value
                    scaleY = bump.value
                },
            horizontalArrangement = Arrangement.Center,
        ) {
            val big = MoodType.Base.copy(fontSize = 56.sp, fontWeight = FontWeight.Medium, letterSpacing = MoodType.Big.letterSpacing)
            val unit = MoodType.Base.copy(fontSize = 30.sp, color = Palette.Ink.copy(alpha = 0.6f))
            Text("${minutes / 60}", style = big, modifier = Modifier.alignByBaseline())
            Text(stringResource(R.string.unit_h), style = unit, modifier = Modifier.alignByBaseline())
            Spacer(Modifier.width(10.dp))
            Text((minutes % 60).toString().padStart(2, '0'), style = big, modifier = Modifier.alignByBaseline())
            Text(stringResource(R.string.unit_min), style = unit, modifier = Modifier.alignByBaseline())
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton("−", stringResource(R.string.less_15)) {
                change(minutes - 15)
                scope.launch {
                    bump.snapTo(0.94f)
                    bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
                }
            }
            Slider(
                value = minutes.toFloat(),
                onValueChange = { change(((it / 5f).roundToInt()) * 5) },
                valueRange = AppData.MIN_SLEEP_MINUTES.toFloat()..AppData.MAX_SLEEP_MINUTES.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Palette.PeachInk,
                    activeTrackColor = Palette.PeachInk,
                    inactiveTrackColor = Palette.Mist2,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            RoundButton("+", stringResource(R.string.more_15)) {
                change(minutes + 15)
                scope.launch {
                    bump.snapTo(1.06f)
                    bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
                }
            }
        }
        val hint = when {
            minutes < 420 -> R.string.sleep_hint_short
            minutes <= 540 -> R.string.sleep_hint_ok
            else -> R.string.sleep_hint_long
        }
        RollingText(
            hint,
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            style = MoodType.Base.copy(fontSize = 12.5.sp, color = Palette.Grey2, textAlign = TextAlign.Center),
        ) { stringResource(it) }
        PrimaryButton(
            text = stringResource(R.string.save),
            onClick = { onSave(minutes) },
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}

@Composable
private fun RoundButton(symbol: String, contentDescription: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .bounceClick(onClick = onClick, pressedScale = 0.86f, haptic = false)
            .clip(CircleShape)
            .background(Palette.Mist)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = MoodType.Base.copy(fontSize = 24.sp, fontWeight = FontWeight.Medium))
    }
}

/* ---------------------------------------- Estrés ---------------------------------------- */

@Composable
fun StressSheet(level: StressLevel?, quizIndex: Int, quizTotal: Int, onQuiz: () -> Unit) {
    val pop = rememberStagger(level, totalMs = 900)
    val done = quizIndex >= quizTotal
    Column {
        if (level == null) {
            SheetHeader(stringResource(R.string.stress_indicator), stringResource(R.string.stress_no_data))
            Text(
                stringResource(R.string.stress_no_data_text, quizTotal),
                style = MoodType.Body.copy(color = Palette.Grey2),
                modifier = Modifier.padding(top = 8.dp),
            )
            PrimaryButton(
                text = stringResource(if (quizIndex == 0) R.string.quiz_start else R.string.quiz_continue),
                onClick = onQuiz,
                modifier = Modifier.padding(top = 18.dp),
            )
            return@Column
        }
        SheetHeader(stringResource(R.string.stress_indicator), stringResource(level.labelRes))
        Text(
            stringResource(level.textRes) + " " + (
                if (done) stringResource(R.string.stress_based_on_quiz)
                else stringResource(R.string.stress_finish_quiz, quizIndex, quizTotal)
                ),
            style = MoodType.Body.copy(color = Palette.Grey2),
            modifier = Modifier.padding(top = 8.dp),
        )
        Column(
            Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            level.tipsRes.forEachIndexed { i, res ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .staggered(pop, 120 + i * 70, 480, 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.Mist)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Palette.Lavender),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", style = MoodType.Base.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(res), style = MoodType.Body)
                }
            }
        }
        PrimaryButton(
            text = stringResource(if (done) R.string.quiz_retake_long else R.string.quiz_continue),
            onClick = onQuiz,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}

/* ---------------------------------------- Mes ---------------------------------------- */

@Composable
fun MonthPickerSheet(options: List<MonthOption>, current: YearMonth, onSelect: (YearMonth) -> Unit) {
    val pop = rememberStagger(Unit, totalMs = 700)
    Column {
        SheetHeader(stringResource(R.string.calendar_title), stringResource(R.string.choose_month))
        Column(Modifier.padding(top = 10.dp)) {
            options.forEachIndexed { i, option ->
                val isCurrent = option.month == current
                Row(
                    Modifier
                        .fillMaxWidth()
                        .staggered(pop, 60 + i * 50, 420, 10.dp)
                        .bounceClick(onClick = { onSelect(option.month) }, pressedScale = 0.98f)
                        .padding(horizontal = 4.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        monthYear(option.month).replace(",", ""),
                        style = MoodType.Base.copy(
                            fontSize = 15.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        option.topMoods.forEach { mood ->
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(mood.color),
                            )
                        }
                    }
                }
                if (i < options.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Palette.Mist2),
                    )
                }
            }
        }
    }
}

/* ---------------------------------------- Recordatorio ---------------------------------------- */

@Composable
fun ReminderSheet(initialMinutes: Int, onSave: (Int) -> Unit) {
    var minutes by rememberSaveable { mutableIntStateOf(initialMinutes) }
    Column {
        SheetHeader(stringResource(R.string.reminder_section), stringResource(R.string.reminder_time))
        MoodTimePicker(
            initialMinutes = initialMinutes,
            onChange = { minutes = it },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 18.dp),
        )
        PrimaryButton(
            text = stringResource(R.string.save),
            onClick = { onSave(minutes) },
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/* ---------------------------------------- Borrar todo ---------------------------------------- */

@Composable
fun ResetSheet(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Column {
        SheetHeader(stringResource(R.string.settings_title), stringResource(R.string.reset_title))
        Text(
            stringResource(R.string.reset_body),
            style = MoodType.Body.copy(color = Palette.Grey2),
            modifier = Modifier.padding(top = 8.dp),
        )
        PrimaryButton(
            text = stringResource(R.string.reset_confirm),
            onClick = onConfirm,
            container = Palette.DangerFill,
            modifier = Modifier.padding(top = 18.dp),
        )
        PrimaryButton(
            text = stringResource(R.string.cancel),
            onClick = onCancel,
            container = Palette.Mist,
            content = Palette.Ink,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/* ---------------------------------------- Etiquetas ---------------------------------------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsSheet(
    customTags: List<CustomTag>,
    usage: (id: String) -> Int,
    onAdd: suspend (String) -> String?,
    onRename: (id: String, label: String) -> Unit,
    onDelete: (id: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHaptics.current
    var draft by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<String?>(null) }
    var editDraft by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf<String?>(null) }
    val scroll = rememberScrollState()

    fun add() {
        scope.launch {
            if (onAdd(draft) == null) {
                haptics.reject()
            } else {
                draft = ""
                haptics.confirm()
            }
        }
    }

    fun rename() {
        editing?.let { onRename(it, editDraft) }
        editing = null
    }

    Column(Modifier.verticalScroll(scroll, enabled = scroll.maxValue > 0)) {
        SheetHeader(stringResource(R.string.settings_title), stringResource(R.string.tags_title))

        SectionLabel(stringResource(R.string.tags_presets))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PresetTag.entries.forEach { tag ->
                Box(
                    Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(Palette.Mist)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(tag.labelRes), style = MoodType.Label.copy(color = Palette.Grey2))
                }
            }
        }

        SectionLabel(stringResource(R.string.tags_custom))
        if (customTags.isEmpty()) {
            Text(stringResource(R.string.tags_custom_empty), style = MoodType.Body.copy(color = Palette.Grey2))
        }
        customTags.forEachIndexed { i, tag ->
            when (tag.id) {
                editing -> {
                    val focus = remember { FocusRequester() }
                    LaunchedEffect(Unit) { focus.requestFocus() }
                    SheetTextField(
                        value = editDraft,
                        onValueChange = { editDraft = it.take(AppData.MAX_TAG_LENGTH) },
                        hint = tag.label,
                        singleLine = true,
                        onDone = ::rename,
                        trailing = { ConfirmButton(stringResource(R.string.save), ::rename) },
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .focusRequester(focus),
                    )
                }
                confirming -> Column(Modifier.padding(vertical = 10.dp)) {
                    Text(
                        stringResource(R.string.tag_delete_confirm, tag.label) + " " +
                            pluralStringResource(R.plurals.tag_delete_days, usage(tag.id), usage(tag.id)),
                        style = MoodType.Body,
                    )
                    Row(
                        Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PrimaryButton(
                            text = stringResource(R.string.cancel),
                            onClick = { confirming = null },
                            container = Palette.Mist,
                            content = Palette.Ink,
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryButton(
                            text = stringResource(R.string.delete),
                            onClick = {
                                onDelete(tag.id)
                                haptics.confirm()
                                confirming = null
                            },
                            container = Palette.DangerFill,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tag.label,
                        style = MoodType.Base.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .bounceClick(
                                onClick = {
                                    editing = tag.id
                                    editDraft = tag.label
                                    confirming = null
                                },
                                pressedScale = 0.98f,
                                onClickLabel = stringResource(R.string.cd_rename_tag, tag.label),
                            )
                            .padding(horizontal = 4.dp, vertical = 15.dp),
                    )
                    val cd = stringResource(R.string.cd_delete_tag, tag.label)
                    Box(
                        Modifier
                            .size(36.dp)
                            .bounceClick(
                                onClick = {
                                    confirming = tag.id
                                    editing = null
                                },
                                pressedScale = 0.86f,
                            )
                            .clip(CircleShape)
                            .background(Palette.Mist)
                            .clearAndSetSemantics { contentDescription = cd },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", style = MoodType.Label.copy(color = Palette.Grey2))
                    }
                }
            }
            if (i < customTags.lastIndex) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Palette.Mist2),
                )
            }
        }

        if (customTags.size < AppData.MAX_CUSTOM_TAGS) {
            SheetTextField(
                value = draft,
                onValueChange = { draft = it.take(AppData.MAX_TAG_LENGTH) },
                hint = stringResource(R.string.cd_add_tag),
                singleLine = true,
                onDone = ::add,
                trailing = { ConfirmButton(stringResource(R.string.cd_confirm_tag), ::add) },
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            Text(
                stringResource(R.string.tags_limit, AppData.MAX_CUSTOM_TAGS),
                style = MoodType.Body.copy(color = Palette.Grey2),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
