package com.asahioo.moodly.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.asahioo.moodly.ui.Tab
import com.asahioo.moodly.ui.theme.Palette

@Composable
fun TabBar(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Palette.Night)
            .navigationBarsPadding()
            .padding(horizontal = 26.dp, vertical = 10.dp)
            .height(44.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cell = maxWidth / Tab.entries.size
            val indicatorX by animateDpAsState(
                targetValue = cell * current.ordinal + (cell - 50.dp) / 2,
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 380f),
                label = "indicator",
            )
            Box(
                Modifier
                    .offset { IntOffset(indicatorX.roundToPx(), 2.dp.roundToPx()) }
                    .size(width = 50.dp, height = 40.dp)
                    .background(Palette.Night2, RoundedCornerShape(14.dp)),
            )
            Row(
                Modifier
                    .fillMaxSize()
                    .selectableGroup(),
            ) {
                Tab.entries.forEach { tab ->
                    val isSelected = tab == current
                    val tint by animateColorAsState(
                        if (isSelected) Palette.Paper else Palette.TabIdle,
                        tween(260),
                        label = "tint",
                    )
                    val label = stringResource(tab.label)
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .bounceClick(onClick = { onSelect(tab) }, pressedScale = 0.84f, role = Role.Tab)
                            .semantics {
                                selected = isSelected
                                contentDescription = label
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Glyph(tab.icon, Modifier.size(22.dp), tint = tint)
                    }
                }
            }
        }
    }
}
