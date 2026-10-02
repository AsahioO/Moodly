package com.asahioo.moodly.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import java.util.Locale

/**
 * Caras (viewBox 40x40) y siluetas de cada ánimo: blobs orgánicos con rasgos de puntos y líneas.
 * ponytail: icon() está duplicado en res/drawable/ic_mood_*.xml para la notificación y el widget
 * (RemoteViews no puede usar Compose); si cambias una cara, cambia ambos lados.
 */
object MoodGlyphs {

    private const val STROKE = 2.4f
    private val Cheek = Color(0xFFF0846A)

    /** Rasgos animables por separado (slots de [GPart]); en reposo dibujan idéntico. */
    const val FACE = 0
    const val EYES = 1
    const val PUPILS = 2
    const val MOUTH = 3
    const val BROWS = 4

    /** Rasgos de una cara en reposo. [eyesY] y [mouthY] son los pivotes de sus gestos (y del parpadeo). */
    internal class Features(
        val eyesY: Float,
        val eyes: List<GlyphItem>,
        val mouthY: Float,
        val mouth: List<GlyphItem>,
        val brows: List<GlyphItem> = emptyList(),
        /** Detalles fijos (mejillas) que acompañan a la cara sin animarse aparte. */
        val extra: List<GlyphItem> = emptyList(),
    )

    private fun ring(cx: Float, cy: Float, r: Float) =
        GCircle(cx, cy, r, fill = Color.White, stroke = Color.Unspecified, strokeWidth = 1.4f)

    internal fun features(mood: Mood, sw: Float): Features = when (mood) {
        Mood.HAPPY -> Features(
            eyesY = 18.5f,
            eyes = listOf(GOval(15f, 18.5f, 1.6f, 2.3f), GOval(25f, 18.5f, 1.6f, 2.3f)),
            mouthY = 26f,
            mouth = listOf(GFill("M14.5 24h11q-1.2 6.5-5.5 6.5t-5.5-6.5z")),
            extra = listOf(GOval(12.5f, 22.5f, 2.3f, 1.5f, Cheek), GOval(27.5f, 22.5f, 2.3f, 1.5f, Cheek)),
        )
        Mood.CALM -> Features(
            eyesY = 20f,
            eyes = listOf(GStroke("M11.5 20h5M23.5 20h5", sw)),
            mouthY = 26f,
            mouth = listOf(GStroke("M18 25.5q3 2.6 6 0", sw)),
        )
        Mood.ANGRY -> Features(
            eyesY = 24f,
            eyes = listOf(GOval(15f, 24f, 1.5f, 2f), GOval(25f, 24f, 1.5f, 2f)),
            mouthY = 30f,
            mouth = listOf(GStroke("M14 31l3-2 3 2 3-2 3 2", sw)),
            brows = listOf(GStroke("M11 17l9 4 9-4", sw + 0.2f)),
        )
        Mood.SLEEPY -> Features(
            eyesY = 23.5f,
            eyes = listOf(GFill("M12 23h6a3 3 0 0 1-6 0zM22 23h6a3 3 0 0 1-6 0z")),
            mouthY = 30f,
            mouth = listOf(GOval(20f, 30f, 1.5f, 2f)),
        )
        // Ojos entornados: el párpado es del color del cuerpo y tapa la mitad de arriba (y las pupilas al rodar).
        Mood.BORED -> Features(
            eyesY = 18f,
            eyes = listOf(
                ring(14f, 18f, 3.8f),
                ring(26f, 18f, 3.8f),
                GPart(PUPILS, 20f, 19f, listOf(GCircle(15.3f, 19.3f, 1.6f), GCircle(27.3f, 19.3f, 1.6f))),
                GFill("M10.2 18a3.8 3.8 0 0 1 7.6 0zM22.2 18a3.8 3.8 0 0 1 7.6 0z", Palette.MoodBored),
                GStroke("M10.2 18h7.6M22.2 18h7.6", 1.4f),
            ),
            mouthY = 28f,
            mouth = listOf(GStroke("M16 28.5l7.5-1", sw)),
        )
        Mood.STRESSED -> Features(
            eyesY = 18f,
            eyes = listOf(ring(14f, 18f, 3.6f), ring(26f, 18f, 3.6f), GCircle(14f, 18f, 1.3f), GCircle(26f, 18f, 1.3f)),
            mouthY = 27.5f,
            mouth = listOf(GStroke("M12.5 27.5q1.9-2.6 3.8 0t3.8 0t3.8 0t3.8 0", sw)),
        )
    }

