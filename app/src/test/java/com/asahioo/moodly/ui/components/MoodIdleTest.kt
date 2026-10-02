package com.asahioo.moodly.ui.components

import com.asahioo.moodly.data.model.Mood
import org.junit.Assert.assertEquals
import org.junit.Test

class MoodIdleTest {

    private fun assertRest(msg: String, part: Part) {
        assertEquals(msg, 0f, part.dx, 1e-4f)
        assertEquals(msg, 0f, part.dy, 1e-4f)
        assertEquals(msg, 1f, part.sx, 1e-4f)
        assertEquals(msg, 1f, part.sy, 1e-4f)
        assertEquals(msg, 0f, part.rotation, 1e-4f)
    }

    /** El gesto debe empezar y terminar en la pose neutra, o la cara salta al entrar/salir. */
    @Test
    fun gestureStartsAndEndsAtRest() {
        for (mood in Mood.entries) for (v in 0 until MoodIdle.VARIANTS) for (p in listOf(0f, 1f)) {
            val pose = MoodIdle.pose(mood, p, v)
            val msg = "$mood v=$v p=$p"
            assertRest(msg, Part(pose.dx, pose.dy, pose.scaleX, pose.scaleY, pose.rotation))
            listOf(pose.face, pose.eyes, pose.pupils, pose.mouth, pose.brows).forEach { assertRest(msg, it) }
        }
    }
}
