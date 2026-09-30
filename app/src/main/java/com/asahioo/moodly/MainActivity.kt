package com.asahioo.moodly

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.asahioo.moodly.ui.AppLock
import com.asahioo.moodly.ui.AppViewModel
import com.asahioo.moodly.ui.MoodlyRoot
import com.asahioo.moodly.ui.theme.MoodlyTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// FragmentActivity (sigue siendo ComponentActivity): BiometricPrompt lo exige.
class MainActivity : FragmentActivity() {

    private val appViewModel: AppViewModel by viewModels { AppViewModel.Factory }

    /** Momento en que la app salió de primer plano; sirve para no pedir la credencial en idas y vueltas cortas. */
    private var stoppedAt = 0L

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
            val state by appViewModel.uiState.collectAsStateWithLifecycle()
            MoodlyTheme(state.settings.theme) {
                MoodlyRoot(appViewModel, onUnlock = ::promptUnlock)
            }
        }

        // Con bloqueo activo, la miniatura de "recientes" no debe mostrar los registros.
        lifecycleScope.launch {
            appViewModel.uiState.map { it.settings.appLock }.distinctUntilChanged().collect { locked ->
                if (locked) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        // Cada vez que la app queda bloqueada estando visible (arranque, regreso de segundo plano), pide la credencial.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                appViewModel.uiState.map { it.locked }.distinctUntilChanged().filter { it }.collect { promptUnlock() }
            }
        }
    }

    private fun promptUnlock() {
        AppLock.authenticate(this) { ok -> if (ok) appViewModel.unlock() }
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }

    /** Al abrir o volver a la app: Health Connect solo se lee en primer plano. */
    override fun onResume() {
        super.onResume()
        if (stoppedAt != 0L && SystemClock.elapsedRealtime() - stoppedAt > LOCK_GRACE_MS) appViewModel.lock()
        stoppedAt = 0L
        val container = (application as MoodlyApplication).container
        container.appScope.launch {
            container.health.refresh()
            container.health.sync()
        }
    }

    private companion object {
        /** Selectores de archivo y permisos del sistema salen de la app unos segundos sin que cuente como abandono. */
        const val LOCK_GRACE_MS = 30_000L
    }
}
