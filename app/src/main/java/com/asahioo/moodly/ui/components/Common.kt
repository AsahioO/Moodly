package com.asahioo.moodly.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.asahioo.moodly.R
import com.asahioo.moodly.ui.theme.LocalReduceMotion
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.Motion
import com.asahioo.moodly.ui.theme.Palette
import kotlin.math.roundToInt

/** Panel blanco con esquinas inferiores redondeadas sobre el fondo negro. */
val PanelShape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
val CardShape = RoundedCornerShape(22.dp)

/** Número que cuenta hasta su valor cada vez que cambia. */
@Composable
fun AnimatedNumber(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    durationMs: Int = 900,
    delayMs: Int = 0,
    format: (Int) -> String = { it.toString() },
) {
    val reduce = LocalReduceMotion.current
    val anim = remember { Animatable(if (reduce) value.toFloat() else 0f) }
    LaunchedEffect(value, reduce) {
        if (reduce) anim.snapTo(value.toFloat())
        else anim.animateTo(value.toFloat(), tween(durationMs, delayMs, Motion.EaseOut))
    }
    Text(text = format(anim.value.roundToInt()), style = style, modifier = modifier)
}

/** Texto que se desliza verticalmente al cambiar (niveles, etiquetas). */
@Composable
fun <T> RollingText(
    target: T,
    modifier: Modifier = Modifier,
    style: TextStyle,
    contentAlignment: Alignment = Alignment.TopStart,
    text: @Composable (T) -> String,
) {
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        contentAlignment = contentAlignment,
        transitionSpec = {
            (slideInVertically(spring(dampingRatio = 0.7f, stiffness = 380f)) { it / 2 } + fadeIn(tween(220))) togetherWith
                (slideOutVertically(tween(180)) { -it / 2 } + fadeOut(tween(150)))
        },
        label = "rolling",
    ) { value ->
        Text(text(value), style = style)
    }
}

/** Valor de una tarjeta que aún no tiene registro: "—" y una pista de qué hacer. */
@Composable
fun EmptyValue(hint: String, style: TextStyle, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(stringResource(R.string.no_data), style = style)
        Text(hint, style = MoodType.Caption.copy(color = Palette.Ink.copy(alpha = 0.6f)))
    }
}

@Composable
fun CircleIconButton(
    icon: GlyphSpec,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bordered: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .bounceClick(onClick = onClick, pressedScale = 0.88f)
            .semantics { this.contentDescription = contentDescription }
            .then(if (bordered) Modifier.border(1.5.dp, Palette.Mist2, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Glyph(icon, Modifier.size(20.dp))
    }
}

/** Encabezado de pantalla: atrás, título centrado con subtítulo animado y acción opcional. */
@Composable
fun NavHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CircleIconButton(AppIcons.Back, stringResource(R.string.cd_back), onBack)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                style = MoodType.Title,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            RollingText(subtitle, style = MoodType.Caption) { it }
        }
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { action?.invoke() }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MoodType.Section,
        modifier = modifier
            .padding(start = 2.dp, top = 22.dp, bottom = 10.dp)
            .semantics { heading() },
    )
}

@Composable
fun CardHeader(icon: GlyphSpec?, text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Glyph(icon, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MoodType.Label)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Palette.Ink,
    content: Color = Palette.Paper,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .bounceClick(onClick = onClick, enabled = enabled, pressedScale = 0.97f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) container else container.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MoodType.Button.copy(color = content))
    }
}

/** Interruptor con resorte, en la línea visual del diseño (negro encendido). */
@Composable
fun MoodSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val track by animateColorAsState(if (checked) Palette.Ink else Palette.SwitchOff, tween(250), label = "track")
    val knobX by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 520f),
        label = "knob",
    )
    val haptics = LocalHaptics.current
    Box(
        modifier = modifier
            .size(width = 50.dp, height = 30.dp)
            .clip(CircleShape)
            .background(track)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onValueChange = {
                    haptics.tick()
                    onCheckedChange(it)
                },
            )
            .semantics { this.contentDescription = contentDescription }
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset { IntOffset(knobX.roundToPx(), 0) }
                .size(24.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}
