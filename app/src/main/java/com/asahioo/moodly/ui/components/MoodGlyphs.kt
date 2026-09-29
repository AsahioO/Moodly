package com.asahioo.moodly.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.ui.color
import com.asahioo.moodly.ui.theme.LocalReduceMotion
import com.asahioo.moodly.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Caras (viewBox 40x40) y figuras de cada ánimo.
 * ponytail: icon() está duplicado en res/drawable/ic_mood_*.xml para la notificación y el widget
 * (RemoteViews no puede usar Compose); si cambias una cara, cambia ambos lados.
 */
object MoodGlyphs {

    private const val STROKE = 2.4f
    private const val BIG_STROKE = 2.1f

    /** Rasgos animables por separado (slots de [GPart]); en reposo dibujan idéntico. */
    const val FACE = 0
    const val EYES = 1
    const val PUPILS = 2
    const val MOUTH = 3
    const val BROWS = 4

    private fun eyes(y: Float, vararg items: GlyphItem) = GPart(EYES, 20f, y, items.toList())
    private fun mouth(y: Float, vararg items: GlyphItem) = GPart(MOUTH, 20f, y, items.toList())

    private fun face(mood: Mood, sw: Float): List<GlyphItem> = listOf(
        GPart(
            FACE, 20f, 20f, when (mood) {
                Mood.HAPPY -> listOf(
                    eyes(17.8f, GStroke("M11 17q3 3 6 0M23 17q3 3 6 0", sw)),
                    mouth(24f, GStroke("M14 24q6 5.5 12 0", sw)),
                )
                Mood.CALM -> listOf(
                    eyes(18f, GStroke("M11 19q3-3.5 6 0M23 19q3-3.5 6 0", sw)),
                    mouth(24.5f, GStroke("M14 24.5q6 5 12 0", sw)),
                )
                Mood.ANGRY -> listOf(
                    GPart(BROWS, 20f, 15f, listOf(GStroke("M10.5 13.5l6.5 3M29.5 13.5l-6.5 3", sw))),
                    eyes(21f, GCircle(14.5f, 21f, 1.8f), GCircle(25.5f, 21f, 1.8f)),
                    mouth(28f, GStroke("M15 29q5-3.5 10 0", sw)),
                )
                Mood.SLEEPY -> listOf(
                    eyes(18.6f, GStroke("M11 18q3 2.5 6 0M23 18q3 2.5 6 0", sw)),
                    mouth(26.5f, GCircle(20f, 26.5f, 2.1f, fill = null, stroke = Color.Unspecified, strokeWidth = sw)),
                )
                Mood.BORED -> listOf(
                    eyes(
                        18f,
                        GCircle(14f, 18f, 3.9f, fill = Color.White, stroke = Color.Unspecified, strokeWidth = 1.8f),
                        GCircle(26f, 18f, 3.9f, fill = Color.White, stroke = Color.Unspecified, strokeWidth = 1.8f),
                        GPart(PUPILS, 20f, 17f, listOf(GCircle(15.1f, 16.9f, 1.7f), GCircle(27.1f, 16.9f, 1.7f))),
                    ),
                    mouth(28f, GStroke("M15.5 28h9", sw)),
                )
                Mood.STRESSED -> listOf(
                    eyes(18f, GStroke("M11 15l5 3-5 3M29 15l-5 3 5 3", sw)),
                    mouth(27f, GOval(20f, 27f, 2.7f, 2.3f)),
                )
            }
        )
    )

    private fun shape(mood: Mood): GlyphItem {
        val c = mood.color
        return when (mood) {
            Mood.HAPPY -> GCircle(20f, 20f, 19f, fill = c)
            Mood.ANGRY -> GRoundRect(2.5f, 2.5f, 35f, 35f, 8f, c, rotation = -8f)
            Mood.SLEEPY -> GFill("M20 1.5c11 0 18.5 8 18.5 18.5S30.5 38.5 19.5 38.5 1.5 31 1.5 20.5 9 1.5 20 1.5z", c)
            Mood.BORED -> GRoundRect(1.5f, 1.5f, 37f, 37f, 15f, c)
            Mood.CALM -> GRoundRect(1.5f, 1.5f, 37f, 37f, 11f, c)
            Mood.STRESSED -> GRoundRect(6f, 6f, 28f, 28f, 7f, c, rotation = 45f)
        }
    }

