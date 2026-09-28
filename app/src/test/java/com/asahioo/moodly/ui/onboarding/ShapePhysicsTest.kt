package com.asahioo.moodly.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class ShapePhysicsTest {

    private fun ball(x: Float, y: Float) = Body(50f, x, y, 0f).apply { active = true }

    private fun World.run(seconds: Float, gy: Float = 1f) = repeat((seconds * 60).toInt()) { step(1f / 60f, 0f, gy) }

    @Test
    fun ballFallsAndRestsOnFloor() {
        val b = ball(200f, 100f)
        World(400f, 800f, listOf(b), density = 1f).run(5f)
        assertEquals(750f, b.y, 1f)
        assertTrue(abs(b.vy) < 5f)
    }

    @Test
    fun overlappingBallsSeparate() {
        val a = ball(180f, 400f)
        val b = ball(220f, 400f)
        World(400f, 800f, listOf(a, b), density = 1f).run(0.1f, gy = 0f)
        assertTrue(hypot(b.x - a.x, b.y - a.y) >= 99f)
    }

    @Test
    fun onlyHardHitsMakeDizzy() {
        fun hit(speed: Float): Float {
            val a = ball(100f, 400f)
            val b = ball(300f, 400f)
            val world = World(400f, 800f, listOf(a, b), density = 1f)
            world.run(3f, gy = 0f) // pasa el periodo de gracia
            a.vx = speed
            world.run(0.5f, gy = 0f)
            return b.dizzy
        }
        assertTrue(hit(2000f) > 0f)
        assertEquals(0f, hit(200f))
    }

    @Test
    fun pileSettlesUprightWithoutSpinningLikeATop() {
        val bodies = listOf(ball(100f, 300f), ball(160f, 150f), ball(240f, 300f), ball(300f, 100f))
        bodies.forEachIndexed { i, b -> b.spin = if (i % 2 == 0) -200f else 200f }
        val world = World(400f, 800f, bodies, density = 1f)
        repeat(5 * 60) {
            world.step(1f / 60f, 0.3f, 1f) // un poco inclinado: se deslizan entre sí
            bodies.forEach { assertTrue(abs(it.spin) <= 240f) }
        }
        world.run(5f)
        bodies.forEach {
            assertTrue("spin ${it.spin}", abs(it.spin) < 30f)
            assertTrue("angle ${it.angle}", abs(((it.angle % 360f) + 540f) % 360f - 180f) < 15f)
        }
    }
}
