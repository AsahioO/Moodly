package com.asahioo.moodly.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.asahioo.moodly.ui.theme.LocalReduceMotion
import com.asahioo.moodly.ui.theme.Motion

/**
 * Click con rebote elástico (escala al presionar, resorte al soltar) + vibración ligera.
 * Sustituye el ripple por una respuesta más física, como en el diseño de referencia.
 */
fun Modifier.bounceClick(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    role: Role = Role.Button,
    onClickLabel: String? = null,
    haptic: Boolean = true,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = if (pressed) 1500f else 450f),
        label = "press",
    )
    val haptics = LocalHaptics.current
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
        ) {
            if (haptic) haptics.tick()
            onClick()
        }
}

/**
 * Reloj compartido para animaciones de entrada escalonadas. Un solo Animatable alimenta a
 * todos los elementos y la lectura ocurre en la fase de dibujo (graphicsLayer), así que no
 * provoca recomposiciones por frame.
 */
@Stable
class Stagger internal constructor(
    private val clock: Animatable<Float, AnimationVector1D>,
    private val totalMs: Int,
) {
    fun progress(delayMs: Int, durationMs: Int, easing: Easing): Float {
        val t = clock.value * totalMs
        return easing.transform(((t - delayMs) / durationMs).coerceIn(0f, 1f))
    }
}

@Composable
fun rememberStagger(key: Any?, totalMs: Int = 1800): Stagger {
    val reduce = LocalReduceMotion.current
    val clock = remember(key) { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(clock) {
        if (clock.value < 1f) clock.animateTo(1f, tween(totalMs, easing = LinearEasing))
    }
    return remember(clock) { Stagger(clock, totalMs) }
}

fun Modifier.staggered(
    stagger: Stagger?,
    delayMs: Int,
    durationMs: Int = 600,
    offsetY: Dp = 16.dp,
    fromScale: Float = 1f,
    easing: Easing = Motion.EaseOut,
): Modifier = if (stagger == null) this else graphicsLayer {
    val p = stagger.progress(delayMs, durationMs, easing)
    alpha = p.coerceIn(0f, 1f)
    translationY = (1f - p) * offsetY.toPx()
    if (fromScale != 1f) {
        val s = fromScale + (1f - fromScale) * p
        scaleX = s
        scaleY = s
    }
}