    private val faces = Mood.entries.associateWith { GlyphSpec(40f, 40f, face(it, STROKE)) }
    private val bigFaces = Mood.entries.associateWith { GlyphSpec(40f, 40f, face(it, BIG_STROKE)) }
    private val icons = Mood.entries.associateWith { GlyphSpec(40f, 40f, listOf(shape(it)) + face(it, STROKE)) }

    fun face(mood: Mood): GlyphSpec = faces.getValue(mood)
    fun bigFace(mood: Mood): GlyphSpec = bigFaces.getValue(mood)
    fun icon(mood: Mood): GlyphSpec = icons.getValue(mood)
}

/** Solo la cara (para celdas del calendario). */
@Composable
fun MoodFace(mood: Mood, modifier: Modifier = Modifier) = Glyph(MoodGlyphs.face(mood), modifier)

/** Figura de color + cara (chips, hojas, avisos). */
@Composable
fun MoodIcon(mood: Mood, modifier: Modifier = Modifier) = Glyph(MoodGlyphs.icon(mood), modifier)

/**
 * Pose de un gesto idle en p ∈ [0, 1]: cuerpo (graphicsLayer, traslaciones en dp) + rasgos de la
 * cara ([GPart], unidades del viewBox 40x40). p = 0 y p = 1 son la pose neutra: entrar y salir
 * del gesto no salta.
 */
data class IdlePose(
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val face: Part = Part.Rest,
    val eyes: Part = Part.Rest,
    val pupils: Part = Part.Rest,
    val mouth: Part = Part.Rest,
    val brows: Part = Part.Rest,
) : GlyphParts {
    override fun part(slot: Int): Part = when (slot) {
        MoodGlyphs.FACE -> face
        MoodGlyphs.EYES -> eyes
        MoodGlyphs.PUPILS -> pupils
        MoodGlyphs.MOUTH -> mouth
        MoodGlyphs.BROWS -> brows
        else -> Part.Rest
    }
}

object MoodIdle {
    private const val PI_F = PI.toFloat()
    private const val TAU = 2f * PI_F

    /** Campana 0 → 1 → 0 dentro de [a, b]; 0 fuera. */
    private fun win(p: Float, a: Float, b: Float): Float =
        if (p <= a || p >= b) 0f else sin(PI_F * (p - a) / (b - a))

