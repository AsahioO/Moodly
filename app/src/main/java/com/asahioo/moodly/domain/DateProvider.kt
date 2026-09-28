package com.asahioo.moodly.domain

import java.time.LocalDate

/** Abstrae la fecha actual para poder probar la lógica con fechas fijas. */
fun interface DateProvider {
    fun today(): LocalDate
}

object SystemDateProvider : DateProvider {
    override fun today(): LocalDate = LocalDate.now()
}
