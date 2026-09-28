package com.asahioo.moodly.domain

import com.asahioo.moodly.data.model.AppData

/** Recorta, colapsa espacios internos y limita el largo. Vacío = etiqueta inválida. */
fun normalizeTagLabel(raw: String): String =
    raw.trim().split(Regex("\\s+")).joinToString(" ").take(AppData.MAX_TAG_LENGTH).trim()
