package com.asahioo.moodly.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asahioo.moodly.R
import com.asahioo.moodly.data.model.Settings
import com.asahioo.moodly.ui.UiText
import com.asahioo.moodly.ui.components.LocalHaptics
import com.asahioo.moodly.ui.components.LocalToast
import com.asahioo.moodly.ui.components.MoodTimePicker
import com.asahioo.moodly.ui.components.PrimaryButton
import com.asahioo.moodly.ui.components.ToastData
import com.asahioo.moodly.ui.components.ToastIcon
import com.asahioo.moodly.ui.components.rememberNotificationPermission
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.Palette

/* Pasos del onboarding después de la introducción animada. */

@Composable
private fun SetupPage(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 20.dp),
    ) {
        Text(title, style = MoodType.Greeting, modifier = Modifier.semantics { heading() })
        Text(
            subtitle,
            style = MoodType.Body.copy(color = Palette.Grey2),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        content()
    }
}

@Composable
internal fun NameStep(name: String, onNameChange: (String) -> Unit, onContinue: () -> Unit) {
    val haptics = LocalHaptics.current
    val valid = name.isNotBlank()
    val focus = remember { FocusRequester() }
    // El teclado aparece solo: escribir el nombre y seguir debe tomar segundos.
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun submit() {
        if (valid) onContinue() else haptics.reject()
    }

    SetupPage(stringResource(R.string.ob_name_title), stringResource(R.string.ob_name_sub)) {
        val style = MoodType.Base.copy(fontSize = 20.sp, color = Palette.Ink)
        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(Palette.Ink),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
            decorationBox = { field ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.Mist)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                ) {
                    if (name.isEmpty()) Text(stringResource(R.string.ob_name_hint), style = style.copy(color = Palette.Grey))
                    field()
                }
            },
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = stringResource(R.string.ob_continue),
            onClick = ::submit,
            container = if (valid) Palette.Ink else Palette.Mist2,
            content = if (valid) Palette.Paper else Palette.Grey,
            modifier = Modifier.semantics { if (!valid) disabled() },
        )
    }
}

@Composable
internal fun ReminderStep(onDone: (reminderMinutes: Int?) -> Unit) {
    var minutes by rememberSaveable { mutableIntStateOf(Settings.DEFAULT_REMINDER_MINUTES) }
    val requestNotifications = rememberNotificationPermission()
    val toast = LocalToast.current

    SetupPage(stringResource(R.string.ob_reminder_title), stringResource(R.string.ob_reminder_sub)) {
        MoodTimePicker(
            initialMinutes = minutes,
            onChange = { minutes = it },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            text = stringResource(R.string.ob_reminder_enable),
            onClick = {
                requestNotifications { granted ->
                    if (!granted) {
                        toast.show(
                            ToastData(
                                ToastIcon.Alert,
                                UiText.res(R.string.toast_notifications_off),
                                UiText.res(R.string.toast_notifications_off_sub),
                            )
                        )
                    }
                    onDone(if (granted) minutes else null)
                }
            },
        )
        PrimaryButton(
            text = stringResource(R.string.ob_not_now),
            onClick = { onDone(null) },
            container = Palette.Mist,
            content = Palette.Ink,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
