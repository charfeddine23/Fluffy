package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.GlassTint
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
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

    // Scrim backdrop
    AnimatedVisibility(
        visible = visible && activeInfo != null,
        enter = fadeIn(tween(240)),
        exit = fadeOut(tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.56f else 0.34f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (!isDownloading) onDismiss() }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Stop backdrop clicks from leaking through the card
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
                val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                val cardFill = if (isDark) {
                    Color(0xFF0F172A).copy(alpha = 0.88f)
                } else {
                    Color(0xFFFFFFFF).copy(alpha = 0.88f)
                }

                val downloadProgress = (updateState as? UpdateUiState.Downloading)?.progress ?: 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = downloadProgress,
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    label = "download_progress_anim"
                )

                LiquidGlassCard(
                    isDark = isDark,
                    tint = GlassTint.CYAN,
                    shape = RoundedCornerShape(30.dp),
                    borderWidth = 1.8.dp,
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
                                            isDark = isDark,
                                            tint = GlassTint.FROST,
                                            shape = CircleShape,
                                            borderWidth = 1.dp
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

                            // Subtle live release pill
                            Row(
                                modifier = Modifier
                                    .liquidGlass(
                                        isDark = isDark,
                                        tint = GlassTint.AMETHYST,
                                        shape = CircleShape,
                                        borderWidth = 1.dp
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Fluffy OTA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Animated Concentric Rocket Emblem
                        LiquidGlassRocketBadge(
                            isDark = isDark,
                            isDownloading = isDownloading,
                            progress = animatedProgress
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Headline matching reference dialog, styled in Fluffy theme
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

                        // Version Transition Pill (e.g. v1.0.4 -> v1.0.5 • 16.1 MB)
                        val sizeText = if (info.apkSizeBytes > 0L) {
                            String.format(Locale.US, " • %.1f MB", info.apkSizeBytes / (1024f * 1024f))
                        } else ""

                        Box(
                            modifier = Modifier
                                .liquidGlass(
                                    isDark = isDark,
                                    tint = GlassTint.CYAN,
                                    shape = CircleShape,
                                    borderWidth = 1.2.dp,
                                    customFill = if (isDark) Color(0x2A06B6D4) else Color(0x1E0284C7)
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}  ➔  v${info.versionName}$sizeText",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Release Notes or Dynamic State Message
                        when (updateState) {
                            is UpdateUiState.Downloading -> {
                                val downloadedMb = updateState.downloadedBytes / (1024f * 1024f)
                                val totalMb = (updateState.totalBytes.takeIf { it > 0L } ?: info.apkSizeBytes) / (1024f * 1024f)
                                val percent = (animatedProgress * 100).toInt().coerceIn(0, 100)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Smooth Liquid Glass Progress Bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(12.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isDark) Color(0x28FFFFFF) else Color(0x180F172A)
                                            )
                                            .border(
                                                width = 0.8.dp,
                                                color = if (isDark) Color(0x35FFFFFF) else Color(0x22000000),
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
                                                            Color(0xFF06B6D4),
                                                            Color(0xFF6366F1),
                                                            Color(0xFFEC4899)
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
                                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                        )
                                    }
                                }
                            }

                            is UpdateUiState.NeedsInstallPermission -> {
                                FrostedInfoBox(
                                    isDark = isDark,
                                    tint = GlassTint.AMBER,
                                    text = if (isArabic)
                                        "يرجى تفعيل خيار «السماح بتثبيت التطبيقات من هذا المصدر» لمتابعة التحديث مباشرة دون حذف النسخة الحالية."
                                    else
                                        "Please enable \"Allow from this source\" so Fluffy can install the update directly over your existing app.",
                                    textColor = metaColor
                                )
                            }

                            is UpdateUiState.Error -> {
                                FrostedInfoBox(
                                    isDark = isDark,
                                    tint = GlassTint.ROSE,
                                    text = if (isArabic) updateState.messageAr else updateState.messageEn,
                                    textColor = Color(0xFFF43F5E)
                                )
                            }

                            else -> {
                                val notesDisplay = info.releaseNotes.ifBlank {
                                    if (isArabic)
                                        "يتضمن هذا الإصدار تحسينات في الأداء والاستقرار وواجهة الزجاج السائل، مع دعم التحديث المباشر دون فقدان الملاحظات."
                                    else
                                        "Includes performance improvements, visual polish, and direct in-place upgrade while keeping all your notes safe."
                                }
                                FrostedInfoBox(
                                    isDark = isDark,
                                    tint = GlassTint.FROST,
                                    text = notesDisplay,
                                    textColor = metaColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Signature Liquid Glass CTA Pill Button
                        val buttonColors = when (updateState) {
                            is UpdateUiState.ReadyToInstall -> listOf(Color(0xFF10B981), Color(0xFF06B6D4))
                            is UpdateUiState.NeedsInstallPermission -> listOf(Color(0xFFF59E0B), Color(0xFFEC4899))
                            is UpdateUiState.Error -> listOf(Color(0xFFF43F5E), Color(0xFF8B5CF6))
                            else -> listOf(Color(0xFF0EA5E9), Color(0xFF6366F1), Color(0xFFEC4899))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(
                                    elevation = if (isDownloading) 0.dp else 10.dp,
                                    shape = RoundedCornerShape(26.dp),
                                    ambientColor = buttonColors.first().copy(alpha = 0.45f),
                                    spotColor = buttonColors.last().copy(alpha = 0.45f)
                                )
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = if (isDownloading) {
                                            buttonColors.map { it.copy(alpha = 0.45f) }
                                        } else {
                                            buttonColors
                                        }
                                    )
                                )
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.60f),
                                            Color.White.copy(alpha = 0.12f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(26.dp)
                                )
                                .clickable(
                                    enabled = !isDownloading,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = Color.White),
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
                                        tint = Color.White,
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
                                        color = Color.White
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
    tint: GlassTint,
    text: String,
    textColor: Color
) {
    val fill = if (isDark) {
        Color(0xFF1E293B).copy(alpha = 0.55f)
    } else {
        Color(0xFFF1F5F9).copy(alpha = 0.72f)
    }

    LiquidGlassCard(
        isDark = isDark,
        tint = tint,
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

            // Soft aurora glow behind medallion
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x5538BDF8),
                        Color(0x33A855F7),
                        Color(0x18EC4899),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // Outer concentric chromatic ring
            val ringRadius = outerRadius * 0.82f
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.70f),
                        Color(0xFFC084FC).copy(alpha = 0.75f),
                        Color(0xFFF472B6).copy(alpha = 0.70f),
                        Color(0xFF38BDF8).copy(alpha = 0.70f)
                    )
                ),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner frosted disc
            val innerRadius = outerRadius * 0.68f
            drawCircle(
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(Color(0xCC1E293B), Color(0xCC0F172A))
                    } else {
                        listOf(Color(0xEEFFFFFF), Color(0xDDE0F2FE))
                    },
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                radius = innerRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.50f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Sweep circular progress arc while downloading
            if (isDownloading) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF06B6D4),
                            Color(0xFF8B5CF6),
                            Color(0xFFEC4899),
                            Color(0xFF06B6D4)
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
                color = Color(0xFFFDE047).copy(alpha = starTwinkle),
                radius = 2.2.dp.toPx(),
                center = Offset(center.x - innerRadius * 0.56f, center.y - innerRadius * 0.38f)
            )
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 1.2f - starTwinkle * 0.6f),
                radius = 2.0.dp.toPx(),
                center = Offset(center.x + innerRadius * 0.58f, center.y - innerRadius * 0.28f)
            )
            drawCircle(
                color = Color(0xFFF472B6).copy(alpha = starTwinkle),
                radius = 1.8.dp.toPx(),
                center = Offset(center.x + innerRadius * 0.45f, center.y + innerRadius * 0.48f)
            )
        }

        // Stylized Floating Liquid-Glass Rocket drawn in Canvas
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
                        Color(0xFFFDE047),
                        Color(0xFFF43F5E),
                        Color(0xFFEC4899).copy(alpha = 0.2f)
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
                    colors = listOf(Color(0xFF06B6D4), Color(0xFF6366F1))
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
                    colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
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
                        listOf(Color(0xFFFFFFFF), Color(0xFFE0F2FE), Color(0xFFC7D2FE))
                    } else {
                        listOf(Color(0xFFFFFFFF), Color(0xFFBAE6FD), Color(0xFFDDD6FE))
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
                    colors = listOf(Color(0xFF0EA5E9), Color(0xFF8B5CF6), Color(0xFFEC4899))
                )
            )

            // Thruster Nozzle Base
            drawRoundRect(
                color = Color(0xFF334155),
                topLeft = Offset(cx - w * 0.08f, cy + h * 0.18f),
                size = Size(w * 0.16f, h * 0.045f),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Glowing Porthole Window
            drawCircle(
                color = Color(0xFF0F172A),
                radius = w * 0.095f,
                center = Offset(cx, cy - h * 0.02f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF67E8F9), Color(0xFF0284C7)),
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
