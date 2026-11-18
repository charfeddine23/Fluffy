package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.rememberAudioPlaybackController
import com.example.data.model.GlassTint
import com.example.data.model.NoteEntity
import com.example.data.model.VaultItemEntity
import com.example.data.storage.AudioCloudPolicy
import com.example.ui.components.VoiceRecorderDialog
import com.example.ui.components.formatMillis
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AudioFilterMode {
    ALL,
    CLOUD_ELIGIBLE,
    LOCAL_ONLY_OVER_900MB
}

private data class AudioTrackItem(
    val trackId: String,
    val title: String,
    val sourceLabel: String,
    val filePathOrUri: String,
    val durationSec: Int,
    val byteSize: Long,
    val createdAt: Long,
    val noteRef: NoteEntity? = null,
    val vaultRef: VaultItemEntity? = null
) {
    val isLocalOnlyOver900Mb: Boolean
        get() = AudioCloudPolicy.isAudioOverCloudLimit(byteSize)
}

@Composable
fun AudioRecordingsScreen(
    audioNotes: List<NoteEntity>,
    vaultItems: List<VaultItemEntity>,
    isDark: Boolean,
    isArabic: Boolean,
    onCreateAudioNote: (title: String, durationSec: Int, filePath: String, byteSize: Long) -> Unit,
    onImportAudioUri: (Uri) -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onMoveNoteToVault: (NoteEntity) -> Unit = {},
    onMoveVaultAudioToRecordings: (VaultItemEntity) -> Unit = {},
    snackbarMessage: String? = null,
    onClearSnackbar: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val playbackController = rememberAudioPlaybackController()
    var filterMode by remember { mutableStateOf(AudioFilterMode.ALL) }
    var showRecorderDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        if (!snackbarMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(snackbarMessage)
            onClearSnackbar()
        }
    }

    val importAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onImportAudioUri(it) }
    }

    val allTracks = remember(audioNotes, vaultItems, isArabic) {
        val fromNotes = audioNotes.map { note ->
            val resolvedSize = AudioCloudPolicy.effectiveAudioByteSize(note.audioByteSize, note.audioFilePath)
            AudioTrackItem(
                trackId = "note_${note.id}",
                title = note.title.ifBlank {
                    if (isArabic) "ملاحظة صوتية" else "Voice Note"
                },
                sourceLabel = note.category,
                filePathOrUri = note.audioFilePath,
                durationSec = note.audioDurationSec,
                byteSize = resolvedSize,
                createdAt = note.updatedAt,
                noteRef = note
            )
        }
        val fromVault = vaultItems
            .filter { AudioCloudPolicy.isAudioFile(it.mimeType, it.fileName) }
            .map { v ->
                val resolvedSize = AudioCloudPolicy.effectiveAudioByteSize(v.byteSize, v.localFilePath)
                AudioTrackItem(
                    trackId = "vault_${v.id}",
                    title = v.title.ifBlank { v.fileName },
                    sourceLabel = "Vault",
                    filePathOrUri = v.localFilePath,
                    durationSec = 0,
                    byteSize = resolvedSize,
                    createdAt = v.createdAt,
                    noteRef = null,
                    vaultRef = v
                )
            }
        (fromNotes + fromVault).sortedByDescending { it.createdAt }
    }

    val filteredTracks = remember(allTracks, filterMode) {
        when (filterMode) {
            AudioFilterMode.ALL -> allTracks
            AudioFilterMode.CLOUD_ELIGIBLE -> allTracks.filter { !it.isLocalOnlyOver900Mb }
            AudioFilterMode.LOCAL_ONLY_OVER_900MB -> allTracks.filter { it.isLocalOnlyOver900Mb }
        }
    }

    val activeTrack = remember(filteredTracks, allTracks, playbackController.activeTrackId) {
        val id = playbackController.activeTrackId ?: return@remember null
        allTracks.firstOrNull { it.trackId == id }
    }

    // Auto-advance to next playable recording on completion
    LaunchedEffect(filteredTracks) {
        playbackController.onTrackCompleted = { completedId ->
            val idx = filteredTracks.indexOfFirst { it.trackId == completedId }
            if (idx != -1 && idx + 1 < filteredTracks.size) {
                val next = filteredTracks[idx + 1]
                if (next.filePathOrUri.isNotBlank()) {
                    playbackController.playOrToggle(next.trackId, next.filePathOrUri, next.durationSec)
                }
            }
        }
    }

    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accentPink = Color(0xFFEC4899)
    val accentCyan = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    FrostedBackdrop(isDark = isDark, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .testTag("audio_recordings_screen")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("audio_screen_back_button")
                                .liquidGlass(
                                    isDark = isDark,
                                    tint = GlassTint.FROST,
                                    shape = CircleShape,
                                    borderWidth = 1.2.dp
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true),
                                    onClick = onBack
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isArabic) "رجوع" else "Back",
                                tint = textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (isArabic) "قارئ التسجيلات الصوتية" else "Audio Recordings Player",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = textColor
                                )
                            )
                            Text(
                                text = if (isArabic) "${allTracks.size} تسجيل صوتي" else "${allTracks.size} recordings",
                                fontSize = 12.sp,
                                color = metaColor
                            )
                        }
                    }

                    // Quick Record & Import Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("audio_screen_import_button")
                                .liquidGlass(
                                    isDark = isDark,
                                    tint = GlassTint.CYAN,
                                    shape = CircleShape,
                                    borderWidth = 1.2.dp
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    importAudioLauncher.launch(arrayOf("audio/*"))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = if (isArabic) "استيراد صوت" else "Import Audio",
                                tint = accentCyan,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("audio_screen_record_button")
                                .clip(CircleShape)
                                .background(accentPink)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    showRecorderDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = if (isArabic) "تسجيل جديد" else "New Recording",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 900 MB Cloud Exclusion Rule Banner
                LiquidGlassCard(
                    isDark = isDark,
                    tint = GlassTint.AMBER,
                    shape = RoundedCornerShape(16.dp),
                    borderWidth = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "قاعدة التخزين السحابي للتسجيلات" else "Audio Cloud Storage Rule",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = if (isArabic) {
                                    "التسجيلات الصوتية التي يتعدى حجمها 900 ميغابايت تُحفظ محلياً على الهاتف فقط ولا تُخزَّن سحابياً."
                                } else {
                                    "Audio recordings larger than 900 MB are kept locally on your phone and are never stored in the cloud."
                                },
                                fontSize = 11.sp,
                                color = metaColor,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioFilterPill(
                        label = if (isArabic) "الكل (${allTracks.size})" else "All (${allTracks.size})",
                        selected = filterMode == AudioFilterMode.ALL,
                        accent = accentPink,
                        isDark = isDark,
                        onClick = { filterMode = AudioFilterMode.ALL }
                    )
                    AudioFilterPill(
                        label = if (isArabic) "سحابي ≤ 900م" else "Cloud ≤ 900MB",
                        selected = filterMode == AudioFilterMode.CLOUD_ELIGIBLE,
                        accent = Color(0xFF10B981),
                        isDark = isDark,
                        onClick = { filterMode = AudioFilterMode.CLOUD_ELIGIBLE }
                    )
                    AudioFilterPill(
                        label = if (isArabic) "محلي > 900م" else "Local > 900MB",
                        selected = filterMode == AudioFilterMode.LOCAL_ONLY_OVER_900MB,
                        accent = Color(0xFFF59E0B),
                        isDark = isDark,
                        onClick = { filterMode = AudioFilterMode.LOCAL_ONLY_OVER_900MB }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = metaColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (isArabic) "لا توجد تسجيلات صوتية في هذا القسم" else "No audio recordings here",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (isArabic) {
                                    "سجّل مذكرة صوتية أو استورد ملفاً صوتياً من جهازك للاستماع إليه هنا."
                                } else {
                                    "Record a voice memo or import an audio file from your phone to listen here."
                                },
                                fontSize = 12.5.sp,
                                color = metaColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = if (activeTrack != null) 140.dp else 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(filteredTracks, key = { _, item -> item.trackId }) { _, track ->
                            val isActive = playbackController.activeTrackId == track.trackId
                            val isPlaying = isActive && playbackController.isPlaying
                            val isOverLimit = track.isLocalOnlyOver900Mb
                            val rowAccent = if (isOverLimit) Color(0xFFF59E0B) else accentPink

                            LiquidGlassCard(
                                isDark = isDark,
                                tint = if (isActive) GlassTint.ROSE else GlassTint.FROST,
                                shape = RoundedCornerShape(18.dp),
                                borderWidth = if (isActive) 1.6.dp else 1.dp,
                                onClick = {
                                    playbackController.playOrToggle(
                                        trackId = track.trackId,
                                        sourcePathOrUri = track.filePathOrUri,
                                        fallbackDurationSec = track.durationSec
                                    )
                                },
                                testTag = "audio_track_row_${track.trackId}"
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(rowAccent.copy(alpha = if (isActive) 1f else 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isActive && playbackController.isPreparing) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = null,
                                                tint = if (isActive) Color.White else rowAccent,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (track.durationSec > 0) {
                                                Text(
                                                    text = formatMillis(track.durationSec * 1000L),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = metaColor
                                                )
                                            }
                                            if (track.byteSize > 0L) {
                                                Text(
                                                    text = "• ${AudioCloudPolicy.formatByteSize(track.byteSize, isArabic)}",
                                                    fontSize = 11.5.sp,
                                                    color = metaColor
                                                )
                                            }
                                            Text(
                                                text = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(track.createdAt)),
                                                fontSize = 11.sp,
                                                color = metaColor.copy(alpha = 0.8f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isOverLimit) Color(0xFFF59E0B).copy(alpha = 0.16f)
                                                        else Color(0xFF10B981).copy(alpha = 0.15f)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isOverLimit) Icons.Rounded.CloudOff else Icons.Rounded.CloudDone,
                                                    contentDescription = null,
                                                    tint = if (isOverLimit) Color(0xFFF59E0B) else Color(0xFF10B981),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = if (isOverLimit) {
                                                        if (isArabic) "محلي فقط (> 900 م.ب) • لا يُرفع" else "Local Only (> 900 MB)"
                                                    } else {
                                                        if (isArabic) "متاح سحابياً" else "Cloud Syncable"
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOverLimit) Color(0xFFF59E0B) else Color(0xFF10B981)
                                                )
                                            }

                                            if (track.vaultRef != null) {
                                                Row(
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFC084FC).copy(alpha = 0.16f))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Shield,
                                                        contentDescription = null,
                                                        tint = Color(0xFFC084FC),
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Text(
                                                        text = "Vault",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFC084FC)
                                                    )
                                                }
                                            }
                                        }

                                        if (track.noteRef != null || track.vaultRef != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (track.noteRef != null) {
                                                    Text(
                                                        text = if (isArabic) "فتح الملاحظة" else "Open Note",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = accentCyan,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(accentCyan.copy(alpha = 0.12f))
                                                            .clickable {
                                                                playbackController.stop()
                                                                onOpenNote(track.noteRef)
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )

                                                    Row(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color(0xFFC084FC).copy(alpha = 0.15f))
                                                            .clickable {
                                                                if (playbackController.activeTrackId == track.trackId) {
                                                                    playbackController.stop()
                                                                }
                                                                onMoveNoteToVault(track.noteRef)
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            .testTag("audio_move_to_vault_${track.noteRef.id}"),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Shield,
                                                            contentDescription = null,
                                                            tint = Color(0xFFC084FC),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = if (isArabic) "نقل إلى Vault" else "Move to Vault",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFC084FC)
                                                        )
                                                    }
                                                }

                                                if (track.vaultRef != null) {
                                                    Row(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                                            .clickable {
                                                                if (playbackController.activeTrackId == track.trackId) {
                                                                    playbackController.stop()
                                                                }
                                                                onMoveVaultAudioToRecordings(track.vaultRef)
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            .testTag("audio_move_from_vault_${track.vaultRef.id}"),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.LockOpen,
                                                            contentDescription = null,
                                                            tint = Color(0xFF10B981),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = if (isArabic) "إخراج للتسجيلات" else "Move to Recordings",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF10B981)
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
                }
            }

            // Sticky Now Playing Mini-Console at bottom
            AnimatedVisibility(
                visible = activeTrack != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                activeTrack?.let { current ->
                    val currentIndex = filteredTracks.indexOfFirst { it.trackId == current.trackId }
                    val totalMs = playbackController.totalDurationMs.coerceAtLeast(
                        (current.durationSec.coerceAtLeast(1)) * 1000L
                    )
                    val progress = (playbackController.currentPositionMs.toFloat() / totalMs.coerceAtLeast(1L).toFloat())
                        .coerceIn(0f, 1f)

                    LiquidGlassCard(
                        isDark = isDark,
                        tint = if (current.isLocalOnlyOver900Mb) GlassTint.AMBER else GlassTint.ROSE,
                        shape = RoundedCornerShape(24.dp),
                        borderWidth = 1.5.dp,
                        customFill = if (isDark) Color(0xFF161B2C).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.94f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                    Icon(
                                        imageVector = Icons.Rounded.AudioFile,
                                        contentDescription = null,
                                        tint = accentPink,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = current.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${formatMillis(playbackController.currentPositionMs)} / ${formatMillis(totalMs)}",
                                            fontSize = 11.sp,
                                            color = metaColor
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Speed button
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(accentPink.copy(alpha = 0.14f))
                                            .clickable { playbackController.cyclePlaybackSpeed() }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = when (playbackController.playbackSpeed) {
                                                1.5f -> "1.5x"
                                                2.0f -> "2x"
                                                else -> "1x"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentPink
                                        )
                                    }

                                    // Previous
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .clickable(enabled = currentIndex > 0) {
                                                if (currentIndex > 0) {
                                                    val prev = filteredTracks[currentIndex - 1]
                                                    playbackController.playOrToggle(prev.trackId, prev.filePathOrUri, prev.durationSec)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SkipPrevious,
                                            contentDescription = "Previous",
                                            tint = if (currentIndex > 0) textColor else metaColor.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Play/Pause
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(accentPink)
                                            .clickable {
                                                playbackController.playOrToggle(
                                                    current.trackId,
                                                    current.filePathOrUri,
                                                    current.durationSec
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (playbackController.isPreparing) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (playbackController.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = "Play/Pause",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    // Next
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .clickable(enabled = currentIndex in 0 until filteredTracks.lastIndex) {
                                                if (currentIndex in 0 until filteredTracks.lastIndex) {
                                                    val next = filteredTracks[currentIndex + 1]
                                                    playbackController.playOrToggle(next.trackId, next.filePathOrUri, next.durationSec)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SkipNext,
                                            contentDescription = "Next",
                                            tint = if (currentIndex in 0 until filteredTracks.lastIndex) textColor else metaColor.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Stop
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .clickable { playbackController.stop() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Stop,
                                            contentDescription = "Stop",
                                            tint = metaColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Slider(
                                value = progress,
                                onValueChange = { frac ->
                                    playbackController.seekTo((frac * totalMs).toLong())
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = accentPink,
                                    activeTrackColor = accentPink,
                                    inactiveTrackColor = metaColor.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                            )

                            if (playbackController.lastError != null) {
                                val errMsg = playbackController.lastError!!
                                Text(
                                    text = when (errMsg) {
                                        "MISSING_FILE" -> if (isArabic) {
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
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = if (activeTrack != null) 142.dp else 18.dp)
            ) { data ->
                LiquidGlassCard(
                    isDark = isDark,
                    tint = GlassTint.AMETHYST,
                    shape = RoundedCornerShape(18.dp),
                    borderWidth = 1.2.dp
                ) {
                    Text(
                        text = data.visuals.message,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }

        if (showRecorderDialog) {
            VoiceRecorderDialog(
                isDark = isDark,
                isArabic = isArabic,
                onDismiss = { showRecorderDialog = false },
                onSaveVoiceNote = { duration, filePath, byteSize ->
                    showRecorderDialog = false
                    val defaultTitle = if (isArabic) {
                        "تسجيل صوتي (${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())})"
                    } else {
                        "Voice Memo (${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())})"
                    }
                    onCreateAudioNote(defaultTitle, duration, filePath, byteSize)
                }
            )
        }
    }
}

@Composable
private fun AudioFilterPill(
    label: String,
    selected: Boolean,
    accent: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (selected) accent else if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val bgColor = if (selected) accent.copy(alpha = 0.16f) else if (isDark) Color(0xFF1A2030) else Color.White.copy(alpha = 0.8f)
    val borderColor = if (selected) accent.copy(alpha = 0.55f) else if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}
