package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.audio.rememberAudioPlaybackController
import com.example.data.model.GlassTint
import com.example.data.storage.AudioCloudPolicy
import com.example.data.storage.AudioStorageManager
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
import kotlinx.coroutines.delay
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

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var hasRecordedOrImported by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var secondsElapsed by remember { mutableIntStateOf(0) }
    var recordedFilePath by remember { mutableStateOf("") }
    var recordedByteSize by remember { mutableLongStateOf(0L) }
    var recorderError by remember { mutableStateOf<String?>(null) }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    val waveLevels = remember { mutableStateListOf(0.25f, 0.4f, 0.65f, 0.5f, 0.7f, 0.35f, 0.45f) }
    var currentAmplitude by remember { mutableFloatStateOf(0.2f) }

    fun releaseRecorderSafely(discardFile: Boolean) {
        runCatching {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {
                }
                reset()
                release()
            }
        }
        mediaRecorder = null
        if (discardFile && recordedFilePath.isNotBlank() && !recordedFilePath.startsWith("content://")) {
            runCatching { File(recordedFilePath).delete() }
            recordedFilePath = ""
            recordedByteSize = 0L
        }
    }

    fun startRealRecording() {
        recorderError = null
        playbackController.stop()
        releaseRecorderSafely(discardFile = false)

        val outFile = AudioStorageManager.createNewRecordingFile(context)
        val started = runCatching {
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = rec
            recordedFilePath = outFile.absolutePath
            recordedByteSize = 0L
            secondsElapsed = 0
            hasRecordedOrImported = false
            isRecording = true
        }
        started.onFailure { err ->
            runCatching { outFile.delete() }
            mediaRecorder = null
            isRecording = false
            recorderError = if (isArabic) {
                "تعذر بدء التسجيل الصوتي: ${err.localizedMessage ?: "الميكروفون غير متاح"}"
            } else {
                "Unable to start microphone: ${err.localizedMessage ?: "Mic unavailable"}"
            }
        }
    }

    fun stopRealRecording() {
        isRecording = false
        runCatching {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        }
        mediaRecorder = null
        if (recordedFilePath.isNotBlank()) {
            val file = File(recordedFilePath)
            recordedByteSize = if (file.exists()) file.length() else 0L
            val computedDur = AudioStorageManager.computeDurationSec(context, recordedFilePath)
            if (computedDur > 0) {
                secondsElapsed = maxOf(secondsElapsed, computedDur)
            }
            hasRecordedOrImported = recordedByteSize > 0L
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        permissionRequestedOnce = true
        if (granted) {
            startRealRecording()
        }
    }

    val importAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                recorderError = null
                if (isRecording) stopRealRecording()
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

    // Timer + live amplitude sampling while recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            var tickMs = 0L
            while (isRecording) {
                delay(100L)
                tickMs += 100L
                val rawAmp = runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                val normalized = (rawAmp / 22000f).coerceIn(0.15f, 1.0f)
                currentAmplitude = normalized
                for (i in 0 until waveLevels.size - 1) {
                    waveLevels[i] = waveLevels[i + 1]
                }
                waveLevels[waveLevels.lastIndex] = normalized
                if (tickMs % 1000L == 0L) {
                    secondsElapsed++
                    if (recordedFilePath.isNotBlank()) {
                        recordedByteSize = AudioStorageManager.getFileByteSize(context, recordedFilePath)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playbackController.release()
            runCatching {
                mediaRecorder?.apply {
                    try {
                        stop()
                    } catch (_: Exception) {
                    }
                    reset()
                    release()
                }
            }
            mediaRecorder = null
        }
    }

    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val isPreviewPlaying = playbackController.activeTrackId == "preview" && playbackController.isPlaying
    val isOverCloudLimit = AudioCloudPolicy.isAudioOverCloudLimit(recordedByteSize)

    Dialog(
        onDismissRequest = {
            if (isRecording) stopRealRecording()
            playbackController.stop()
            onDismiss()
        }
    ) {
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
                                if (isRecording) stopRealRecording()
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
                    color = if (isRecording) Color(0xFFEC4899) else textColor
                )

                Text(
                    text = when {
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

                if (!recorderError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = recorderError!!,
                        fontSize = 11.5.sp,
                        color = Color(0xFFF43F5E),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Reactive Live Waveform Visualizer
                ReactiveWaveform(
                    isActive = isRecording || isPreviewPlaying,
                    levels = waveLevels
                )

                Spacer(modifier = Modifier.height(22.dp))

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
                                .background(Color(0xFFEC4899))
                                .clickable { stopRealRecording() },
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
                        if (hasRecordedOrImported && recordedFilePath.isNotBlank()) {
                            // Preview Play / Pause button
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("preview_audio_button")
                                    .clip(CircleShape)
                                    .liquidGlass(
                                        isDark = isDark,
                                        tint = GlassTint.ROSE,
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
                                    tint = Color(0xFFEC4899),
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
                                .background(Color(0xFFEC4899).copy(alpha = if (hasRecordedOrImported) 0.16f else 1f))
                                .clickable {
                                    if (hasMicPermission) {
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
                                tint = if (hasRecordedOrImported) Color(0xFFEC4899) else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Import audio file from phone (zero-permission SAF)
                OutlinedButton(
                    onClick = { importAudioLauncher.launch(arrayOf("audio/*")) },
                    enabled = !isImporting,
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
                            tint = Color(0xFF38BDF8),
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
                        if (isRecording) stopRealRecording()
                        playbackController.stop()
                        val duration = secondsElapsed.coerceAtLeast(1)
                        onSaveVoiceNote(duration, recordedFilePath, recordedByteSize)
                    },
                    enabled = !isImporting && (hasRecordedOrImported || isRecording),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_voice_note_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEC4899)
                    )
                ) {
                    Text(
                        text = if (isArabic) "حفظ التسجيل في الملاحظة ✨" else "Save Recording to Note ✨",
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
private fun ReactiveWaveform(
    isActive: Boolean,
    levels: List<Float>
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
                animationSpec = tween(durationMillis = 110),
                label = "wave_bar_$idx"
            )
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(animatedHeight)
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
