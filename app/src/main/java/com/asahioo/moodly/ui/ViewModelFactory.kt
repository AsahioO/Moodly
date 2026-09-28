package com.asahioo.moodly.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asahioo.moodly.MoodlyApplication
import com.asahioo.moodly.di.AppContainer

/** Crea una factory que obtiene sus dependencias del [AppContainer] de la aplicación. */
inline fun <reified VM : ViewModel> containerViewModelFactory(
    crossinline create: (AppContainer) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MoodlyApplication
        create(app.container)
    }
}
