package com.asahioo.moodly.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.asahioo.moodly.ui.theme.Palette

/** Margen lateral de [Screen]. */
val ScreenGutter = 20.dp

/** Contenedor de cada pantalla: papel a pantalla completa, con scroll y los márgenes comunes. */
@Composable
fun Screen(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .padding(start = ScreenGutter, end = ScreenGutter, top = 14.dp, bottom = 28.dp),
        content = content,
    )
}

/** Ocupa también los márgenes laterales de [Screen]: carruseles de borde a borde. */
fun Modifier.bleed(horizontal: Dp = ScreenGutter): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx()
    val placeable = measurable.measure(constraints.offset(horizontal = 2 * extra))
    layout(placeable.width - 2 * extra, placeable.height) { placeable.place(-extra, 0) }
}
