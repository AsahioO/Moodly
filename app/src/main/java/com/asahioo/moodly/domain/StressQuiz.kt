package com.asahioo.moodly.domain

import com.asahioo.moodly.data.model.StressLevel

/** Reglas del quiz de Sí/No. Los textos viven en la capa de UI (strings.xml). */
object StressQuiz {

    /** Respuesta saludable de cada pregunta: true = "Sí". */
    val healthyAnswers: List<Boolean> = listOf(true, false, true, false, true, false, true, true)

    val size: Int get() = healthyAnswers.size

    fun evaluate(answers: List<Boolean>): StressLevel {
        val unhealthy = answers.zip(healthyAnswers).count { (answer, healthy) -> answer != healthy }
        return when {
            unhealthy <= 2 -> StressLevel.LOW
            unhealthy <= 4 -> StressLevel.MEDIUM
            else -> StressLevel.HIGH
        }
    }
}
