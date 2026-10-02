package com.asahioo.moodly.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asahioo.moodly.R
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.ui.theme.LocalReduceMotion
import com.asahioo.moodly.ui.theme.Motion
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

// Diámetro del logo en el splash nativo (110/288 de un icono de 240dp): empalma sin salto.
private val START_SIZE = 92.dp
private const val EXIT_MS = 1150f
private const val END_MS = 1550f

/** Campana 0 → 1 → 0 dentro de [a, b]; 0 fuera. */
private fun win(t: Float, a: Float, b: Float): Float =
    if (t <= a || t >= b) 0f else sin(PI.toFloat() * (t - a) / (b - a))

/** 0 → 1 lineal dentro de [a, b]. */
private fun ramp(t: Float, a: Float, b: Float): Float = ((t - a) / (b - a)).coerceIn(0f, 1f)

/**
 * Entrada al abrir la app: el logo del splash se agacha, salta con anillos, mira a los lados,
 * parpadea, aparece "Moodly" letra a letra y todo hace zoom hacia la app. Un toque la adelanta.
 * Un solo reloj en ms; todo se lee en fase de dibujo.
 */
@Composable
fun LaunchIntro(onFinished: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val t = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    suspend fun runTo(end: Float) {
        t.animateTo(end, tween((end - t.value).toInt().coerceAtLeast(0), easing = LinearEasing))
        onFinished()
    }

    LaunchedEffect(reduce) {
        if (reduce) onFinished() else runTo(END_MS)
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - ramp(t.value, EXIT_MS + 100f, END_MS) }
            .background(Palette.Paper)
            .pointerInput(Unit) {
                detectTapGestures {
                    scope.launch {
                        if (t.value < EXIT_MS) t.snapTo(EXIT_MS)
                        runTo(END_MS)
                    }
                }
            }
            .drawBehind {
                // Dos anillos del color de Feliz que se expanden desde el logo durante el salto.
                val r0 = START_SIZE.toPx() / 2f
                for (i in 0..1) {
                    val p = ramp(t.value, 300f + i * 130f, 950f + i * 130f)
                    if (p <= 0f || p >= 1f) continue
                    drawCircle(
                        color = Palette.MoodHappy,
                        radius = r0 * (1f + 1.3f * Motion.EaseOut.transform(p)),
                        center = center,
                        alpha = 0.55f * (1f - p),
                        style = Stroke(width = (3f - 2f * p).dp.toPx()),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Glyph(
            OnboardingGlyphs.shape(Mood.HAPPY),
            Modifier
                .size(START_SIZE)
                .graphicsLayer {
                    val v = t.value
                    val squash = win(v, 0f, 220f)
                    val pop = 0.15f * win(v, 220f, 600f) - 0.04f * win(v, 600f, 800f)
                    val zoom = 1f + 7f * Motion.EaseIn.transform(ramp(v, EXIT_MS, END_MS))
                    val sy = 1f - 0.1f * squash
                    scaleX = (1f + 0.08f * squash + pop) * zoom
                    scaleY = (sy + pop) * zoom
                    // El squash se apoya en la base del círculo.
                    translationY = (1f - sy) * size.height / 2f - 10.dp.toPx() * win(v, 220f, 600f)
                },
            look = { Offset(win(t.value, 600f, 760f) * -1f + win(t.value, 740f, 900f), 0f) },
            tint = Palette.FaceInk,
            blink = { win(t.value, 830f, 970f) },
        )
        Row(Modifier.offset(y = START_SIZE / 2 + 44.dp)) {
            stringResource(R.string.app_name).forEachIndexed { i, c ->
                Text(
                    c.toString(),
                    style = MoodType.Greeting,
                    modifier = Modifier.graphicsLayer {
                        val start = 450f + i * 60f
                        val p = Motion.EaseOut.transform(ramp(t.value, start, start + 380f))
                        alpha = p * (1f - ramp(t.value, EXIT_MS, EXIT_MS + 150f))
                        translationY = (1f - p) * 14.dp.toPx()
                    },
                )
            }
        }
    }
}
