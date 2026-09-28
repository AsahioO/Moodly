package com.asahioo.moodly.ui.onboarding

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

/*
 * Física mínima de pelotas para el intro: gravedad, choques entre círculos y contra los bordes,
 * rodamiento y aplastamiento tipo gelatina. Unidades internas: px y segundos; las constantes
 * están en dp y se escalan con la densidad de pantalla.
 * ponytail: colisión solo por círculos; las puntas del rayo/triángulo pueden encimarse un poco.
 * Polígonos si llega a molestar.
 */

// ponytail: calibración a ojo; ajustar en dispositivo.
private const val GRAVITY = 1900f // dp/s² con el celular vertical
private const val RESTITUTION = 0.42f
private const val AIR = 0.35f // 1/s
private const val FLOOR_FRICTION = 1.4f // 1/s, frena al rodar por el piso
private const val ROLL_GRIP = 12f // 1/s, qué tan rápido el giro se ajusta al rodar por el piso
private const val CONTACT_GRIP = 6f // 1/s, lo mismo al rozarse entre figuras
private const val ROLL = 0.3f // fracción de la rodada de una pelota: ruedan un poco, no como trompo
private const val MAX_SPIN = 240f // °/s
private const val REST_SPEED = 40f // dp/s: por debajo no rebota (evita temblor en reposo)
private const val DIZZY_SPEED = 750f // dp/s de choque para marearse
private const val DIZZY_TIME = 1.4f // s
private const val DIZZY_GRACE = 2.2f // s desde el inicio: la caída inicial no marea
private const val SQUISH_PER_SPEED = 0.0024f
private const val SQUISH_K = 300f
private const val SQUISH_C = 14f
private const val RIGHTING = 30f // tentempié: vuelve a quedar derecha al calmarse
private const val RIGHTING_SPEED = 300f // dp/s: a esta velocidad el enderezado baja a su mínimo (30%)
private const val SPIN_DAMP = 2.5f // 1/s, siempre activo
private const val POKE_SPEED = 650f // dp/s
private const val SUBSTEPS = 4
private const val DEG = 57.29578f

class Body(val r: Float, var x: Float, var y: Float, val baseRotation: Float) {
    var vx = 0f
    var vy = 0f

    /** Grados. */
    var angle = baseRotation

    /** Grados por segundo. */
    var spin = 0f
    var squish = 0f
    var squishVel = 0f

    /** Segundos de mareo restantes. */
    var dizzy = 0f

    /** Arrastrada por el dedo: masa infinita, no la mueve la gravedad. */
    var held = false
    var active = false

    /** Ya entró a la pantalla (antes de eso el techo no la detiene). */
    internal var inside = false
    internal val invMass get() = if (held) 0f else 1f / (r * r)
}

class World(val width: Float, val height: Float, val bodies: List<Body>, private val density: Float) {
    var time = 0f
        private set

    /** Avanza la simulación con gravedad ([gx], [gy]) en g; devuelve true si alguien se acaba de marear. */
    fun step(dt: Float, gx: Float, gy: Float): Boolean {
        val frame = dt.coerceIn(0f, 1f / 30f)
        time += frame
        val h = frame / SUBSTEPS
        val g = GRAVITY * density
        var dizzied = false
        repeat(SUBSTEPS) {
            for (b in bodies) {
                if (!b.active || b.held) continue
                val air = 1f - AIR * h
                b.vx = (b.vx + gx * g * h) * air
                b.vy = (b.vy + gy * g * h) * air
                b.x += b.vx * h
                b.y += b.vy * h
            }
            for (i in bodies.indices) for (j in i + 1 until bodies.size) {
                if (collide(bodies[i], bodies[j], h)) dizzied = true
            }
            for (b in bodies) if (b.active && walls(b, h)) dizzied = true
        }
        for (b in bodies) {
            if (!b.active) continue
            var off = (b.angle - b.baseRotation) % 360f
            if (off > 180f) off -= 360f
            if (off < -180f) off += 360f
            val calm = (1f - hypot(b.vx, b.vy) / (RIGHTING_SPEED * density)).coerceAtLeast(0.3f)
            b.spin = (b.spin + (-off * RIGHTING * calm - b.spin * SPIN_DAMP) * frame).coerceIn(-MAX_SPIN, MAX_SPIN)
            b.angle += b.spin * frame
            b.squishVel += (-SQUISH_K * b.squish - SQUISH_C * b.squishVel) * frame
            b.squish = (b.squish + b.squishVel * frame).coerceIn(-0.25f, 0.25f)
            b.dizzy = (b.dizzy - frame).coerceAtLeast(0f)
        }
        return dizzied
    }

