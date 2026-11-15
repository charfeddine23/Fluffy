package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

@Stable
class AudioPlaybackController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var mediaPlayer: MediaPlayer? = null
    private var tickerJob: Job? = null

    var activeTrackId by mutableStateOf<String?>(null)
        private set

    var isPreparing by mutableStateOf(false)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var currentPositionMs by mutableLongStateOf(0L)
        private set

    var totalDurationMs by mutableLongStateOf(0L)
        private set

    var playbackSpeed by mutableFloatStateOf(1.0f)
        private set

    var lastError by mutableStateOf<String?>(null)
        private set

    var onTrackCompleted: ((trackId: String) -> Unit)? = null

    fun playOrToggle(
        trackId: String,
        sourcePathOrUri: String,
        fallbackDurationSec: Int = 0
    ) {
        lastError = null
        if (activeTrackId == trackId && mediaPlayer != null) {
            if (!isPreparing) {
                toggleCurrentPlayback()
            }
            return
        }

        if (sourcePathOrUri.isBlank()) {
            stopInternal(resetTrackId = false)
            activeTrackId = trackId
            lastError = "MISSING_FILE"
            return
        }

        val isContentUri = sourcePathOrUri.startsWith("content://")
        if (!isContentUri) {
            val file = File(sourcePathOrUri)
            if (!file.exists() || file.length() <= 0L) {
                stopInternal(resetTrackId = false)
                activeTrackId = trackId
                lastError = "MISSING_FILE"
                return
            }
        }

        stopInternal(resetTrackId = false)
        activeTrackId = trackId
        currentPositionMs = 0L
        totalDurationMs = fallbackDurationSec.coerceAtLeast(1) * 1000L
        isPreparing = true

        runCatching {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            if (isContentUri) {
                val uri = Uri.parse(sourcePathOrUri)
                val setByCtx = runCatching {
                    mp.setDataSource(context.applicationContext, uri)
                }.isSuccess
                if (!setByCtx) {
                    val pfd = context.applicationContext.contentResolver.openFileDescriptor(uri, "r")
                        ?: throw IllegalStateException("Cannot open audio URI")
                    pfd.use { descriptor ->
                        mp.setDataSource(descriptor.fileDescriptor)
                    }
                }
            } else {
                FileInputStream(File(sourcePathOrUri)).use { fis ->
                    mp.setDataSource(fis.fd)
                }
            }

            mp.setOnPreparedListener { prepared ->
                scope.launch(Dispatchers.Main.immediate) {
                    if (mediaPlayer !== prepared) {
                        runCatching { prepared.release() }
                        return@launch
                    }
                    isPreparing = false
                    requestAudioFocus()
                    val realDuration = prepared.duration.toLong()
                    totalDurationMs = if (realDuration > 0L) {
                        realDuration
                    } else {
                        fallbackDurationSec.coerceAtLeast(1) * 1000L
                    }
                    runCatching { prepared.setVolume(1.0f, 1.0f) }
                    prepared.start()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && playbackSpeed != 1.0f) {
                        runCatching {
                            prepared.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
                        }
                    }
                    isPlaying = true
                    startTicker()
                }
            }

            mp.setOnCompletionListener { completed ->
                scope.launch(Dispatchers.Main.immediate) {
                    if (mediaPlayer !== completed) return@launch
                    isPlaying = false
                    currentPositionMs = totalDurationMs
                    tickerJob?.cancel()
                    abandonAudioFocus()
                    onTrackCompleted?.invoke(trackId)
                }
            }

            mp.setOnErrorListener { errored, what, extra ->
                scope.launch(Dispatchers.Main.immediate) {
                    if (mediaPlayer !== errored) return@launch
                    isPreparing = false
                    stopInternal(resetTrackId = false)
                    activeTrackId = trackId
                    lastError = "PLAYBACK_ERROR ($what/$extra)"
                }
                true
            }

            mediaPlayer = mp
            mp.prepareAsync()
        }.onFailure { err ->
            isPreparing = false
            stopInternal(resetTrackId = false)
            activeTrackId = trackId
            lastError = err.localizedMessage ?: "PLAYBACK_ERROR"
        }
    }

    private fun toggleCurrentPlayback() {
        val mp = mediaPlayer ?: return
        runCatching {
            if (mp.isPlaying) {
                mp.pause()
                isPlaying = false
                tickerJob?.cancel()
                currentPositionMs = mp.currentPosition.toLong().coerceAtLeast(0L)
                abandonAudioFocus()
            } else {
                requestAudioFocus()
                if (totalDurationMs > 0L && currentPositionMs >= totalDurationMs - 120L) {
                    mp.seekTo(0)
                    currentPositionMs = 0L
                }
                runCatching { mp.setVolume(1.0f, 1.0f) }
                mp.start()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && playbackSpeed != 1.0f) {
                    runCatching {
                        mp.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
                    }
                }
                isPlaying = true
                startTicker()
            }
        }.onFailure { err ->
            lastError = err.localizedMessage ?: "PLAYBACK_ERROR"
            isPlaying = false
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, totalDurationMs.coerceAtLeast(1L))
        currentPositionMs = clamped
        if (!isPreparing) {
            runCatching {
                mediaPlayer?.seekTo(clamped.toInt())
            }
        }
    }

    fun cyclePlaybackSpeed() {
        val next = when (playbackSpeed) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        playbackSpeed = next
        val mp = mediaPlayer ?: return
        if (!isPreparing && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val wasPlaying = mp.isPlaying
                mp.playbackParams = PlaybackParams().setSpeed(next)
                if (!wasPlaying && mp.isPlaying) {
                    mp.pause()
                }
            }
        }
    }

    fun clearError() {
        lastError = null
    }

    fun stop() {
        stopInternal(resetTrackId = true)
    }

    private fun stopInternal(resetTrackId: Boolean) {
        tickerJob?.cancel()
        tickerJob = null
        isPreparing = false
        abandonAudioFocus()
        runCatching {
            mediaPlayer?.apply {
                try {
                    if (isPlaying) stop()
                } catch (_: Exception) {
                }
                reset()
                release()
            }
        }
        mediaPlayer = null
        isPlaying = false
        currentPositionMs = 0L
        if (resetTrackId) {
            activeTrackId = null
            totalDurationMs = 0L
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null && isPlaying && !isPreparing) {
                    runCatching {
                        currentPositionMs = mp.currentPosition.toLong().coerceAtLeast(0L)
                    }
                }
                delay(100L)
            }
        }
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                    .also { focusRequest = it }
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        }
    }

    fun release() {
        stopInternal(resetTrackId = true)
    }
}

@Composable
fun rememberAudioPlaybackController(): AudioPlaybackController {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember(context) {
        AudioPlaybackController(context.applicationContext, scope)
    }
    DisposableEffect(controller) {
        onDispose {
            controller.release()
        }
    }
    return controller
}