    private fun face(mood: Mood, sw: Float): List<GlyphItem> {
        val f = features(mood, sw)
        val parts = listOfNotNull(
            f.brows.takeIf { it.isNotEmpty() }?.let { GPart(BROWS, 20f, 19f, it) },
            GPart(EYES, 20f, f.eyesY, f.eyes),
            GPart(MOUTH, 20f, f.mouthY, f.mouth),
        )
        return listOf(GPart(FACE, 20f, 20f, f.extra + parts))
    }

    /** Silueta de cada ánimo (path en el viewBox 40x40). */
    internal fun silhouette(mood: Mood): String = when (mood) {
        // Mochi: domo de base ancha.
        Mood.HAPPY -> "M20 4C30.5 4 37.5 12 37.5 23C37.5 32.5 31 36.5 20 36.5S2.5 32.5 2.5 23C2.5 12 9.5 4 20 4z"
        // Gota con la punta ladeada.
        Mood.ANGRY -> "M22.5 1.5C23.5 9.5 35.5 12 35.5 24S28.5 38.5 20 38.5 4.5 35 4.5 24 17 9 22.5 1.5z"
        // Nube de base plana.
        Mood.SLEEPY -> "M9 35.5C3 35.5 1.5 27.5 7 25C5.5 17 12 12.5 17.5 15.5C19 6 31 6 32.5 15.5" +
            "C38.5 14.5 40 24 34 26C38.5 30 36 35.5 31 35.5z"
        // Huevo.
        Mood.BORED -> "M20 1.5C28.5 1.5 34.5 12 34.5 22S28.5 38.5 20 38.5 5.5 32 5.5 22 11.5 1.5 20 1.5z"
        // Guijarro ancho, apenas irregular.
        Mood.CALM -> lobed(20f, 21.5f, 18.5f, 16f, lobes = 3, amp = 0.04f, phase = 0.6f, points = 18)
        // Contorno erizado de 11 bultos.
        Mood.STRESSED -> lobed(20f, 20f, 17.5f, 17.5f, lobes = 11, amp = 0.055f, phase = 0.3f, points = 44)
    }

    /**
     * Contorno cerrado con radio r(t) = 1 + amp·cos(lobes·t + phase), suavizado con Catmull-Rom.
     * Los drawables ic_mood_calm/stressed.xml llevan este mismo path ya calculado.
     */
    private fun lobed(
        cx: Float, cy: Float, rx: Float, ry: Float,
        lobes: Int, amp: Float, phase: Float, points: Int,
    ): String {
        val pts = List(points) { i ->
            val t = 2.0 * PI * i / points
            val r = 1.0 + amp * cos(lobes * t + phase)
            Offset((cx + rx * r * cos(t)).toFloat(), (cy + ry * r * sin(t)).toFloat())
        }
        fun f(v: Float) = String.format(Locale.ROOT, "%.1f", v)
        return buildString {
            append("M${f(pts[0].x)} ${f(pts[0].y)}")
            for (i in pts.indices) {
                val p0 = pts[(i - 1 + points) % points]
                val p1 = pts[i]
                val p2 = pts[(i + 1) % points]
                val p3 = pts[(i + 2) % points]
                val c1 = p1 + (p2 - p0) / 6f
                val c2 = p2 - (p3 - p1) / 6f
                append("C${f(c1.x)} ${f(c1.y)} ${f(c2.x)} ${f(c2.y)} ${f(p2.x)} ${f(p2.y)}")
            }
            append('z')
        }
    }

    private fun shape(mood: Mood): GlyphItem = GFill(silhouette(mood), mood.color)

    private val icons = Mood.entries.associateWith { GlyphSpec(40f, 40f, listOf(shape(it)) + face(it, STROKE)) }

    fun icon(mood: Mood): GlyphSpec = icons.getValue(mood)
}

