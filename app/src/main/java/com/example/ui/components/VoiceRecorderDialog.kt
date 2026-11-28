package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.audio.VoiceRecorderEngine
import com.example.audio.rememberAudioPlaybackController
import com.example.data.model.GlassTint
import com.example.data.storage.AudioCloudPolicy
import com.example.data.storage.AudioStorageManager
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
import com.example.ui.theme.LocalAppThemePreset
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun VoiceRecorderDialog(
    isDark: Boolean,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSaveVoiceNote: (durationSec: Int, filePath: String, byteSize: Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playbackController = rememberAudioPlaybackController()
    val recorderEngine = remember(context) {
        VoiceRecorderEngine(context.applicationContext, scope)
    }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isFinalizing by remember { mutableStateOf(false) }
    var hasRecordedOrImported by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var secondsElapsed by remember { mutableIntStateOf(0) }
    var recordedFilePath by remember { mutableStateOf("") }
    var recordedByteSize by remember { mutableLongStateOf(0L) }
    var recorderError by remember { mutableStateOf<String?>(null) }

    val waveLevels = remember { mutableStateListOf(0.25f, 0.4f, 0.65f, 0.5f, 0.7f, 0.35f, 0.45f) }

    fun pushWaveLevel(level: Float) {
        for (i in 0 until waveLevels.size - 1) {
            waveLevels[i] = waveLevels[i + 1]
        }
        waveLevels[waveLevels.lastIndex] = level.coerceIn(0.15f, 1.0f)
    }

    fun startRealRecording() {
        recorderError = null
        playbackController.stop()
        secondsElapsed = 0
        recordedByteSize = 0L
        hasRecordedOrImported = false

        val res = recorderEngine.start(
            onAmplitude = { amp -> pushWaveLevel(amp) },
            onProgress = { sec, bytes ->
                secondsElapsed = sec
                recordedByteSize = bytes
            }
        )
        res.onSuccess { path ->
            recordedFilePath = path
            isRecording = true
        }.onFailure { err ->
            isRecording = false
            recordedFilePath = ""
            recorderError = if (isArabic) {
                "تعذر تشغيل الميكروفون: ${err.localizedMessage ?: "تأكد من السماح بالصوت"}"
            } else {
                "Unable to start microphone: ${err.localizedMessage ?: "Audio input unavailable"}"
            }
        }
    }

    suspend fun stopAndFinalizeRecording(saveAfterFinalize: Boolean = false) {
        if (!isRecording || isFinalizing) return
        isRecording = false
        isFinalizing = true
        val result = recorderEngine.stopAndFinalize()
        isFinalizing = false

        result.onSuccess { res ->
            recordedFilePath = res.filePath
            recordedByteSize = res.byteSize
            secondsElapsed = res.durationSec
            hasRecordedOrImported = true
            recorderError = null
            if (saveAfterFinalize) {
                onSaveVoiceNote(res.durationSec, res.filePath, res.byteSize)
            }
        }.onFailure { err ->
            hasRecordedOrImported = false
            recordedByteSize = 0L
            recorderError = if (isArabic) {
                "تعذر حفظ التسجيل الصوتي: ${err.localizedMessage ?: "لم يتم التقاط صوت"}"
            } else {
                "Failed to finalize recording: ${err.localizedMessage ?: "No audio captured"}"
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        permissionRequestedOnce = true
        if (granted) {
            startRealRecording()
        } else {
            recorderError = if (isArabic) {
                "صلاحية الميكروفون مطلوبة للتسجيل الصوتي المباشر"
            } else {
                "Microphone permission is required for voice recording"
            }
        }
    }

    val importAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                recorderError = null
                if (isRecording) {
                    isRecording = false
                    recorderEngine.stopAndFinalize()
                }
                playbackController.stop()
                try {
                    val info = AudioStorageManager.importAudioFromUri(context, uri)
                    recordedFilePath = info.filePathOrUri
                    recordedByteSize = info.byteSize
                    secondsElapsed = info.durationSec
                    hasRecordedOrImported = true
                } catch (e: Exception) {
                    recorderError = if (isArabic) {
                        "تعذر قراءة الملف الصوتي: ${e.localizedMessage}"
                    } else {
                        "Failed to load audio file: ${e.localizedMessage}"
                    }
                } finally {
                    isImporting = false
                }
            }
        }
    }

    // Request mic permission or auto-start recording on dialog open
    LaunchedEffect(Unit) {
        if (hasMicPermission) {
            startRealRecording()
        } else if (!permissionRequestedOnce) {
            permissionRequestedOnce = true
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playbackController.release()
            recorderEngine.cleanupSync()
        }
    }

    val themePreset = LocalAppThemePreset.current
    val primaryAccent = themePreset.primary
    val secondaryAccent = themePreset.secondary
    val tertiaryAccent = themePreset.tertiary
    val onAccentColor = if (primaryAccent.luminance() > 0.52f) Color(0xFF0F172A) else Color.White
    val textColor = themePreset.onSurface
    val metaColor = themePreset.onSurfaceVariant
    val isPreviewPlaying = playbackController.activeTrackId == "preview" && playbackController.isPlaying
    val isOverCloudLimit = AudioCloudPolicy.isAudioOverCloudLimit(recordedByteSize)

    Dialog(
        onDismissRequest = {
            recorderEngine.cleanupSync()
            playbackController.stop()
            onDismiss()
        }
    ) {
        val recorderFill = themePreset.surface.copy(alpha = 0.95f)
        LiquidGlassCard(
            isDark = isDark,
            tint = GlassTint.FROST,
            shape = RoundedCornerShape(28.dp),
            borderWidth = 1.8.dp,
            customFill = recorderFill,
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
                        text = if (isArabic) "مسجل ومرفق الصوت 🎙️" else "Voice & Audio Studio 🎙️",
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
                                indication = ripple(bounded = true)
                            ) {
                                recorderEngine.cleanupSync()
                                playbackController.stop()
                                onDismiss()
                            },
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

                Spacer(modifier = Modifier.height(18.dp))

                // Timer
                val mins = secondsElapsed / 60
                val secs = secondsElapsed % 60
                Text(
                    text = String.format(Locale.US, "%02d:%02d", mins, secs),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isRecording) primaryAccent else textColor
                )

                Text(
                    text = when {
                        isFinalizing -> if (isArabic) "جارٍ تجهيز الملف الصوتي..." else "Finalizing audio..."
                        isImporting -> if (isArabic) "جارٍ استيراد الملف الصوتي..." else "Importing audio file..."
                        !hasMicPermission && !hasRecordedOrImported ->
                            if (isArabic) "يرجى السماح بصلاحية الميكروفون أو استيراد ملف صوتي"
                            else "Allow mic permission or import an audio file"
                        isRecording ->
                            if (isArabic) "جارٍ التسجيل الصوتي المباشر..." else "Recording live audio..."
                        hasRecordedOrImported ->
                            if (isArabic) "جاهز للاستماع والحفظ • ${AudioCloudPolicy.formatByteSize(recordedByteSize, true)}"
                            else "Ready to play & attach • ${AudioCloudPolicy.formatByteSize(recordedByteSize, false)}"
                        else ->
                            if (isArabic) "اضغط على الميكروفون لبدء التسجيل" else "Tap mic to start recording"
                    },
                    fontSize = 12.sp,
                    color = metaColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (isOverCloudLimit) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isArabic) {
                                "الحجم > 900 م.ب: سيُحفظ على الهاتف فقط ولن يُخزَّن سحابياً"
                            } else {
                                "Size > 900 MB: Local device only, excluded from cloud"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF59E0B),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                val recError = recorderError
                if (!recError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = recError,
                        fontSize = 11.5.sp,
                        color = Color(0xFFF43F5E),
                        textAlign = TextAlign.Center
                    )
                }

                if (playbackController.activeTrackId == "preview" && playbackController.lastError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isArabic) {
                            "تعذر تشغيل المعاينة: ${playbackController.lastError}"
                        } else {
                            "Preview error: ${playbackController.lastError}"
                        },
                        fontSize = 11.sp,
                        color = Color(0xFFF43F5E),
                        textAlign = TextAlign.Center
                    )
                }

                if (!hasMicPermission) {
                    TextButton(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            runCatching { context.startActivity(intent) }
                        }
                    ) {
                        Text(
                            text = if (isArabic) "فتح إعدادات صلاحية الميكروفون" else "Open App Mic Settings",
                            fontSize = 11.5.sp,
                            color = secondaryAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reactive Live Waveform Visualizer
                ReactiveWaveform(
                    isActive = isRecording || isPreviewPlaying,
                    levels = waveLevels,
                    waveColors = listOf(tertiaryAccent, primaryAccent, secondaryAccent)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Control Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("stop_recording_button")
                                .clip(CircleShape)
                                .background(primaryAccent)
                                .clickable {
                                    scope.launch { stopAndFinalizeRecording(saveAfterFinalize = false) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Recording",
                                tint = onAccentColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        if (hasRecordedOrImported && recordedFilePath.isNotBlank()) {
                            // Preview Play / Pause button
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("preview_audio_button")
                                    .clip(CircleShape)
                                    .liquidGlass(
                                        isDark = isDark,
                                        tint = GlassTint.FROST,
                                        shape = CircleShape,
                                        borderWidth = 1.5.dp
                                    )
                                    .clickable {
                                        playbackController.playOrToggle(
                                            trackId = "preview",
                                            sourcePathOrUri = recordedFilePath,
                                            fallbackDurationSec = secondsElapsed
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isArabic) "تشغيل المعاينة" else "Preview",
                                    tint = primaryAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Record / Re-record Button
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("start_recording_button")
                                .clip(CircleShape)
                                .background(primaryAccent.copy(alpha = if (hasRecordedOrImported) 0.16f else 1f))
                                .clickable(enabled = !isFinalizing && !isImporting) {
                                    val grantedNow = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    hasMicPermission = grantedNow
                                    if (grantedNow) {
                                        startRealRecording()
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = if (isArabic) "تسجيل صوتي" else "Record Audio",
                                tint = if (hasRecordedOrImported) primaryAccent else onAccentColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Import audio file from phone (zero-permission SAF)
                OutlinedButton(
                    onClick = { importAudioLauncher.launch(arrayOf("audio/*")) },
                    enabled = !isImporting && !isFinalizing,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_audio_file_button")
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = secondaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isArabic) "اختيار ملف صوتي من الجهاز" else "Import Audio File from Device",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save / Attach Button
                Button(
                    onClick = {
                        playbackController.stop()
                        if (isRecording) {
                            scope.launch { stopAndFinalizeRecording(saveAfterFinalize = true) }
                        } else if (recordedFilePath.isNotBlank()) {
                            val existsOk = recordedFilePath.startsWith("content://") || File(recordedFilePath).exists()
                            if (existsOk) {
                                val duration = secondsElapsed.coerceAtLeast(1)
                                onSaveVoiceNote(duration, recordedFilePath, recordedByteSize)
                            } else {
                                recorderError = if (isArabic) {
                                    "ملف التسجيل غير متاح، جرّب التسجيل مرة أخرى"
                                } else {
                                    "Recording file not found, please record again"
                                }
                            }
                        }
                    },
                    enabled = !isImporting && !isFinalizing && (hasRecordedOrImported || isRecording),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_voice_note_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryAccent
                    )
                ) {
                    if (isFinalizing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = onAccentColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isArabic) "حفظ التسجيل في الملاحظة ✨" else "Save Recording to Note ✨",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = onAccentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ReactiveWaveform(
    isActive: Boolean,
    levels: List<Float>,
    waveColors: List<Color>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        levels.forEachIndexed { idx, level ->
            val targetDp = if (isActive) {
                (14f + level * 50f).dp
            } else {
                (14f + (idx % 3) * 6f).dp
            }
            val animatedHeight by animateDpAsState(
                targetValue = targetDp,
                animationSpec = tween(durationMillis = 100),
                label = "wave_bar_$idx"
            )
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(animatedHeight)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(colors = waveColors)
                    )
            )
        }
    }
}
