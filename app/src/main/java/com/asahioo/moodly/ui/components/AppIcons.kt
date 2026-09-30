package com.asahioo.moodly.ui.components

import androidx.compose.ui.graphics.Color
import com.asahioo.moodly.ui.theme.Palette

/** Íconos de la app (viewBox 24x24, tintables). */
object AppIcons {
    private const val SW = 1.8f
    private const val CAL_RECT =
        "M7 5h10a3.5 3.5 0 0 1 3.5 3.5v8.5a3.5 3.5 0 0 1-3.5 3.5H7a3.5 3.5 0 0 1-3.5-3.5V8.5A3.5 3.5 0 0 1 7 5z"

    val Home = GlyphSpec(24f, GFill("M3.5 10.6L12 3.5l8.5 7.1V20a1 1 0 0 1-1 1H15v-6h-6v6H4.5a1 1 0 0 1-1-1z"))
    val Pie = GlyphSpec(
        24f,
        GStroke("M11 4.1A8 8 0 1 0 19.9 13H11z", SW),
        GStroke("M14 2.6V10h7.4A7.5 7.5 0 0 0 14 2.6z", SW),
    )
    val Calendar = GlyphSpec(
        24f,
        GStroke(CAL_RECT, SW),
        GStroke("M8 3v4M16 3v4M3.5 10h17", SW),
        GCircle(8f, 14f, 1.2f), GCircle(12f, 14f, 1.2f), GCircle(16f, 14f, 1.2f),
        GCircle(8f, 17.2f, 1.2f), GCircle(12f, 17.2f, 1.2f),
    )
    val CalendarCheck = GlyphSpec(
        24f,
        GStroke(CAL_RECT, SW),
        GStroke("M8 3v4M16 3v4M3.5 10h17M13 15.5l1.6 1.6 3-3", SW),
    )
    val Grid = GlyphSpec(
        24f,
        GRoundRect(9.5f, 3f, 5f, 5f, 1.6f),
        GRoundRect(3f, 9.5f, 5f, 5f, 1.6f),
        GRoundRect(16f, 9.5f, 5f, 5f, 1.6f),
        GRoundRect(9.5f, 16f, 5f, 5f, 1.6f),
    )
    val Menu = GlyphSpec(24f, GStroke("M6.5 9.5h11M6.5 14.5h11", SW))
    val Back = GlyphSpec(24f, GStroke("M14.5 6l-6 6 6 6", 2f))
    val ChevronRight = GlyphSpec(24f, GStroke("M9.5 6l6 6-6 6", 2f))
    val ArrowRight = GlyphSpec(24f, GStroke("M5 12h14M13 6l6 6-6 6", 2.2f))
    val Bed = GlyphSpec(
        24f,
        GStroke("M3 19V6M3 15h18v4M21 15v-2.5a3 3 0 0 0-3-3h-7V15", SW),
        GCircle(7f, 11.5f, 1.8f, fill = null, stroke = Color.Unspecified, strokeWidth = SW),
    )
    val StressFace = GlyphSpec(
        24f,
        GCircle(12f, 12f, 9f, fill = null, stroke = Color.Unspecified, strokeWidth = SW),
        GCircle(9f, 10f, 1.2f), GCircle(15f, 10f, 1.2f),
        GStroke("M8.5 16c2-1.5 5-1.5 7 0", SW),
    )
    val Steps = GlyphSpec(
        24f,
        GFill("M7 3c1.9 0 3 1.8 3 4.3 0 2.2-.8 3.7-.8 5.2H5.8C5.8 11 4 9.5 4 7.3 4 4.8 5.1 3 7 3z"),
        GRoundRect(5.8f, 13.6f, 3.4f, 3.2f, 1.6f),
        GFill("M17 7c1.9 0 3 1.8 3 4.3 0 2.2-1.8 3.7-1.8 5.2h-3.4c0-1.5-.8-3-.8-5.2 0-2.5 1.1-4.3 3-4.3z"),
        GRoundRect(14.8f, 17.6f, 3.4f, 3.2f, 1.6f),
    )
    val Search = GlyphSpec(24f, GStroke("M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM16.5 16.5L20.5 20.5", SW))
    val Puzzle =GlyphSpec(24f, GFill("M4 7h4.2a2.8 2.8 0 1 1 5.6 0H18v4.2a2.8 2.8 0 1 1 0 5.6V21H4z"))

    // Íconos a color para los avisos (viewBox 40x40).
    val ToastCheck = GlyphSpec(
        40f,
        GCircle(20f, 20f, 19f, fill = Palette.Lime),
        GStroke("M13 20.5l4.5 4.5 9-9.5", 2.6f, Palette.Ink),
    )
    val ToastSleep = GlyphSpec(
        40f,
        GCircle(20f, 20f, 19f, fill = Palette.Peach),
        GFill("M24.5 12.5a8.5 8.5 0 1 0 5 12.4A7 7 0 0 1 24.5 12.5z", Palette.Ink),
    )
    val ToastAlert = GlyphSpec(
        40f,
        GCircle(20f, 20f, 19f, fill = Color(0xFF3A3A3E)),
        GStroke("M20 12v10M20 27.5v.1", 3f, Color.White),
    )
}
