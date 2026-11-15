package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.AppThemeRegistry
import com.example.ui.theme.LocalAppThemePreset
import kotlinx.coroutines.launch

private val SmoothEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

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
    selectedThemeId: String = "obsidian_liquid_glass",
    onOpenThemes: () -> Unit = {},
    trashedCount: Int = 0,
    archivedCount: Int = 0,
    audioCount: Int = 0,
    onOpenTrash: () -> Unit = {},
    onOpenArchive: () -> Unit = {},
    onOpenAudioPlayer: () -> Unit = {},
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

    val activePreset = LocalAppThemePreset.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Calm, refined surface palette
    val drawerSurface by animateColorAsState(
        targetValue = if (isDark) Color(0xFF121622) else Color(0xFFF8FAFC),
        animationSpec = tween(320),
        label = "drawer_surface"
    )
    val cardTileColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF1A2030) else Color(0xFFFFFFFF),
        animationSpec = tween(320),
        label = "drawer_tile"
    )
    val textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.06f)
    val edgeBorderColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f)

    // Muted, serene accent colors
    val calmAccent = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0284C7)
    val calmLavender = if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9)
    val calmRose = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)

    // Interactive finger-tracking horizontal swipe offset for ultra-smooth closing
    val dragOffset = remember { Animatable(0f) }
    LaunchedEffect(isOpen) {
        if (isOpen) {
            dragOffset.snapTo(0f)
        }
    }

    // Scrim backdrop with silky fade
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(260, easing = FastOutSlowInEasing))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.48f else 0.24f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        )
    }

    // Edge-Docked Flush Drawer Panel (No outer margins, attached strictly to the screen edge)
    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(
            animationSpec = tween(durationMillis = 360, easing = SmoothEaseOut),
            initialOffsetX = { fullWidth -> if (isArabic) fullWidth else -fullWidth }
        ),
        exit = slideOutHorizontally(
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            targetOffsetX = { fullWidth -> if (isArabic) fullWidth else -fullWidth }
        ),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            // Only inner corners rounded; attached edge, top, and bottom are completely flush (0.dp)
            val panelShape = RoundedCornerShape(
                topStart = 0.dp,
                bottomStart = 0.dp,
                topEnd = 28.dp,
                bottomEnd = 28.dp
            )
            val dismissThresholdPx = with(density) { 96.dp.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.82f)
                    .widthIn(max = 330.dp)
                    .graphicsLayer {
                        translationX = dragOffset.value
                    }
                    .clip(panelShape)
                    .background(drawerSurface)
                    .border(
                        width = 1.dp,
                        color = edgeBorderColor,
                        shape = panelShape
                    )
                    .pointerInput(isArabic) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    val shouldClose = if (isArabic) {
                                        dragOffset.value > dismissThresholdPx
                                    } else {
                                        dragOffset.value < -dismissThresholdPx
                                    }
                                    if (shouldClose) {
                                        onClose()
                                    } else {
                                        dragOffset.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    dragOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                scope.launch {
                                    val next = if (isArabic) {
                                        (dragOffset.value + dragAmount).coerceAtLeast(0f)
                                    } else {
                                        (dragOffset.value + dragAmount).coerceAtMost(0f)
                                    }
                                    dragOffset.snapTo(next)
                                }
                            }
                        )
                    }
                    .testTag("settings_drawer_panel")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    // 1. Minimal Calm Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "الإعدادات" else "Settings",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = textColor
                                )
                            )
                            Text(
                                text = "Fluffy",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = subTextColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color.White.copy(alpha = 0.06f)
                                    else Color.Black.copy(alpha = 0.05f)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true),
                                    onClick = onClose
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = if (isArabic) "إغلاق" else "Close",
                                tint = subTextColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // 2. Clean Folders Section (Trash & Archive)
                    CalmSectionLabel(
                        title = if (isArabic) "المكتبة" else "Library",
                        color = subTextColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardTileColor)
                            .border(1.dp, dividerColor, RoundedCornerShape(20.dp))
                    ) {
                        CalmNavigationRow(
                            icon = Icons.Rounded.DeleteOutline,
                            iconTint = calmRose,
                            title = if (isArabic) "سلة المهملات" else "Trash",
                            subtitle = if (isArabic) "تحذف تلقائياً خلال 30 يوماً" else "Auto-deleted in 30 days",
                            badgeCount = trashedCount,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            testTag = "settings_trash_button",
                            onClick = {
                                onClose()
                                onOpenTrash()
                            }
                        )

                        HorizontalDivider(
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        CalmNavigationRow(
                            icon = Icons.Rounded.Archive,
                            iconTint = calmAccent,
                            title = if (isArabic) "الأرشيف" else "Archive",
                            subtitle = if (isArabic) "الملاحظات المؤرشفة" else "Archived notes",
                            badgeCount = archivedCount,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            testTag = "settings_archive_button",
                            onClick = {
                                onClose()
                                onOpenArchive()
                            }
                        )

                        HorizontalDivider(
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        CalmNavigationRow(
                            icon = Icons.Rounded.GraphicEq,
                            iconTint = Color(0xFFEC4899),
                            title = if (isArabic) "قارئ التسجيلات الصوتية" else "Audio Recordings",
                            subtitle = if (isArabic) "تشغيل واستيراد الصوتيات" else "Play & import audio",
                            badgeCount = audioCount,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            testTag = "settings_audio_player_button",
                            onClick = {
                                onClose()
                                onOpenAudioPlayer()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // 3. Calm Security Section
                    CalmSectionLabel(
                        title = if (isArabic) "الأمان" else "Security",
                        color = subTextColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardTileColor)
                            .border(1.dp, dividerColor, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp)
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
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(calmAccent.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fingerprint,
                                        contentDescription = null,
                                        tint = calmAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) "قفل التطبيق بالبصمة" else "Biometric Lock",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor
                                    )
                                    Text(
                                        text = if (isArabic) "حماية الملاحظات عند الفتح" else "Protect notes on launch",
                                        fontSize = 11.5.sp,
                                        color = subTextColor
                                    )
                                }
                            }

                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = onToggleBiometric,
                                modifier = Modifier.testTag("biometric_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = calmAccent,
                                    uncheckedThumbColor = subTextColor,
                                    uncheckedTrackColor = if (isDark) Color(0xFF283044) else Color(0xFFE2E8F0)
                                )
                            )
                        }

                        AnimatedVisibility(
                            visible = isBiometricEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(calmAccent.copy(alpha = 0.10f))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true)
                                        ) {
                                            onClose()
                                            onLockAppNow()
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = calmAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isArabic) "قفل التطبيق الآن" else "Lock App Now",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = calmAccent
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // 4. Calm Appearance & Preferences Section
                    CalmSectionLabel(
                        title = if (isArabic) "المظهر والتفضيلات" else "Appearance",
                        color = subTextColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val currentPreset = AppThemeRegistry.resolve(selectedThemeId, isDark)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardTileColor)
                            .border(1.dp, dividerColor, RoundedCornerShape(20.dp))
                    ) {
                        // Themes Gallery
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_themes_button")
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    onClose()
                                    onOpenThemes()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(calmLavender.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Palette,
                                        contentDescription = null,
                                        tint = calmLavender,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) "الثيمات" else "Themes",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor
                                    )
                                    Text(
                                        text = currentPreset.name,
                                        fontSize = 11.5.sp,
                                        color = subTextColor
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    currentPreset.primary,
                                    currentPreset.secondary,
                                    currentPreset.tertiary
                                ).forEach { dot ->
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(dot.copy(alpha = 0.85f))
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = subTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        CalmSettingRow(
                            icon = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            iconTint = calmLavender,
                            title = if (isArabic) "المظهر" else "Theme Mode",
                            subtitle = if (isDark) {
                                if (isArabic) "الوضع الليلي الهادئ" else "Dark Mode"
                            } else {
                                if (isArabic) "الوضع النهاري الناعم" else "Light Mode"
                            },
                            textColor = textColor,
                            subTextColor = subTextColor,
                            onClick = onToggleTheme
                        )

                        HorizontalDivider(
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        CalmSettingRow(
                            icon = Icons.Rounded.Language,
                            iconTint = calmLavender,
                            title = if (isArabic) "اللغة" else "Language",
                            subtitle = if (isArabic) "العربية" else "English",
                            textColor = textColor,
                            subTextColor = subTextColor,
                            onClick = onToggleLanguage
                        )

                        HorizontalDivider(
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        CalmSettingRow(
                            icon = if (isGridView) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                            iconTint = calmLavender,
                            title = if (isArabic) "عرض الملاحظات" else "Notes Layout",
                            subtitle = if (isGridView) {
                                if (isArabic) "شبكة عمودين" else "Grid View"
                            } else {
                                if (isArabic) "قائمة عمودية" else "List View"
                            },
                            textColor = textColor,
                            subTextColor = subTextColor,
                            onClick = onToggleViewMode
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // 5. Minimal Updates & App Version
                    CalmSectionLabel(
                        title = if (isArabic) "التطبيق" else "Application",
                        color = subTextColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("check_updates_button")
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardTileColor)
                            .border(1.dp, dividerColor, RoundedCornerShape(20.dp))
                            .clickable(
                                enabled = !isCheckingUpdates,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onCheckForUpdates
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(calmAccent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCheckingUpdates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = calmAccent
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdate,
                                    contentDescription = if (isArabic) "التحقق من التحديثات" else "Check for Updates",
                                    tint = calmAccent,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "التحقق من التحديثات" else "Check for Updates",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                            Text(
                                text = when {
                                    isCheckingUpdates ->
                                        if (isArabic) "جاري الفحص..." else "Checking..."
                                    hasPendingUpdate ->
                                        if (isArabic) "يتوفر إصدار جديد" else "Update available"
                                    else ->
                                        if (isArabic) "الإصدار ${BuildConfig.VERSION_NAME}" else "Version ${BuildConfig.VERSION_NAME}"
                                },
                                fontSize = 11.5.sp,
                                color = if (hasPendingUpdate) calmAccent else subTextColor
                            )
                        }

                        if (hasPendingUpdate && !isCheckingUpdates) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(calmAccent)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Fluffy Notes • v${BuildConfig.VERSION_NAME}",
                        fontSize = 11.sp,
                        color = subTextColor.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalmSectionLabel(
    title: String,
    color: Color
) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.padding(horizontal = 6.dp)
    )
}

@Composable
private fun CalmNavigationRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badgeCount: Int,
    textColor: Color,
    subTextColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = subTextColor
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconTint
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = subTextColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CalmSettingRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = subTextColor
            )
        }
    }
}
