package com.example

import com.example.data.model.NoteEntity
import com.example.data.model.VaultItemEntity
import com.example.data.storage.AudioCloudPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCloudPolicyTest {

    @Test
    fun exact900MbLimit_isAllowedInCloud() {
        val exactLimit = 900L * 1024L * 1024L
        assertEquals(exactLimit, AudioCloudPolicy.MAX_CLOUD_AUDIO_BYTES)
        assertFalse(AudioCloudPolicy.isAudioOverCloudLimit(exactLimit))
        assertTrue(AudioCloudPolicy.canStoreAudioInCloud(exactLimit))
    }

    @Test
    fun exceeding900Mb_isExcludedFromCloud() {
        val overLimit = (900L * 1024L * 1024L) + 1L
        assertTrue(AudioCloudPolicy.isAudioOverCloudLimit(overLimit))
        assertFalse(AudioCloudPolicy.canStoreAudioInCloud(overLimit))
    }

    @Test
    fun noteAudioOver900Mb_cannotBeStoredInCloud() {
        val exactNote = NoteEntity(
            title = "Boundary Recording",
            audioDurationSec = 3600,
            audioByteSize = AudioCloudPolicy.MAX_CLOUD_AUDIO_BYTES
        )
        assertTrue(AudioCloudPolicy.canStoreNoteAudioInCloud(exactNote))

        val oversizedNote = NoteEntity(
            title = "Oversized Recording",
            audioDurationSec = 7200,
            audioByteSize = AudioCloudPolicy.MAX_CLOUD_AUDIO_BYTES + 1024L
        )
        assertFalse(AudioCloudPolicy.canStoreNoteAudioInCloud(oversizedNote))
    }

    @Test
    fun vaultAudioOver900Mb_isExcludedButNonAudioIsAllowed() {
        val oversizedAudioVaultMime = VaultItemEntity(
            title = "Long Podcast",
            fileName = "episode.bin",
            mimeType = "audio/mp4",
            byteSize = AudioCloudPolicy.MAX_CLOUD_AUDIO_BYTES + 500L
        )
        assertFalse(AudioCloudPolicy.canStoreVaultItemInCloud(oversizedAudioVaultMime))

        val oversizedAudioVaultExt = VaultItemEntity(
            title = "Raw Track",
            fileName = "session.wav",
            mimeType = "application/octet-stream",
            byteSize = AudioCloudPolicy.MAX_CLOUD_AUDIO_BYTES + 1L
        )
        assertFalse(AudioCloudPolicy.canStoreVaultItemInCloud(oversizedAudioVaultExt))

        val eligibleAudioVault = VaultItemEntity(
            title = "Short Voice",
            fileName = "memo.m4a",
            mimeType = "audio/m4a",
            byteSize = 15L * 1024L * 1024L
        )
        assertTrue(AudioCloudPolicy.canStoreVaultItemInCloud(eligibleAudioVault))

        val oversizedDocumentVault = VaultItemEntity(
            title = "Archive Zip",
            fileName = "backup.zip",
            mimeType = "application/zip",
            byteSize = AudioCloudPolicy.MAX_CLOUD_AUDIO_BYTES + 10_000L
        )
        assertTrue(AudioCloudPolicy.canStoreVaultItemInCloud(oversizedDocumentVault))
    }

    @Test
    fun audioFileDetection_recognizesMimeAndExtensions() {
        assertTrue(AudioCloudPolicy.isAudioFile("audio/mpeg", "file"))
        assertTrue(AudioCloudPolicy.isAudioFile("application/octet-stream", "meeting.m4a"))
        assertTrue(AudioCloudPolicy.isAudioFile("", "song.FLAC"))
        assertFalse(AudioCloudPolicy.isAudioFile("image/png", "photo.png"))
        assertFalse(AudioCloudPolicy.isAudioFile("application/pdf", "notes.pdf"))
    }
}
