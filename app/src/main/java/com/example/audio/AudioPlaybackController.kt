package com.example.audio

import android.content.Context
import android.media.AudioAttributes
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@Stable
class AudioPlaybackController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var tickerJob: Job? = null

    var activeTrackId by mutableStateOf<String?>(null)
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
            toggleCurrentPlayback()
            return
        }

        if (sourcePathOrUri.isBlank()) {
            lastError = "MISSING_FILE"
            return
        }

        val isContentUri = sourcePathOrUri.startsWith("content://")
        if (!isContentUri && !File(sourcePathOrUri).exists()) {
            lastError = "MISSING_FILE"
            return
        }

        stopInternal(resetTrackId = false)
        activeTrackId = trackId
        currentPositionMs = 0L
        totalDurationMs = fallbackDurationSec * 1000L

        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (isContentUri) {
                    setDataSource(context.applicationContext, Uri.parse(sourcePathOrUri))
                } else {
                    setDataSource(sourcePathOrUri)
                }
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && playbackSpeed != 1.0f) {
                    runCatching {
                        playbackParams = PlaybackParams().setSpeed(playbackSpeed)
                    }
                }
                setOnCompletionListener {
                    this@AudioPlaybackController.isPlaying = false
                    this@AudioPlaybackController.currentPositionMs = totalDurationMs
                    tickerJob?.cancel()
                    onTrackCompleted?.invoke(trackId)
                }
            }
            mediaPlayer = mp
            totalDurationMs = mp.duration.toLong().coerceAtLeast(fallbackDurationSec * 1000L)
            mp.start()
            isPlaying = true
            startTicker()
        }.onFailure {
            stopInternal(resetTrackId = true)
            lastError = it.localizedMessage ?: "PLAYBACK_ERROR"
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
            } else {
                if (currentPositionMs >= totalDurationMs && totalDurationMs > 0L) {
                    mp.seekTo(0)
                    currentPositionMs = 0L
                }
                mp.start()
                isPlaying = true
                startTicker()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, totalDurationMs.coerceAtLeast(1L))
        currentPositionMs = clamped
        runCatching {
            mediaPlayer?.seekTo(clamped.toInt())
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        runCatching {
            mediaPlayer?.apply {
                if (isPlaying) stop()
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
                if (mp != null && isPlaying) {
                    runCatching {
                        currentPositionMs = mp.currentPosition.toLong().coerceAtLeast(0L)
                    }
                }
                delay(120L)
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
