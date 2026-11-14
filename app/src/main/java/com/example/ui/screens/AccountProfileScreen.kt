package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlassTint
import com.example.data.repository.CloudSyncStatus
import com.example.data.repository.FirebaseUserInfo
import com.example.ui.components.AvatarPickerSheet
import com.example.ui.components.ProfileAvatar
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
import com.example.ui.theme.LocalAppThemePreset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AccountProfileScreen(
    currentUser: FirebaseUserInfo?,
    isGuestMode: Boolean,
    firstName: String,
    lastName: String,
    avatarChoice: String,
    useGoogleAccountName: Boolean,
    cloudSyncStatus: CloudSyncStatus,
    isDark: Boolean,
    isArabic: Boolean,
    onUpdateProfileName: (firstName: String, lastName: String) -> Unit,
    onToggleUseGoogleAccountName: (Boolean) -> Unit,
    onSelectCustomPhoto: (android.net.Uri) -> Unit,
    onSelectPresetAvatar: (String) -> Unit,
    onUseGooglePhoto: () -> Unit,
    onUseInitialsAvatar: () -> Unit,
    onOpenAuthDialog: () -> Unit,
    onSyncCloudNow: () -> Unit,
    onSignOutFirebase: () -> Unit,
    onExportLocalBackup: (android.net.Uri) -> Unit,
    onImportLocalBackup: (android.net.Uri) -> Unit,
    snackbarMessage: String? = null,
    onClearSnackbar: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearSnackbar()
        }
    }

    val activePreset = LocalAppThemePreset.current
    val textColor = activePreset.onBackground
    val metaColor = activePreset.onSurfaceVariant
    val isLinked = currentUser != null

    var showAvatarPicker by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    // Dynamic atmospheric lighting palette tied to Google link safety status:
    // Linked -> Emerald Green (Safe & Synced), Unlinked -> Crimson Red (Danger)
    val lightPrimary by animateColorAsState(
        targetValue = if (isLinked) Color(0xFF10B981) else Color(0xFFEF4444),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "status_light_primary"
    )
    val lightSecondary by animateColorAsState(
        targetValue = if (isLinked) Color(0xFF34D399) else Color(0xFFF43F5E),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "status_light_secondary"
    )
    val lightDeep by animateColorAsState(
        targetValue = if (isLinked) Color(0xFF059669) else Color(0xFF991B1B),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "status_light_deep"
    )

    // Continuous smooth motion transitions
    val infiniteTransition = rememberInfiniteTransition(label = "profile_luxury_lights")
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rotation"
    )
    val orbPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2.0).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_phase"
    )
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )
    val beaconScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_scale"
    )

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

    val fullDisplayName = remember(firstName, lastName, useGoogleAccountName, currentUser, isGuestMode, isArabic) {
        val googleName = currentUser?.displayName?.trim().orEmpty()
        val customFull = listOf(firstName.trim(), lastName.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
        when {
            useGoogleAccountName && googleName.isNotBlank() -> googleName
            customFull.isNotBlank() -> customFull
            googleName.isNotBlank() -> googleName
            currentUser != null -> if (isArabic) "مستخدم Fluffy" else "Fluffy User"
            else -> if (isArabic) "مستخدم ضيف" else "Guest User"
        }
    }

    FrostedBackdrop(isDark = isDark, modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dynamic Luxury Atmospheric Aurora Lighting Layer (Green when Linked, Red Danger when Unlinked)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val driftX = cos(orbPhase) * (w * 0.08f)
                val driftY = sin(orbPhase) * (h * 0.04f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            lightPrimary.copy(alpha = (if (isDark) 0.34f else 0.22f) * auraPulse.coerceAtMost(1.05f)),
                            lightDeep.copy(alpha = if (isDark) 0.14f else 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f + driftX, h * 0.24f + driftY),
                        radius = w * 0.88f
                    ),
                    radius = w * 0.95f,
                    center = Offset(w * 0.5f + driftX, h * 0.24f + driftY)
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            lightSecondary.copy(alpha = if (isDark) 0.20f else 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.2f - driftX * 0.6f, h * 0.66f),
                        radius = w * 0.65f
                    ),
                    radius = w * 0.7f,
                    center = Offset(w * 0.2f - driftX * 0.6f, h * 0.66f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Minimal Luxury Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("account_back_button")
                            .liquidGlass(
                                isDark = isDark,
                                tint = GlassTint.FROST,
                                shape = CircleShape,
                                borderWidth = 1.2.dp
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onBack
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back",
                            tint = textColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Text(
                        text = if (isArabic) "الملف الشخصي" else "Profile",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Minimal Pulsing Status Dot Pill
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(lightPrimary.copy(alpha = 0.16f))
                            .border(1.dp, lightPrimary.copy(alpha = 0.45f), CircleShape)
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(beaconScale)
                                .clip(CircleShape)
                                .background(lightSecondary)
                        )
                        Text(
                            text = if (isLinked) {
                                if (isArabic) "مرتبط" else "Linked"
                            } else {
                                if (isArabic) "غير مرتبط" else "Unlinked"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = lightSecondary
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 1. MINIMAL LUXURY ANIMATED HERO PROFILE CARD
                    AnimatedVisibility(
                        visible = contentVisible,
                        enter = fadeIn(tween(420)) + slideInVertically(
                            initialOffsetY = { -40 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .border(
                                    width = 1.6.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            lightSecondary.copy(alpha = 0.72f),
                                            lightPrimary.copy(alpha = 0.25f),
                                            activePreset.primary.copy(alpha = 0.45f),
                                            lightPrimary.copy(alpha = 0.72f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(32.dp)
                                )
                        ) {
                            LiquidGlassCard(
                                isDark = isDark,
                                tint = if (isLinked) GlassTint.EMERALD else GlassTint.FROST,
                                shape = RoundedCornerShape(32.dp),
                                borderWidth = 0.dp,
                                customFill = activePreset.surface.copy(alpha = if (isDark) 0.76f else 0.90f),
                                testTag = "account_hero_card",
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    // Soft atmospheric radial aura inside hero card
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                    ) {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    lightPrimary.copy(alpha = if (isDark) 0.28f else 0.16f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(size.width / 2f, size.height * 0.48f),
                                                radius = size.width * 0.56f
                                            ),
                                            radius = size.width * 0.56f,
                                            center = Offset(size.width / 2f, size.height * 0.48f)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Tap Circular Profile Picture -> Opens Photo/50-Avatars + Name & Surname + Google Naming Sheet
                                        Box(
                                            modifier = Modifier
                                                .size(128.dp)
                                                .testTag("profile_avatar_edit_trigger")
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) { showAvatarPicker = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Canvas(
                                                modifier = Modifier
                                                    .size(126.dp)
                                                    .scale(auraPulse)
                                            ) {
                                                drawCircle(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(
                                                            lightPrimary.copy(alpha = 0.36f),
                                                            lightSecondary.copy(alpha = 0.14f),
                                                            Color.Transparent
                                                        )
                                                    )
                                                )
                                            }

                                            Canvas(
                                                modifier = Modifier
                                                    .size(114.dp)
                                                    .graphicsLayer { rotationZ = haloRotation }
                                            ) {
                                                drawCircle(
                                                    brush = Brush.sweepGradient(
                                                        colors = listOf(
                                                            lightSecondary,
                                                            lightPrimary.copy(alpha = 0.22f),
                                                            Color.White.copy(alpha = 0.88f),
                                                            lightPrimary.copy(alpha = 0.22f),
                                                            lightSecondary
                                                        )
                                                    ),
                                                    style = Stroke(width = 3.2.dp.toPx())
                                                )
                                            }

                                            ProfileAvatar(
                                                avatarChoice = avatarChoice,
                                                photoUrl = currentUser?.photoUrl,
                                                firstName = firstName,
                                                lastName = lastName,
                                                displayName = currentUser?.displayName,
                                                email = currentUser?.email,
                                                isDark = isDark,
                                                size = 102.dp,
                                                borderWidth = 2.dp,
                                                fontSize = 33.sp,
                                                customRingBrush = Brush.linearGradient(
                                                    listOf(lightSecondary, lightPrimary)
                                                )
                                            )

                                            // Edit Photo / Profile Badge
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(5.dp)
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(lightPrimary, activePreset.primary)
                                                        )
                                                    )
                                                    .border(2.dp, activePreset.surface, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CameraAlt,
                                                    contentDescription = if (isArabic) "تعديل الصورة والاسم" else "Edit Photo & Name",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Full Display Name (clean, sleek, zero extra buttons)
                                        Text(
                                            text = fullDisplayName,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 24.sp,
                                                color = textColor,
                                                textAlign = TextAlign.Center
                                            )
                                        )

                                        val subIdentity = when {
                                            !currentUser?.email.isNullOrBlank() -> currentUser?.email.orEmpty()
                                            isGuestMode -> if (isArabic) "وضع الضيف" else "Guest Mode"
                                            else -> ""
                                        }
                                        if (subIdentity.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(5.dp))
                                            Text(
                                                text = subIdentity,
                                                fontSize = 12.5.sp,
                                                color = metaColor,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. CLOUD SYNC & LINK CARD (Clean — without "حساب Google و Firebase" title)
                    AnimatedVisibility(
                        visible = contentVisible,
                        enter = fadeIn(tween(520, delayMillis = 90)) + slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                        )
                    ) {
                        val isSyncBusy = cloudSyncStatus is CloudSyncStatus.Syncing

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .border(
                                    width = 1.4.dp,
                                    color = lightPrimary.copy(alpha = 0.42f),
                                    shape = RoundedCornerShape(26.dp)
                                )
                        ) {
                            LiquidGlassCard(
                                isDark = isDark,
                                tint = if (isLinked) GlassTint.EMERALD else GlassTint.FROST,
                                shape = RoundedCornerShape(26.dp),
                                borderWidth = 0.dp,
                                customFill = activePreset.surface.copy(alpha = if (isDark) 0.76f else 0.90f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(15.dp))
                                                .background(lightPrimary.copy(alpha = 0.18f))
                                                .border(
                                                    width = 1.dp,
                                                    color = lightPrimary.copy(alpha = 0.40f),
                                                    shape = RoundedCornerShape(15.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isLinked) Icons.Default.CloudDone else Icons.Default.GppBad,
                                                contentDescription = null,
                                                tint = lightSecondary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isLinked) {
                                                    if (isArabic) "مرتبط بالسحابة" else "Cloud Sync Active"
                                                } else {
                                                    if (isArabic) "غير مرتبط بحساب Google" else "Google Not Linked"
                                                },
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )

                                            val subText = if (isLinked) {
                                                when (cloudSyncStatus) {
                                                    CloudSyncStatus.Syncing ->
                                                        if (isArabic) "جاري المزامنة الفورية..." else "Syncing..."
                                                    is CloudSyncStatus.Synced -> {
                                                        val formatted = SimpleDateFormat("HH:mm", Locale.getDefault())
                                                            .format(Date(cloudSyncStatus.lastSyncAt))
                                                        if (isArabic)
                                                            "${cloudSyncStatus.syncedCount} ملاحظة • $formatted"
                                                        else
                                                            "${cloudSyncStatus.syncedCount} notes • $formatted"
                                                    }
                                                    is CloudSyncStatus.Error ->
                                                        if (isArabic) cloudSyncStatus.messageAr else cloudSyncStatus.messageEn
                                                    CloudSyncStatus.Disconnected ->
                                                        if (isArabic) "متصل" else "Connected"
                                                }
                                            } else {
                                                if (isArabic) "الحفظ محلي حالياً" else "Local storage only"
                                            }

                                            Text(
                                                text = subText,
                                                fontSize = 11.5.sp,
                                                color = if (cloudSyncStatus is CloudSyncStatus.Error) Color(0xFFF43F5E) else metaColor
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = metaColor.copy(alpha = 0.16f))

                                    if (!isLinked) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("firebase_auth_open_button")
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            Color(0xFFEF4444).copy(alpha = 0.22f),
                                                            Color(0xFF10B981).copy(alpha = 0.22f)
                                                        )
                                                    )
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = lightPrimary.copy(alpha = 0.45f),
                                                    shape = CircleShape
                                                )
                                                .clickable(onClick = onOpenAuthDialog)
                                                .padding(horizontal = 14.dp, vertical = 13.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Login,
                                                contentDescription = null,
                                                tint = Color(0xFF34D399),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isArabic) "ربط الحساب بـ Google" else "Link Google Account",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("firebase_sync_now_button")
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF10B981).copy(alpha = 0.18f))
                                                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.42f), CircleShape)
                                                    .clickable(enabled = !isSyncBusy, onClick = onSyncCloudNow)
                                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                if (isSyncBusy) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        strokeWidth = 2.dp,
                                                        color = Color(0xFF34D399)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Sync,
                                                        contentDescription = "Sync Now",
                                                        tint = Color(0xFF34D399),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isArabic) "مزامنة الآن" else "Sync Now",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF34D399)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("firebase_sign_out_button")
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFF43F5E).copy(alpha = 0.16f))
                                                    .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.35f), CircleShape)
                                                    .clickable(onClick = onSignOutFirebase)
                                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Logout,
                                                    contentDescription = "Sign Out",
                                                    tint = Color(0xFFF43F5E),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isArabic) "تسجيل الخروج" else "Sign Out",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFF43F5E)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. LOCAL BACKUP SECTION ("النسخ الإحتياطي المحلي" — NO Toggle Switch)
                    AnimatedVisibility(
                        visible = contentVisible,
                        enter = fadeIn(tween(600, delayMillis = 160)) + slideInVertically(
                            initialOffsetY = { 70 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                        )
                    ) {
                        LiquidGlassCard(
                            isDark = isDark,
                            tint = GlassTint.EMERALD,
                            shape = RoundedCornerShape(26.dp),
                            borderWidth = 1.4.dp,
                            customFill = activePreset.surface.copy(alpha = if (isDark) 0.76f else 0.90f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SdStorage,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isArabic) "النسخ الإحتياطي المحلي" else "Local Backup",
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                        Text(
                                            text = if (isArabic)
                                                "حفظ واستعادة نسخة احتياطية على الهاتف"
                                            else
                                                "Save & restore backup on device",
                                            fontSize = 11.5.sp,
                                            color = metaColor
                                        )
                                    }
                                }

                                HorizontalDivider(color = metaColor.copy(alpha = 0.16f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("local_backup_export_button")
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981).copy(alpha = 0.16f))
                                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), CircleShape)
                                            .clickable {
                                                exportBackupLauncher.launch("Fluffy_Backup_${System.currentTimeMillis()}.json")
                                            }
                                            .padding(horizontal = 10.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Save,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isArabic) "حفظ نسخة بالهاتف" else "Save to Phone",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("local_backup_import_button")
                                            .clip(CircleShape)
                                            .background(Color(0xFFF59E0B).copy(alpha = 0.16f))
                                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), CircleShape)
                                            .clickable {
                                                importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                            }
                                            .padding(horizontal = 10.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isArabic) "استعادة من الهاتف" else "Load Backup",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF59E0B)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Floating Liquid Glass Snackbar Feedback
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) { data ->
                LiquidGlassCard(
                    isDark = isDark,
                    tint = GlassTint.FROST,
                    shape = RoundedCornerShape(18.dp),
                    borderWidth = 1.2.dp
                ) {
                    Text(
                        text = data.visuals.message,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }

        // Profile Picture, 50 Avatars, Name & Surname & Google Naming Sheet (Opened by tapping circular profile photo)
        AvatarPickerSheet(
            visible = showAvatarPicker,
            currentAvatarChoice = avatarChoice,
            googlePhotoUrl = currentUser?.photoUrl,
            firstName = firstName,
            lastName = lastName,
            displayName = currentUser?.displayName,
            email = currentUser?.email,
            useGoogleAccountName = useGoogleAccountName,
            isGoogleLinked = isLinked,
            statusGlowColor = lightSecondary,
            isDark = isDark,
            isArabic = isArabic,
            onDismiss = { showAvatarPicker = false },
            onUpdateProfileName = onUpdateProfileName,
            onToggleUseGoogleAccountName = onToggleUseGoogleAccountName,
            onPickCustomPhoto = onSelectCustomPhoto,
            onSelectPresetAvatar = { presetId ->
                onSelectPresetAvatar(presetId)
                showAvatarPicker = false
            },
            onUseGooglePhoto = onUseGooglePhoto,
            onUseInitials = onUseInitialsAvatar
        )
    }
}
