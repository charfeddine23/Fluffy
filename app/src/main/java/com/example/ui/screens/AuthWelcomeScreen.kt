package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.rememberGoogleSignInController
import com.example.data.model.GlassTint
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass

@Composable
fun AuthWelcomeScreen(
    isDark: Boolean,
    isArabic: Boolean,
    isAuthBusy: Boolean = false,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onGoogleIdToken: (String) -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val googleController = rememberGoogleSignInController(
        isArabic = isArabic,
        onGoogleIdToken = onGoogleIdToken
    )
    val isLoading = googleController.isLoading || isAuthBusy

    val infiniteTransition = rememberInfiniteTransition(label = "welcome_pulse")
    val heroScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_scale"
    )

    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val cardFill = if (isDark) {
        Color(0xFF0F172A).copy(alpha = 0.82f)
    } else {
        Color.White.copy(alpha = 0.86f)
    }

    FrostedBackdrop(isDark = isDark) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top bar: quick Theme & Language pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("welcome_language_toggle")
                        .liquidGlass(
                            isDark = isDark,
                            tint = GlassTint.FROST,
                            shape = RoundedCornerShape(21.dp),
                            borderWidth = 1.dp
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onToggleLanguage
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = if (isArabic) "تبديل اللغة" else "Switch Language",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isArabic) "EN" else "عربي",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("welcome_theme_toggle")
                        .liquidGlass(
                            isDark = isDark,
                            tint = GlassTint.FROST,
                            shape = CircleShape,
                            borderWidth = 1.dp
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onToggleTheme
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isArabic) "تبديل المظهر" else "Toggle Theme",
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Main Welcome Card
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 52.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LiquidGlassCard(
                    isDark = isDark,
                    tint = GlassTint.CYAN,
                    shape = RoundedCornerShape(34.dp),
                    borderWidth = 1.8.dp,
                    customFill = cardFill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp)
                        .testTag("auth_welcome_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Glowing Emblem
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .scale(heroScale)
                                .liquidGlass(
                                    isDark = isDark,
                                    tint = GlassTint.AMETHYST,
                                    shape = RoundedCornerShape(26.dp),
                                    borderWidth = 1.8.dp,
                                    customFill = if (isDark) {
                                        Color(0xFF0EA5E9).copy(alpha = 0.24f)
                                    } else {
                                        Color(0xFF38BDF8).copy(alpha = 0.20f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Fluffy",
                                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = if (isArabic) "أهلاً بك في Fluffy" else "Welcome to Fluffy",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isArabic)
                                "أنشئ حساباً عبر Google لمزامنة ملاحظاتك وخزنتك فوريّاً بين أجهزتك، أو ادخل كضيف للحفظ المحلي الكامل."
                            else
                                "Create an account with Google for instant Cloud sync across devices, or enter as a Guest for full offline phone storage.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                color = metaColor,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Feature Highlights Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FeatureHighlightPill(
                                icon = Icons.Default.CloudSync,
                                label = if (isArabic) "مزامنة Google" else "Cloud Sync",
                                accent = Color(0xFF38BDF8),
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                            FeatureHighlightPill(
                                icon = Icons.Default.Lock,
                                label = if (isArabic) "خزنة Vault" else "Safe Vault",
                                accent = Color(0xFFC084FC),
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                            FeatureHighlightPill(
                                icon = Icons.Default.PhoneAndroid,
                                label = if (isArabic) "حفظ محلي" else "Offline Ready",
                                accent = Color(0xFF10B981),
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Primary Button: Create Account / Continue with Google
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("welcome_google_sign_in_button")
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6))
                                    )
                                )
                                .clickable(
                                    enabled = !isLoading,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    googleController.startSignIn(isArabic = isArabic)
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.4.dp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isArabic) "جاري الاتصال بـ Google..." else "Connecting to Google...",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.22f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "G",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (isArabic) "إنشاء حساب / الدخول عبر Google" else "Create Account / Sign in with Google",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = googleController.errorMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = metaColor.copy(alpha = 0.22f)
                            )
                            Text(
                                text = if (isArabic) "  أو  " else "  OR  ",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = metaColor
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = metaColor.copy(alpha = 0.22f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Secondary Button: Guest Mode (الدخول كضيف)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("welcome_guest_mode_button")
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f)
                                    else Color(0xFFF1F5F9).copy(alpha = 0.95f)
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = if (isDark) Color(0xFF38BDF8).copy(alpha = 0.35f)
                                    else Color(0xFF0EA5E9).copy(alpha = 0.28f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable(
                                    enabled = !isLoading,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true),
                                    onClick = onContinueAsGuest
                                )
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonOutline,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                    modifier = Modifier.size(21.dp)
                                )
                                Text(
                                    text = if (isArabic) "الدخول كضيف (Guest Mode)" else "Continue as Guest (Guest Mode)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isArabic)
                                "في وضع الضيف تُحفظ ملاحظاتك محلياً على هاتفك دون إنشاء حساب، ويمكنك ربط حساب Google في أي وقت دون فقدان أي ملاحظة."
                            else
                                "Guest Mode saves notes locally on your device without an account. You can link a Google account later anytime without losing data.",
                            fontSize = 11.sp,
                            color = metaColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlightPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = if (isDark) 0.14f else 0.10f))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
            maxLines = 1
        )
    }
}
