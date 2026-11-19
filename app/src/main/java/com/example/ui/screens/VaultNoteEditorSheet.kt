package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.rememberAudioPlaybackController
import com.example.data.model.ChecklistItem
import com.example.data.model.GlassTint
import com.example.data.model.NoteBackgroundCanvas
import com.example.data.model.NoteBackgroundCatalog
import com.example.data.model.NoteEntity
import com.example.data.model.NotePagesCodec
import com.example.data.model.VaultItemEntity
import com.example.data.storage.AudioCloudPolicy
import com.example.data.storage.AudioStorageManager
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.DocumentStyleBottomSheet
import com.example.ui.components.VoiceRecorderDialog
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.theme.LocalAppThemePreset
import com.example.ui.theme.tr

@Composable
fun VaultNoteEditorSheet(
    existingItem: VaultItemEntity?,
    existingMeta: NoteEntity?,
    isDark: Boolean,
    isArabic: Boolean,
    isPremium: Boolean,
    initialStartVoiceRecorder: Boolean = false,
    onRequestPremiumUpgrade: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        content: String,
        pagesJson: String,
        category: String,
        colorPreset: String,
        backgroundId: String,
        customColorArgb: Long,
        isPinned: Boolean,
        checklist: List<ChecklistItem>,
        audioDurationSec: Int,
        audioFilePath: String,
        audioByteSize: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val activePreset = LocalAppThemePreset.current
    val effectiveDark = activePreset.isDark || isDark
    val playbackController = rememberAudioPlaybackController()

    DisposableEffect(Unit) {
        onDispose { playbackController.stop() }
    }

    val initialAudioPath = remember(existingItem?.id, existingMeta?.audioFilePath) {
        val metaPath = existingMeta?.audioFilePath.orEmpty()
        val itemPath = existingItem?.localFilePath.orEmpty()
        val isItemAudio = existingItem != null &&
            (AudioCloudPolicy.isAudioFile(existingItem.mimeType, existingItem.fileName) ||
                (existingMeta?.audioDurationSec ?: 0) > 0)
        when {
            metaPath.isNotBlank() -> metaPath
            isItemAudio && itemPath.isNotBlank() -> itemPath
            else -> ""
        }
    }

    var title by remember(existingItem?.id) {
        mutableStateOf(
            existingMeta?.title?.ifBlank { existingItem?.title.orEmpty() }
                ?: existingItem?.title.orEmpty()
        )
    }

    val pages = remember(existingItem?.id, existingMeta?.syncId) {
        mutableStateListOf<String>().apply {
            val decoded = if (existingMeta != null) {
                NotePagesCodec.decodePages(existingMeta.content, existingMeta.pagesJson)
            } else {
                listOf(existingItem?.literalContent.orEmpty())
            }
            addAll(decoded.ifEmpty { listOf("") })
        }
    }
    var activePageIndex by remember(existingItem?.id) { mutableIntStateOf(0) }

    val checklist = remember(existingItem?.id) {
        mutableStateListOf<ChecklistItem>().apply {
            addAll(existingMeta?.let { ChecklistItem.jsonToList(it.checklistJson) }.orEmpty())
        }
    }
    var newCheckText by remember { mutableStateOf("") }

    var category by remember(existingItem?.id) {
        mutableStateOf(existingMeta?.category?.ifBlank { if (isArabic) "شخصي" else "Personal" }
            ?: if (isArabic) "شخصي" else "Personal")
    }
    var selectedTint by remember(existingItem?.id) {
        mutableStateOf(
            existingMeta?.let { GlassTint.fromId(it.colorPreset) }
                ?: if (initialAudioPath.isNotBlank() || initialStartVoiceRecorder) GlassTint.ROSE else GlassTint.AMETHYST
        )
    }
    var backgroundId by remember(existingItem?.id) {
        mutableStateOf(existingMeta?.backgroundId ?: NoteBackgroundCatalog.NONE_ID)
    }
    var customColorArgb by remember(existingItem?.id) {
        mutableLongStateOf(existingMeta?.customColorArgb ?: 0L)
    }
    var isPinned by remember(existingItem?.id) {
        mutableStateOf(existingMeta?.isPinned ?: false)
    }

    var audioFilePath by remember(existingItem?.id) { mutableStateOf(initialAudioPath) }
    var audioDurationSec by remember(existingItem?.id) {
        mutableIntStateOf(existingMeta?.audioDurationSec ?: 0)
    }
    var audioByteSize by remember(existingItem?.id) {
        mutableLongStateOf(
            AudioCloudPolicy.effectiveAudioByteSize(
                existingMeta?.audioByteSize ?: existingItem?.byteSize ?: 0L,
                initialAudioPath
            )
        )
    }

    var showStyleSheet by remember { mutableStateOf(false) }
    var showVoiceRecorder by remember(existingItem?.id) { mutableStateOf(initialStartVoiceRecorder) }

    val audioPickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            playbackController.stop()
            val path = uri.toString()
            audioFilePath = path
            audioDurationSec = AudioStorageManager.computeDurationSec(context, path)
            audioByteSize = AudioCloudPolicy.effectiveAudioByteSize(0L, path)
        }
    }

    val bgPreset = remember(backgroundId) { NoteBackgroundCatalog.findById(backgroundId) }
    val surfaceColors = remember(effectiveDark, selectedTint, customColorArgb, backgroundId) {
        NoteBackgroundCatalog.resolveSurfaceColors(
            themeIsDark = effectiveDark,
            tint = selectedTint,
            customColorArgb = customColorArgb,
            backgroundId = backgroundId
        )
    }
    val textColor = surfaceColors.titleColor
    val metaColor = surfaceColors.metaColor
    val accentColor = surfaceColors.accentColor

    val maxAllowedPages = NotePagesCodec.maxPagesForTier(isPremium)
    val safePageIdx = activePageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))

    Dialog(
        onDismissRequest = {
            playbackController.stop()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        FrostedBackdrop(isDark = effectiveDark) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (bgPreset != null) {
                    NoteBackgroundCanvas(
                        preset = bgPreset,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (customColorArgb != 0L) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(customColorArgb.toInt()).copy(alpha = 0.25f))
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("vault_note_editor_sheet"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    playbackController.stop()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(surfaceColors.pillFill)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = tr("رجوع", "Retour", "Back", "Uɣal"),
                                    tint = textColor
                                )
                            }

                            Text(
                                text = if (existingItem == null) {
                                    tr("إنشاء في Vault", "Nouveau dans Vault", "New in Vault", "Amaynut deg Vault")
                                } else {
                                    tr("تعديل داخل Vault", "Modifier dans Vault", "Edit in Vault", "Ẓreg deg Vault")
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { isPinned = !isPinned },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isPinned) accentColor.copy(alpha = 0.22f) else surfaceColors.pillFill)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = tr("تثبيت", "Épingler", "Pin", "Senteḍ"),
                                    tint = if (isPinned) accentColor else metaColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { showStyleSheet = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(surfaceColors.pillFill)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = tr("المظهر واللون", "Style", "Style", "Udem"),
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    playbackController.stop()
                                    val snapshotPages = pages.toList()
                                    val mainContent = snapshotPages.firstOrNull().orEmpty()
                                    val extraPagesJson = NotePagesCodec.encodeExtraPages(snapshotPages)
                                    onSave(
                                        title.trim(),
                                        mainContent,
                                        extraPagesJson,
                                        category,
                                        selectedTint.id,
                                        backgroundId,
                                        customColorArgb,
                                        isPinned,
                                        checklist.toList(),
                                        audioDurationSec,
                                        audioFilePath,
                                        audioByteSize
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor,
                                    contentColor = Color.White
                                ),
                                shape = CircleShape,
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("vault_editor_save_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tr("حفظ", "Enregistrer", "Save", "Sekles"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Scrollable Body: Title, Audio Player / Recorder Controls, Page Selector & Content, Checklist
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(tr("العنوان", "Titre", "Title", "Azwel")) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = metaColor.copy(alpha = 0.35f),
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = metaColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_editor_title_input")
                        )

                        // Voice Note Card + Record / Replace / Import Audio Controls
                        LiquidGlassCard(
                            isDark = effectiveDark,
                            tint = if (audioFilePath.isNotBlank()) GlassTint.ROSE else GlassTint.AMETHYST,
                            shape = RoundedCornerShape(20.dp),
                            borderWidth = 1.1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (audioFilePath.isNotBlank()) {
                                    AudioPlayerCard(
                                        trackId = "vault_edit_${existingItem?.id ?: "new"}",
                                        title = title.ifBlank { tr("تسجيل صوتي", "Note vocale", "Voice Note", "Amesli") },
                                        filePathOrUri = audioFilePath,
                                        durationSec = audioDurationSec,
                                        byteSize = audioByteSize,
                                        playbackController = playbackController,
                                        isDark = effectiveDark,
                                        isArabic = isArabic
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            playbackController.stop()
                                            showVoiceRecorder = true
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .minimumInteractiveComponentSize()
                                            .testTag("vault_editor_record_audio_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = Color(0xFFF43F5E),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (audioFilePath.isNotBlank()) {
                                                tr("إعادة تسجيل / استبدال", "Réenregistrer", "Re-record", "Ales asekles")
                                            } else {
                                                tr("إضافة تسجيل صوتي", "Ajouter audio", "Record Voice", "Rnu amesli")
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { audioPickLauncher.launch(arrayOf("audio/*")) },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Text(
                                            text = tr("ملف صوتي", "Fichier", "Audio File", "Afaylu"),
                                            fontSize = 12.sp,
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (audioFilePath.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                playbackController.stop()
                                                audioFilePath = ""
                                                audioDurationSec = 0
                                                audioByteSize = 0L
                                            },
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = tr("حذف الصوت", "Supprimer l'audio", "Remove Audio", "Kkes amesli"),
                                                tint = Color(0xFFF43F5E)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Multi-Page Tabs + Add Page
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            pages.forEachIndexed { idx, _ ->
                                val selected = idx == safePageIdx
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) accentColor.copy(alpha = 0.22f)
                                            else surfaceColors.pillFill
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) accentColor else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { activePageIndex = idx }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "صفحة ${idx + 1}" else "Page ${idx + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) accentColor else textColor
                                    )
                                    if (pages.size > 1 && selected) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = tr("حذف الصفحة", "Supprimer page", "Remove Page", "Kkes asebter"),
                                            tint = metaColor,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    pages.removeAt(idx)
                                                    activePageIndex = (idx - 1).coerceAtLeast(0)
                                                }
                                        )
                                    }
                                }
                            }

                            if (pages.size < maxAllowedPages) {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(surfaceColors.pillFill)
                                        .clickable {
                                            pages.add("")
                                            activePageIndex = pages.lastIndex
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = tr("صفحة جديدة", "Nouvelle page", "Add Page", "Rnu asebter"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor
                                    )
                                }
                            }
                        }

                        // Page Content Input
                        OutlinedTextField(
                            value = pages.getOrElse(safePageIdx) { "" },
                            onValueChange = { updated ->
                                if (safePageIdx in pages.indices) {
                                    pages[safePageIdx] = updated
                                }
                            },
                            placeholder = {
                                Text(
                                    text = tr(
                                        "اكتب أو عدّل محتوى المدونة هنا...",
                                        "Écrivez ou modifiez le contenu ici...",
                                        "Write or edit your note content here...",
                                        "Aru neɣ ẓreg agbur da..."
                                    ),
                                    color = metaColor.copy(alpha = 0.7f)
                                )
                            },
                            minLines = 6,
                            maxLines = 16,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = metaColor.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_editor_content_input")
                        )

                        // Checklist Section
                        LiquidGlassCard(
                            isDark = effectiveDark,
                            tint = selectedTint,
                            shape = RoundedCornerShape(18.dp),
                            borderWidth = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = tr("قائمة المهام", "Liste de tâches", "Checklist", "Tabdart n twuriwin"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )

                                checklist.forEachIndexed { idx, chk ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { checklist[idx] = chk.copy(isDone = !chk.isDone) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (chk.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                contentDescription = null,
                                                tint = accentColor
                                            )
                                        }
                                        Text(
                                            text = chk.text,
                                            fontSize = 13.5.sp,
                                            color = if (chk.isDone) metaColor else textColor,
                                            textDecoration = if (chk.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { checklist.removeAt(idx) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = tr("حذف", "Supprimer", "Remove", "Kkes"),
                                                tint = metaColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newCheckText,
                                        onValueChange = { newCheckText = it },
                                        placeholder = {
                                            Text(
                                                tr("أضف مهمة...", "Ajouter tâche...", "Add task...", "Rnu tawuri..."),
                                                color = metaColor.copy(alpha = 0.7f),
                                                fontSize = 12.5.sp
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = textColor,
                                            unfocusedTextColor = textColor,
                                            focusedBorderColor = accentColor,
                                            unfocusedBorderColor = metaColor.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = {
                                            val clean = newCheckText.trim()
                                            if (clean.isNotEmpty()) {
                                                checklist.add(ChecklistItem(text = clean))
                                                newCheckText = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.2f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = tr("إضافة", "Ajouter", "Add", "Rnu"),
                                            tint = accentColor
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (showStyleSheet) {
        DocumentStyleBottomSheet(
            isDark = effectiveDark,
            isArabic = isArabic,
            isPremium = isPremium,
            selectedTint = selectedTint,
            customColorArgb = customColorArgb,
            backgroundId = backgroundId,
            category = category,
            onSelectTint = { selectedTint = it },
            onSelectCustomColor = { customColorArgb = it },
            onSelectBackground = { backgroundId = it },
            onSelectCategory = { category = it },
            onResetStyle = {
                selectedTint = GlassTint.AMETHYST
                backgroundId = NoteBackgroundCatalog.NONE_ID
                customColorArgb = 0L
            },
            onRequestPremiumUpgrade = onRequestPremiumUpgrade,
            onDismiss = { showStyleSheet = false }
        )
    }

    if (showVoiceRecorder) {
        VoiceRecorderDialog(
            isDark = effectiveDark,
            isArabic = isArabic,
            onDismiss = { showVoiceRecorder = false },
            onSaveVoiceNote = { durSec, path, size ->
                audioDurationSec = durSec
                audioFilePath = path
                audioByteSize = size
                showVoiceRecorder = false
            }
        )
    }
}
