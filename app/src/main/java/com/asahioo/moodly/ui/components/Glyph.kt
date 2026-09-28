package com.asahioo.moodly.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.PathParser
import com.asahioo.moodly.ui.theme.Palette
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/*
 * Motor de dibujo vectorial ligero: las caras, figuras e íconos se describen con los mismos
 * paths SVG del diseño y se dibujan directo en Canvas (sin bitmaps, nítidos a cualquier tamaño).
 * Los Path se parsean una sola vez y se reutilizan.
 * Color.Unspecified = usar el tinte recibido al dibujar.
 */

@Immutable
sealed interface GlyphItem

@Immutable
class GFill(pathData: String, val color: Color = Color.Unspecified) : GlyphItem {
    val path: Path by lazy { PathParser().parsePathString(pathData).toPath() }
}

@Immutable
class GStroke(
    pathData: String,
    val width: Float,
    val color: Color = Color.Unspecified,
) : GlyphItem {
    val path: Path by lazy { PathParser().parsePathString(pathData).toPath() }
    val style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
}

@Immutable
class GCircle(
    val cx: Float,
    val cy: Float,
    val r: Float,
    val fill: Color? = Color.Unspecified,
    val stroke: Color? = null,
    val strokeWidth: Float = 0f,
) : GlyphItem

@Immutable
class GOval(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val color: Color = Color.Unspecified) : GlyphItem

@Immutable
class GRoundRect(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val radius: Float,
    val color: Color = Color.Unspecified,
    /** Grados, alrededor del centro del rectángulo. */
    val rotation: Float = 0f,
) : GlyphItem

@Immutable
class GGroup(val rotation: Float, val pivotX: Float, val pivotY: Float, val items: List<GlyphItem>) : GlyphItem

/**
 * Rasgos que "miran": se desplazan look * [depth] (unidades del viewBox). Si hay [blinkPivotY],
 * parpadean aplastándose en Y sobre esa altura.
 */
@Immutable
class GLook(val depth: Float, val items: List<GlyphItem>, val blinkPivotY: Float? = null) : GlyphItem

/** Dibujo en coordenadas de un viewBox de [width] x [height]. */
@Immutable
class GlyphSpec(val width: Float, val height: Float, val items: List<GlyphItem>) {
    constructor(size: Float, vararg items: GlyphItem) : this(size, size, items.toList())

    val aspectRatio: Float get() = width / height
}

/** [look] en [-1, 1] (hacia dónde mira la cara); [blink] en [0, 1] (1 = ojos cerrados). */
fun DrawScope.drawGlyph(spec: GlyphSpec, tint: Color = Palette.Ink, look: Offset = Offset.Zero, blink: Float = 0f) {
    val k = min(size.width / spec.width, size.height / spec.height)
    val dx = (size.width - spec.width * k) / 2f
    val dy = (size.height - spec.height * k) / 2f
    withTransform({
        translate(dx, dy)
        scale(k, k, pivot = Offset.Zero)
    }) {
        spec.items.forEach { drawItem(it, tint, look, blink) }
    }
}

private fun Color.or(tint: Color): Color = if (isSpecified) this else tint

private fun DrawScope.drawItem(item: GlyphItem, tint: Color, look: Offset, blink: Float) {
    when (item) {
        is GFill -> drawPath(item.path, item.color.or(tint))
        is GStroke -> drawPath(item.path, item.color.or(tint), style = item.style)
        is GCircle -> {
            val center = Offset(item.cx, item.cy)
            item.fill?.let { drawCircle(it.or(tint), item.r, center) }
            item.stroke?.let { drawCircle(it.or(tint), item.r, center, style = Stroke(item.strokeWidth)) }
        }
        is GOval -> drawOval(
            color = item.color.or(tint),
            topLeft = Offset(item.cx - item.rx, item.cy - item.ry),
            size = Size(item.rx * 2f, item.ry * 2f),
        )
        is GRoundRect -> rotate(item.rotation, pivot = Offset(item.x + item.w / 2f, item.y + item.h / 2f)) {
            drawRoundRect(
                color = item.color.or(tint),
                topLeft = Offset(item.x, item.y),
                size = Size(item.w, item.h),
                cornerRadius = CornerRadius(item.radius),
            )
        }
        is GGroup -> rotate(item.rotation, pivot = Offset(item.pivotX, item.pivotY)) {
            // Contra-rotar look para que los hijos miren hacia la misma dirección en pantalla.
            val r = Math.toRadians(-item.rotation.toDouble())
            val c = cos(r).toFloat()
            val s = sin(r).toFloat()
            val local = Offset(look.x * c - look.y * s, look.x * s + look.y * c)
            item.items.forEach { drawItem(it, tint, local, blink) }
        }
        is GLook -> withTransform({
            translate(look.x * item.depth, look.y * item.depth)
            item.blinkPivotY?.let { scale(1f, 1f - blink * 0.9f, pivot = Offset(0f, it)) }
        }) {
            item.items.forEach { drawItem(it, tint, look, blink) }
        }
    }
}

/** Dibuja un [GlyphSpec] ocupando el tamaño que le dé el [modifier]. */
@Composable
fun Glyph(
    spec: GlyphSpec,
    modifier: Modifier = Modifier,
    tint: Color = Palette.Ink,
    look: () -> Offset = { Offset.Zero },
    blink: () -> Float = { 0f },
) {
    Spacer(modifier.drawBehind { drawGlyph(spec, tint, look(), blink()) })
}
