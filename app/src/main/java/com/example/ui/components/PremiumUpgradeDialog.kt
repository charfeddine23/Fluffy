package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GlassTint
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.theme.LocalAppThemePreset

/**
 * Refined Free / Premium badge positioned beside the username on AccountProfileScreen.
 * - Free: Sleek silver-slate frosted capsule framing.
 * - Premium: Luxurious multi-stop royal gold & champagne metallic gradient frame with animated sheen.
 */
@Composable
fun AccountTierBadge(
    isPremium: Boolean,
    isDark: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePreset = LocalAppThemePreset.current
    val infiniteTransition = rememberInfiniteTransition(label = "tier_badge_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 160f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_shift"
    )

    if (isPremium) {
        val goldBorderBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFF3C4),
                Color(0xFFF59E0B),
                Color(0xFFFFE082),
                Color(0xFFD97706),
                Color(0xFFFFF8E1)
            ),
            start = Offset(shimmerOffset - 60f, 0f),
            end = Offset(shimmerOffset + 120f, 90f)
        )
        val goldFillBrush = Brush.horizontalGradient(
            colors = if (isDark) {
                listOf(
                    Color(0xFF451A03).copy(alpha = 0.92f),
                    Color(0xFF78350F).copy(alpha = 0.88f),
                    Color(0xFF451A03).copy(alpha = 0.92f)
                )
            } else {
                listOf(
                    Color(0xFFFFFBEB),
                    Color(0xFFFEF3C7),
                    Color(0xFFFDE68A)
                )
            }
        )
        val premiumTextColor = if (isDark) Color(0xFFFFE082) else Color(0xFF92400E)

        Row(
            modifier = modifier
                .minimumInteractiveComponentSize()
                .testTag("account_tier_badge")
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFFF59E0B).copy(alpha = 0.45f),
                    spotColor = Color(0xFFFBBF24).copy(alpha = 0.55f)
                )
                .clip(CircleShape)
                .background(goldFillBrush)
                .border(width = 1.6.dp, brush = goldBorderBrush, shape = CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onClick
                )
                .padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = if (isArabic) "حساب Premium" else "Premium Account",
                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Premium",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp,
                color = premiumTextColor
            )
        }
    } else {
        val freeBorderBrush = Brush.linearGradient(
            colors = if (isDark) {
                listOf(
                    Color(0xFF94A3B8).copy(alpha = 0.65f),
                    Color(0xFF475569).copy(alpha = 0.45f),
                    activePreset.primary.copy(alpha = 0.50f)
                )
            } else {
                listOf(
                    Color(0xFF64748B).copy(alpha = 0.55f),
                    Color(0xFF94A3B8).copy(alpha = 0.45f),
                    activePreset.primary.copy(alpha = 0.45f)
                )
            }
        )
        val freeFill = if (isDark) {
            Color(0xFF1E293B).copy(alpha = 0.72f)
        } else {
            Color(0xFFF1F5F9).copy(alpha = 0.92f)
        }
        val freeTextColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)

        Row(
            modifier = modifier
                .minimumInteractiveComponentSize()
                .testTag("account_tier_badge")
                .clip(CircleShape)
                .background(freeFill)
                .border(width = 1.2.dp, brush = freeBorderBrush, shape = CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onClick
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(activePreset.primary.copy(alpha = 0.85f))
            )
            Text(
                text = "Free",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                color = freeTextColor
            )
        }
    }
}

/**
 * Interactive Premium Upgrade & Redeem Code Modal Dialog.
 * Accepts exact code "Charfeddine23a" to unlock all 20 Premium backgrounds & golden account badge.
 */
