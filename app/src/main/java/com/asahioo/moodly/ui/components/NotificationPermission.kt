package com.asahioo.moodly.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

private class PendingResult {
    var callback: ((Boolean) -> Unit)? = null
}

/**
 * Devuelve una función que pide el permiso de notificaciones (Android 13+) y responde si la app
 * puede notificar. En versiones previas no hay permiso, pero el usuario pudo apagarlas en el sistema.
 */
@Composable
fun rememberNotificationPermission(): (onResult: (granted: Boolean) -> Unit) -> Unit {
    val context = LocalContext.current
    val pending = remember { PendingResult() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pending.callback?.invoke(granted)
        pending.callback = null
    }
    return remember(context, launcher) {
        { onResult ->
            val needsRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsRequest) {
                pending.callback = onResult
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onResult(NotificationManagerCompat.from(context).areNotificationsEnabled())
            }
        }
    }
}
