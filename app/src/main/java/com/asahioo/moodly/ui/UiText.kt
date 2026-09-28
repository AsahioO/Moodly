package com.asahioo.moodly.ui

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource

/**
 * Texto que se resuelve en la UI. Permite que lógica fuera de la composición (callbacks,
 * eventos) describa mensajes sin tocar Context ni Resources.
 */
@Immutable
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class ArrayItem(@ArrayRes val id: Int, val index: Int) : UiText
    data class Plain(val value: String) : UiText

    companion object {
        fun res(@StringRes id: Int, vararg args: Any): UiText = Res(id, args.toList())
    }
}

@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Plain -> value
    is UiText.ArrayItem -> stringArrayResource(id)[index]
    is UiText.Res -> {
        val resolved = args.map { if (it is UiText) it.resolve() else it }
        stringResource(id, *resolved.toTypedArray())
    }
}
