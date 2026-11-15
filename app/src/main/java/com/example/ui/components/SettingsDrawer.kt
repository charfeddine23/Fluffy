package com.example.ui.components

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sync
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.GlassTint
import com.example.data.repository.CloudSyncStatus
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
import com.example.ui.theme.AppThemeRegistry
import com.example.ui.theme.LocalAppThemePreset
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SmoothEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private enum class SettingsPage {
    ROOT,
    ACCOUNT_CLOUD,
    APPEARANCE,
    SECURITY
}

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
    isCloudSignedIn: Boolean = false,
    cloudSyncStatus: CloudSyncStatus = CloudSyncStatus.Disconnected,
    profileDisplayName: String? = null,
    profileEmail: String? = null,
    profilePhotoUrl: String? = null,
    profileAvatarChoice: String = "",
    profileFirstName: String = "",
    profileLastName: String = "",
    onOpenProfile: () -> Unit = {},
    onOpenCloudAuth: () -> Unit = {},
    onSyncCloudNow: () -> Unit = {},
    onExportLocalBackup: (Uri) -> Unit = {},
    onImportLocalBackup: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(SettingsPage.ROOT) }

    LaunchedEffect(isOpen) {
        if (!isOpen) {
            currentPage = SettingsPage.ROOT
        }
    }

    if (isOpen) {
        BackHandler {
            if (currentPage != SettingsPage.ROOT) {
                currentPage = SettingsPage.ROOT
            } else {
                onClose()
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let(onExportLocalBackup)
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(onImportLocalBackup)
    }

    val activePreset = LocalAppThemePreset.current
    val effectiveDark = activePreset.isDark || isDark
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val lastUpdatedText = remember(context) {
        val timestamp = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).lastUpdateTime
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
            }
        }.getOrDefault(0L)
        val effectiveMs = if (timestamp > 0L) timestamp else System.currentTimeMillis()
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(effectiveMs))
    }

    // Dynamic palette derived strictly from the active AppThemePreset
    val drawerBackground by animateColorAsState(
        targetValue = activePreset.background.copy(
            alpha = if (activePreset.isGlassStyle) 0.92f else 0.98f
        ),
        animationSpec = tween(320),
        label = "drawer_surface"
    )
    val cardTileColor by animateColorAsState(
        targetValue = activePreset.surface.copy(
            alpha = when {
                activePreset.isGlassStyle && effectiveDark -> 0.68f
                activePreset.isGlassStyle && !effectiveDark -> 0.86f
                else -> 0.94f
            }
        ),
        animationSpec = tween(320),
        label = "drawer_tile"
    )
    val textColor by animateColorAsState(
        targetValue = activePreset.onSurface,
        animationSpec = tween(280),
        label = "drawer_text"
    )
    val subTextColor by animateColorAsState(
        targetValue = activePreset.onSurfaceVariant,
        animationSpec = tween(280),
        label = "drawer_subtext"
    )
    val dividerColor = if (effectiveDark) {
        activePreset.onSurface.copy(alpha = 0.09f)
    } else {
        activePreset.onSurface.copy(alpha = 0.07f)
    }

    // Active theme accent tokens
    val primaryAccent by animateColorAsState(
        targetValue = activePreset.primary,
        animationSpec = tween(320),
        label = "drawer_primary"
    )
    val secondaryAccent by animateColorAsState(
        targetValue = activePreset.secondary,
        animationSpec = tween(320),
        label = "drawer_secondary"
    )
    val tertiaryAccent by animateColorAsState(
        targetValue = activePreset.tertiary,
        animationSpec = tween(320),
        label = "drawer_tertiary"
    )

    val dragOffset = remember { Animatable(0f) }
    LaunchedEffect(isOpen) {
        if (isOpen) {
            dragOffset.snapTo(0f)
        }
    }

    // Scrim backdrop tinted softly with the active theme
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(260, easing = FastOutSlowInEasing))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    (if (effectiveDark) Color.Black else activePreset.onBackground)
                        .copy(alpha = if (effectiveDark) 0.50f else 0.22f)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        )
    }

    // Edge-Docked Flush Drawer Panel
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
                    .fillMaxWidth(0.84f)
                    .widthIn(max = 340.dp)
                    .graphicsLayer {
                        translationX = dragOffset.value
                    }
                    .liquidGlass(
                        isDark = effectiveDark,
                        tint = GlassTint.FROST,
                        shape = panelShape,
                        borderWidth = 1.4.dp,
                        customFill = drawerBackground
                    )
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    activePreset.orb1.copy(alpha = if (activePreset.isGlassStyle) 0.28f else 0.16f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.18f, size.height * 0.12f),
                                radius = size.width * 0.78f
                            ),
                            center = Offset(size.width * 0.18f, size.height * 0.12f),
                            radius = size.width * 0.78f
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    activePreset.orb2.copy(alpha = if (activePreset.isGlassStyle) 0.24f else 0.14f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.85f, size.height * 0.76f),
                                radius = size.width * 0.75f
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.76f),
                            radius = size.width * 0.75f
                        )
                    }
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
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    // Header with optional Sub-Page Back Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (currentPage != SettingsPage.ROOT) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("settings_subpage_back")
                                        .clip(CircleShape)
                                        .background(activePreset.surfaceVariant.copy(alpha = 0.72f))
                                        .border(1.dp, dividerColor, CircleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true)
                                        ) {
                                            currentPage = SettingsPage.ROOT
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = if (isArabic) "رجوع" else "Back",
                                        tint = textColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            Column {
                                Text(
                                    text = when (currentPage) {
                                        SettingsPage.ROOT -> if (isArabic) "الإعدادات" else "Settings"
                                        SettingsPage.ACCOUNT_CLOUD -> "Account & Cloud"
                                        SettingsPage.APPEARANCE -> "Appearance"
                                        SettingsPage.SECURITY -> "Security"
                                    },
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = textColor
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when (currentPage) {
                                        SettingsPage.ROOT -> "Fluffy"
                                        else -> if (isArabic) "الإعدادات" else "Settings"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = subTextColor
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(activePreset.surfaceVariant.copy(alpha = 0.72f))
                                .border(1.dp, dividerColor, CircleShape)
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

                    Spacer(modifier = Modifier.height(18.dp))

                    val currentPreset = AppThemeRegistry.resolve(selectedThemeId, isDark)

                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            val forward = targetState != SettingsPage.ROOT
                            val dir = if (forward) 1 else -1
                            (slideInHorizontally(tween(240, easing = SmoothEaseOut)) { it * dir / 3 } +
                                fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(tween(200)) { -it * dir / 4 } +
                                    fadeOut(tween(180)))
                        },
                        label = "settings_page_transition",
                        modifier = Modifier.weight(1f)
                    ) { page ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            when (page) {
                                SettingsPage.ROOT -> {
                                    // 1. Topmost Button: Account & Cloud
                                    LiquidGlassCard(
                                        isDark = effectiveDark,
                                        tint = GlassTint.FROST,
                                        shape = RoundedCornerShape(20.dp),
                                        borderWidth = 1.dp,
                                        customFill = cardTileColor,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = 56.dp)
                                                .testTag("settings_account_cloud_button")
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple(bounded = true)
                                                ) {
                                                    currentPage = SettingsPage.ACCOUNT_CLOUD
                                                }
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (isCloudSignedIn) {
                                                    ProfileAvatar(
                                                        avatarChoice = profileAvatarChoice,
                                                        photoUrl = profilePhotoUrl,
                                                        firstName = profileFirstName,
                                                        lastName = profileLastName,
                                                        displayName = profileDisplayName,
                                                        email = profileEmail,
                                                        isDark = effectiveDark,
                                                        size = 38.dp,
                                                        borderWidth = 1.5.dp,
                                                        fontSize = 13.sp
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(38.dp)
                                                            .clip(CircleShape)
                                                            .background(primaryAccent.copy(alpha = 0.14f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.ManageAccounts,
                                                            contentDescription = null,
                                                            tint = primaryAccent,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "Account & Cloud",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = textColor
                                                    )
                                                    Text(
                                                        text = if (isCloudSignedIn) {
                                                            "Cloud Sync Active"
                                                        } else {
                                                            if (isArabic) "الملف الشخصي والنسخ الاحتياطي" else "Sync, Profile & Local Backup"
                                                        },
                                                        fontSize = 11.5.sp,
                                                        color = if (isCloudSignedIn) Color(0xFF10B981) else subTextColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
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

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Ordered Core Menu: Trash -> Archives -> Appearance -> Security
                                    LiquidGlassCard(
                                        isDark = effectiveDark,
                                        tint = GlassTint.FROST,
                                        shape = RoundedCornerShape(20.dp),
                                        borderWidth = 1.dp,
                                        customFill = cardTileColor,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            // 2. Trash
                                            CalmNavigationRow(
                                                icon = Icons.Rounded.DeleteOutline,
                                                iconTint = tertiaryAccent,
                                                title = "Trash",
                                                subtitle = if (isArabic) "سلة المهملات • تحذف خلال 30 يوماً" else "Auto-deleted in 30 days",
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

                                            // 3. Archives
                                            CalmNavigationRow(
                                                icon = Icons.Rounded.Archive,
                                                iconTint = primaryAccent,
                                                title = "Archives",
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

                                            // 4. Appearance
                                            CalmNavigationRow(
                                                icon = Icons.Rounded.Palette,
                                                iconTint = secondaryAccent,
                                                title = "Appearance",
                                                subtitle = currentPreset.name,
                                                badgeCount = 0,
                                                textColor = textColor,
                                                subTextColor = subTextColor,
                                                testTag = "settings_appearance_button",
                                                onClick = {
                                                    currentPage = SettingsPage.APPEARANCE
                                                }
                                            )

                                            HorizontalDivider(
                                                color = dividerColor,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )

                                            // 5. Security
                                            CalmNavigationRow(
                                                icon = Icons.Rounded.Security,
                                                iconTint = primaryAccent,
                                                title = "Security",
                                                subtitle = if (isBiometricEnabled) {
                                                    if (isArabic) "Biometric Lock • مفعّل" else "Biometric Lock • Enabled"
                                                } else {
                                                    if (isArabic) "Biometric Lock • متوقف" else "Biometric Lock • Off"
                                                },
                                                badgeCount = 0,
                                                textColor = textColor,
                                                subTextColor = subTextColor,
                                                testTag = "settings_security_button",
                                                onClick = {
                                                    currentPage = SettingsPage.SECURITY
                                                }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Secondary Utilities (Audio Player, Language, Updates)
                                    CalmSectionLabel(
                                        title = if (isArabic) "المزيد" else "More",
                                        color = subTextColor
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    LiquidGlassCard(
                                        isDark = effectiveDark,
                                        tint = GlassTint.FROST,
                                        shape = RoundedCornerShape(20.dp),
                                        borderWidth = 1.dp,
                                        customFill = cardTileColor,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            CalmNavigationRow(
                                                icon = Icons.Rounded.GraphicEq,
                                                iconTint = secondaryAccent,
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

                                            HorizontalDivider(
                                                color = dividerColor,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )

                                            CalmSettingRow(
                                                icon = Icons.Rounded.Language,
                                                iconTint = primaryAccent,
                                                title = if (isArabic) "اللغة" else "Language",
                                                subtitle = if (isArabic) "العربية" else "English",
                                                textColor = textColor,
                                                subTextColor = subTextColor,
                                                onClick = onToggleLanguage
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Check for Updates
                                    LiquidGlassCard(
                                        isDark = effectiveDark,
                                        tint = GlassTint.FROST,
                                        shape = RoundedCornerShape(20.dp),
                                        borderWidth = 1.dp,
                                        customFill = cardTileColor,
                                        onClick = if (!isCheckingUpdates) onCheckForUpdates else null,
                                        testTag = "check_updates_button",
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = 52.dp)
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(primaryAccent.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isCheckingUpdates) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        strokeWidth = 2.dp,
                                                        color = primaryAccent
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Rounded.SystemUpdate,
                                                        contentDescription = "Check for Updates",
                                                        tint = primaryAccent,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Check for Updates",
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
                                                            if (isArabic) "تحقق من توفر تحديث جديد" else "Tap to check latest release"
                                                    },
                                                    fontSize = 11.5.sp,
                                                    color = if (hasPendingUpdate) primaryAccent else subTextColor
                                                )
                                            }

                                            if (hasPendingUpdate && !isCheckingUpdates) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(primaryAccent)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Developer, Version & Last Update Info
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("settings_developer_info")
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(cardTileColor.copy(alpha = if (effectiveDark) 0.55f else 0.72f))
                                            .border(1.dp, dividerColor, RoundedCornerShape(18.dp))
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Developed by Charfeddine Ka",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor
                                        )
                                        Text(
                                            text = if (isArabic) {
                                                "الإصدار ${BuildConfig.VERSION_NAME} • آخر تحديث $lastUpdatedText"
                                            } else {
                                                "Version ${BuildConfig.VERSION_NAME} • Last updated $lastUpdatedText"
                                            },
                                            fontSize = 11.sp,
                                            color = subTextColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                SettingsPage.ACCOUNT_CLOUD -> {
                                    // 1. Cloud Sync Active
                                    val isSyncBusy = cloudSyncStatus is CloudSyncStatus.Syncing
                                    val syncColor = if (isCloudSignedIn) Color(0xFF10B981) else primaryAccent

                                    LiquidGlassCard(
                                        isDark = effectiveDark,
                                        tint = GlassTint.FROST,
                                        shape = RoundedCornerShape(20.dp),
                                        borderWidth = 1.dp,
                                        customFill = cardTileColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("settings_cloud_sync_card")
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(syncColor.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (isCloudSignedIn) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                                                        contentDescription = null,
                                                        tint = syncColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (isCloudSignedIn) {
                                                            "Cloud Sync Active"
                                                        } else {
                                                            if (isArabic) "المزامنة السحابية غير مفعلة" else "Cloud Sync Inactive"
                                                        },
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = textColor
                                                    )
                                                    val syncSubtitle = if (isCloudSignedIn) {
                                                        when (cloudSyncStatus) {
                                                            CloudSyncStatus.Syncing ->
                                                                if (isArabic) "جاري المزامنة..." else "Syncing..."
                                                            is CloudSyncStatus.Synced -> {
                                                                val t = SimpleDateFormat("HH:mm", Locale.getDefault())
                                                                    .format(Date(cloudSyncStatus.lastSyncAt))
                                                                if (isArabic) "${cloudSyncStatus.syncedCount} ملاحظة • $t"
                                                                else "${cloudSyncStatus.syncedCount} notes • $t"
                                                            }
                                                            is CloudSyncStatus.Error ->
                                                                if (isArabic) cloudSyncStatus.messageAr else cloudSyncStatus.messageEn
                                                            CloudSyncStatus.Disconnected ->
                                                                if (isArabic) "مرتبط بالسحابة" else "Connected"
                                                        }
                                                    } else {
                                                        if (isArabic) "سجّل الدخول لتفعيل المزامنة السحابية" else "Sign in to sync across devices"
                                                    }
                                                    Text(
                                                        text = syncSubtitle,
                                                        fontSize = 11.5.sp,
                                                        color = if (cloudSyncStatus is CloudSyncStatus.Error) Color(0xFFF43F5E) else subTextColor
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .defaultMinSize(minHeight = 44.dp)
                                                    .testTag("settings_cloud_sync_action")
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(syncColor.copy(alpha = 0.14f))
                                                    .clickable(
                                                        enabled = !isSyncBusy,
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = ripple(bounded = true)
                                                    ) {
                                                        if (isCloudSignedIn) {
                                                            onSyncCloudNow()
                                                        } else {
                                                            onClose()
                                                            onOpenCloudAuth()
                                                        }
                                                    }
                                                    .padding(vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                if (isSyncBusy) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        strokeWidth = 2.dp,
                                                        color = syncColor
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = if (isCloudSignedIn) Icons.Rounded.Sync else Icons.Rounded.Login,
                                                        contentDescription = null,
                                                        tint = syncColor,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isCloudSignedIn) {
                                                        if (isArabic) "مزامنة الآن" else "Sync Now"
                                                    } else {
                                                        if (isArabic) "تسجيل الدخول للمزامنة" else "Sign In to Activate"
                                                    },
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = syncColor
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 2. Profile (الملف الشخصي)
                                    LiquidGlassCard(
                                        isDark = effectiveDark,
                                        tint = GlassTint.FROST,
                                        shape = RoundedCornerShape(20.dp),
                                        borderWidth = 1.dp,
                                        customFill = cardTileColor,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val fullProfileName = listOf(profileFirstName, profileLastName)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" ")
                                            .ifBlank { profileDisplayName?.takeIf { it.isNotBlank() } ?: profileEmail ?: "" }

                                        CalmNavigationRow(
                                            icon = Icons.Rounded.Person,
                                            iconTint = secondaryAccent,
                                            title = if (isArabic) "الملف الشخصي" else "Profile",
                                            subtitle = fullProfileName.ifBlank {
                                                if (isArabic) "تعديل الاسم وصورة الحساب" else "Manage name & avatar"
                                            },
                                            badgeCount = 0,
                                            textColor = textColor,
                                            subTextColor = subTextColor,
                                            testTag = "settings_profile_button",
                                            onClick = {
                                                onClose()
                                                onOpenProfile()
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 3. Local Backup
                                    LiquidGlassCard(
                                        isDark = effectiveDark,
                                        tint = GlassTint.FROST,
                                        shape = RoundedCornerShape(20.dp),
                                        borderWidth = 1.dp,
                                        customFill = cardTileColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("settings_local_backup_card")
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(tertiaryAccent.copy(alpha = 0.14f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.SdStorage,
                                                        contentDescription = null,
                                                        tint = tertiaryAccent,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Local Backup",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = textColor
                                                    )
                                                    Text(
                                                        text = if (isArabic) {
                                                            "حفظ واستعادة نسخة على الهاتف"
                                                        } else {
                                                            "Save & restore backup on device"
                                                        },
                                                        fontSize = 11.5.sp,
                                                        color = subTextColor
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .defaultMinSize(minHeight = 44.dp)
                                                        .testTag("settings_local_backup_export")
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                                        .clickable {
                                                            exportBackupLauncher.launch(
                                                                "Fluffy_Backup_${System.currentTimeMillis()}.json"
                                                            )
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Save,
                                                        contentDescription = null,
                                                        tint = Color(0xFF10B981),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isArabic) "حفظ نسخة" else "Save",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF10B981)
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .defaultMinSize(minHeight = 44.dp)
                                                        .testTag("settings_local_backup_import")
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(primaryAccent.copy(alpha = 0.14f))
                                                        .clickable {
                                                            importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.FolderOpen,
                                                        contentDescription = null,
                                                        tint = primaryAccent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isArabic) "استعادة" else "Restore",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = primaryAccent
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                SettingsPage.APPEARANCE -> {
                                    LiquidGlassCard(
                                        isDark = effectiveDark,
                                        tint = GlassTint.FROST,
                                        shape = RoundedCornerShape(20.dp),
                                        borderWidth = 1.dp,
                                        customFill = cardTileColor,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            // 1. Themes
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .defaultMinSize(minHeight = 56.dp)
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
                                                            .background(secondaryAccent.copy(alpha = 0.14f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Palette,
                                                            contentDescription = null,
                                                            tint = secondaryAccent,
                                                            modifier = Modifier.size(19.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = "Themes",
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
                                                                .background(dot.copy(alpha = 0.9f))
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

                                            // 2. Theme Mode
                                            CalmSettingRow(
                                                icon = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                                iconTint = primaryAccent,
                                                title = "Theme Mode",
                                                subtitle = if (isDark) {
                                                    if (isArabic) "الوضع الليلي" else "Dark Mode"
                                                } else {
                                                    if (isArabic) "الوضع النهاري" else "Light Mode"
                                                },
                                                textColor = textColor,
                                                subTextColor = subTextColor,
                                                onClick = onToggleTheme
                                            )

                                            HorizontalDivider(
                                                color = dividerColor,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )

                                            // 3. Notes Layout
                                            CalmSettingRow(
                                                icon = if (isGridView) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                                                iconTint = tertiaryAccent,
                                                title = "Notes Layout",
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
                                    }
                                }

                                SettingsPage.SECURITY -> {
                                    LiquidGlassCard(
                                        isDark = effectiveDark,
                                        tint = GlassTint.FROST,
                                        shape = RoundedCornerShape(20.dp),
                                        borderWidth = 1.dp,
                                        customFill = cardTileColor,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
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
                                                            .background(primaryAccent.copy(alpha = 0.14f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Fingerprint,
                                                            contentDescription = null,
                                                            tint = primaryAccent,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = "Biometric Lock",
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
                                                        checkedThumbColor = if (effectiveDark) activePreset.background else Color.White,
                                                        checkedTrackColor = primaryAccent,
                                                        uncheckedThumbColor = subTextColor,
                                                        uncheckedTrackColor = activePreset.surfaceVariant
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
                                                            .defaultMinSize(minHeight = 44.dp)
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .background(primaryAccent.copy(alpha = 0.12f))
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
                                                            tint = primaryAccent,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = if (isArabic) "قفل التطبيق الآن" else "Lock App Now",
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = primaryAccent
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
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
            .defaultMinSize(minHeight = 52.dp)
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
                    color = subTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
            .defaultMinSize(minHeight = 52.dp)
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
