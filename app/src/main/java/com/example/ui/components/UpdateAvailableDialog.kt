package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.GlassTint
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.LocalAppThemePreset
import com.example.update.ReleaseUpdateInfo
import com.example.update.UpdateUiState
import java.util.Locale

@Composable
fun UpdateAvailableDialog(
    visible: Boolean,
    updateState: UpdateUiState,
    isDark: Boolean,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onStartDownload: (ReleaseUpdateInfo) -> Unit,
    onInstallNow: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val activeInfo: ReleaseUpdateInfo? = when (updateState) {
        is UpdateUiState.Available -> updateState.info
        is UpdateUiState.Downloading -> updateState.info
        is UpdateUiState.ReadyToInstall -> updateState.info
        is UpdateUiState.NeedsInstallPermission -> updateState.info
        is UpdateUiState.Error -> updateState.info
        else -> null
    }

    val isDownloading = updateState is UpdateUiState.Downloading

    if (visible && activeInfo != null) {
        BackHandler {
            if (!isDownloading) onDismiss()
        }
    }

    val activePreset = LocalAppThemePreset.current
    val effectiveDark = activePreset.isDark || isDark

    val textColor by animateColorAsState(activePreset.onSurface, tween(280), label = "upd_text")
    val metaColor by animateColorAsState(activePreset.onSurfaceVariant, tween(280), label = "upd_meta")
    val primaryAccent by animateColorAsState(activePreset.primary, tween(280), label = "upd_primary")
    val secondaryAccent by animateColorAsState(activePreset.secondary, tween(280), label = "upd_secondary")
    val tertiaryAccent by animateColorAsState(activePreset.tertiary, tween(280), label = "upd_tertiary")
    val cardFill by animateColorAsState(
        targetValue = activePreset.surface.copy(
            alpha = if (activePreset.isGlassStyle) 0.88f else 0.96f
        ),
        animationSpec = tween(280),
        label = "upd_surface"
    )

    // Scrim backdrop
    AnimatedVisibility(
        visible = visible && activeInfo != null,
        enter = fadeIn(tween(240)),
        exit = fadeOut(tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    (if (effectiveDark) Color.Black else activePreset.onBackground)
                        .copy(alpha = if (effectiveDark) 0.56f else 0.30f)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (!isDownloading) onDismiss() }
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible && activeInfo != null,
                enter = scaleIn(
                    initialScale = 0.86f,
                    animationSpec = spring(dampingRatio = 0.74f, stiffness = 360f)
                ) + fadeIn(tween(220)),
                exit = scaleOut(
                    targetScale = 0.90f,
                    animationSpec = tween(170)
                ) + fadeOut(tween(160))
            ) {
                val info = activeInfo ?: return@AnimatedVisibility

                val downloadProgress = (updateState as? UpdateUiState.Downloading)?.progress ?: 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = downloadProgress,
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    label = "download_progress_anim"
                )

                LiquidGlassCard(
                    isDark = effectiveDark,
                    tint = GlassTint.FROST,
                    shape = RoundedCornerShape(30.dp),
                    borderWidth = 1.6.dp,
                    customFill = cardFill,
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .widthIn(max = 368.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .testTag("update_available_dialog")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            activePreset.orb1.copy(alpha = if (activePreset.isGlassStyle) 0.22f else 0.14f),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width * 0.22f, size.height * 0.14f),
                                        radius = size.width * 0.72f
                                    ),
                                    center = Offset(size.width * 0.22f, size.height * 0.14f),
                                    radius = size.width * 0.72f
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            activePreset.orb2.copy(alpha = if (activePreset.isGlassStyle) 0.18f else 0.12f),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width * 0.82f, size.height * 0.84f),
                                        radius = size.width * 0.68f
                                    ),
                                    center = Offset(size.width * 0.82f, size.height * 0.84f),
                                    radius = size.width * 0.68f
                                )
                            }
                            .padding(horizontal = 22.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Row: Close button + compact badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("update_dialog_close")
                                    .clip(CircleShape)
                                    .clickable(
                                        enabled = !isDownloading,
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true),
                                        onClick = onDismiss
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .liquidGlass(
                                            isDark = effectiveDark,
                                            tint = GlassTint.FROST,
                                            shape = CircleShape,
                                            borderWidth = 1.dp,
                                            customFill = activePreset.surfaceVariant.copy(alpha = 0.72f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (isArabic) "إغلاق" else "Close",
                                        tint = if (isDownloading) metaColor.copy(alpha = 0.35f) else textColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Theme-aligned release pill
                            Row(
                                modifier = Modifier
                                    .liquidGlass(
                                        isDark = effectiveDark,
                                        tint = GlassTint.FROST,
                                        shape = CircleShape,
                                        borderWidth = 1.dp,
                                        customFill = activePreset.surfaceVariant.copy(alpha = 0.72f)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = secondaryAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Fluffy OTA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Animated Concentric Rocket Emblem matching active theme colors
                        LiquidGlassRocketBadge(
                            isDark = effectiveDark,
                            preset = activePreset,
                            primaryAccent = primaryAccent,
                            secondaryAccent = secondaryAccent,
                            tertiaryAccent = tertiaryAccent,
                            isDownloading = isDownloading,
                            progress = animatedProgress
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Headline styled in active theme
                        Text(
                            text = when (updateState) {
                                is UpdateUiState.Downloading ->
                                    if (isArabic) "جاري تنزيل التحديث" else "Downloading Update"
                                is UpdateUiState.ReadyToInstall ->
                                    if (isArabic) "التحديث جاهز للتثبيت" else "Ready to Install"
                                is UpdateUiState.NeedsInstallPermission ->
                                    if (isArabic) "تفعيل صلاحية التثبيت" else "Allow App Installs"
                                else ->
                                    if (isArabic) "تحديث جديد متاح" else "New Update Available"
                            },
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Version Transition Pill tinted with active theme primary color
                        val sizeText = if (info.apkSizeBytes > 0L) {
                            String.format(Locale.US, " • %.1f MB", info.apkSizeBytes / (1024f * 1024f))
                        } else ""

                        Box(
                            modifier = Modifier
                                .liquidGlass(
                                    isDark = effectiveDark,
                                    tint = GlassTint.FROST,
                                    shape = CircleShape,
                                    borderWidth = 1.2.dp,
                                    customFill = primaryAccent.copy(alpha = if (effectiveDark) 0.16f else 0.12f)
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}  ➔  v${info.versionName}$sizeText",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryAccent
                            )
                        }

                        // Dynamic State Content (progress bar, permission prompt, or error)
                        when (updateState) {
                            is UpdateUiState.Downloading -> {
                                Spacer(modifier = Modifier.height(14.dp))
                                val downloadedMb = updateState.downloadedBytes / (1024f * 1024f)
                                val totalMb = (updateState.totalBytes.takeIf { it > 0L } ?: info.apkSizeBytes) / (1024f * 1024f)
                                val percent = (animatedProgress * 100).toInt().coerceIn(0, 100)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Smooth Theme-Matched Progress Bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(12.dp)
                                            .clip(CircleShape)
                                            .background(activePreset.surfaceVariant.copy(alpha = 0.72f))
                                            .border(
                                                width = 0.8.dp,
                                                color = activePreset.onSurface.copy(alpha = 0.14f),
                                                shape = CircleShape
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(animatedProgress.coerceAtLeast(0.03f))
                                                .fillMaxHeight()
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            primaryAccent,
                                                            secondaryAccent,
                                                            tertiaryAccent
                                                        )
                                                    )
                                                )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (totalMb > 0f) {
                                                String.format(Locale.US, "%.1f / %.1f MB", downloadedMb, totalMb)
                                            } else {
                                                String.format(Locale.US, "%.1f MB", downloadedMb)
                                            },
                                            fontSize = 12.sp,
                                            color = metaColor
                                        )
                                        Text(
                                            text = "$percent%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryAccent
                                        )
                                    }
                                }
                            }

                            is UpdateUiState.NeedsInstallPermission -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                FrostedInfoBox(
                                    isDark = effectiveDark,
                                    fill = activePreset.surfaceVariant.copy(alpha = 0.68f),
                                    text = if (isArabic)
                                        "يرجى تفعيل خيار «السماح بتثبيت التطبيقات من هذا المصدر» لمتابعة التحديث مباشرة."
                                    else
                                        "Please enable \"Allow from this source\" to install the update directly.",
                                    textColor = metaColor
                                )
                            }

                            is UpdateUiState.Error -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                FrostedInfoBox(
                                    isDark = effectiveDark,
                                    fill = activePreset.surfaceVariant.copy(alpha = 0.68f),
                                    text = if (isArabic) updateState.messageAr else updateState.messageEn,
                                    textColor = Color(0xFFF43F5E)
                                )
                            }

                            else -> Unit
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Theme-Matched CTA Pill Button
                        val buttonColors = when (updateState) {
                            is UpdateUiState.ReadyToInstall -> listOf(primaryAccent, secondaryAccent)
                            is UpdateUiState.NeedsInstallPermission -> listOf(tertiaryAccent, secondaryAccent)
                            is UpdateUiState.Error -> listOf(Color(0xFFF43F5E), secondaryAccent)
                            else -> listOf(primaryAccent, secondaryAccent, tertiaryAccent)
                        }
                        val avgLuminance = buttonColors.map { it.luminance() }.average()
                        val ctaForeground = if (avgLuminance > 0.52) Color(0xFF0F172A) else Color.White

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(
                                    elevation = if (isDownloading) 0.dp else 10.dp,
                                    shape = RoundedCornerShape(26.dp),
                                    ambientColor = buttonColors.first().copy(alpha = 0.42f),
                                    spotColor = buttonColors.last().copy(alpha = 0.42f)
                                )
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = if (isDownloading) {
                                            buttonColors.map { it.copy(alpha = 0.55f) }
                                        } else {
                                            buttonColors
                                        }
                                    )
                                )
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.48f),
                                            Color.White.copy(alpha = 0.12f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(26.dp)
                                )
                                .clickable(
                                    enabled = !isDownloading,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = ctaForeground),
                                    onClick = {
                                        when (updateState) {
                                            is UpdateUiState.Available,
                                            is UpdateUiState.Error -> onStartDownload(info)
                                            is UpdateUiState.ReadyToInstall -> onInstallNow()
                                            is UpdateUiState.NeedsInstallPermission -> onRequestPermission()
                                            else -> Unit
                                        }
                                    }
                                )
                                .testTag("update_dialog_action_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = updateState::class.simpleName ?: "",
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "cta_text_transition"
                            ) { _ ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val ctaIcon = when (updateState) {
                                        is UpdateUiState.ReadyToInstall -> Icons.Default.DownloadDone
                                        is UpdateUiState.NeedsInstallPermission -> Icons.Default.Security
                                        is UpdateUiState.Error -> Icons.Default.Refresh
                                        else -> Icons.Default.RocketLaunch
                                    }
                                    Icon(
                                        imageVector = ctaIcon,
                                        contentDescription = null,
                                        tint = ctaForeground,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Text(
                                        text = when (updateState) {
                                            is UpdateUiState.Downloading ->
                                                if (isArabic) "جاري التحميل..." else "Downloading..."
                                            is UpdateUiState.ReadyToInstall ->
                                                if (isArabic) "تثبيت التحديث الآن" else "Install Update"
                                            is UpdateUiState.NeedsInstallPermission ->
                                                if (isArabic) "منح الصلاحية والتثبيت" else "Allow & Install"
                                            is UpdateUiState.Error ->
                                                if (isArabic) "إعادة المحاولة" else "Retry Update"
                                            else ->
                                                if (isArabic) "تحديث الآن" else "Update Now"
                                        },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ctaForeground
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

@Composable
private fun FrostedInfoBox(
    isDark: Boolean,
    fill: Color,
    text: String,
    textColor: Color
) {
    LiquidGlassCard(
        isDark = isDark,
        tint = GlassTint.FROST,
        shape = RoundedCornerShape(18.dp),
        borderWidth = 1.dp,
        customFill = fill,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 112.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            color = textColor,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun LiquidGlassRocketBadge(
    isDark: Boolean,
    preset: AppThemePreset,
    primaryAccent: Color,
    secondaryAccent: Color,
    tertiaryAccent: Color,
    isDownloading: Boolean,
    progress: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rocket_anim")

    val hoverOffset by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isDownloading) 550 else 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rocket_hover"
    )

    val ringPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_pulse"
    )

    val flameFlicker by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_flicker"
    )

    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_twinkle"
    )

    Box(
        modifier = Modifier.size(124.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = ringPulse
                    scaleY = ringPulse
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f

            // Soft aurora glow behind medallion tinted with active theme orbs
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        preset.orb1.copy(alpha = 0.36f),
                        preset.orb2.copy(alpha = 0.24f),
                        preset.orb3.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // Outer concentric chromatic ring using active theme accents
            val ringRadius = outerRadius * 0.82f
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryAccent.copy(alpha = 0.72f),
                        secondaryAccent.copy(alpha = 0.78f),
                        tertiaryAccent.copy(alpha = 0.72f),
                        primaryAccent.copy(alpha = 0.72f)
                    )
                ),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner frosted disc using active theme surface/background
            val innerRadius = outerRadius * 0.68f
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        preset.surfaceVariant.copy(alpha = 0.88f),
                        preset.background.copy(alpha = 0.92f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                radius = innerRadius,
                center = center
            )
            drawCircle(
                color = primaryAccent.copy(alpha = 0.48f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Sweep circular progress arc while downloading
            if (isDownloading) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryAccent,
                            secondaryAccent,
                            tertiaryAccent,
                            primaryAccent
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0.02f, 1f),
                    useCenter = false,
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                    size = Size(ringRadius * 2f, ringRadius * 2f),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Twinkling micro stars inside the medallion
            drawCircle(
                color = tertiaryAccent.copy(alpha = starTwinkle),
                radius = 2.2.dp.toPx(),
                center = Offset(center.x - innerRadius * 0.56f, center.y - innerRadius * 0.38f)
            )
            drawCircle(
                color = primaryAccent.copy(alpha = (1.2f - starTwinkle * 0.6f).coerceIn(0f, 1f)),
                radius = 2.0.dp.toPx(),
                center = Offset(center.x + innerRadius * 0.58f, center.y - innerRadius * 0.28f)
            )
            drawCircle(
                color = secondaryAccent.copy(alpha = starTwinkle),
                radius = 1.8.dp.toPx(),
                center = Offset(center.x + innerRadius * 0.45f, center.y + innerRadius * 0.48f)
            )
        }

        // Stylized Floating Rocket drawn in Canvas using active theme accents
        Canvas(
            modifier = Modifier
                .size(68.dp)
                .graphicsLayer {
                    translationY = hoverOffset.dp.toPx()
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val w = size.width
            val h = size.height

            // Thruster Plasma Flame
            val flameHeight = h * 0.24f * flameFlicker
            val flamePath = Path().apply {
                moveTo(cx - w * 0.085f, cy + h * 0.20f)
                lineTo(cx, cy + h * 0.20f + flameHeight)
                lineTo(cx + w * 0.085f, cy + h * 0.20f)
                close()
            }
            drawPath(
                path = flamePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tertiaryAccent,
                        secondaryAccent,
                        primaryAccent.copy(alpha = 0.22f)
                    )
                )
            )

            // Left Wing Fin
            val leftFin = Path().apply {
                moveTo(cx - w * 0.13f, cy + h * 0.03f)
                lineTo(cx - w * 0.29f, cy + h * 0.22f)
                lineTo(cx - w * 0.11f, cy + h * 0.18f)
                close()
            }
            drawPath(
                path = leftFin,
                brush = Brush.linearGradient(
                    colors = listOf(primaryAccent, secondaryAccent)
                )
            )

            // Right Wing Fin
            val rightFin = Path().apply {
                moveTo(cx + w * 0.13f, cy + h * 0.03f)
                lineTo(cx + w * 0.29f, cy + h * 0.22f)
                lineTo(cx + w * 0.11f, cy + h * 0.18f)
                close()
            }
            drawPath(
                path = rightFin,
                brush = Brush.linearGradient(
                    colors = listOf(secondaryAccent, tertiaryAccent)
                )
            )

            // Rocket Fuselage Body
            val bodyPath = Path().apply {
                moveTo(cx, cy - h * 0.36f)
                cubicTo(
                    cx + w * 0.19f, cy - h * 0.16f,
                    cx + w * 0.17f, cy + h * 0.08f,
                    cx + w * 0.11f, cy + h * 0.20f
                )
                lineTo(cx - w * 0.11f, cy + h * 0.20f)
                cubicTo(
                    cx - w * 0.17f, cy + h * 0.08f,
                    cx - w * 0.19f, cy - h * 0.16f,
                    cx, cy - h * 0.36f
                )
                close()
            }
            drawPath(
                path = bodyPath,
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White,
                            preset.onSurface.copy(alpha = 0.92f),
                            primaryAccent.copy(alpha = 0.35f)
                        )
                    } else {
                        listOf(
                            Color.White,
                            preset.surfaceVariant,
                            primaryAccent.copy(alpha = 0.25f)
                        )
                    }
                )
            )

            // Rocket Nose Cone Accent
            val nosePath = Path().apply {
                moveTo(cx, cy - h * 0.36f)
                cubicTo(
                    cx + w * 0.08f, cy - h * 0.27f,
                    cx + w * 0.11f, cy - h * 0.20f,
                    cx + w * 0.12f, cy - h * 0.17f
                )
                lineTo(cx - w * 0.12f, cy - h * 0.17f)
                cubicTo(
                    cx - w * 0.11f, cy - h * 0.20f,
                    cx - w * 0.08f, cy - h * 0.27f,
                    cx, cy - h * 0.36f
                )
                close()
            }
            drawPath(
                path = nosePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(primaryAccent, secondaryAccent, tertiaryAccent)
                )
            )

            // Thruster Nozzle Base
            drawRoundRect(
                color = preset.onSurfaceVariant,
                topLeft = Offset(cx - w * 0.08f, cy + h * 0.18f),
                size = Size(w * 0.16f, h * 0.045f),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Glowing Porthole Window
            drawCircle(
                color = preset.background,
                radius = w * 0.095f,
                center = Offset(cx, cy - h * 0.02f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryAccent, primaryAccent),
                    center = Offset(cx - w * 0.02f, cy - h * 0.04f),
                    radius = w * 0.085f
                ),
                radius = w * 0.072f,
                center = Offset(cx, cy - h * 0.02f)
            )
            // Porthole specular glint
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = w * 0.022f,
                center = Offset(cx - w * 0.025f, cy - h * 0.045f)
            )
        }
    }
}