    /** 0 → 1 suave dentro de [a, b]. */
    private fun ramp(p: Float, a: Float, b: Float): Float {
        val t = ((p - a) / (b - a)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Oscilación de [cycles] vueltas dentro de [a, b], con envolvente [win]. */
    private fun wobble(p: Float, a: Float, b: Float, cycles: Float): Float =
        sin(TAU * cycles * ((p - a) / (b - a))) * win(p, a, b)

    fun durationMs(mood: Mood): Int = when (mood) {
        Mood.HAPPY -> 1100
        Mood.CALM -> 2200
        Mood.ANGRY -> 1000
        Mood.SLEEPY -> 2400
        Mood.BORED -> 2200
        Mood.STRESSED -> 1100
    }

    /** HAPPY y SLEEPY se apoyan en la base; el resto gira/escala sobre el centro. */
    fun anchoredAtBase(mood: Mood): Boolean = mood == Mood.HAPPY || mood == Mood.SLEEPY

    fun pose(mood: Mood, p: Float): IdlePose = when (mood) {
        // Se agacha, salta con una sonrisa enorme y ojos apretados de gusto, aterriza y se menea.
        Mood.HAPPY -> {
            val crouch = win(p, 0f, 0.22f)
            val hop = win(p, 0.18f, 0.7f)
            val land = win(p, 0.66f, 0.86f)
            val grin = win(p, 0.12f, 0.95f)
            IdlePose(
                scaleX = 1f + 0.1f * crouch - 0.06f * hop + 0.1f * land,
                scaleY = 1f - 0.12f * crouch + 0.1f * hop - 0.12f * land,
                rotation = 7f * wobble(p, 0.72f, 1f, 1.5f),
                dy = -6f * hop,
                face = Part(dy = -1.5f * hop),
                eyes = Part(sy = 1f + 0.5f * grin, dy = -0.6f * grin),
                mouth = Part(sx = 1f + 0.22f * grin, sy = 1f + 0.6f * grin),
            )
        }
        // Inhala hondo (crece, cierra los ojos, sonríe), exhala con un vaivén lento.
        Mood.CALM -> {
            val inhale = win(p, 0f, 1f)
            val sway = wobble(p, 0.35f, 1f, 1f)
            IdlePose(
                scaleX = 1f + 0.05f * inhale,
                scaleY = 1f + 0.06f * inhale,
                rotation = 5f * sway,
                dy = -1.5f * inhale,
                face = Part(dy = -1f * inhale, dx = 1f * sway),
                eyes = Part(sy = 1f - 0.45f * inhale, sx = 1f + 0.05f * inhale),
                mouth = Part(sx = 1f + 0.12f * inhale, sy = 1f + 0.35f * inhale),
            )
        }
        // Se hincha: cejas se juntan y bajan, ojos entrecerrados, boca apretada; luego tiembla de rabia.
        Mood.ANGRY -> {
            val fume = (1.8f * sin(PI_F * p)).coerceAtMost(1f)
            val shake = wobble(p, 0.3f, 0.9f, 5f)
            IdlePose(
                scaleX = 1f + 0.08f * fume,
                scaleY = 1f + 0.05f * fume,
                rotation = 6f * shake,
                dx = 1.2f * wobble(p, 0.3f, 0.9f, 7f),
                face = Part(dy = 0.8f * fume),
                brows = Part(dy = 1.6f * fume, sx = 1f - 0.14f * fume),
                eyes = Part(sy = 1f - 0.55f * fume),
                mouth = Part(sx = 1f - 0.2f * fume, sy = 1f + 1f * fume),
            )
        }
        // Bostezo (boca muy abierta, ojos apretados, se estira) y luego cabecea hundiéndose.
        Mood.SLEEPY -> {
            val yawn = win(p, 0f, 0.55f)
            val nod = win(p, 0.5f, 1f)
            IdlePose(
                scaleX = 1f - 0.03f * yawn + 0.03f * nod,
                scaleY = 1f + 0.07f * yawn - 0.05f * nod,
                rotation = -4f * yawn + 9f * nod,
                dy = 2f * nod,
                face = Part(dy = -1f * yawn + 1.8f * nod),
                eyes = Part(sy = 1f - 0.6f * yawn, dy = 0.8f * nod),
                mouth = Part(sx = 1f + 0.3f * yawn - 0.2f * nod, sy = 1f + 0.8f * yawn - 0.2f * nod),
            )
        }
        // Pone los ojos en blanco (las pupilas dan la vuelta), baja los párpados y suelta un "meh" de lado.
        Mood.BORED -> {
            // Pupilas giran alrededor del centro del ojo, partiendo de su posición (arriba-derecha).
            val r = 1.55f
            val a0 = -PI_F / 4f
            val a = a0 - TAU * ramp(p, 0.05f, 0.55f)
            val lids = win(p, 0.5f, 1f)
            val meh = win(p, 0.55f, 1f)
            IdlePose(
                scaleY = 1f - 0.04f * meh,
                rotation = -4f * meh,
                dy = 1f * meh,
                pupils = Part(dx = r * (cos(a) - cos(a0)), dy = r * (sin(a) - sin(a0))),
                eyes = Part(sy = 1f - 0.4f * lids),
                mouth = Part(dx = 1.8f * meh, sx = 1f - 0.25f * meh, rotation = -10f * meh),
            )
        }
        // Se encoge tenso, tiembla rápido, aprieta los ojos y jadea (la boca late).
        Mood.STRESSED -> {
            val tense = (1.6f * sin(PI_F * p)).coerceAtMost(1f)
            val gasp = abs(sin(TAU * 3f * p)) * tense
            IdlePose(
                scaleX = 1f - 0.06f * tense,
                scaleY = 1f - 0.06f * tense,
                rotation = 3f * wobble(p, 0f, 1f, 9f),
                dx = 1.3f * wobble(p, 0f, 1f, 12f),
                face = Part(dx = 0.5f * wobble(p, 0f, 1f, 10f), dy = -0.6f * tense),
                eyes = Part(sx = 1f - 0.18f * tense, sy = 1f - 0.2f * tense),
                mouth = Part(sx = 1f + 0.15f * gasp, sy = 1f + 0.45f * gasp),
            )
        }
    }
}

/**
 * [MoodIcon] que, mientras está [active] (el ánimo elegido), hace su gesto al activarse y luego
 * lo repite tras pausas aleatorias. Inactivo queda quieto (termina el gesto en curso, sin saltos).
 * En la pausa no se piden frames; el gesto se lee en fase de dibujo (sin recomponer).
 */
@Composable
fun AnimatedMoodIcon(mood: Mood, active: Boolean, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val p = remember { Animatable(0f) }
    LaunchedEffect(mood, reduce, active) {
        val ms = MoodIdle.durationMs(mood)
        if (reduce || !active) {
            if (p.value > 0f && !reduce) p.animateTo(1f, tween(((1f - p.value) * ms).toInt(), easing = LinearEasing))
            p.snapTo(0f)
            return@LaunchedEffect
        }
        delay(350L) // deja terminar el "pop" de selección antes del gesto
        while (true) {
            p.animateTo(1f, tween(((1f - p.value) * ms).toInt(), easing = LinearEasing))
            p.snapTo(0f)
            delay(Random.nextLong(2500L, 6000L))
        }
    }
    val origin = if (MoodIdle.anchoredAtBase(mood)) TransformOrigin(0.5f, 1f) else TransformOrigin.Center
    Glyph(
        MoodGlyphs.icon(mood),
        // Después del modifier del llamador: sus drawBehind (p. ej. el estallido del chip) no se mueven.
        modifier.graphicsLayer {
            val pose = MoodIdle.pose(mood, p.value)
            transformOrigin = origin
            scaleX = pose.scaleX
            scaleY = pose.scaleY
            rotationZ = pose.rotation
            translationX = pose.dx.dp.toPx()
            translationY = pose.dy.dp.toPx()
        },
        parts = { MoodIdle.pose(mood, p.value) },
    )
}

/**
 * Figuras grandes del onboarding (viewBox 100x100; el rayo es 100x146).
 * Los rasgos van en [GLook]: la cara se asoma hacia la inclinación y las pupilas ruedan un poco más.
 */
object OnboardingGlyphs {
    private const val SW = 4.6f
    private const val FACE = 4f
    private val Ink = Palette.Ink

    val Blue = GlyphSpec(
        100f, 100f, listOf(
            GCircle(50f, 50f, 50f, fill = Palette.MoodSleepy),
            GLook(FACE, listOf(GStroke("M28 44q7 6 14 0M58 44q7 6 14 0M44 62q6 4 12 0", SW, Ink))),
        )
    )
    val Pink = GlyphSpec(
        100f, 100f, listOf(
            GCircle(50f, 50f, 50f, fill = Palette.MoodBored),
            GLook(
                FACE, listOf(
                    GLook(
                        0f, listOf(
                            GCircle(36f, 42f, 10f, fill = Color.White, stroke = Ink, strokeWidth = 3.5f),
                            GCircle(64f, 42f, 10f, fill = Color.White, stroke = Ink, strokeWidth = 3.5f),
                            GLook(1.2f, listOf(GCircle(39f, 38f, 4.5f, fill = Ink), GCircle(67f, 38f, 4.5f, fill = Ink))),
                        ),
                        blinkPivotY = 42f,
                    ),
                    GStroke("M38 71q12-9 24 0", SW, Ink),
                )
            ),
        )
    )
    val Triangle = GlyphSpec(
        100f, 100f, listOf(
            GFill("M50 10L92 86H8z", Color(0xFFFF8080)),
            GStroke("M50 10L92 86H8z", 14f, Color(0xFFFF8080)),
            GLook(FACE, listOf(GStroke("M33 52l11 6-11 6M67 52l-11 6 11 6M42 76q4-4 8 0t8 0", SW, Ink))),
        )
    )
    val Orange = GlyphSpec(
        100f, 100f, listOf(
            GRoundRect(4f, 4f, 92f, 92f, 20f, Palette.MoodAngry),
            GLook(
                FACE, listOf(
                    GStroke("M26 36l16 7M74 36l-16 7M37 72q13-10 26 0", SW, Ink),
                    GLook(0f, listOf(GCircle(36f, 54f, 5f, fill = Ink), GCircle(64f, 54f, 5f, fill = Ink)), blinkPivotY = 54f),
                )
            ),
        )
    )
    val Green = GlyphSpec(
        100f, 100f, listOf(
            GCircle(50f, 50f, 50f, fill = Palette.MoodHappy),
            GLook(FACE, listOf(GStroke("M27 45q7.5 7 15 0M58 45q7.5 7 15 0M33 62q17 15 34 0", SW, Ink))),
        )
    )
    val Purple = GlyphSpec(
        100f, 100f, listOf(
            GCircle(50f, 50f, 50f, fill = Palette.MoodStressed),
            GStroke(
                "M50 50A5 5 0 0 1 60 50A10 10 0 0 1 40 50A15 15 0 0 1 70 50A20 20 0 0 1 30 50" +
                    "A25 25 0 0 1 80 50A30 30 0 0 1 20 50A35 35 0 0 1 90 50A40 40 0 0 1 10 50",
                6f, Color(0xFFC18CF8),
            ),
            GGroup(
                -24f, 50f, 50f, listOf(
                    GLook(
                        FACE, listOf(
                            GLook(
                                0f, listOf(
                                    GCircle(37f, 42f, 9f, fill = Color.White, stroke = Ink, strokeWidth = 3.5f),
                                    GCircle(61f, 42f, 9f, fill = Color.White, stroke = Ink, strokeWidth = 3.5f),
                                    GLook(2f, listOf(GCircle(39f, 44f, 4f, fill = Ink), GCircle(63f, 44f, 4f, fill = Ink))),
                                ),
                                blinkPivotY = 42f,
                            ),
                            GOval(50f, 65f, 6f, 7.5f, Ink),
                        )
                    ),
                )
            ),
        )
    )
    val Bolt = GlyphSpec(
        100f, 146f, listOf(
            GFill("M60 8L14 86h34l-12 52 52-80H54l14-50z", Palette.MoodCalm),
            GStroke("M60 8L14 86h34l-12 52 52-80H54l14-50z", 12f, Palette.MoodCalm),
            GLook(3f, listOf(GStroke("M34 66q5-6.5 10 0M56 62q5-6.5 10 0M38 78q12 10 26-3", SW, Ink))),
        )
    )

    // Versiones mareadas (tras un choque fuerte): mismo cuerpo, cara de espirales.
    val BlueDizzy = dizzy(Blue, 50f, 50f)
    val PinkDizzy = dizzy(Pink, 50f, 50f)
    val TriangleDizzy = dizzy(Triangle, 50f, 62f, 0.75f)
    val OrangeDizzy = dizzy(Orange, 50f, 52f)
    val GreenDizzy = dizzy(Green, 50f, 50f)
    val PurpleDizzy = dizzy(Purple, 50f, 50f)
    val BoltDizzy = dizzy(Bolt, 50f, 68f, 0.8f)

    /** Quita la cara de [spec] (los GLook/GGroup) y pone ojos en espiral + boca ondulada en ([cx], [cy]). */
    private fun dizzy(spec: GlyphSpec, cx: Float, cy: Float, s: Float = 1f): GlyphSpec {
        val a = 3.2f * s
        // Tres medias vueltas de radio a, 2a, 3a; el trazo abarca [ex - 2a, ex + 4a], así que se corre -a.
        fun spiral(eyeX: Float, ey: Float): String {
            val ex = eyeX - a
            return "M$ex ${ey}a$a $a 0 1 1 ${2 * a} 0a${2 * a} ${2 * a} 0 1 1 ${-4 * a} 0a${3 * a} ${3 * a} 0 1 1 ${6 * a} 0"
        }
        val eyes = spiral(cx - 15f * s, cy - 6f * s) + spiral(cx + 15f * s, cy - 6f * s)
        val m = 6f * s
        val mouth = "M${cx - 2 * m} ${cy + 16f * s}q${m / 2} ${-m * 0.7f} $m 0t$m 0t$m 0t$m 0"
        return GlyphSpec(
            spec.width, spec.height,
            spec.items.filter { it !is GLook && it !is GGroup } +
                GLook(FACE, listOf(GStroke(eyes, 3.2f * s, Ink), GStroke(mouth, SW * s, Ink))),
        )
    }
}

/** Avatar ilustrado genérico (viewBox 40x40). */
private val AvatarSpec = GlyphSpec(
    40f, 40f, listOf(
        GFill("M0 0h40v40H0z", Color(0xFFCFE0FF)),
        GFill("M7 41c1-8.5 6-12.5 13-12.5S32 32.5 33 41z", Color(0xFF4F7BE8)),
        GFill("M17 23h6v6.5a3 3 0 0 1-6 0z", Color(0xFFE4AE88)),
        GOval(20f, 18.5f, 7f, 8f, Color(0xFFF2C39E)),
        GFill("M12.6 18c-.8-6.4 3.4-10 7.8-10 5.2 0 8.4 3.7 7.2 9.4-1.2-3-3.6-4.6-7.6-4.6-3.6 0-6.2 1.8-7.4 5.2z", Color(0xFF3A2A22)),
        GCircle(17.3f, 19.5f, 0.9f, fill = Color(0xFF3A2A22)),
        GCircle(22.7f, 19.5f, 0.9f, fill = Color(0xFF3A2A22)),
        GStroke("M18 23q2 1.4 4 0", 0.9f, Color(0xFFB9785A)),
    )
)

@Composable
fun Avatar(modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape)) {
        Glyph(AvatarSpec, Modifier.fillMaxSize())
    }
}
