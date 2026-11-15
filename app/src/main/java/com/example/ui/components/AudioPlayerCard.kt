package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlaybackController
import com.example.data.model.GlassTint
import com.example.data.storage.AudioCloudPolicy
import com.example.ui.glass.LiquidGlassCard
import java.util.Locale

@Composable
fun AudioPlayerCard(
    trackId: String,
    title: String,
    filePathOrUri: String,
    durationSec: Int,
    byteSize: Long,
    playbackController: AudioPlaybackController,
    isDark: Boolean,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    canDelete: Boolean = false,
    onDelete: (() -> Unit)? = null
) {
    val isThisTrackActive = playbackController.activeTrackId == trackId
    val isPreparing = isThisTrackActive && playbackController.isPreparing
    val isPlaying = isThisTrackActive && playbackController.isPlaying
    val isOverCloudLimit = AudioCloudPolicy.isAudioOverCloudLimit(byteSize)
    val accentColor = if (isOverCloudLimit) Color(0xFFF59E0B) else Color(0xFFEC4899)
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val totalMs = if (isThisTrackActive && playbackController.totalDurationMs > 0L) {
        playbackController.totalDurationMs
    } else {
        (durationSec.coerceAtLeast(1)) * 1000L
    }
    val currentMs = if (isThisTrackActive) playbackController.currentPositionMs else 0L
    val progress = (currentMs.toFloat() / totalMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)

    LiquidGlassCard(
        isDark = isDark,
        tint = if (isOverCloudLimit) GlassTint.AMBER else GlassTint.ROSE,
        shape = RoundedCornerShape(18.dp),
        borderWidth = 1.2.dp,
        testTag = "audio_player_card_$trackId",
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Play / Pause Circular Control
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("audio_play_button_$trackId")
                            .clip(CircleShape)
                            .background(accentColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true)
                            ) {
                                playbackController.playOrToggle(
                                    trackId = trackId,
                                    sourcePathOrUri = filePathOrUri,
                                    fallbackDurationSec = durationSec
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPreparing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) {
                                    if (isArabic) "إيقاف مؤقت" else "Pause"
                                } else {
                                    if (isArabic) "تشغيل" else "Play"
                                },
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = title.ifBlank {
                                    if (isArabic) "تسجيل صوتي" else "Voice Recording"
                                },
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (byteSize > 0L) {
                                Text(
                                    text = AudioCloudPolicy.formatByteSize(byteSize, isArabic),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = metaColor
                                )
                            }

                            // Cloud rule status badge
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (isOverCloudLimit) Color(0xFFF59E0B).copy(alpha = 0.16f)
                                        else Color(0xFF10B981).copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isOverCloudLimit) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = if (isOverCloudLimit) Color(0xFFF59E0B) else Color(0xFF10B981),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = if (isOverCloudLimit) {
                                        if (isArabic) "محلي فقط (> 900 م.ب)" else "Local Only (> 900 MB)"
                                    } else {
                                        if (isArabic) "متاح سحابياً" else "Cloud Eligible"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverCloudLimit) Color(0xFFF59E0B) else Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }

                // Speed Pill + Optional Delete Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.14f))
                            .clickable { playbackController.cyclePlaybackSpeed() }
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val speedLabel = when (playbackController.playbackSpeed) {
                            1.5f -> "1.5x"
                            2.0f -> "2x"
                            else -> "1x"
                        }
                        Text(
                            text = speedLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }

                    if (canDelete && onDelete != null) {
                        IconButton(
                            onClick = {
                                if (isThisTrackActive) playbackController.stop()
                                onDelete()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isArabic) "حذف التسجيل" else "Remove Recording",
                                tint = metaColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Seek Slider & Elapsed / Total Time
            Slider(
                value = progress,
                onValueChange = { fraction ->
                    if (isThisTrackActive) {
                        playbackController.seekTo((fraction * totalMs).toLong())
                    }
                },
                enabled = isThisTrackActive && filePathOrUri.isNotBlank(),
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = metaColor.copy(alpha = 0.28f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMillis(currentMs),
                    fontSize = 11.sp,
                    color = metaColor
                )
                Text(
                    text = formatMillis(totalMs),
                    fontSize = 11.sp,
                    color = metaColor
                )
            }

            if (isThisTrackActive && playbackController.lastError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val errMsg = playbackController.lastError!!
                Text(
                    text = when {
                        errMsg == "MISSING_FILE" -> if (isArabic) {
                            "ملف الصوت غير متوفر محلياً على هذا الجهاز"
                        } else {
                            "Audio file is not available locally on this device"
                        }
                        else -> if (isArabic) {
                            "تعذر تشغيل الصوت: $errMsg"
                        } else {
                            "Unable to play audio: $errMsg"
                        }
                    },
                    fontSize = 11.sp,
                    color = Color(0xFFF43F5E)
                )
            }
        }
    }
}

fun formatMillis(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
