package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.GlassTint
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun VoiceRecorderDialog(
    isDark: Boolean,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSaveVoiceNote: (durationSec: Int) -> Unit
) {
    var isRecording by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var secondsElapsed by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                secondsElapsed++
            }
        }
    }

    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Dialog(onDismissRequest = onDismiss) {
        LiquidGlassCard(
            isDark = isDark,
            tint = GlassTint.ROSE,
            shape = RoundedCornerShape(28.dp),
            borderWidth = 2.dp,
            testTag = "voice_recorder_dialog"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "ملاحظة صوتية زجاجية 🎙️" else "Glass Voice Memo 🎙️",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onDismiss
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = metaColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Timer
                val mins = secondsElapsed / 60
                val secs = secondsElapsed % 60
                Text(
                    text = String.format(Locale.US, "%02d:%02d", mins, secs),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isRecording) Color(0xFFEC4899) else textColor
                )

                Text(
                    text = if (isRecording) {
                        if (isArabic) "جارٍ التسجيل الصوتي المباشر..." else "Recording voice memo..."
                    } else {
                        if (isArabic) "تم إيقاف التسجيل - جاهز للحفظ" else "Recording stopped - ready to attach"
                    },
                    fontSize = 12.sp,
                    color = metaColor,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Animated Sound Wave Visualizer Bars
                AnimatedWaveform(isRecording = isRecording, isDark = isDark)

                Spacer(modifier = Modifier.height(32.dp))

                // Record / Stop Control Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRecording) {
                        // Stop recording button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("stop_recording_button")
                                .clip(CircleShape)
                                .background(Color(0xFFEC4899))
                                .clickable { isRecording = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Recording",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        // Play / Pause Preview
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .liquidGlass(
                                    isDark = isDark,
                                    tint = GlassTint.ROSE,
                                    shape = CircleShape,
                                    borderWidth = 1.5.dp
                                )
                                .clickable { isPlaying = !isPlaying },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Preview",
                                tint = Color(0xFFEC4899),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Re-record Button
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .liquidGlass(
                                    isDark = isDark,
                                    tint = GlassTint.FROST,
                                    shape = CircleShape,
                                    borderWidth = 1.2.dp
                                )
                                .clickable {
                                    secondsElapsed = 0
                                    isRecording = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Record Again",
                                tint = metaColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save to Note Action
                Button(
                    onClick = {
                        val duration = if (secondsElapsed == 0) 5 else secondsElapsed
                        onSaveVoiceNote(duration)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_voice_note_button"),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEC4899)
                    )
                ) {
                    Text(
                        text = if (isArabic) "إدراج التسجيل في ملاحظة جديدة ✨" else "Attach to New Note ✨",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedWaveform(isRecording: Boolean, isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val waveHeights = listOf(
        infiniteTransition.animateFloat(
            initialValue = 12f, targetValue = 48f,
            animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "h1"
        ),
        infiniteTransition.animateFloat(
            initialValue = 20f, targetValue = 65f,
            animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "h2"
        ),
        infiniteTransition.animateFloat(
            initialValue = 30f, targetValue = 75f,
            animationSpec = infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "h3"
        ),
        infiniteTransition.animateFloat(
            initialValue = 15f, targetValue = 55f,
            animationSpec = infiniteRepeatable(tween(390, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "h4"
        ),
        infiniteTransition.animateFloat(
            initialValue = 25f, targetValue = 70f,
            animationSpec = infiniteRepeatable(tween(310, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "h5"
        ),
        infiniteTransition.animateFloat(
            initialValue = 10f, targetValue = 45f,
            animationSpec = infiniteRepeatable(tween(460, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "h6"
        ),
        infiniteTransition.animateFloat(
            initialValue = 18f, targetValue = 60f,
            animationSpec = infiniteRepeatable(tween(330, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "h7"
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        waveHeights.forEach { animHeight ->
            val h = if (isRecording) animHeight.value.dp else 12.dp
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(h)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF472B6),
                                Color(0xFFEC4899),
                                Color(0xFF8B5CF6)
                            )
                        )
                    )
            )
        }
    }
}
