package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.data.model.GlassTint
import com.example.data.repository.CloudSyncStatus
import com.example.data.repository.FirebaseSyncRepository
import com.example.data.repository.FirebaseUserInfo
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun FirebaseAuthDialog(
    visible: Boolean,
    currentUser: FirebaseUserInfo?,
    cloudSyncStatus: CloudSyncStatus,
    isDark: Boolean,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onGoogleIdToken: (String) -> Unit,
    onEmailSignIn: (email: String, password: String, onError: (String) -> Unit) -> Unit,
    onEmailRegister: (email: String, password: String, onError: (String) -> Unit) -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var isEmailLoading by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(currentUser, visible) {
        isGoogleLoading = false
        isEmailLoading = false
        if (currentUser != null) {
            localError = null
        }
    }

    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val cardFill = if (isDark) Color(0xFF0F172A).copy(alpha = 0.90f) else Color.White.copy(alpha = 0.92f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard(
                isDark = isDark,
                tint = GlassTint.CYAN,
                shape = RoundedCornerShape(28.dp),
                borderWidth = 1.6.dp,
                customFill = cardFill,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .testTag("firebase_auth_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .liquidGlass(
                                        isDark = isDark,
                                        tint = GlassTint.CYAN,
                                        shape = RoundedCornerShape(14.dp),
                                        borderWidth = 1.2.dp
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (currentUser != null) {
                                        if (isArabic) "الحساب السحابي المتصل" else "Connected Cloud Account"
                                    } else {
                                        if (isArabic) "المزامنة السحابية (Firebase)" else "Cloud Sync (Firebase)"
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = if (isArabic)
                                        "مزامنة فورية تلقائية للملاحظات والإعدادات"
                                    else
                                        "Real-time Cloud Firestore auto-sync",
                                    fontSize = 11.5.sp,
                                    color = metaColor
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("auth_dialog_close")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isArabic) "إغلاق" else "Close",
                                tint = textColor
                            )
                        }
                    }

                    if (currentUser != null) {
                        // Signed-in status view
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    isDark = isDark,
                                    tint = GlassTint.EMERALD,
                                    shape = RoundedCornerShape(18.dp),
                                    borderWidth = 1.dp,
                                    customFill = if (isDark) Color(0xFF1E293B).copy(alpha = 0.72f) else Color(0xFFF1F5F9).copy(alpha = 0.85f)
                                )
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Column {
                                        Text(
                                            text = currentUser.displayName?.takeIf { it.isNotBlank() }
                                                ?: currentUser.email
                                                ?: "Firebase User",
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                        if (!currentUser.email.isNullOrBlank()) {
                                            Text(
                                                text = currentUser.email,
                                                fontSize = 12.sp,
                                                color = metaColor
                                            )
                                        }
                                    }
                                }

                                val statusText = when (cloudSyncStatus) {
                                    CloudSyncStatus.Syncing ->
                                        if (isArabic) "جاري المزامنة الفورية..." else "Syncing with Cloud Firestore..."
                                    is CloudSyncStatus.Synced -> {
                                        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                            .format(Date(cloudSyncStatus.lastSyncAt))
                                        if (isArabic)
                                            "متصل ومُزامَن تلقائياً • ${cloudSyncStatus.syncedCount} ملاحظة ($time)"
                                        else
                                            "Synced in real-time • ${cloudSyncStatus.syncedCount} notes ($time)"
                                    }
                                    is CloudSyncStatus.Error ->
                                        if (isArabic) cloudSyncStatus.messageAr else cloudSyncStatus.messageEn
                                    CloudSyncStatus.Disconnected ->
                                        if (isArabic) "في انتظار الاتصال" else "Connecting..."
                                }
                                Text(
                                    text = statusText,
                                    fontSize = 11.5.sp,
                                    color = if (cloudSyncStatus is CloudSyncStatus.Error) Color(0xFFF43F5E) else Color(0xFF10B981),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onSyncNow,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("auth_sync_now_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0EA5E9),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "مزامنة الآن" else "Sync Now",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("auth_sign_out_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF43F5E).copy(alpha = 0.85f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "تسجيل الخروج" else "Sign Out",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Signed-out login / signup flow
                        // Primary Google Sign-In Liquid Glass button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_sign_in_button")
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6))
                                    )
                                )
                                .clickable(enabled = !isGoogleLoading && !isEmailLoading) {
                                    localError = null
                                    val activity = context.findActivity()
                                    val webClientId = FirebaseSyncRepository.getWebClientId(context)
                                    if (!FirebaseSyncRepository.isFirebaseAvailable(context)) {
                                        localError = if (isArabic) {
                                            "لم يتم العثور على إعدادات google-services.json في هذه النسخة."
                                        } else {
                                            "Firebase google-services.json is not configured in this build."
                                        }
                                        return@clickable
                                    }
                                    if (activity == null) {
                                        localError = if (isArabic) "تعذر فتح نافذة تسجيل الدخول" else "Unable to launch Google Sign-In"
                                        return@clickable
                                    }
                                    if (webClientId.isBlank()) {
                                        localError = if (isArabic) {
                                            "معرّف default_web_client_id غير متوفر في google-services.json. فعّل تسجيل الدخول بجوجل في Firebase Console."
                                        } else {
                                            "default_web_client_id missing from google-services.json. Enable Google Sign-In in Firebase Console."
                                        }
                                        return@clickable
                                    }

                                    isGoogleLoading = true
                                    scope.launch {
                                        try {
                                            val credentialManager = CredentialManager.create(activity)
                                            val googleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                                            val request = GetCredentialRequest.Builder()
                                                .addCredentialOption(googleOption)
                                                .build()
                                            val response = credentialManager.getCredential(
                                                request = request,
                                                context = activity
                                            )
                                            val credential = response.credential
                                            if (credential is CustomCredential &&
                                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                            ) {
                                                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                                                isGoogleLoading = false
                                                onGoogleIdToken(googleCred.idToken)
                                            } else {
                                                isGoogleLoading = false
                                                localError = if (isArabic) "نوع الاعتماد غير مدعوم" else "Unexpected credential type"
                                            }
                                        } catch (_: GetCredentialCancellationException) {
                                            isGoogleLoading = false
                                        } catch (_: NoCredentialException) {
                                            isGoogleLoading = false
                                            localError = if (isArabic) {
                                                "لا يوجد حساب Google مسجّل على الجهاز أو أضف SHA-1 في Firebase Console"
                                            } else {
                                                "No Google account available or SHA-1 missing in Firebase Console"
                                            }
                                        } catch (e: GetCredentialException) {
                                            isGoogleLoading = false
                                            localError = e.localizedMessage ?: "Google Sign-In failed"
                                        } catch (e: Exception) {
                                            isGoogleLoading = false
                                            localError = e.localizedMessage ?: "Google Sign-In failed"
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGoogleLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.4.dp,
                                    color = Color.White
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "G",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isArabic) "المتابعة بحساب Google" else "Continue with Google",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = metaColor.copy(alpha = 0.25f))
                            Text(
                                text = if (isArabic) "  أو بالبريد الإلكتروني  " else "  OR EMAIL  ",
                                fontSize = 11.sp,
                                color = metaColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = metaColor.copy(alpha = 0.25f))
                        }

                        // Mode Switch Tabs (Sign In / Create Account)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color(0xFFF1F5F9))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(false to (if (isArabic) "تسجيل الدخول" else "Sign In"),
                                true to (if (isArabic) "إنشاء حساب" else "Create Account")
                            ).forEach { (signUpTab, label) ->
                                val selected = isSignUpMode == signUpTab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) Color(0xFF38BDF8).copy(alpha = 0.22f) else Color.Transparent
                                        )
                                        .clickable {
                                            isSignUpMode = signUpTab
                                            localError = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color(0xFF38BDF8) else metaColor
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                localError = null
                            },
                            label = { Text(if (isArabic) "البريد الإلكتروني" else "Email") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF38BDF8))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = metaColor.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input")
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                localError = null
                            },
                            label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFC084FC))
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = metaColor
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = Color(0xFFC084FC),
                                unfocusedBorderColor = metaColor.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input")
                        )

                        AnimatedVisibility(visible = localError != null) {
                            Text(
                                text = localError.orEmpty(),
                                fontSize = 12.sp,
                                color = Color(0xFFF43F5E),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Button(
                            onClick = {
                                val trimmedEmail = email.trim()
                                if (trimmedEmail.isEmpty() || password.length < 6) {
                                    localError = if (isArabic) {
                                        "يرجى إدخال بريد إلكتروني صحيح وكلمة مرور من 6 أحرف على الأقل"
                                    } else {
                                        "Enter a valid email and at least 6 characters password"
                                    }
                                    return@Button
                                }
                                if (!FirebaseSyncRepository.isFirebaseAvailable(context)) {
                                    localError = if (isArabic) {
                                        "لم يتم العثور على إعدادات google-services.json في هذه النسخة."
                                    } else {
                                        "Firebase google-services.json is not configured in this build."
                                    }
                                    return@Button
                                }

                                isEmailLoading = true
                                localError = null
                                val errorHandler: (String) -> Unit = { errMsg ->
                                    isEmailLoading = false
                                    localError = errMsg
                                }
                                if (isSignUpMode) {
                                    onEmailRegister(trimmedEmail, password, errorHandler)
                                } else {
                                    onEmailSignIn(trimmedEmail, password, errorHandler)
                                }
                            },
                            enabled = !isEmailLoading && !isGoogleLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5CF6),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("auth_email_submit_button")
                        ) {
                            if (isEmailLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = if (isSignUpMode) {
                                        if (isArabic) "إنشاء حساب ومزامنة" else "Create Account & Sync"
                                    } else {
                                        if (isArabic) "دخول بالبريد الإلكتروني" else "Sign In with Email"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
