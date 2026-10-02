package com.asahioo.moodly.ui.onboarding

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.asahioo.moodly.R
import com.asahioo.moodly.data.model.AppData
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.ui.components.AppIcons
import com.asahioo.moodly.ui.components.Glyph
import com.asahioo.moodly.ui.components.GlyphSpec
import com.asahioo.moodly.ui.components.LocalHaptics
import com.asahioo.moodly.ui.components.OnboardingGlyphs
import com.asahioo.moodly.ui.components.bounceClick
import com.asahioo.moodly.ui.components.drawGlyph
import com.asahioo.moodly.ui.components.rememberStagger
import com.asahioo.moodly.ui.components.staggered
import com.asahioo.moodly.ui.theme.LocalReduceMotion
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.Motion
import com.asahioo.moodly.ui.theme.Palette
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Posición en % del área de figuras (390x520 en el diseño), ancho en % y giro base.
 * La posición es de donde cae (x) y donde queda si se quitan las animaciones (x, y).
 * [radius] = radio de colisión como fracción del ancho.
 */
private class ShapeSpot(
    mood: Mood,
    val x: Float,
    val y: Float,
    val w: Float,
    val rotation: Float,
    val radius: Float,
    val blinks: Boolean = false,
) {
    val glyph: GlyphSpec = OnboardingGlyphs.shape(mood)
    val dizzy: GlyphSpec = OnboardingGlyphs.dizzy(mood)
}

private val Spots = listOf(
    ShapeSpot(Mood.CALM, 3.6f, 64.4f, 43.8f, 0f, 0.44f),
    ShapeSpot(Mood.BORED, -4.4f, 34.8f, 39f, 0f, 0.44f),
    ShapeSpot(Mood.ANGRY, 59f, 59.4f, 41.5f, -6f, 0.44f, blinks = true),
    ShapeSpot(Mood.SLEEPY, 68.2f, 28.7f, 34.6f, 12f, 0.40f),
    ShapeSpot(Mood.HAPPY, 19.5f, 8.7f, 36.9f, 0f, 0.45f, blinks = true),
    ShapeSpot(Mood.STRESSED, 58f, 0.2f, 34.6f, -10f, 0.45f, blinks = true),
    ShapeSpot(Mood.HAPPY, 35.9f, 30.4f, 30f, 8f, 0.45f, blinks = true),
)

/** Orden de caída (índices de [Spots]): primero las de abajo, así la pila se parece al diseño. */
private val DropOrder = listOf(0, 2, 1, 6, 3, 4, 5)

// ponytail: calibración a ojo; ajustar en dispositivo.
/** Gravedad mínima (en g) aunque el celular esté casi plano, para que no queden flotando. */
private const val MIN_G = 0.45f
/** Velocidad (dp/s) a la que la cara ya mira del todo hacia donde va. */
private const val LOOK_SPEED = 900f
/** Velocidad máxima (dp/s) al lanzar una figura con el dedo. */
private const val MAX_FLING = 4000f

private const val EXIT_MS = 1000
private const val FINISH_AT_MS = 620L

private enum class Step { Intro, Name, Reminder }

/**
 * Introducción animada y luego dos pasos cortos: nombre y hora del recordatorio.
 * [onFinished] recibe reminderMinutes = null si el usuario eligió "Ahora no" o negó el permiso.
 */
@Composable
fun OnboardingScreen(initialName: String, onFinished: (name: String, reminderMinutes: Int?) -> Unit) {
    var step by rememberSaveable { mutableStateOf(Step.Intro) }
    var name by rememberSaveable { mutableStateOf(initialName) }
    val reduce = LocalReduceMotion.current

    BackHandler(enabled = step == Step.Reminder) { step = Step.Name }

    AnimatedContent(
        targetState = step,
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper),
        transitionSpec = {
            if (reduce) {
                fadeIn(tween(200)) togetherWith fadeOut(tween(150))
            } else {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(420, easing = Motion.EaseOut)) { dir * it / 4 } + fadeIn(tween(300, 80))) togetherWith
                    (slideOutHorizontally(tween(220, easing = Motion.EaseIn)) { -dir * it / 4 } + fadeOut(tween(160)))
            }
        },
        label = "onboardingStep",
    ) { current ->
        when (current) {
            Step.Intro -> Intro(onDone = { step = Step.Name })
            Step.Name -> NameStep(
                name = name,
                onNameChange = { name = it.take(AppData.MAX_NAME_LENGTH) },
                onContinue = { step = Step.Reminder },
            )
            Step.Reminder -> ReminderStep(onDone = { minutes -> onFinished(name.trim(), minutes) })
        }
    }
}

