package com.asahioo.moodly.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.ui.UiText
import com.asahioo.moodly.ui.resolve
import com.asahioo.moodly.ui.theme.MoodType
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

/** Avisos tipo "isla": una píldora negra que se expande desde arriba. */
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

    private companion object {
        const val VISIBLE_MS = 2400L
    }
}

val LocalToast = staticCompositionLocalOf<IslandToastState> { error("IslandToastState no provisto") }

@Composable
fun IslandToast(state: IslandToastState, modifier: Modifier = Modifier) {
    val data = state.current
    var last by remember { mutableStateOf<ToastData?>(null) }
    LaunchedEffect(data) { if (data != null) last = data }
    val shown = data ?: last
    val open = data != null

    // Pequeño "latido" cuando llega un aviso nuevo con la isla ya abierta.
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(data?.id) {
        if (data != null) {
            pulse.snapTo(0.94f)
            pulse.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
        }
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 6.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        val openWidth = min(340.dp, maxWidth - 24.dp)
        val transition = updateTransition(open, label = "island")
        val springDp = spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.72f, stiffness = 420f)
        val width by transition.animateDp({ springDp }, label = "w") { if (it) openWidth else 120.dp }
        val height by transition.animateDp({ springDp }, label = "h") { if (it) 64.dp else 36.dp }
        val alpha by transition.animateFloat({ tween(if (open) 160 else 260) }, label = "a") { if (it) 1f else 0f }
        val lift by transition.animateFloat({ spring(dampingRatio = 0.7f, stiffness = 420f) }, label = "y") { if (it) 0f else 1f }
        val contentAlpha by transition.animateFloat(
            { if (open) tween(260, delayMillis = 140) else tween(120) },
            label = "c",
        ) { if (it) 1f else 0f }

        if (shown != null) {
            Box(
                Modifier
                    .size(width, height)
                    .graphicsLayer {
                        this.alpha = alpha
                        val s = (1f - 0.3f * lift) * pulse.value
                        scaleX = s
                        scaleY = s
                        translationY = -14.dp.toPx() * lift
                    }
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier
                        .requiredWidth(openWidth)
                        .height(64.dp)
                        .graphicsLayer { this.alpha = contentAlpha }
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .padding(start = 12.dp, end = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ToastIconView(shown.icon, Modifier.size(40.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            shown.title.resolve(),
                            style = MoodType.Base.copy(color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            shown.subtitle.resolve(),
                            style = MoodType.Base.copy(color = Palette.ToastSub, fontSize = 12.5.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToastIconView(icon: ToastIcon, modifier: Modifier) {
    when (icon) {
        is ToastIcon.OfMood -> MoodIcon(icon.mood, modifier)
        ToastIcon.Check -> Glyph(AppIcons.ToastCheck, modifier)
        ToastIcon.Sleep -> Glyph(AppIcons.ToastSleep, modifier)
        ToastIcon.Alert -> Glyph(AppIcons.ToastAlert, modifier)
    }
}
