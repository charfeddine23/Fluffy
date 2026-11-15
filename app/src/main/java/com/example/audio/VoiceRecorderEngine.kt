package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import com.example.data.storage.AudioStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.abs

data class RecordedAudioResult(
    val filePath: String,
    val byteSize: Long,
    val durationSec: Int
)

/**
 * Reliable audio recording engine using PCM AudioRecord -> RIFF WAV (primary, never suffers
 * Stagefright moov-atom corruption or <1s stop crashes across devices/emulators) with
 * automatic MediaRecorder AAC fallback if AudioRecord initialization is unavailable.
 */
class VoiceRecorderEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var audioRecord: AudioRecord? = null
    private var fallbackMediaRecorder: MediaRecorder? = null
    private var recordingJob: Job? = null
    private var amplitudeTickerJob: Job? = null

    @Volatile
    private var isRecordingFlag = false
    private var outputFile: File? = null
    private var pcmBytesWritten: Long = 0L
    private var activeSampleRate: Int = 16000
    private var usingMediaRecorderFallback = false
    private var startedAtMs: Long = 0L

    @SuppressLint("MissingPermission")
    fun start(
        onAmplitude: (Float) -> Unit,
        onProgress: (seconds: Int, bytes: Long) -> Unit
    ): Result<String> {
        cleanupSync()

        // Try AudioRecord PCM -> .wav first
        val candidateSources = intArrayOf(
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION
        )
        val candidateRates = intArrayOf(44100, 16000, 8000)

        for (source in candidateSources) {
            for (rate in candidateRates) {
                val minBuf = AudioRecord.getMinBufferSize(
                    rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuf <= 0) continue
                val bufferSize = (minBuf * 2).coerceAtLeast(4096)
                val record = runCatching {
                    AudioRecord(
                        source,
                        rate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )
                }.getOrNull() ?: continue

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    runCatching { record.release() }
                    continue
                }

                val wavFile = AudioStorageManager.createNewRecordingFile(context, "wav")
                val started = runCatching {
                    record.startRecording()
                    record.recordingState == AudioRecord.RECORDSTATE_RECORDING
                }.getOrDefault(false)

                if (!started) {
                    runCatching { record.release() }
                    runCatching { wavFile.delete() }
                    continue
                }

                audioRecord = record
                outputFile = wavFile
                activeSampleRate = rate
                pcmBytesWritten = 0L
                usingMediaRecorderFallback = false
                isRecordingFlag = true
                startedAtMs = System.currentTimeMillis()

                recordingJob = scope.launch(Dispatchers.IO) {
                    writePcmToWav(record, wavFile, bufferSize, rate, onAmplitude, onProgress)
                }
                return Result.success(wavFile.absolutePath)
            }
        }

        // Fallback: MediaRecorder AAC mono 16kHz -> .m4a
        val m4aFile = AudioStorageManager.createNewRecordingFile(context, "m4a")
        val mrResult = runCatching {
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(16_000)
                setAudioEncodingBitRate(64_000)
                setOutputFile(m4aFile.absolutePath)
                prepare()
                start()
            }
            fallbackMediaRecorder = mr
            outputFile = m4aFile
            usingMediaRecorderFallback = true
            isRecordingFlag = true
            startedAtMs = System.currentTimeMillis()
            amplitudeTickerJob = scope.launch {
                var lastSec = -1
                while (isActive && isRecordingFlag) {
                    val rawAmp = runCatching { fallbackMediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                    onAmplitude((rawAmp / 22000f).coerceIn(0.15f, 1.0f))
                    val sec = ((System.currentTimeMillis() - startedAtMs) / 1000L).toInt().coerceAtLeast(0)
                    if (sec != lastSec) {
                        lastSec = sec
                        val len = if (m4aFile.exists()) m4aFile.length() else 0L
                        onProgress(sec, len)
                    }
                    kotlinx.coroutines.delay(100L)
                }
            }
            m4aFile.absolutePath
        }

        return mrResult.onFailure {
            runCatching { m4aFile.delete() }
            cleanupSync()
        }
    }

    private suspend fun writePcmToWav(
        record: AudioRecord,
        wavFile: File,
        bufferBytes: Int,
        sampleRate: Int,
        onAmplitude: (Float) -> Unit,
        onProgress: (seconds: Int, bytes: Long) -> Unit
    ) {
        val sampleShorts = ShortArray(bufferBytes / 2)
        val byteBuffer = ByteArray(sampleShorts.size * 2)
        var totalWritten = 0L
        var lastEmitAt = 0L
        var lastSecondReported = -1

        try {
            FileOutputStream(wavFile).use { fos ->
                // Placeholder 44-byte RIFF WAV header
                fos.write(createWavHeader(0L, sampleRate, 1, 16))
                fos.flush()

                while (isRecordingFlag) {
                    val readCount = runCatching {
                        record.read(sampleShorts, 0, sampleShorts.size)
                    }.getOrDefault(-1)

                    if (readCount > 0) {
                        var peak = 0
                        for (i in 0 until readCount) {
                            val s = sampleShorts[i].toInt()
                            val a = abs(s)
                            if (a > peak) peak = a
                            byteBuffer[i * 2] = (s and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
                        }
                        val chunkBytes = readCount * 2
                        fos.write(byteBuffer, 0, chunkBytes)
                        totalWritten += chunkBytes
                        pcmBytesWritten = totalWritten

                        val now = System.currentTimeMillis()
                        if (now - lastEmitAt >= 90L) {
                            lastEmitAt = now
                            val norm = (peak / 18000f).coerceIn(0.15f, 1.0f)
                            val elapsedSec = ((now - startedAtMs) / 1000L).toInt().coerceAtLeast(0)
                            withContext(Dispatchers.Main) {
                                onAmplitude(norm)
                                if (elapsedSec != lastSecondReported) {
                                    lastSecondReported = elapsedSec
                                    onProgress(elapsedSec, totalWritten + 44L)
                                }
                            }
                        }
                    } else if (readCount == 0) {
                        kotlinx.coroutines.delay(10L)
                    } else {
                        break
                    }
                }
                fos.flush()
            }
        } finally {
            patchWavHeader(wavFile, totalWritten, sampleRate, 1, 16)
        }
    }

    suspend fun stopAndFinalize(): Result<RecordedAudioResult> = withContext(Dispatchers.IO) {
        isRecordingFlag = false
        val file = outputFile
            ?: return@withContext Result.failure(IllegalStateException("No active recording"))

        if (!usingMediaRecorderFallback) {
            runCatching { audioRecord?.stop() }
            runCatching { recordingJob?.join() }
            recordingJob = null
            runCatching { audioRecord?.release() }
            audioRecord = null

            val totalSize = if (file.exists()) file.length() else 0L
            if (totalSize <= 44L) {
                return@withContext Result.failure(IllegalStateException("Recorded audio is empty"))
            }
            val elapsedSec = ((System.currentTimeMillis() - startedAtMs + 500L) / 1000L)
                .toInt()
                .coerceAtLeast(1)
            val computed = AudioStorageManager.computeDurationSec(context, file.absolutePath)
            val finalDuration = maxOf(elapsedSec, computed).coerceAtLeast(1)
            Result.success(
                RecordedAudioResult(
                    filePath = file.absolutePath,
                    byteSize = totalSize,
                    durationSec = finalDuration
                )
            )
        } else {
            amplitudeTickerJob?.cancel()
            amplitudeTickerJob = null
            val stopErr = runCatching {
                fallbackMediaRecorder?.stop()
            }.exceptionOrNull()
            runCatching {
                fallbackMediaRecorder?.reset()
                fallbackMediaRecorder?.release()
            }
            fallbackMediaRecorder = null

            val totalSize = if (file.exists()) file.length() else 0L
            if (stopErr != null || totalSize <= 0L) {
                runCatching { file.delete() }
                return@withContext Result.failure(stopErr ?: IllegalStateException("MediaRecorder failed"))
            }
            val elapsedSec = ((System.currentTimeMillis() - startedAtMs + 500L) / 1000L)
                .toInt()
                .coerceAtLeast(1)
            val computed = AudioStorageManager.computeDurationSec(context, file.absolutePath)
            Result.success(
                RecordedAudioResult(
                    filePath = file.absolutePath,
                    byteSize = totalSize,
                    durationSec = maxOf(elapsedSec, computed).coerceAtLeast(1)
                )
            )
        }
    }

    fun cleanupSync() {
        isRecordingFlag = false
        amplitudeTickerJob?.cancel()
        amplitudeTickerJob = null
        recordingJob?.cancel()
        recordingJob = null
        runCatching {
            audioRecord?.apply {
                try {
                    stop()
                } catch (_: Exception) {
                }
                release()
            }
        }
        audioRecord = null
        runCatching {
            fallbackMediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {
                }
                reset()
                release()
            }
        }
        fallbackMediaRecorder = null
    }

    private fun patchWavHeader(
        file: File,
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        if (!file.exists() || file.length() < 44L) return
        val header = createWavHeader(totalAudioLen, sampleRate, channels, bitsPerSample)
        runCatching {
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(0L)
                raf.write(header)
            }
        }
    }

    private fun createWavHeader(
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val totalDataLen = totalAudioLen + 36L
        val byteRate = sampleRate.toLong() * channels * (bitsPerSample / 8)
        val blockAlign = (channels * (bitsPerSample / 8)).toShort()

        val header = ByteArray(44)
        // "RIFF"
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        writeIntLE(header, 4, totalDataLen.toInt())
        // "WAVE"
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        // "fmt "
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        writeIntLE(header, 16, 16) // PCM subchunk size
        writeShortLE(header, 20, 1) // AudioFormat = 1 (PCM)
        writeShortLE(header, 22, channels.toShort())
        writeIntLE(header, 24, sampleRate)
        writeIntLE(header, 28, byteRate.toInt())
        writeShortLE(header, 32, blockAlign)
        writeShortLE(header, 34, bitsPerSample.toShort())
        // "data"
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        writeIntLE(header, 40, totalAudioLen.toInt())
        return header
    }

    private fun writeIntLE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buf[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buf[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(buf: ByteArray, offset: Int, value: Short) {
        val v = value.toInt()
        buf[offset] = (v and 0xFF).toByte()
        buf[offset + 1] = ((v shr 8) and 0xFF).toByte()
    }
}
