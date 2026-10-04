package com.asahioo.moodly.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.asahioo.moodly.R
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.ui.color
import com.asahioo.moodly.ui.components.AnimatedMoodIcon
import com.asahioo.moodly.ui.components.CardShape
import com.asahioo.moodly.ui.components.MoodIcon
import com.asahioo.moodly.ui.components.RollingText
import com.asahioo.moodly.ui.components.Stagger
import com.asahioo.moodly.ui.components.bounceClick
import com.asahioo.moodly.ui.components.staggered
import com.asahioo.moodly.ui.labelRes
import com.asahioo.moodly.ui.theme.LocalReduceMotion
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.Motion
import com.asahioo.moodly.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** El ánimo de hoy en grande sobre su color; sin registro, un hueco punteado que invita a elegir. */
@Composable
internal fun MoodHero(mood: Mood?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val reduce = LocalReduceMotion.current
    val bg by animateColorAsState(mood?.color?.copy(alpha = 0.35f) ?: Palette.Mist, tween(400), label = "heroBg")
    val burst = remember { Animatable(1f) }
    // El primer valor llega al componer: solo los cambios posteriores (elegir ánimo) disparan el estallido.
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(mood) {
        if (armed && mood != null && !reduce) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(620, easing = Motion.EaseOut))
        }
        armed = true
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(bg)
            .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedContent(
            targetState = mood,
            // La cara crece o se encoge con el alto que le deje el héroe.
            modifier = Modifier
                .weight(1f, fill = false)
                .sizeIn(minHeight = 72.dp, maxHeight = 128.dp)
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .drawBehind {
                    val b = burst.value
                    if (b < 1f && mood != null) {
                        for (k in 0 until 10) {
                            val angle = (k / 10f) * 2f * PI.toFloat() + 0.2f
                            val dist = (60.dp.toPx() + (k % 3) * 8.dp.toPx()) * b
                            drawCircle(
                                color = if (k % 2 == 0) mood.color else Palette.Ink,
                                radius = 6.dp.toPx() * (1f - 0.8f * b),
                                center = Offset(center.x + cos(angle) * dist, center.y + sin(angle) * dist),
                                alpha = 1f - b,
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center,
            transitionSpec = {
                if (reduce) {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                } else {
                    (scaleIn(spring(dampingRatio = 0.45f, stiffness = 320f), initialScale = 0.5f) + fadeIn(tween(160))) togetherWith
                        (scaleOut(tween(160, easing = Motion.EaseIn), targetScale = 0.6f) + fadeOut(tween(140)))
                }
            },
            label = "heroFace",
        ) { m ->
            if (m == null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .drawBehind {
                            val w = 2.dp.toPx()
                            drawCircle(
                                Palette.Ink.copy(alpha = 0.22f),
                                radius = size.minDimension / 2 - w,
                                style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 7.dp.toPx()))),
                            )
                        },
                )
            } else {
                AnimatedMoodIcon(m, active = true, modifier = Modifier.fillMaxSize())
            }
        }
        RollingText(
            mood,
            style = MoodType.SheetTitle,
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = 12.dp)
                .semantics { heading() },
        ) { m -> if (m == null) stringResource(R.string.home_pick_title) else stringResource(m.labelRes) }
        content()
    }
}

/** Fila de las seis caras, sin cajas: la elegida crece y lleva un punto; las demás se atenúan. */
@Composable
internal fun MoodStrip(selected: Mood?, stagger: Stagger?, onPick: (Mood) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Mood.entries.forEachIndexed { i, mood ->
            StripFace(
                mood = mood,
                isSelected = mood == selected,
                dimmed = selected != null && mood != selected,
                onClick = { onPick(mood) },
                modifier = Modifier.staggered(stagger, 200 + i * 45, 650, 14.dp, 0.85f, Motion.Back),
            )
        }
    }
}

@Composable
private fun StripFace(mood: Mood, isSelected: Boolean, dimmed: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scale by animateFloatAsState(if (isSelected) 1.15f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "faceScale")
    val fade by animateFloatAsState(if (dimmed) 0.55f else 1f, tween(250), label = "faceAlpha")
    val dot by animateFloatAsState(if (isSelected) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "faceDot")
    val label = stringResource(mood.labelRes)
    Box(
        modifier
            .size(width = 48.dp, height = 62.dp)
            .bounceClick(
                onClick = onClick,
                pressedScale = 0.88f,
                haptic = false,
                onClickLabel = stringResource(R.string.cd_log_mood, label),
            )
            .semantics {
                contentDescription = label
                selected = isSelected
            }
            .drawBehind {
                val d = dot
                if (d > 0.01f) drawCircle(Palette.Ink, 3.dp.toPx() * d, Offset(center.x, size.height - 4.dp.toPx()))
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        MoodIcon(
            mood,
            Modifier
                .padding(top = 6.dp)
                .size(40.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = fade
                },
        )
    }
}
