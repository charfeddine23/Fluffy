package com.example.ui.components

import com.example.BuildConfig
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlassTint
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass

@Composable
fun SettingsDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    isDark: Boolean,
    isArabic: Boolean,
    isGridView: Boolean,
    isBiometricEnabled: Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    onLockAppNow: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleViewMode: () -> Unit,
    isCheckingUpdates: Boolean = false,
    hasPendingUpdate: Boolean = false,
    onCheckForUpdates: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isOpen) {
        BackHandler {
            onClose()
        }
    }

    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // Scrim backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.35f else 0.20f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        )
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> if (isArabic) fullWidth else -fullWidth }
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> if (isArabic) fullWidth else -fullWidth }
        ),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.85f)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 12.dp, horizontal = 12.dp)
            ) {
                val settingsDrawerFill = if (isDark) {
                    Color(0xFF0F172A).copy(alpha = 0.85f)
                } else {
                    Color(0xFFFFFFFF).copy(alpha = 0.85f)
                }

                LiquidGlassCard(
                    isDark = isDark,
                    tint = GlassTint.FROST,
                    shape = RoundedCornerShape(28.dp),
                    borderWidth = 1.8.dp,
                    customFill = settingsDrawerFill,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isArabic) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                if (isArabic) {
                                    if (dragAmount > 20f) {
                                        onClose()
                                    }
                                } else {
                                    if (dragAmount < -20f) {
                                        onClose()
                                    }
                                }
                            }
                        }
                        .testTag("settings_drawer_panel")
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .liquidGlass(
                                        isDark = isDark,
                                        tint = GlassTint.CYAN,
                                        shape = RoundedCornerShape(12.dp),
                                        borderWidth = 1.2.dp
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "✨", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "الإعدادات" else "Settings",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                )
                                Text(
                                    text = "Fluffy Liquid Glass",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = metaColor
                                    )
                                )
                            }
                        }

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true),
                                    onClick = onClose
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 1: Biometric Authentication (الأمان وبصمة الإصبع)
                    DrawerSectionHeader(
                        title = if (isArabic) "الأمان والخصوصية" else "Security & Privacy",
                        color = Color(0xFF38BDF8)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Biometric Lock Toggle
                    LiquidGlassCard(
                        isDark = isDark,
                        tint = GlassTint.CYAN,
                        shape = RoundedCornerShape(18.dp),
                        borderWidth = 1.2.dp,
                        customFill = if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f) else Color(0xFFF1F5F9).copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isArabic) "قفل التطبيق بالبصمة" else "Biometric Lock",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                        Text(
                                            text = if (isArabic)
                                                "طلب بصمة الإصبع أو رمز الجهاز عند الفتح"
                                            else
                                                "Require fingerprint / face to open app",
                                            fontSize = 11.5.sp,
                                            color = metaColor
                                        )
                                    }
                                }

                                Switch(
                                    checked = isBiometricEnabled,
                                    onCheckedChange = { onToggleBiometric(it) },
                                    modifier = Modifier.testTag("biometric_switch"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF38BDF8)
                                    )
                                )
                            }

                            if (isBiometricEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                                        .clickable {
                                            onClose()
                                            onLockAppNow()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isArabic) "قفل التطبيق الآن" else "Lock App Now",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 2: Appearance & Preferences (المظهر والتخصيص)
                    DrawerSectionHeader(
                        title = if (isArabic) "المظهر والتخصيص" else "Appearance & Preferences",
                        color = Color(0xFFC084FC)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Theme toggle option
                    SettingsRow(
                        icon = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        title = if (isArabic) "الوضع الليلي والنهاري" else "Dark / Light Mode",
                        subtitle = if (isDark) {
                            if (isArabic) "الوضع الليلي مفعّل (أجواء كوزمية)" else "Dark Theme Active"
                        } else {
                            if (isArabic) "الوضع النهاري مفعّل (زجاج لؤلؤي)" else "Light Theme Active"
                        },
                        isDark = isDark,
                        onClick = onToggleTheme
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Language toggle option
                    SettingsRow(
                        icon = Icons.Default.Language,
                        title = if (isArabic) "اللغة (Language)" else "Language (اللغة)",
                        subtitle = if (isArabic) "العربية (RTL)" else "English (LTR)",
                        isDark = isDark,
                        onClick = onToggleLanguage
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // View Mode option
                    SettingsRow(
                        icon = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                        title = if (isArabic) "طريقة عرض الملاحظات" else "Notes Layout View",
                        subtitle = if (isGridView) {
                            if (isArabic) "شبكة زجاجية موزعة (2 أعمدة)" else "Staggered Grid View"
                        } else {
                            if (isArabic) "قائمة متسلسلة عمودية" else "Single Column List"
                        },
                        isDark = isDark,
                        onClick = onToggleViewMode
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 3: Liquid Glass Presets (تقنية الزجاج السائل)
                    DrawerSectionHeader(
                        title = if (isArabic) "أطياف الزجاج السائل" else "Liquid Glass Tints",
                        color = Color(0xFFF472B6)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GlassTint.entries.forEach { tint ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(tint.accentColor)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isArabic) tint.nameAr.split(" ").first() else tint.nameEn.split(" ").first(),
                                    fontSize = 9.sp,
                                    color = metaColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // SECTION 4: About & System Info
                    DrawerSectionHeader(
                        title = if (isArabic) "عن التطبيق" else "About Fluffy",
                        color = Color(0xFF10B981)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LiquidGlassCard(
                        isDark = isDark,
                        tint = GlassTint.EMERALD,
                        shape = RoundedCornerShape(16.dp),
                        borderWidth = 1.dp,
                        customFill = if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f) else Color(0xFFF1F5F9).copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Fluffy Notes v${BuildConfig.VERSION_NAME} ✨",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isArabic)
                                    "إصدار البناء: ${BuildConfig.VERSION_CODE}"
                                else
                                    "Build Code: ${BuildConfig.VERSION_CODE}",
                                fontSize = 11.sp,
                                color = metaColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isArabic)
                                    "تطبيق الملاحظات الذكي بتقنية Liquid Glass و Frosted Glass مع التشتت اللوني والظلال الداخلية والتشفير المحلي الكامل."
                                else
                                    "Next-gen note taking built with Liquid Glassmorphism, Chromatic Prism refraction, and local SQLite security.",
                                fontSize = 11.5.sp,
                                color = metaColor,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // In-App Update Check Row
                    LiquidGlassCard(
                        isDark = isDark,
                        tint = GlassTint.CYAN,
                        shape = RoundedCornerShape(16.dp),
                        borderWidth = 1.dp,
                        customFill = if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f) else Color(0xFFF1F5F9).copy(alpha = 0.85f),
                        onClick = { if (!isCheckingUpdates) onCheckForUpdates() },
                        testTag = "check_updates_button",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .liquidGlass(
                                        isDark = isDark,
                                        tint = GlassTint.CYAN,
                                        shape = RoundedCornerShape(12.dp),
                                        borderWidth = 1.dp
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCheckingUpdates) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF38BDF8)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = if (isArabic) "التحقق من التحديثات" else "Check for Updates",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "التحقق من التحديثات" else "Check for Updates",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                                Text(
                                    text = when {
                                        isCheckingUpdates ->
                                            if (isArabic) "جاري البحث عن إصدار جديد..." else "Checking GitHub Releases..."
                                        hasPendingUpdate ->
                                            if (isArabic) "يوجد تحديث جديد جاهز للتنزيل ✨" else "New update available ✨"
                                        else ->
                                            if (isArabic) "تحديث مباشر من داخل التطبيق (v${BuildConfig.VERSION_NAME})" else "Direct in-app OTA (v${BuildConfig.VERSION_NAME})"
                                    },
                                    fontSize = 11.sp,
                                    color = if (hasPendingUpdate) Color(0xFF38BDF8) else metaColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
}

@Composable
private fun DrawerSectionHeader(title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f) else Color(0xFFF1F5F9).copy(alpha = 0.85f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDark) Color(0xFFC084FC) else Color(0xFF8B5CF6),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = metaColor
                )
            }
        }
    }
}
