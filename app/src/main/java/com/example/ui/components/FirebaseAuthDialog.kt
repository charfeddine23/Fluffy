package com.example.ui.components

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.auth.rememberGoogleSignInController
import com.example.data.model.GlassTint
import com.example.data.repository.CloudSyncStatus
import com.example.data.repository.FirebaseUserInfo
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
import com.example.ui.theme.LocalAppThemePreset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FirebaseAuthDialog(
    visible: Boolean,
    currentUser: FirebaseUserInfo?,
    isGuestMode: Boolean,
    isAuthBusy: Boolean,
    cloudSyncStatus: CloudSyncStatus,
    isDark: Boolean,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onGoogleIdToken: (String) -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit
) {
    if (!visible) return

    val googleController = rememberGoogleSignInController(
        isArabic = isArabic,
        onGoogleIdToken = onGoogleIdToken
    )
    val isLoading = googleController.isLoading || isAuthBusy

    LaunchedEffect(currentUser, visible) {
        if (currentUser != null) {
            googleController.clearError()
        }
    }

    val activePreset = LocalAppThemePreset.current
    val effectiveDark = activePreset.isDark || isDark
    val textColor = activePreset.onSurface
    val metaColor = activePreset.onSurfaceVariant
    val primaryAccent = activePreset.primary
    val secondaryAccent = activePreset.secondary
    val cardFill = activePreset.surface.copy(alpha = if (activePreset.isGlassStyle) 0.90f else 0.96f)
    val accentAvgLum = listOf(primaryAccent, secondaryAccent).map { it.luminance() }.average()
    val ctaForeground = if (accentAvgLum > 0.56) Color(0xFF0F172A) else Color.White

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background((if (effectiveDark) Color.Black else activePreset.onBackground).copy(alpha = if (effectiveDark) 0.52f else 0.28f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard(
                isDark = effectiveDark,
                tint = GlassTint.FROST,
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
                                        isDark = effectiveDark,
                                        tint = GlassTint.FROST,
                                        shape = RoundedCornerShape(14.dp),
                                        borderWidth = 1.2.dp,
                                        customFill = activePreset.surfaceVariant.copy(alpha = 0.72f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.PersonOutline,
                                    contentDescription = null,
                                    tint = if (currentUser != null) Color(0xFF10B981) else primaryAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = when {
                                        currentUser != null ->
                                            if (isArabic) "حساب Google المتصل" else "Connected Google Account"
                                        isGuestMode ->
                                            if (isArabic) "أنت في وضع الضيف (Guest)" else "Guest Mode Active"
                                        else ->
                                            if (isArabic) "الحساب والمزامنة السحابية" else "Account & Cloud Sync"
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = when {
                                        currentUser != null ->
                                            if (isArabic) "مزامنة فورية تلقائية عبر Cloud Firestore" else "Real-time Cloud Firestore auto-sync"
                                        else ->
                                            if (isArabic) "اربط حساب Google للمزامنة وحفظ ملاحظاتك" else "Link your Google account to sync & protect notes"
                                    },
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
                        // Signed-in Google Account View
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    isDark = effectiveDark,
                                    tint = GlassTint.FROST,
                                    shape = RoundedCornerShape(18.dp),
                                    borderWidth = 1.dp,
                                    customFill = activePreset.surfaceVariant.copy(alpha = 0.72f)
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
                                                ?: "Google User",
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
                                        if (isArabic) "جاري الاتصال بالسحابة..." else "Connecting..."
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
                                    containerColor = primaryAccent,
                                    contentColor = ctaForeground
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
                        // Guest / Unauthenticated View: Create/Sign in with Google OR Stay/Enter Guest Mode
                        Text(
                            text = if (isArabic)
                                "عند الربط بحساب Google، سيتم إنشاء حسابك (أو الدخول لحسابك الحالي) ومزامنة جميع ملاحظاتك الموجودة على الهاتف تلقائياً دون فقدان أي شيء."
                            else
                                "Linking with Google automatically creates or opens your Cloud account and merges all existing local notes & Vault items.",
                            fontSize = 12.5.sp,
                            color = metaColor,
                            lineHeight = 18.sp
                        )

                        // Primary Google Account Creation / Sign-In Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button")
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(primaryAccent, secondaryAccent)
                                    )
                                )
                                .clickable(
                                    enabled = !isLoading,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    googleController.startSignIn(isArabic = isArabic)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.4.dp,
                                        color = ctaForeground
                                    )
                                    Text(
                                        text = if (isArabic) "جاري الاتصال بـ Google..." else "Connecting...",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ctaForeground
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(ctaForeground.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "G",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ctaForeground
                                        )
                                    }
                                    Text(
                                        text = if (isArabic) "إنشاء حساب / المتابعة عبر Google" else "Create Account / Sign in with Google",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ctaForeground
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = googleController.errorMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF43F5E).copy(alpha = 0.14f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = googleController.errorMessage.orEmpty(),
                                    fontSize = 12.sp,
                                    color = Color(0xFFF43F5E),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
