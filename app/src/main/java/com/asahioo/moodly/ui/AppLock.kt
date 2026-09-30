package com.asahioo.moodly.ui

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.asahioo.moodly.R

/**
 * Bloqueo con la credencial del dispositivo (huella, rostro, PIN o patrón).
 * ponytail: antes de Android 11 no se puede combinar biometría con PIN del sistema en BiometricPrompt,
 * así que allí solo funciona con biometría inscrita; upgrade: KeyguardManager.createConfirmDeviceCredentialIntent.
 */
object AppLock {

    private val authenticators =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_WEAK or DEVICE_CREDENTIAL else BIOMETRIC_WEAK

    fun isAvailable(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    /** Muestra el diálogo del sistema; [onResult] recibe true solo si el usuario se autenticó. */
    fun authenticate(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(false)
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_title))
            .setAllowedAuthenticators(authenticators)
            .apply {
                // Con credencial del sistema el botón negativo está prohibido; sin ella es obligatorio.
                if (authenticators and DEVICE_CREDENTIAL == 0) setNegativeButtonText(activity.getString(R.string.cancel))
            }
            .build()
        prompt.authenticate(info)
    }
}
