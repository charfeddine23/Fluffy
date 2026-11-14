package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.data.repository.FirebaseSyncRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "GoogleSignInHelper"

tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

@Stable
class GoogleSignInController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val launchLegacyFallback: (Activity, String) -> Unit,
    private val onIdTokenReceived: (String) -> Unit
) {
    var isLoading by mutableStateOf(false)
        internal set

    var errorMessage by mutableStateOf<String?>(null)
        internal set

    fun clearError() {
        errorMessage = null
    }

    fun startSignIn(isArabic: Boolean) {
        if (isLoading) return
        errorMessage = null

        if (!FirebaseSyncRepository.isFirebaseAvailable(context)) {
            errorMessage = if (isArabic) {
                "لم يتم العثور على إعدادات google-services.json في هذه النسخة."
            } else {
                "Firebase google-services.json is not configured in this build."
            }
            return
        }

        val activity = context.findHostActivity()
        if (activity == null) {
            errorMessage = if (isArabic) {
                "تعذر فتح نافذة تسجيل الدخول بـ Google."
            } else {
                "Unable to launch Google Sign-In window."
            }
            return
        }

        val webClientId = FirebaseSyncRepository.getWebClientId(context)
        if (webClientId.isBlank()) {
            errorMessage = if (isArabic) {
                "معرّف default_web_client_id غير متوفر في google-services.json. فعّل Google Sign-In في Firebase Console."
            } else {
                "default_web_client_id missing from google-services.json. Enable Google Sign-In in Firebase Console."
            }
            return
        }

        isLoading = true
        val credentialManager = CredentialManager.create(context)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    isLoading = false
                    onIdTokenReceived(googleIdToken)
                } else {
                    Log.w(TAG, "Unexpected credential type returned by CredentialManager; trying legacy fallback")
                    launchLegacyFallback(activity, webClientId)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                isLoading = false
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No credential available via CredentialManager (${e.message}); falling back to GoogleSignInClient")
                launchLegacyFallback(activity, webClientId)
            } catch (e: GetCredentialException) {
                Log.w(TAG, "CredentialManager error (${e.type}: ${e.message}); falling back to GoogleSignInClient")
                launchLegacyFallback(activity, webClientId)
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed via CredentialManager", e)
                launchLegacyFallback(activity, webClientId)
            }
        }
    }
}

@Composable
fun rememberGoogleSignInController(
    isArabic: Boolean,
    onGoogleIdToken: (String) -> Unit
): GoogleSignInController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var controllerRef by remember { mutableStateOf<GoogleSignInController?>(null) }

    val legacyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val ctrl = controllerRef ?: return@rememberLauncherForActivityResult
        ctrl.isLoading = false
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (!idToken.isNullOrBlank()) {
                onGoogleIdToken(idToken)
            } else {
                ctrl.errorMessage = if (isArabic) {
                    "تعذر استلام رمز ID Token من Google."
                } else {
                    "Failed to retrieve ID Token from Google."
                }
            }
        } catch (e: ApiException) {
            when (e.statusCode) {
                CommonStatusCodes.CANCELED, 12501 -> {
                    Log.w(TAG, "GoogleSignInClient cancelled by user (${e.statusCode})")
                }
                CommonStatusCodes.DEVELOPER_ERROR -> {
                    ctrl.errorMessage = if (isArabic) {
                        "خطأ إعدادات (10): تأكد من بصمة SHA-1 و Google Sign-In في Firebase Console"
                    } else {
                        "Configuration error (10): Check SHA-1 fingerprint & Google Sign-In in Firebase Console"
                    }
                }
                CommonStatusCodes.NETWORK_ERROR -> {
                    ctrl.errorMessage = if (isArabic) {
                        "تحقق من اتصال الإنترنت ثم حاول مجدداً"
                    } else {
                        "Network error during Google Sign-In"
                    }
                }
                else -> {
                    ctrl.errorMessage = if (isArabic) {
                        "تعذر الدخول عبر Google (رمز ${e.statusCode})"
                    } else {
                        "Google Sign-In failed (${e.statusCode})"
                    }
                }
            }
        } catch (e: Exception) {
            ctrl.errorMessage = e.localizedMessage ?: "Google Sign-In failed"
        }
    }

    return remember(context, onGoogleIdToken) {
        GoogleSignInController(
            context = context,
            scope = scope,
            launchLegacyFallback = { activity, webClientId ->
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(activity, gso)
                client.signOut().addOnCompleteListener {
                    legacyLauncher.launch(client.signInIntent)
                }
            },
            onIdTokenReceived = onGoogleIdToken
        ).also { controllerRef = it }
    }
}
