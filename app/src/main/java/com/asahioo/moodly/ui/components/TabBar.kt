package com.asahioo.moodly.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asahioo.moodly.ui.Tab
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.Palette

/** Barra de pestañas: píldora sobre el papel, con ícono y nombre; la pestaña activa lleva una cápsula de tinta. */
@Composable
fun TabBar(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Palette.Paper)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(CircleShape)
                .background(Palette.Mist)
                .padding(5.dp),
        ) {
            val cell = maxWidth / Tab.entries.size
            val indicatorX by animateDpAsState(
                targetValue = cell * current.ordinal,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 380f),
                label = "indicator",
            )
            Box(
                Modifier
                    .offset { IntOffset(indicatorX.roundToPx(), 0) }
                    .size(width = cell, height = maxHeight)
                    .background(Palette.Ink, CircleShape),
            )
            Row(
                Modifier
                    .fillMaxSize()
                    .selectableGroup(),
            ) {
                Tab.entries.forEach { tab ->
                    val isSelected = tab == current
                    val tint by animateColorAsState(
                        if (isSelected) Palette.Paper else Palette.Grey2,
                        tween(260),
                        label = "tint",
                    )
                    val label = stringResource(tab.label)
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .bounceClick(onClick = { onSelect(tab) }, pressedScale = 0.9f, role = Role.Tab)
                            .semantics {
                                selected = isSelected
                                contentDescription = label
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Glyph(tab.icon, Modifier.size(19.dp), tint = tint)
                        Text(
                            label,
                            style = MoodType.Label.copy(color = tint, fontSize = 10.5.sp),
                            maxLines = 1,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clearAndSetSemantics { },
                        )
                    }
                }
            }
        }
    }
}
