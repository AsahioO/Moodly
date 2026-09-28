package com.asahioo.moodly.ui.components

import android.text.format.DateFormat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.asahioo.moodly.ui.theme.Palette

/**
 * Reloj de Material 3 con la paleta de la app (lo usan el onboarding y Ajustes). Trabaja en
 * minutos desde medianoche y encapsula la API experimental para que no se filtre a las pantallas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodTimePicker(initialMinutes: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    val latest by rememberUpdatedState(onChange)
    LaunchedEffect(state) {
        snapshotFlow { state.hour * 60 + state.minute }.collect { latest(it) }
    }
    TimePicker(
        state = state,
        modifier = modifier,
        colors = TimePickerDefaults.colors(
            clockDialColor = Palette.Mist,
            clockDialSelectedContentColor = Color.White,
            clockDialUnselectedContentColor = Palette.Ink,
            selectorColor = Palette.Ink,
            periodSelectorBorderColor = Palette.Mist2,
            periodSelectorSelectedContainerColor = Palette.Lime,
            periodSelectorUnselectedContainerColor = Color.Transparent,
            periodSelectorSelectedContentColor = Palette.Ink,
            periodSelectorUnselectedContentColor = Palette.Grey2,
            timeSelectorSelectedContainerColor = Palette.Lime,
            timeSelectorUnselectedContainerColor = Palette.Mist,
            timeSelectorSelectedContentColor = Palette.Ink,
            timeSelectorUnselectedContentColor = Palette.Ink,
        ),
    )
}