    /** La figura bajo el punto; la de más arriba (última dibujada) si se enciman. */
    fun bodyAt(px: Float, py: Float): Body? = bodies.lastOrNull { it.active && hypot(px - it.x, py - it.y) < it.r }

    /** Toque: saltito + aplastón. */
    fun poke(b: Body) {
        b.vy -= POKE_SPEED * density
        b.squishVel += 3f
    }

    private fun collide(a: Body, b: Body, h: Float): Boolean {
        if (!a.active || !b.active) return false
        val dx = b.x - a.x
        val dy = b.y - a.y
        val minD = a.r + b.r
        val d2 = dx * dx + dy * dy
        if (d2 >= minD * minD) return false
        val wa = a.invMass
        val wb = b.invMass
        val w = wa + wb
        if (w == 0f) return false
        val d = sqrt(d2)
        val nx = if (d > 0.01f) dx / d else 0f
        val ny = if (d > 0.01f) dy / d else 1f
        // Separar según masa.
        val push = (minD - d) / w
        a.x -= nx * push * wa
        a.y -= ny * push * wa
        b.x += nx * push * wb
        b.y += ny * push * wb
        // Roce tangencial: al deslizarse una contra otra, giran un poco (con peso por dt, sin acumular).
        val vt = -(b.vx - a.vx) * ny + (b.vy - a.vy) * nx
        val grip = (CONTACT_GRIP * h).coerceAtMost(1f)
        a.spin += (-vt / a.r * DEG * ROLL - a.spin) * grip
        b.spin += (-vt / b.r * DEG * ROLL - b.spin) * grip
        val rv = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
        if (rv >= 0f) return false
        val j = -(1f + restitution(-rv)) * rv / w
        a.vx -= j * wa * nx
        a.vy -= j * wa * ny
        b.vx += j * wb * nx
        b.vy += j * wb * ny
        val hitA = impact(a, -rv)
        val hitB = impact(b, -rv)
        return hitA || hitB
    }

    private fun walls(b: Body, h: Float): Boolean {
        var hit = false
        if (b.x < b.r) {
            b.x = b.r
            if (b.vx < 0f) hit = impact(b, -b.vx).also { b.vx = -b.vx * restitution(-b.vx) }
        }
        if (b.x > width - b.r) {
            b.x = width - b.r
            if (b.vx > 0f) hit = impact(b, b.vx).also { b.vx = -b.vx * restitution(b.vx) } || hit
        }
        if (b.y > height - b.r) {
            b.y = height - b.r
            if (b.vy > 0f) hit = impact(b, b.vy).also { b.vy = -b.vy * restitution(b.vy) } || hit
            // Rueda sobre el piso.
            b.vx *= 1f - FLOOR_FRICTION * h
            b.spin += (b.vx / b.r * DEG * ROLL - b.spin) * (ROLL_GRIP * h).coerceAtMost(1f)
        }
        if (b.y >= b.r) b.inside = true
        if (b.inside && b.y < b.r) {
            b.y = b.r
            if (b.vy < 0f) hit = impact(b, -b.vy).also { b.vy = -b.vy * restitution(-b.vy) } || hit
        }
        return hit
    }

    private fun restitution(speed: Float) = if (abs(speed) < REST_SPEED * density) 0f else RESTITUTION

    /** Golpe a [speed] px/s: aplasta y, si es fuerte, marea. True si se acaba de marear. */
    private fun impact(b: Body, speed: Float): Boolean {
        b.squishVel += speed / density * SQUISH_PER_SPEED
        if (speed < DIZZY_SPEED * density || time < DIZZY_GRACE) return false
        val fresh = b.dizzy == 0f
        b.dizzy = DIZZY_TIME
        return fresh
    }
}
