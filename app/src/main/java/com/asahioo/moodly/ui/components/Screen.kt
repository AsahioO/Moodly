package com.asahioo.moodly.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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

/**
 * Contenedor de cada pantalla: papel a pantalla completa, con scroll y los márgenes comunes.
 * Con [fillViewport] mide al menos la altura visible, así los hijos con `weight` reparten el
 * sobrante; si el contenido no cabe (pantalla pequeña, fuente grande) sigue haciendo scroll.
 */
@Composable
fun Screen(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    fillViewport: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(Palette.Paper),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .then(if (fillViewport) Modifier.heightIn(min = maxHeight) else Modifier)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = ScreenGutter, end = ScreenGutter, top = 14.dp, bottom = 28.dp + TabBarClearance),
            content = content,
        )
    }
}

/** Ocupa también los márgenes laterales de [Screen]: carruseles de borde a borde. */
fun Modifier.bleed(horizontal: Dp = ScreenGutter): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx()
    val placeable = measurable.measure(constraints.offset(horizontal = 2 * extra))
    layout(placeable.width - 2 * extra, placeable.height) { placeable.place(-extra, 0) }
}
