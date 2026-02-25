package com.sebo.seboencrypt

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Fix 5: App-Sperre per Biometrie oder Geräte-PIN/Passwort.
 * Nutzt androidx.biometric:1.1.0 mit FragmentActivity-Konstruktor.
 * MainActivity erbt von FragmentActivity – kein Cast nötig.
 */
object BiometricAuthHelper {

    private const val ALLOWED_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit = {},
        onNotEnrolled: () -> Unit = {}
    ) {
        val biometricManager = BiometricManager.from(activity)

        when (biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> launchPrompt(activity, onSuccess, onFailure)
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> onNotEnrolled()
            else -> onSuccess()
        }
    }

    private fun launchPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure(errString.toString())
                }
                override fun onAuthenticationFailed() { }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("S.E.B.O. Encrypt")
            .setSubtitle("Identität bestätigen")
            .setDescription("Biometrie oder Geräte-PIN zur Entsperrung erforderlich")
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()
        prompt.authenticate(promptInfo)
    }
}