@Composable
fun PremiumUpgradeDialog(
    visible: Boolean,
    isPremium: Boolean,
    isDark: Boolean,
    isArabic: Boolean,
    onRedeemCode: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val activePreset = LocalAppThemePreset.current
    val effectiveDark = activePreset.isDark || isDark
    val titleColor = if (effectiveDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val bodyColor = if (effectiveDark) Color(0xFFCBD5E1) else Color(0xFF475569)

    var codeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val goldAccent = Color(0xFFF59E0B)
    val goldBright = Color(0xFFFFD54F)
    val cardBorderBrush = Brush.linearGradient(
        colors = listOf(
            goldBright.copy(alpha = 0.85f),
            goldAccent.copy(alpha = 0.45f),
            activePreset.primary.copy(alpha = 0.55f),
            goldBright.copy(alpha = 0.85f)
        )
    )

    fun submitCode() {
        val trimmed = codeInput.trim()
        if (trimmed.isEmpty()) {
            errorMessage = if (isArabic) "يرجى إدخال كود الترقية أولاً" else "Please enter a redeem code"
            return
        }
        val accepted = onRedeemCode(trimmed)
        if (accepted) {
            errorMessage = null
            codeInput = ""
            onDismiss()
        } else {
            errorMessage = if (isArabic) {
                "كود الترقية غير صحيح، يرجى التحقق والمحاولة مرة أخرى"
            } else {
                "Invalid redeem code. Please check and try again."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .widthIn(max = 440.dp)
                .clip(RoundedCornerShape(30.dp))
                .border(width = 1.6.dp, brush = cardBorderBrush, shape = RoundedCornerShape(30.dp))
        ) {
            LiquidGlassCard(
                isDark = effectiveDark,
                tint = GlassTint.AMBER,
                shape = RoundedCornerShape(30.dp),
                borderWidth = 0.dp,
                customFill = activePreset.surface.copy(alpha = if (effectiveDark) 0.94f else 0.97f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Bar with Close button
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(36.dp)
                                .testTag("premium_dialog_close")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isArabic) "إغلاق" else "Close",
                                tint = bodyColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Crown Icon Hero
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            goldAccent.copy(alpha = 0.32f),
                                            goldBright.copy(alpha = 0.10f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(
                                    width = 1.6.dp,
                                    brush = Brush.linearGradient(listOf(goldBright, goldAccent)),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = goldBright,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    if (isPremium) {
                        Text(
                            text = if (isArabic) "وضع Premium مفعّل بالكامل" else "Premium Mode Active",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = titleColor,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isArabic) {
                                "حسابك في الوضع المدفوع الفاخر. جميع الخلفيات الحصرية الـ 20 متاحة ومفتوحة لك الآن."
                            } else {
                                "Your account has Premium access. All 20 exclusive luxury backgrounds are unlocked."
                            },
                            fontSize = 13.5.sp,
                            color = bodyColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.14f))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.42f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (isArabic) "تم فتح 20 خلفية فاخرة وإطار Premium الذهبي" else "20 Luxury Backgrounds & Gold Frame Unlocked",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (effectiveDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                            )
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = goldAccent,
                                contentColor = Color(0xFF1F1200)
                            )
                        ) {
                            Text(
                                text = if (isArabic) "متابعة" else "Continue",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else {
                        Text(
                            text = if (isArabic) "ترقية الحساب إلى Premium" else "Upgrade Account to Premium",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = titleColor,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isArabic) {
                                "أدخل كود الترقية (Redeem Code) لفتح جميع الخلفيات الـ 20 الحصرية والحصول على إطار Premium الفاخر."
                            } else {
                                "Enter your Redeem Code to unlock all 20 exclusive backgrounds and the luxury Premium badge."
                            },
                            fontSize = 13.sp,
                            color = bodyColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )

                        // Perks Summary Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(goldAccent.copy(alpha = if (effectiveDark) 0.10f else 0.08f))
                                .border(1.dp, goldAccent.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wallpaper,
                                    contentDescription = null,
                                    tint = goldBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isArabic) "فتح 20 خلفية فاخرة جديدة للمستندات" else "Unlock 20 new luxury document backgrounds",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = titleColor
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = goldBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isArabic) "إطار وشارة Premium الذهبية في ملفك الشخصي" else "Exclusive Golden Premium badge on your profile",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = titleColor
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = goldBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isArabic) "إضافة حتى 100 صفحة في المسودة الواحدة (بدل 5)" else "Add up to 100 pages per draft (instead of 5)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = titleColor
                                )
                            }
                        }

                        // Redeem Code Input Field
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = {
                                codeInput = it
                                if (errorMessage != null) errorMessage = null
                            },
                            singleLine = true,
                            label = {
                                Text(
                                    text = if (isArabic) "كود الترقية (Redeem Code)" else "Redeem Code",
                                    fontSize = 12.5.sp
                                )
                            },
                            placeholder = {
                                Text(
                                    text = if (isArabic) "أدخل كود التفعيل هنا..." else "Enter redeem code...",
                                    fontSize = 13.sp,
                                    color = bodyColor.copy(alpha = 0.6f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = goldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            isError = errorMessage != null,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submitCode() }),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = goldAccent,
                                unfocusedBorderColor = bodyColor.copy(alpha = 0.35f),
                                errorBorderColor = Color(0xFFEF4444),
                                focusedLabelColor = goldAccent,
                                unfocusedLabelColor = bodyColor,
                                cursorColor = goldAccent,
                                focusedTextColor = titleColor,
                                unfocusedTextColor = titleColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("premium_redeem_code_input")
                        )

                        AnimatedVisibility(visible = errorMessage != null) {
                            Text(
                                text = errorMessage.orEmpty(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF87171),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(0.38f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "إلغاء" else "Cancel",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = bodyColor
                                )
                            }

                            Button(
                                onClick = { submitCode() },
                                modifier = Modifier
                                    .weight(0.62f)
                                    .height(48.dp)
                                    .testTag("premium_redeem_submit_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = goldAccent,
                                    contentColor = Color(0xFF1F1200)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "تفعيل الترقية" else "Redeem Code",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