@Composable
private fun Intro(onDone: () -> Unit) {
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()
    val finished by rememberUpdatedState(onDone)
    val intro = rememberStagger(Unit, totalMs = 1300)
    val exit = remember { Animatable(0f) }
    var leaving by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val statusTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val description = stringResource(R.string.ob_cd)
    val reduce = LocalReduceMotion.current
    val gravity by rememberGravity(enabled = !reduce)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .semantics { contentDescription = description },
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        // Área de figuras anclada abajo, con la proporción del diseño (390x520).
        val areaHeight = min(maxWidth * (520f / 390f), maxHeight - 262.dp - statusTop)
        val areaWidth = areaHeight * (390f / 520f)
        val areaW = with(density) { areaWidth.toPx() }
        val areaH = with(density) { areaHeight.toPx() }

        // Las figuras viven en toda la pantalla (piso = borde inferior); nacen en su lugar del diseño.
        val world = remember(screenWidthPx, screenHeightPx, areaW) {
            val left = (screenWidthPx - areaW) / 2f
            val top = screenHeightPx - areaH
            World(
                screenWidthPx,
                screenHeightPx,
                Spots.map { spot ->
                    val w = areaW * spot.w / 100f
                    val h = w / spot.glyph.aspectRatio
                    Body(w * spot.radius, left + areaW * spot.x / 100f + w / 2f, top + areaH * spot.y / 100f + h / 2f, spot.rotation)
                },
                density.density,
            )
        }
        var frame by remember { mutableLongStateOf(0L) }

        LaunchedEffect(world) {
            if (reduce) return@LaunchedEffect
            // Arrancan justo arriba de la pantalla, girando un poco, y caen escalonadas.
            world.bodies.forEachIndexed { i, b ->
                b.x = b.x.coerceIn(b.r, world.width - b.r)
                b.y = -b.r * 1.3f
                b.spin = (if (i % 2 == 0) -1f else 1f) * (20f + i * 5f)
            }
            launch {
                delay(380L)
                DropOrder.forEach { i ->
                    world.bodies[i].active = true
                    delay(170L)
                }
            }
            var last = withFrameNanos { it }
            var lastBuzz = 0L
            while (true) {
                withFrameNanos { now ->
                    val g = gravity
                    if (world.step((now - last) / 1e9f, g.x, g.y) && now - lastBuzz > 150_000_000L) {
                        haptics.tick()
                        lastBuzz = now
                    }
                    last = now
                    frame++
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .then(if (reduce) Modifier else Modifier.shapeGestures(world)),
        ) {
            Spots.forEachIndexed { index, spot ->
                PhysicsShape(
                    spot = spot,
                    body = world.bodies[index],
                    index = index,
                    frame = { frame },
                    time = { world.time },
                    screenHeightPx = screenHeightPx,
                    exitProgress = { exit.value },
                )
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            listOf(R.string.ob_line_1, R.string.ob_line_2, R.string.ob_line_3).forEachIndexed { i, res ->
                Text(
                    stringResource(res),
                    style = MoodType.Display,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .staggered(intro, 100 + i * 90, 750, 28.dp)
                        .graphicsLayer {
                            val e = localProgress(exit.value * EXIT_MS, i * 50f, 380f, Motion.EaseIn)
                            alpha = 1f - e
                            translationY = -30.dp.toPx() * e
                        },
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier
                    .staggered(intro, 420, 700, 16.dp, 0.9f, Motion.Back)
                    .graphicsLayer {
                        val e = localProgress(exit.value * EXIT_MS, 180f, 300f, Motion.EaseIn)
                        alpha = 1f - e
                        scaleX = 1f - 0.1f * e
                        scaleY = 1f - 0.1f * e
                    }
                    .bounceClick(
                        onClick = {
                            if (!leaving) {
                                leaving = true
                                haptics.confirm()
                                scope.launch { exit.animateTo(1f, tween(EXIT_MS, easing = LinearEasing)) }
                                scope.launch {
                                    delay(FINISH_AT_MS)
                                    finished()
                                }
                            }
                        },
                        pressedScale = 0.95f,
                        haptic = false,
                    )
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Palette.Mist)
                    .padding(start = 22.dp, end = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.ob_cta), style = MoodType.Button.copy(fontSize = MoodType.Chip.fontSize * 1.12f))
                Spacer(Modifier.width(14.dp))
                Box(
                    Modifier
                        .graphicsLayer {
                            val e = localProgress(exit.value * EXIT_MS, 0f, 420f, Motion.EaseIn)
                            translationX = 40.dp.toPx() * e
                            val s = 1f - 0.8f * e
                            scaleX = s
                            scaleY = s
                        }
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Palette.Ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Glyph(AppIcons.ArrowRight, Modifier.size(18.dp), tint = Palette.Paper)
                }
            }
        }
    }
}

/**
 * Arrastrar: la figura sigue el dedo y empuja a las demás; al soltarla sale lanzada.
 * Tocar: saltito y aplastón. Se hace sobre el contenedor para atinarle al círculo de cada figura.
 */
private fun Modifier.shapeGestures(world: World): Modifier = composed {
    val haptics = LocalHaptics.current
    val maxFling = with(LocalDensity.current) { MAX_FLING.dp.toPx() }
    this
        .pointerInput(world) {
            detectTapGestures { p ->
                world.bodyAt(p.x, p.y)?.let {
                    haptics.tick()
                    world.poke(it)
                }
            }
        }
        .pointerInput(world) {
            val tracker = VelocityTracker()
            var held: Body? = null
            fun release() {
                held?.let {
                    val v = tracker.calculateVelocity()
                    it.vx = v.x.coerceIn(-maxFling, maxFling)
                    it.vy = v.y.coerceIn(-maxFling, maxFling)
                    it.held = false
                }
                held = null
            }
            detectDragGestures(
                onDragStart = { p ->
                    held = world.bodyAt(p.x, p.y)?.also {
                        it.held = true
                        tracker.resetTracking()
                        haptics.tick()
                    }
                },
                onDragEnd = ::release,
                onDragCancel = ::release,
            ) { change, amount ->
                val b = held ?: return@detectDragGestures
                change.consume()
                tracker.addPosition(change.uptimeMillis, change.position)
                b.x += amount.x
                b.y += amount.y
                // Mientras se arrastra, su velocidad es la del dedo (para empujar a las demás).
                val v = tracker.calculateVelocity()
                b.vx = v.x
                b.vy = v.y
            }
        }
}

/**
 * Vector de gravedad en el plano de la pantalla, en g (x → derecha, y → abajo; vertical = (0, 1)).
 * TYPE_GRAVITY ya fusiona giroscopio + acelerómetro; si no existe, acelerómetro suavizado.
 */
@Composable
private fun rememberGravity(enabled: Boolean): State<Offset> {
    val gravity = remember { mutableStateOf(Offset(0f, 1f)) }
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val sm = context.getSystemService(SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!enabled || sensor == null) {
            gravity.value = Offset(0f, 1f)
            return@DisposableEffect onDispose {}
        }
        val smooth = if (sensor.type == Sensor.TYPE_GRAVITY) 1f else 0.15f
        var gx = 0f
        var gy = SensorManager.GRAVITY_EARTH
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                gx += (e.values[0] - gx) * smooth
                gy += (e.values[1] - gy) * smooth
                val g = Offset(-gx, gy) / SensorManager.GRAVITY_EARTH
                val len = g.getDistance()
                gravity.value = when {
                    len < 0.01f -> Offset(0f, MIN_G)
                    len < MIN_G -> g * (MIN_G / len)
                    len > 1f -> g / len
                    else -> g
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }
    return gravity
}

private fun localProgress(t: Float, delay: Float, duration: Float, easing: androidx.compose.animation.core.Easing): Float =
    easing.transform(((t - delay) / duration).coerceIn(0f, 1f))

/**
 * Figura dibujada donde diga su [body] (posición, giro y aplastamiento vienen de la física).
 * La cara mira hacia donde se mueve; tras un golpe fuerte pone cara de mareo y la mirada da vueltas.
 * Las de ojos abiertos parpadean de vez en cuando.
 */
@Composable
private fun PhysicsShape(
    spot: ShapeSpot,
    body: Body,
    index: Int,
    frame: () -> Long,
    time: () -> Float,
    screenHeightPx: Float,
    exitProgress: () -> Float,
) {
    val reduce = LocalReduceMotion.current
    val density = LocalDensity.current
    val blink = remember { Animatable(0f) }
    val lookSpeed = with(density) { LOOK_SPEED.dp.toPx() }
    val widthPx = body.r / spot.radius
    val heightPx = widthPx / spot.glyph.aspectRatio

    LaunchedEffect(Unit) {
        if (!spot.blinks || reduce) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(2500L, 6500L))
            blink.animateTo(1f, tween(70))
            blink.animateTo(0f, tween(120))
        }
    }

    Spacer(
        Modifier
            .size(with(density) { widthPx.toDp() }, with(density) { heightPx.toDp() })
            .graphicsLayer {
                frame() // se re-evalúa en cada paso de la física, sin recomponer
                val dir = if (index % 2 == 0) -1f else 1f
                val e = localProgress(exitProgress() * EXIT_MS, 60f + index * 45f, 760f, Motion.EaseIn)
                translationX = body.x - size.width / 2f
                translationY = body.y - size.height / 2f + e.pow(1.4f) * screenHeightPx * 1.1f
                rotationZ = body.angle + dir * (30f + index * 6f) * e
                scaleX = 1f + body.squish
                scaleY = 1f - body.squish * 0.9f
            }
            .drawBehind {
                frame()
                val look = if (body.dizzy > 0f) {
                    val t = time() * 9f
                    Offset(cos(t), sin(t)) * 0.8f
                } else {
                    // Velocidad en pantalla → marco de la figura (que está girada).
                    val a = Math.toRadians(-body.angle.toDouble())
                    val c = cos(a).toFloat()
                    val s = sin(a).toFloat()
                    val v = Offset(body.vx * c - body.vy * s, body.vx * s + body.vy * c) / lookSpeed
                    val d = v.getDistance()
                    if (d > 1f) v / d else v
                }
                drawGlyph(if (body.dizzy > 0f) spot.dizzy else spot.glyph, Palette.FaceInk, look, blink.value)
            },
    )
}
