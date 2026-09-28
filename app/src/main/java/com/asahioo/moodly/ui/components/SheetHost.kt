package com.asahioo.moodly.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.asahioo.moodly.ui.theme.LocalReduceMotion
import com.asahioo.moodly.ui.theme.Motion
import com.asahioo.moodly.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Hoja inferior propia: entra con resorte, se arrastra hacia abajo para cerrar y responde al
 * gesto de "atrás predictivo" de Android. [offset] va de 0 (abierta) a 1 (cerrada) y la
 * pantalla de fondo lo lee para encogerse como en iOS.
 */
@Composable
fun <T : Any> SheetHost(
    request: T?,
    offset: Animatable<Float, AnimationVector1D>,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(T) -> Unit,
) {
    val reduce = LocalReduceMotion.current
    var shown by remember { mutableStateOf<T?>(null) }
    val dismiss by rememberUpdatedState(onDismiss)
    val scope = rememberCoroutineScope()

    LaunchedEffect(request) {
        if (request != null) {
            if (shown != null && shown != request) offset.animateTo(1f, tween(if (reduce) 1 else 200, easing = Motion.EaseIn))
            if (shown == null) offset.snapTo(1f)
            shown = request
            offset.animateTo(0f, if (reduce) snap<Float>() else spring<Float>(dampingRatio = 0.86f, stiffness = 320f))
        } else if (shown != null) {
            offset.animateTo(1f, tween(if (reduce) 1 else 280, easing = Motion.EaseIn))
            shown = null
        }
    }

    PredictiveBackHandler(enabled = request != null) { events ->
        try {
            events.collect { e -> offset.snapTo(e.progress * 0.25f) }
            dismiss()
        } catch (e: CancellationException) {
            scope.launch { offset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f)) }
            throw e
        }
    }

    val current = shown ?: return
    var sheetHeight by remember { mutableFloatStateOf(1f) }
    val dragState = rememberDraggableState { delta ->
        scope.launch {
            // Tope en 0: no se despega de su posición abierta al jalar hacia arriba.
            offset.snapTo((offset.value + delta / sheetHeight).coerceAtLeast(0f))
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // La hoja nunca pasa por debajo de la barra de estado, aunque el teclado la empuje hacia arriba.
        val maxSheetHeight = maxHeight - WindowInsets.statusBars.asPaddingValues().calculateTopPadding() - 8.dp
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = (1f - offset.value).coerceIn(0f, 1f) }
                .background(Palette.Scrim)
                .pointerInput(Unit) { detectTapGestures { dismiss() } },
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .onSizeChanged { sheetHeight = it.height.coerceAtLeast(1).toFloat() }
                .graphicsLayer { translationY = offset.value * size.height }
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Palette.Paper)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        if (offset.value > 0.25f || velocity > 1400f) dismiss()
                        else offset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                    },
                )
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 18.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Palette.Mist2),
            )
            content(current)
        }
    }
}
