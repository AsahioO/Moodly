package com.asahioo.moodly.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.asahioo.moodly.R
import com.asahioo.moodly.data.model.AppTheme
import com.asahioo.moodly.ui.labelRes
import com.asahioo.moodly.ui.components.bounceClick
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.colorsFor

/**
 * Selector de tema. Cada fila se pinta con los colores de su propio tema y, al elegir, la app entera
 * cambia detrás de la hoja: así se ve el resultado antes de cerrarla.
 */
@Composable
fun ThemeSheet(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    Column {
        SheetHeader(stringResource(R.string.preferences), stringResource(R.string.theme_setting))
        // ponytail: Column sin scroll; con más de ~8 temas pasar a LazyColumn.
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTheme.entries.forEach { theme ->
                ThemeRow(theme, isSelected = theme == selected, onClick = { onSelect(theme) })
            }
        }
    }
}

@Composable
private fun ThemeRow(theme: AppTheme, isSelected: Boolean, onClick: () -> Unit) {
    val c = colorsFor(theme)
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick, pressedScale = 0.98f, role = Role.RadioButton)
            .clip(shape)
            .background(c.mist)
            .border(2.dp, if (isSelected) c.ink else Color.Transparent, shape)
            .semantics { selected = isSelected }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            stringResource(theme.labelRes),
            style = MoodType.Title.copy(color = c.ink),
            modifier = Modifier.weight(1f),
        )
        for (swatch in listOf(c.paper, c.lime, c.peach, c.lavender)) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .border(1.dp, c.ink.copy(alpha = 0.12f), CircleShape),
            )
        }
    }
}
