package com.asahioo.moodly.ui.components

import androidx.compose.animation.Animatable as colorAnimatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.ui.UiText
import com.asahioo.moodly.ui.color
import com.asahioo.moodly.ui.resolve
import com.asahioo.moodly.ui.theme.LocalReduceMotion
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.Motion
import com.asahioo.moodly.ui.theme.Palette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface ToastIcon {
    data class OfMood(val mood: Mood) : ToastIcon
    data object Check : ToastIcon
    data object Sleep : ToastIcon
    data object Alert : ToastIcon
}

@Immutable
data class ToastData(
    val icon: ToastIcon,
    val title: UiText,
    val subtitle: UiText,
    val id: Long = System.nanoTime(),
)

/** Avisos tipo tarjeta heads-up: bajan desde la barra de estado tintados con el color del ánimo. */
@Stable
class IslandToastState(private val scope: CoroutineScope) {
    var current by mutableStateOf<ToastData?>(null)
        private set
    private var hideJob: Job? = null

    fun show(data: ToastData) {
        current = data
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(VISIBLE_MS)
            current = null
        }
    }

    fun dismiss() {
        hideJob?.cancel()
        current = null
    }

    internal companion object {
        const val VISIBLE_MS = 2400L
    }
}

val LocalToast = staticCompositionLocalOf<IslandToastState> { error("IslandToastState no provisto") }

/** Fondo pastel (mezcla con el fondo del tema, así en oscuro queda un tinte oscuro): el icono conserva su color pleno y resalta sobre la tarjeta. */
private val ToastIcon.tint: Color
    get() = lerp(
        when (this) {
            is ToastIcon.OfMood -> mood.color
            ToastIcon.Check -> Palette.Lime
            ToastIcon.Sleep -> Palette.MoodSleepy
            ToastIcon.Alert -> Palette.Peach
        },
        Palette.Paper,
        0.55f,
    )

@Composable
fun IslandToast(state: IslandToastState, modifier: Modifier = Modifier) {
    val data = state.current
    // Contenido en pantalla: se actualiza tras reiniciar sus animaciones (sin un frame "fantasma")
    // y se conserva durante la salida.
    var shown by remember { mutableStateOf<ToastData?>(null) }
    val open = data != null
    val reduce = LocalReduceMotion.current

    val appear = remember { Animatable(0f) }
    var drag by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(open) {
        if (open) {
            drag = 0f
            appear.animateTo(1f, if (reduce) tween(150) else spring(dampingRatio = 0.78f, stiffness = 380f))
        } else {
            // Tras un swipe sigue el impulso del dedo; si no, acelera hacia la barra de estado.
            val exit = when {
                reduce -> tween<Float>(150)
                drag < 0f -> tween(180, easing = LinearEasing)
                else -> tween(300, easing = Motion.EaseIn)
            }
            appear.animateTo(0f, exit)
        }
    }

    // Por aviso: barra de auto-cierre, rebote de la carita y entrada del texto.
    val progress = remember { Animatable(1f) }
    val iconScale = remember { Animatable(1f) }
    val textAlpha = remember { Animatable(1f) }
    val tint = remember { colorAnimatable(Palette.Paper) }
    LaunchedEffect(data?.id) {
        if (data == null) return@LaunchedEffect
        val chained = appear.value > 0.5f
        if (!reduce) {
            iconScale.snapTo(0.6f)
            textAlpha.snapTo(0f)
            progress.snapTo(1f)
        }
        shown = data
        // Si la tarjeta ya estaba visible, el color transiciona; si no, entra directamente con el suyo.
        if (chained) launch { tint.animateTo(data.icon.tint, tween(300)) } else tint.snapTo(data.icon.tint)
        if (reduce) return@LaunchedEffect
        launch {
            progress.animateTo(0f, tween(IslandToastState.VISIBLE_MS.toInt(), easing = LinearEasing))
        }
        launch {
            delay(80)
            iconScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
        }
        // Encadenado: el texto nuevo entra sin espera para que la tarjeta no quede vacía.
        if (!chained) delay(100)
        textAlpha.animateTo(1f, tween(if (chained) 160 else 200))
    }

    val density = LocalDensity.current
    val dismissPx = with(density) { 40.dp.toPx() }
    val dragState = rememberDraggableState { delta ->
        // Hacia arriba sigue al dedo; hacia abajo opone resistencia.
        drag += if (drag + delta > 0f) delta * 0.2f else delta
    }

    Box(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            // Recorta en el borde de la barra de estado: la tarjeta sale de debajo de ella.
            .clipToBounds()
            .padding(top = 8.dp, start = 12.dp, end = 12.dp, bottom = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        val shown = shown
        // targetValue cubre el frame en que se cierra, antes de que arranque la animación de salida.
        if (shown != null && (open || appear.targetValue > 0f || appear.isRunning)) {
            val shape = RoundedCornerShape(24.dp)
            val shadowColor = Palette.FaceInk.copy(alpha = 0.18f)
            Row(
                Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val a = appear.value
                        // Sin fade: entra y sale por debajo del borde recortado de la barra de estado.
                        alpha = if (reduce) a else 1f
                        translationY = if (reduce) 0f else -(1f - a) * (size.height + 16.dp.toPx()) + drag
                    }
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            if (drag < -dismissPx || velocity < -1000f) state.dismiss()
                            else animate(drag, 0f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)) { v, _ -> drag = v }
                        },
                    )
                    .shadow(8.dp, shape, ambientColor = shadowColor, spotColor = shadowColor)
                    .clip(shape)
                    .drawWithContent {
                        drawRect(tint.value)
                        drawContent()
                        if (!reduce) {
                            val h = 3.dp.toPx()
                            drawRect(
                                Palette.FaceInk.copy(alpha = 0.18f),
                                topLeft = Offset(0f, size.height - h),
                                size = Size(size.width * progress.value, h),
                            )
                        }
                    }
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .heightIn(min = 68.dp)
                    .padding(start = 12.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ToastIconView(
                    shown.icon,
                    Modifier
                        .size(40.dp)
                        .graphicsLayer {
                            scaleX = iconScale.value
                            scaleY = iconScale.value
                        },
                )
                Column(Modifier.weight(1f).graphicsLayer { alpha = textAlpha.value }) {
                    Text(
                        shown.title.resolve(),
                        style = MoodType.Base.copy(color = Palette.Ink, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        shown.subtitle.resolve(),
                        style = MoodType.Base.copy(color = Palette.Ink.copy(alpha = 0.62f), fontSize = 12.5.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToastIconView(icon: ToastIcon, modifier: Modifier) {
    when (icon) {
        is ToastIcon.OfMood -> MoodIcon(icon.mood, modifier)
        ToastIcon.Check -> Glyph(remember(Palette.theme) { AppIcons.ToastCheck }, modifier)
        ToastIcon.Sleep -> Glyph(remember(Palette.theme) { AppIcons.ToastSleep }, modifier)
        ToastIcon.Alert -> Glyph(AppIcons.ToastAlert, modifier)
    }
}
