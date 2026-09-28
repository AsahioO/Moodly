package com.asahioo.moodly.domain

/**
 * Generador determinista (FNV-1a + mulberry32). La misma clave siempre produce el mismo valor,
 * así las barras decorativas del inicio son estables entre sesiones.
 */
internal object SeededRandom {

    fun hash(key: String): Int {
        var h = 0x811C9DC5.toInt()
        for (c in key) {
            h = h xor c.code
            h *= 16777619
        }
        return h
    }

    /** Valor en el rango [0, 1). */
    fun unit(key: String): Float {
        var t = hash(key) + 0x6D2B79F5
        t = (t xor (t ushr 15)) * (1 or t)
        t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
        val bits = (t xor (t ushr 14)).toLong() and 0xFFFFFFFFL
        return (bits.toDouble() / 4294967296.0).toFloat()
    }
}
