package com.asahioo.moodly.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.compositionLocalOf

/** Curvas de movimiento compartidas. */
object Motion {
    /** Curva de navegación estilo iOS: arranque rápido, frenado largo. */
    val Navigation: Easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    val EaseOut: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val EaseIn: Easing = CubicBezierEasing(0.5f, 0f, 0.75f, 0f)
    val FallIn: Easing = CubicBezierEasing(0.55f, 0f, 1f, 0.45f)
    val RiseOut: Easing = CubicBezierEasing(0f, 0.55f, 0.45f, 1f)
    /** Con ligero rebote al final (sobrepasa 1). */
    val Back: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

    const val NAV_MS = 480
}

/** true cuando el usuario activa "Reducir animaciones" en Ajustes. */
val LocalReduceMotion = compositionLocalOf { false }
