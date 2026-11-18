package com.example.auth

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}

class BiometricAuthManager(private val activity: FragmentActivity) {

    private var activePrompt: BiometricPrompt? = null
    private var activePromptTag: String? = null
    private var activeGeneration: Long = 0L

    fun checkAvailability(): BiometricAvailability {
        val biometricManager = BiometricManager.from(activity)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        }

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            else -> BiometricAvailability.UNAVAILABLE
        }
    }

    fun cancelPrompt(tag: String? = null) {
        if (tag == null || tag == activePromptTag) {
            activeGeneration++
            runCatching { activePrompt?.cancelAuthentication() }
            activePrompt = null
            activePromptTag = null
        }
    }

    fun promptBiometricAuth(
        title: String,
        subtitle: String,
        tag: String = "default",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        activeGeneration++
        val callGen = activeGeneration
        runCatching { activePrompt?.cancelAuthentication() }

        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                if (callGen != activeGeneration) return
                activePrompt = null
                activePromptTag = null
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (callGen != activeGeneration) return
                activePrompt = null
                activePromptTag = null
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Individual failed attempt, prompt stays open
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        activePrompt = prompt
        activePromptTag = tag

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        } else {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        }

        try {
            prompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            if (callGen == activeGeneration) {
                activePrompt = null
                activePromptTag = null
                onError(e.message ?: "Authentication failed")
            }
        }
    }
}
