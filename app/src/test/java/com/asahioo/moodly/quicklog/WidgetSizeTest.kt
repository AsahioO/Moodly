package com.asahioo.moodly.quicklog

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSizeTest {

    @Test
    fun thresholds() {
        assertEquals(WidgetSize.SMALL, widgetSize(0))
        assertEquals(WidgetSize.SMALL, widgetSize(89))
        assertEquals(WidgetSize.MEDIUM, widgetSize(90))
        assertEquals(WidgetSize.MEDIUM, widgetSize(159))
        assertEquals(WidgetSize.LARGE, widgetSize(160))
    }
}