/** Silueta de color + cara (selector, calendario, hojas, avisos). */
@Composable
fun MoodIcon(mood: Mood, modifier: Modifier = Modifier) = Glyph(MoodGlyphs.icon(mood), modifier, tint = Palette.FaceInk)

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

    /** Gestos por ánimo: 0 es el gesto insignia (el primero al elegirlo); los demás rotan al azar. */
    const val VARIANTS = 3

    /** Rebotes: |sin| con [n] golpes dentro de [a, b]; 0 en los bordes y fuera. */
    private fun beats(p: Float, a: Float, b: Float, n: Int): Float =
        if (p <= a || p >= b) 0f else abs(sin(PI_F * n * (p - a) / (b - a)))

    fun durationMs(mood: Mood, variant: Int = 0): Int = when (mood) {
        Mood.HAPPY -> intArrayOf(1100, 1300, 1600)
        Mood.CALM -> intArrayOf(2200, 2600, 2400)
        Mood.ANGRY -> intArrayOf(1000, 1300, 1500)
        Mood.SLEEPY -> intArrayOf(2400, 2800, 2600)
        Mood.BORED -> intArrayOf(2200, 2200, 2000)
        Mood.STRESSED -> intArrayOf(1100, 1300, 1500)
    }[variant.coerceIn(0, VARIANTS - 1)]

    /** HAPPY y SLEEPY se apoyan en la base; el resto gira/escala sobre el centro. */
    fun anchoredAtBase(mood: Mood): Boolean = mood == Mood.HAPPY || mood == Mood.SLEEPY

    fun pose(mood: Mood, p: Float, variant: Int = 0): IdlePose = when (mood) {
        Mood.HAPPY -> when (variant) {
            // Carcajada: rebota tres veces, cada "ja" abre la boca; ojos apretados y echado hacia atrás.
            1 -> {
                val laugh = win(p, 0f, 1f)
                val ha = beats(p, 0.1f, 0.85f, 3)
                IdlePose(
                    scaleX = 1f - 0.05f * ha,
                    scaleY = 1f + 0.08f * ha,
                    rotation = -7f * laugh,
                    dy = -4f * ha,
                    face = Part(dy = -1f * laugh),
                    eyes = Part(sy = 1f - 0.75f * laugh, dy = -0.4f * laugh),
                    mouth = Part(sx = 1f + 0.15f * laugh, sy = 1f + 0.9f * ha + 0.2f * laugh),
                )
            }
            // Baile: se mece a los lados marcando el ritmo, con la sonrisa bien abierta.
            2 -> {
                val dance = win(p, 0f, 1f)
                val sway = wobble(p, 0f, 1f, 2f)
                val bob = beats(p, 0f, 1f, 4)
                IdlePose(
                    scaleX = 1f + 0.05f * bob,
                    scaleY = 1f - 0.07f * bob,
                    rotation = 11f * sway,
                    dx = 3f * sway,
                    face = Part(dx = 1.8f * sway, dy = -0.8f * dance),
                    eyes = Part(sy = 1f + 0.3f * dance),
                    mouth = Part(sx = 1f + 0.3f * dance, sy = 1f + 0.5f * dance),
                )
            }
            // Se agacha, salta con una sonrisa enorme y ojos apretados de gusto, aterriza y se menea.
            else -> {
                val crouch = win(p, 0f, 0.22f)
                val hop = win(p, 0.18f, 0.7f)
                val land = win(p, 0.66f, 0.86f)
                val grin = win(p, 0.12f, 0.95f)
                IdlePose(
                    scaleX = 1f + 0.1f * crouch - 0.06f * hop + 0.1f * land,
                    scaleY = 1f - 0.12f * crouch + 0.1f * hop - 0.12f * land,
                    rotation = 9f * wobble(p, 0.72f, 1f, 1.5f),
                    dy = -8f * hop,
                    face = Part(dy = -1.5f * hop),
                    eyes = Part(sy = 1f + 0.5f * grin, dy = -0.6f * grin),
                    mouth = Part(sx = 1f + 0.25f * grin, sy = 1f + 0.8f * grin),
                )
            }
        }
        Mood.CALM -> when (variant) {
            // Flota en paz: sube despacio, se ladea y la sonrisa se ensancha.
            1 -> {
                val float = win(p, 0f, 1f)
                IdlePose(
                    scaleX = 1f + 0.03f * float,
                    scaleY = 1f + 0.03f * float,
                    rotation = -5f * wobble(p, 0f, 1f, 1.5f),
                    dy = -4f * float,
                    face = Part(dy = -0.8f * float),
                    eyes = Part(sx = 1f + 0.12f * float, dy = 0.4f * float),
                    mouth = Part(sx = 1f + 0.25f * float, sy = 1f + 0.5f * float),
                )
            }
            // Tararea: se mece y la boca se redondea a cada nota.
            2 -> {
                val hum = win(p, 0f, 1f)
                val rock = wobble(p, 0f, 1f, 2f)
                val note = beats(p, 0.1f, 0.9f, 4)
                IdlePose(
                    scaleX = 1f + 0.03f * note,
                    scaleY = 1f - 0.03f * note,
                    rotation = 8f * rock,
                    dx = 1.5f * rock,
                    face = Part(dx = 1.2f * rock, dy = -0.5f * hum),
                    eyes = Part(sx = 1f + 0.1f * hum, rotation = 4f * rock),
                    mouth = Part(sx = 1f - 0.25f * note, sy = 1f + 0.9f * note),
                )
            }
            // Inhala hondo (crece, cierra los ojos, sonríe), exhala con un vaivén lento.
            else -> {
                val inhale = win(p, 0f, 1f)
                val sway = wobble(p, 0.35f, 1f, 1f)
                IdlePose(
                    scaleX = 1f + 0.06f * inhale,
                    scaleY = 1f + 0.08f * inhale,
                    rotation = 7f * sway,
                    dy = -2f * inhale,
                    face = Part(dy = -1f * inhale, dx = 1f * sway),
                    eyes = Part(sy = 1f - 0.45f * inhale, sx = 1f + 0.05f * inhale),
                    mouth = Part(sx = 1f + 0.15f * inhale, sy = 1f + 0.45f * inhale),
                )
            }
        }
        Mood.ANGRY -> when (variant) {
            // Grita: se carga temblando (cejas abajo, boca apretada) y estalla estirándose con la boca abierta.
            1 -> {
                val charge = ramp(p, 0f, 0.4f) * (1f - ramp(p, 0.4f, 0.5f))
                val shout = win(p, 0.4f, 0.95f)
                val tremble = wobble(p, 0f, 0.42f, 6f)
                IdlePose(
                    scaleX = 1f + 0.1f * charge - 0.08f * shout,
                    scaleY = 1f - 0.1f * charge + 0.16f * shout,
                    rotation = 3f * tremble,
                    dy = 1.5f * charge - 4f * shout,
                    brows = Part(dy = 2f * charge - 1.5f * shout, sx = 1f - 0.18f * charge),
                    eyes = Part(sy = 1f - 0.6f * charge + 0.3f * shout),
                    mouth = Part(sx = 1f - 0.25f * charge + 0.1f * shout, sy = 1f + 0.5f * charge + 2.2f * shout),
                )
            }
            // "¡Hmph!": voltea la cara con mirada de reojo, resopla y regresa de golpe.
            2 -> {
                val turn = win(p, 0f, 0.8f)
                val huff = win(p, 0.3f, 0.55f)
                val snap = wobble(p, 0.75f, 1f, 1f)
                IdlePose(
                    scaleX = 1f + 0.06f * huff,
                    scaleY = 1f - 0.05f * huff,
                    rotation = -8f * turn + 5f * snap,
                    dx = -2f * turn,
                    face = Part(dx = -3f * turn, dy = 0.5f * turn),
                    brows = Part(rotation = -8f * turn, dy = 1f * turn),
                    eyes = Part(sy = 1f - 0.5f * turn),
                    mouth = Part(dx = -1f * turn, sx = 1f - 0.3f * huff, sy = 1f + 1.2f * huff),
                )
            }
            // Se hincha: cejas se juntan y bajan, ojos entrecerrados, boca apretada; luego tiembla de rabia.
            else -> {
                val fume = (1.8f * sin(PI_F * p)).coerceAtMost(1f)
                val shake = wobble(p, 0.3f, 0.9f, 5f)
                IdlePose(
                    scaleX = 1f + 0.1f * fume,
                    scaleY = 1f + 0.06f * fume,
                    rotation = 8f * shake,
                    dx = 1.5f * wobble(p, 0.3f, 0.9f, 7f),
                    face = Part(dy = 0.8f * fume),
                    brows = Part(dy = 1.8f * fume, sx = 1f - 0.16f * fume),
                    eyes = Part(sy = 1f - 0.55f * fume),
                    mouth = Part(sx = 1f - 0.2f * fume, sy = 1f + 1.2f * fume),
                )
            }
        }
        Mood.SLEEPY -> when (variant) {
            // Se queda dormido hundiéndose (respira por la boca) y despierta de un salto con los ojos abiertos.
            1 -> {
                val doze = ramp(p, 0f, 0.45f) * (1f - ramp(p, 0.6f, 0.66f))
                val breathe = beats(p, 0.1f, 0.6f, 2)
                val startle = win(p, 0.6f, 0.85f)
                IdlePose(
                    scaleX = 1f + 0.07f * doze - 0.06f * startle,
                    scaleY = 1f - 0.1f * doze + 0.03f * breathe + 0.14f * startle,
                    rotation = 10f * doze - 4f * wobble(p, 0.62f, 1f, 1.5f),
                    dy = 2f * doze - 5f * startle,
                    face = Part(dy = 1.5f * doze - 1f * startle),
                    eyes = Part(sy = 1f - 0.85f * doze + 0.7f * startle),
                    mouth = Part(sx = 1f - 0.2f * doze, sy = 1f - 0.4f * doze + 0.6f * breathe + 1f * startle),
                )
            }
            // Flota como nube a la deriva, ronca suave con la boca en "o".
            2 -> {
                val drift = win(p, 0f, 1f)
                val sway = wobble(p, 0f, 1f, 1.5f)
                val snore = beats(p, 0f, 1f, 3)
                IdlePose(
                    scaleX = 1f + 0.04f * snore,
                    scaleY = 1f + 0.05f * drift - 0.03f * snore,
                    rotation = 6f * sway,
                    dx = 2.5f * sway,
                    dy = -5f * drift,
                    face = Part(dx = 1f * sway, dy = 0.5f * drift),
                    eyes = Part(sy = 1f - 0.5f * drift),
                    mouth = Part(sx = 1f - 0.3f * snore, sy = 1f + 0.7f * snore),
                )
            }
            // Bostezo (boca muy abierta, ojos apretados, se estira) y luego cabecea hundiéndose.
            else -> {
                val yawn = win(p, 0f, 0.55f)
                val nod = win(p, 0.5f, 1f)
                IdlePose(
                    scaleX = 1f - 0.04f * yawn + 0.03f * nod,
                    scaleY = 1f + 0.09f * yawn - 0.05f * nod,
                    rotation = -5f * yawn + 11f * nod,
                    dy = 2f * nod,
                    face = Part(dy = -1f * yawn + 1.8f * nod),
                    eyes = Part(sy = 1f - 0.6f * yawn, dy = 0.8f * nod),
                    mouth = Part(sx = 1f + 0.3f * yawn - 0.2f * nod, sy = 1f + 1f * yawn - 0.2f * nod),
                )
            }
        }
        Mood.BORED -> when (variant) {
            // Suspiro: toma aire, se desinfla aplastándose y los párpados caen.
            1 -> {
                val inhale = win(p, 0f, 0.4f)
                val deflate = win(p, 0.3f, 1f)
                IdlePose(
                    scaleX = 1f - 0.03f * inhale + 0.07f * deflate,
                    scaleY = 1f + 0.08f * inhale - 0.1f * deflate,
                    rotation = 3f * wobble(p, 0.35f, 1f, 1f),
                    dy = -1.5f * inhale + 2f * deflate,
                    face = Part(dy = -0.8f * inhale + 1.2f * deflate),
                    pupils = Part(dy = 0.4f * deflate),
                    eyes = Part(sy = 1f - 0.45f * deflate),
                    mouth = Part(dy = 0.8f * deflate, sx = 1f + 0.3f * deflate, rotation = 10f * inhale - 8f * deflate),
                )
            }
            // Mira a un lado y al otro buscando algo que hacer; la boca se tuerce hacia donde mira.
            2 -> {
                val left = win(p, 0.05f, 0.45f)
                val right = win(p, 0.5f, 0.9f)
                val look = right - left
                IdlePose(
                    rotation = 4f * look,
                    dx = 1.5f * look,
                    face = Part(dx = 1.2f * look),
                    // Las pupilas parten abajo-derecha del ojo: hay más recorrido a la izquierda.
                    pupils = Part(dx = -2.4f * left + 0.8f * right),
                    eyes = Part(sy = 1f - 0.15f * (left + right)),
                    mouth = Part(dx = 1.2f * look, rotation = -12f * look),
                )
            }
            // Pone los ojos en blanco (las pupilas dan la vuelta), baja los párpados y suelta un "meh" de lado.
            else -> {
                // Pupilas giran alrededor del centro del ojo, partiendo de su posición (abajo-derecha).
                val r = 1.55f
                val a0 = PI_F / 4f
                val a = a0 - TAU * ramp(p, 0.05f, 0.55f)
                val lids = win(p, 0.5f, 1f)
                val meh = win(p, 0.55f, 1f)
                IdlePose(
                    scaleY = 1f - 0.05f * meh,
                    rotation = -6f * meh,
                    dy = 1f * meh,
                    pupils = Part(dx = r * (cos(a) - cos(a0)), dy = r * (sin(a) - sin(a0))),
                    eyes = Part(sy = 1f - 0.4f * lids),
                    mouth = Part(dx = 1.8f * meh, sx = 1f - 0.25f * meh, rotation = -12f * meh),
                )
            }
        }
        Mood.STRESSED -> when (variant) {
            // Sobresalto: brinca con ojos enormes y la boca abierta, y cae tiritando.
            1 -> {
                val jump = win(p, 0f, 0.35f)
                val after = win(p, 0.3f, 1f)
                val shiver = wobble(p, 0.3f, 1f, 8f)
                IdlePose(
                    scaleX = 1f - 0.08f * jump + 0.03f * after,
                    scaleY = 1f + 0.15f * jump - 0.05f * after,
                    rotation = 4f * shiver,
                    dx = 1f * shiver,
                    dy = -6f * jump,
                    face = Part(dy = -1f * jump),
                    eyes = Part(sx = 1f + 0.35f * jump + 0.1f * after, sy = 1f + 0.4f * jump + 0.1f * after),
                    mouth = Part(sx = 1f - 0.2f * jump, sy = 1f + 1.6f * jump + 0.3f * after),
                )
            }
            // Nervioso: se encoge y mira a los lados mientras le castañetean los dientes.
            2 -> {
                val shrink = (1.5f * win(p, 0f, 1f)).coerceAtMost(1f)
                val glance = wobble(p, 0.1f, 0.9f, 2f)
                val chatter = wobble(p, 0f, 1f, 14f)
                IdlePose(
                    scaleX = 1f - 0.1f * shrink,
                    scaleY = 1f - 0.12f * shrink,
                    rotation = 2f * chatter,
                    dy = 1.5f * shrink,
                    face = Part(dx = 2.2f * glance),
                    eyes = Part(sx = 1f - 0.1f * shrink, sy = 1f - 0.25f * shrink),
                    mouth = Part(dy = 0.3f * chatter, sx = 1f - 0.15f * shrink, rotation = 6f * glance),
                )
            }
            // Se encoge tenso, tiembla rápido, aprieta los ojos y jadea (la boca late).
            else -> {
                val tense = (1.6f * sin(PI_F * p)).coerceAtMost(1f)
                val gasp = abs(sin(TAU * 3f * p)) * tense
                IdlePose(
                    scaleX = 1f - 0.07f * tense,
                    scaleY = 1f - 0.07f * tense,
                    rotation = 4f * wobble(p, 0f, 1f, 9f),
                    dx = 1.6f * wobble(p, 0f, 1f, 12f),
                    face = Part(dx = 0.5f * wobble(p, 0f, 1f, 10f), dy = -0.6f * tense),
                    eyes = Part(sx = 1f - 0.18f * tense, sy = 1f - 0.2f * tense),
                    mouth = Part(sx = 1f + 0.2f * gasp, sy = 1f + 0.55f * gasp),
                )
            }
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
    // Solo cambia con p = 0 (pose neutra en todos los gestos): cambiar de gesto nunca salta.
    var variant by remember { mutableIntStateOf(0) }
    LaunchedEffect(mood, reduce, active) {
        if (reduce || !active) {
            val ms = MoodIdle.durationMs(mood, variant)
            if (p.value > 0f && !reduce) p.animateTo(1f, tween(((1f - p.value) * ms).toInt(), easing = LinearEasing))
            p.snapTo(0f)
            return@LaunchedEffect
        }
        if (p.value == 0f) variant = 0 // al elegirlo, primero el gesto insignia
        delay(350L) // deja terminar el "pop" de selección antes del gesto
        while (true) {
            val ms = MoodIdle.durationMs(mood, variant)
            p.animateTo(1f, tween(((1f - p.value) * ms).toInt(), easing = LinearEasing))
            p.snapTo(0f)
            delay(Random.nextLong(1800L, 4500L))
            variant = (variant + 1 + Random.nextInt(MoodIdle.VARIANTS - 1)) % MoodIdle.VARIANTS
        }
    }
    val origin = if (MoodIdle.anchoredAtBase(mood)) TransformOrigin(0.5f, 1f) else TransformOrigin.Center
    Glyph(
        MoodGlyphs.icon(mood),
        tint = Palette.FaceInk,
        // Después del modifier del llamador: sus drawBehind (p. ej. el estallido del chip) no se mueven.
        modifier = modifier.graphicsLayer {
            val pose = MoodIdle.pose(mood, p.value, variant)
            transformOrigin = origin
            scaleX = pose.scaleX
            scaleY = pose.scaleY
            rotationZ = pose.rotation
            translationX = pose.dx.dp.toPx()
            translationY = pose.dy.dp.toPx()
        },
        parts = { MoodIdle.pose(mood, p.value, variant) },
    )
}

/**
 * Figuras grandes del onboarding y del logo animado: las mismas siluetas y caras de [MoodGlyphs]
 * (viewBox 40x40) con trazo más fino. La cara va en [GLook] (se asoma hacia la inclinación) y los
 * ojos parpadean aplastándose sobre su altura.
 */
object OnboardingGlyphs {
    private const val SW = 1.9f
    private const val LOOK = 1.6f
    private val Ink = Palette.FaceInk

    private val shapes = Mood.entries.associateWith { mood ->
        val f = MoodGlyphs.features(mood, SW)
        val face = f.extra + f.brows + GLook(0f, f.eyes, blinkPivotY = f.eyesY) + f.mouth
        GlyphSpec(40f, 40f, listOf(GFill(MoodGlyphs.silhouette(mood), mood.color), GLook(LOOK, face)))
    }

    /** Centro de la cara mareada: sigue la parte ancha de cada silueta. */
    private fun dizzyCenterY(mood: Mood): Float = when (mood) {
        Mood.HAPPY -> 22f
        Mood.ANGRY -> 25f
        Mood.SLEEPY -> 26f
        Mood.BORED -> 21f
        Mood.CALM -> 23f
        Mood.STRESSED -> 21f
    }

    private val dizzies = Mood.entries.associateWith { dizzy(shapes.getValue(it), 20f, dizzyCenterY(it)) }

    fun shape(mood: Mood): GlyphSpec = shapes.getValue(mood)

    /** Versión mareada (tras un choque fuerte): mismo cuerpo, cara de espirales. */
    fun dizzy(mood: Mood): GlyphSpec = dizzies.getValue(mood)

    /** Quita la cara de [spec] (los GLook) y pone ojos en espiral + boca ondulada en ([cx], [cy]). */
    private fun dizzy(spec: GlyphSpec, cx: Float, cy: Float): GlyphSpec {
        val a = 1.3f
        // Tres medias vueltas de radio a, 2a, 3a; el trazo abarca [ex - 2a, ex + 4a], así que se corre -a.
        fun spiral(eyeX: Float, ey: Float): String {
            val ex = eyeX - a
            return "M$ex ${ey}a$a $a 0 1 1 ${2 * a} 0a${2 * a} ${2 * a} 0 1 1 ${-4 * a} 0a${3 * a} ${3 * a} 0 1 1 ${6 * a} 0"
        }
        val eyes = spiral(cx - 6f, cy - 2.4f) + spiral(cx + 6f, cy - 2.4f)
        val m = 2.4f
        val mouth = "M${cx - 2 * m} ${cy + 6.4f}q${m / 2} ${-m * 0.7f} $m 0t$m 0t$m 0t$m 0"
        return GlyphSpec(
            spec.width, spec.height,
            spec.items.filter { it !is GLook } + GLook(LOOK, listOf(GStroke(eyes, 1.3f, Ink), GStroke(mouth, SW, Ink))),
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
