package com.asahioo.moodly

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.asahioo.moodly.ui.AppViewModel
import com.asahioo.moodly.ui.MoodlyRoot
import com.asahioo.moodly.ui.theme.MoodlyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels { AppViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Mantiene el splash nativo hasta que DataStore entrega el primer estado (sin parpadeos).
        splash.setKeepOnScreenCondition { !appViewModel.uiState.value.isReady }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            MoodlyTheme {
                MoodlyRoot(appViewModel)
            }
        }
    }

    /** Al abrir o volver a la app: Health Connect solo se lee en primer plano. */
    override fun onResume() {
        super.onResume()
        val container = (application as MoodlyApplication).container
        container.appScope.launch {
            container.health.refresh()
            container.health.sync()
        }
    }
}
