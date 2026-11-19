package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.rememberAudioPlaybackController
import com.example.data.model.ChecklistItem
import com.example.data.model.GlassTint
import com.example.data.model.NoteEntity
import com.example.data.model.NotePagesCodec
import com.example.data.model.NotesViewMode
import com.example.data.model.VaultItemEntity
import com.example.data.storage.AudioCloudPolicy
import com.example.data.storage.LocalDeviceStorageManager
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.VaultGalleryCard
import com.example.ui.components.VaultMagazineCard
import com.example.ui.components.VaultNoteStyleCard
import com.example.ui.components.VoiceRecorderDialog
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
import com.example.ui.theme.LocalAppThemePreset
import com.example.ui.theme.tr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultScreen(
    vaultItems: List<VaultItemEntity>,
    isDark: Boolean,
    isArabic: Boolean,
    viewMode: NotesViewMode = NotesViewMode.GRID,
    onSelectViewMode: (NotesViewMode) -> Unit = {},
    vaultNoteMetaMap: Map<String, NoteEntity> = emptyMap(),
    isPremium: Boolean = false,
    onRequestPremiumUpgrade: () -> Unit = {},
    onSaveLiteralText: (title: String, literalContent: String) -> Unit,
    onCreateRichNote: (
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
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onUpdateVaultNote: (
        existingItem: VaultItemEntity,
        existingMeta: NoteEntity?,
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
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onSaveFileUri: (Uri) -> Unit,
    onExportItemToUri: (VaultItemEntity, Uri) -> Unit,
    onMoveToNotes: (VaultItemEntity) -> Unit = {},
    onDeleteItem: (VaultItemEntity) -> Unit = {},
    onBack: () -> Unit,
    isVaultLockEnabled: Boolean = false,
    isVaultUnlocked: Boolean = true,
    isVaultActive: Boolean = true,
    vaultSessionId: Long = 0L,
    hasVaultPasskey: Boolean = false,
    snackbarMessage: String? = null,
    onClearSnackbar: () -> Unit = {},
    onTriggerBiometricUnlock: () -> Unit = {},
    onVerifyPasskey: (String) -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    val playbackController = rememberAudioPlaybackController()
    DisposableEffect(Unit) {
        onDispose { playbackController.stop() }
    }

    BackHandler {
        playbackController.stop()
        onBack()
    }

    val activePreset = LocalAppThemePreset.current
    val effectiveDark = activePreset.isDark || isDark
    val primaryAccent = activePreset.primary

    var showTextComposer by remember { mutableStateOf(false) }
    var showViewModeMenu by remember { mutableStateOf(false) }
    var draftTitle by remember { mutableStateOf("") }
    var draftContent by remember { mutableStateOf("") }
    var pendingExportItem by remember { mutableStateOf<VaultItemEntity?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<VaultItemEntity?>(null) }
    var previewItem by remember { mutableStateOf<VaultItemEntity?>(null) }
    var editingTarget by remember { mutableStateOf<Pair<VaultItemEntity?, NoteEntity?>?>(null) }
    var showVoiceRecorderInVault by remember { mutableStateOf(false) }
    var passkeyAttempt by remember { mutableStateOf("") }
    var passkeyError by remember { mutableStateOf(false) }
    var promptedSessionId by rememberSaveable { mutableLongStateOf(-1L) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        if (!snackbarMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(snackbarMessage)
            onClearSnackbar()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onSaveFileUri(it) }
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { destUri ->
        val item = pendingExportItem
        pendingExportItem = null
        if (destUri != null && item != null) {
            onExportItemToUri(item, destUri)
        }
    }

    val textColor = activePreset.onSurface
    val metaColor = activePreset.onSurfaceVariant

    val infiniteTransition = rememberInfiniteTransition(label = "vault_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vault_halo_scale"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vault_float"
    )

    LaunchedEffect(vaultSessionId, isVaultActive, isVaultLockEnabled, isVaultUnlocked) {
        if (isVaultActive && isVaultLockEnabled && !isVaultUnlocked && promptedSessionId != vaultSessionId) {
            promptedSessionId = vaultSessionId
            passkeyAttempt = ""
            passkeyError = false
            onTriggerBiometricUnlock()
        }
    }

    if (isVaultLockEnabled && !isVaultUnlocked) {
        FrostedBackdrop(isDark = effectiveDark) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .testTag("vault_lock_gate"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("vault_gate_back_button")
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onBack
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .liquidGlass(
                                    isDark = effectiveDark,
                                    tint = GlassTint.FROST,
                                    shape = CircleShape,
                                    borderWidth = 1.2.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = tr("رجوع", "Retour", "Back", "Uɣal"),
                                tint = textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    LiquidGlassCard(
                        isDark = effectiveDark,
                        tint = GlassTint.AMETHYST,
                        shape = RoundedCornerShape(28.dp),
                        borderWidth = 1.4.dp,
                        customFill = activePreset.surface.copy(alpha = 0.92f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(primaryAccent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Vault Lock",
                                    tint = primaryAccent,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            Text(
                                text = tr(
                                    "الـ Vault محمي",
                                    "Coffre Vault verrouillé",
                                    "Vault Protected",
                                    "Vault yettwasekkeṛ"
                                ),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = tr(
                                    "قم بفتح الـ Vault باستخدام البصمة أو رمز الـ Passkey",
                                    "Déverrouillez le Vault avec votre empreinte ou votre Passkey",
                                    "Unlock Vault using your Biometrics or Passkey",
                                    "Ldi Vault s uḍad neɣ s Passkey"
                                ),
                                fontSize = 12.5.sp,
                                color = metaColor,
                                textAlign = TextAlign.Center
                            )

                            OutlinedButton(
                                onClick = onTriggerBiometricUnlock,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("vault_biometric_unlock_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = primaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr(
                                        "فتح بالبصمة",
                                        "Déverrouiller par empreinte",
                                        "Unlock with Biometrics",
                                        "Ldi s uḍad"
                                    ),
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryAccent
                                )
                            }

                            if (hasVaultPasskey) {
                                OutlinedTextField(
                                    value = passkeyAttempt,
                                    onValueChange = {
                                        passkeyAttempt = it
                                        passkeyError = false
                                    },
                                    label = {
                                        Text(
                                            tr(
                                                "رمز Passkey للـ Vault",
                                                "Passkey du Vault",
                                                "Vault Passkey",
                                                "Passkey n Vault"
                                            )
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null,
                                            tint = primaryAccent
                                        )
                                    },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    isError = passkeyError,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor,
                                        focusedBorderColor = primaryAccent,
                                        unfocusedBorderColor = metaColor.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("vault_gate_passkey_input")
                                )

                                if (passkeyError) {
                                    Text(
                                        text = tr(
                                            "رمز Passkey غير صحيح",
                                            "Passkey incorrect",
                                            "Incorrect Passkey",
                                            "Passkey mačči d ameɣtu"
                                        ),
                                        fontSize = 12.sp,
                                        color = Color(0xFFF43F5E)
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (!onVerifyPasskey(passkeyAttempt)) {
                                            passkeyError = true
                                        }
                                    },
                                    enabled = passkeyAttempt.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primaryAccent,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("vault_gate_passkey_submit")
                                ) {
                                    Text(
                                        text = tr(
                                            "فتح بـ Passkey",
                                            "Déverrouiller avec Passkey",
                                            "Unlock with Passkey",
                                            "Ldi s Passkey"
                                        ),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    FrostedBackdrop(isDark = effectiveDark) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("vault_screen"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Liquid Glass Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("vault_back_button")
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = {
                                    playbackController.stop()
                                    onBack()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .liquidGlass(
                                    isDark = isDark,
                                    tint = GlassTint.FROST,
                                    shape = CircleShape,
                                    borderWidth = 1.2.dp
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
                    }

                    Row(
                        modifier = Modifier
                            .liquidGlass(
                                isDark = isDark,
                                tint = GlassTint.AMETHYST,
                                shape = CircleShape,
                                borderWidth = 1.2.dp
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (vaultItems.isNotEmpty()) "Vault (${vaultItems.size})" else "Vault",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    // View Mode Picker matching main interface (Grid, List, Gallery, Magazine)
                    Box {
                        val activeModeIcon = when (viewMode) {
                            NotesViewMode.GRID -> Icons.Default.GridView
                            NotesViewMode.LIST -> Icons.Default.ViewAgenda
                            NotesViewMode.GALLERY -> Icons.Default.AutoAwesomeMosaic
                            NotesViewMode.MAGAZINE -> Icons.Default.MenuBook
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("vault_view_mode_toggle")
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true),
                                    onClick = { showViewModeMenu = true }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .liquidGlass(
                                        isDark = isDark,
                                        tint = GlassTint.FROST,
                                        shape = CircleShape,
                                        borderWidth = 1.2.dp
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = activeModeIcon,
                                    contentDescription = tr("تبديل طريقة العرض", "Changer l'affichage", "View Mode", "Beddel timeẓri"),
                                    tint = textColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showViewModeMenu,
                            onDismissRequest = { showViewModeMenu = false },
                            containerColor = activePreset.surface.copy(alpha = 0.98f)
                        ) {
                            NotesViewMode.entries.forEach { mode ->
                                val selected = mode == viewMode
                                val optionIcon = when (mode) {
                                    NotesViewMode.GRID -> Icons.Default.GridView
                                    NotesViewMode.LIST -> Icons.Default.ViewAgenda
                                    NotesViewMode.GALLERY -> Icons.Default.AutoAwesomeMosaic
                                    NotesViewMode.MAGAZINE -> Icons.Default.MenuBook
                                }
                                val optionLabel = when (mode) {
                                    NotesViewMode.GRID -> tr("شبكة • Grid View", "Vue grille", "Grid View", "Timeẓri n tferkit")
                                    NotesViewMode.LIST -> tr("قائمة • List View", "Vue liste", "List View", "Timeẓri n tebdart")
                                    NotesViewMode.GALLERY -> tr("معرض • Gallery View", "Gallery View", "Gallery View", "Gallery View")
                                    NotesViewMode.MAGAZINE -> tr("مجلة • Magazine View", "Magazine View", "Magazine View", "Magazine View")
                                }
                                DropdownMenuItem(
                                    modifier = Modifier.testTag("vault_view_mode_option_${mode.id}"),
                                    text = {
                                        Text(
                                            text = optionLabel,
                                            color = if (selected) activePreset.primary else textColor,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = optionIcon,
                                            contentDescription = null,
                                            tint = if (selected) activePreset.primary else metaColor
                                        )
                                    },
                                    trailingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = activePreset.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        showViewModeMenu = false
                                        onSelectViewMode(mode)
                                    }
                                )
                            }
                        }
                    }
                }

                // Quick Action Buttons: Save Literal Text / Save Phone File Byte-for-Byte
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .widthIn(max = 540.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassCard(
                        isDark = isDark,
                        tint = GlassTint.AMETHYST,
                        shape = RoundedCornerShape(18.dp),
                        borderWidth = 1.2.dp,
                        onClick = { showTextComposer = !showTextComposer },
                        testTag = "vault_add_text_button",
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NoteAdd,
                                contentDescription = null,
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr("حفظ نص حرفي", "Enregistrer un texte", "Save Text", "Sekles aḍris"),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }

                    LiquidGlassCard(
                        isDark = isDark,
                        tint = GlassTint.CYAN,
                        shape = RoundedCornerShape(18.dp),
                        borderWidth = 1.2.dp,
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        testTag = "vault_add_file_button",
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr("حفظ ملف من الهاتف", "Enregistrer un fichier", "Save File", "Sekles afaylu"),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Action Row 2: Rich Multi-Page Blog/Note & Voice Note inside Vault
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .widthIn(max = 540.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassCard(
                        isDark = isDark,
                        tint = GlassTint.EMERALD,
                        shape = RoundedCornerShape(18.dp),
                        borderWidth = 1.2.dp,
                        onClick = {
                            playbackController.stop()
                            editingTarget = null to null
                        },
                        testTag = "vault_add_rich_note_button",
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr("مدونة ومهام جديدة", "Note enrichie", "Rich Blog / Note", "Tira tamaynut"),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    LiquidGlassCard(
                        isDark = isDark,
                        tint = GlassTint.ROSE,
                        shape = RoundedCornerShape(18.dp),
                        borderWidth = 1.2.dp,
                        onClick = {
                            playbackController.stop()
                            showVoiceRecorderInVault = true
                        },
                        testTag = "vault_add_voice_button",
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color(0xFFF472B6),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr("نوتة صوتية جديدة", "Note vocale", "Record Voice Note", "Imesli amaynut"),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Literal Immutable Text Composer
                AnimatedVisibility(visible = showTextComposer) {
                    LiquidGlassCard(
                        isDark = isDark,
                        tint = GlassTint.AMETHYST,
                        shape = RoundedCornerShape(22.dp),
                        borderWidth = 1.4.dp,
                        customFill = if (isDark) Color(0xFF0F172A).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.88f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .widthIn(max = 540.dp)
                            .padding(top = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isArabic)
                                    "سيُحفظ النص حرفياً كما كتبته في ذاكرة الهاتف والـ Vault ولا يتغير بعدها لأي سبب"
                                else
                                    "Saved verbatim in phone storage & Vault — strictly immutable once sealed",
                                fontSize = 11.5.sp,
                                color = metaColor
                            )

                            OutlinedTextField(
                                value = draftTitle,
                                onValueChange = { draftTitle = it },
                                placeholder = {
                                    Text(
                                        text = if (isArabic) "العنوان (اختياري)" else "Title (optional)",
                                        color = metaColor.copy(alpha = 0.7f)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    focusedBorderColor = Color(0xFFC084FC),
                                    unfocusedBorderColor = metaColor.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("vault_text_title_input")
                            )

                            OutlinedTextField(
                                value = draftContent,
                                onValueChange = { draftContent = it },
                                placeholder = {
                                    Text(
                                        text = if (isArabic) "اكتب أي شيء لحفظه حرفياً..." else "Write anything to preserve literally...",
                                        color = metaColor.copy(alpha = 0.7f)
                                    )
                                },
                                minLines = 3,
                                maxLines = 6,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = metaColor.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("vault_text_content_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        if (draftTitle.isNotEmpty() || draftContent.isNotEmpty()) {
                                            onSaveLiteralText(draftTitle, draftContent)
                                            draftTitle = ""
                                            draftContent = ""
                                            showTextComposer = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF8B5CF6),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("vault_seal_save_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isArabic) "حفظ وتثبيت حرفياً" else "Save Verbatim",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (vaultItems.isEmpty()) {
                    // Empty Vault Emblem
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LiquidGlassCard(
                            isDark = isDark,
                            tint = GlassTint.AMETHYST,
                            shape = RoundedCornerShape(30.dp),
                            borderWidth = 1.6.dp,
                            customFill = if (isDark) {
                                Color(0xFF0F172A).copy(alpha = 0.78f)
                            } else {
                                Color(0xFFFFFFFF).copy(alpha = 0.82f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 380.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 26.dp, vertical = 30.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(104.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = pulseScale
                                                scaleY = pulseScale
                                            }
                                    ) {
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val outerRadius = size.minDimension / 2f

                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0x55C084FC),
                                                    Color(0x3338BDF8),
                                                    Color.Transparent
                                                ),
                                                center = center,
                                                radius = outerRadius
                                            ),
                                            radius = outerRadius,
                                            center = center
                                        )

                                        drawCircle(
                                            brush = Brush.sweepGradient(
                                                colors = listOf(
                                                    Color(0xFF38BDF8).copy(alpha = 0.75f),
                                                    Color(0xFFC084FC).copy(alpha = 0.85f),
                                                    Color(0xFFF472B6).copy(alpha = 0.75f),
                                                    Color(0xFF38BDF8).copy(alpha = 0.75f)
                                                )
                                            ),
                                            radius = outerRadius * 0.80f,
                                            center = center,
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(66.dp)
                                            .graphicsLayer {
                                                translationY = floatOffset.dp.toPx()
                                            }
                                            .liquidGlass(
                                                isDark = isDark,
                                                tint = GlassTint.AMETHYST,
                                                shape = CircleShape,
                                                borderWidth = 1.4.dp
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Vault",
                                            tint = if (isDark) Color(0xFFE9D5FF) else Color(0xFF7C3AED),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Vault",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textColor,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Box(
                                    modifier = Modifier
                                        .testTag("vault_status_badge")
                                        .liquidGlass(
                                            isDark = isDark,
                                            tint = GlassTint.CYAN,
                                            shape = CircleShape,
                                            borderWidth = 1.2.dp,
                                            customFill = if (isDark) Color(0x2E8B5CF6) else Color(0x228B5CF6)
                                        )
                                        .padding(horizontal = 20.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "خزنة محمية • Vault" else "Immutable Vault",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFE9D5FF) else Color(0xFF6D28D9)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = if (isArabic)
                                        "يمكنك حفظ النصوص والملفات أو نقل أي مسودة وتسجيل صوتي إلى هنا وحمايتها أو إرجاعها لاحقاً."
                                    else
                                        "Store literal texts, files, or move drafts and voice recordings here and restore them anytime.",
                                    fontSize = 12.sp,
                                    color = metaColor,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    val launchExport: (VaultItemEntity) -> Unit = { item ->
                        pendingExportItem = item
                        val suggested = item.fileName.ifBlank {
                            (item.title.ifBlank { "vault_note_${item.createdAt}" }) + ".txt"
                        }
                        exportFileLauncher.launch(suggested)
                    }

                    // Render Vault items using the exact 4 NotesViewMode layouts of FluffyHomeScreen
                    when (viewMode) {
                        NotesViewMode.GRID -> {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("vault_notes_grid"),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 4.dp,
                                    bottom = 32.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalItemSpacing = 12.dp
                            ) {
                                staggeredItems(vaultItems, key = { it.id }) { item ->
                                    VaultNoteStyleCard(
                                        item = item,
                                        metaNote = vaultNoteMetaMap[item.id],
                                        isDark = isDark,
                                        isArabic = isArabic,
                                        onClick = { previewItem = item },
                                        onMoveToNotesClick = { onMoveToNotes(item) },
                                        onExportClick = { launchExport(item) },
                                        onDeleteClick = { pendingDeleteItem = item }
                                    )
                                }
                            }
                        }

                        NotesViewMode.LIST -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("vault_notes_list"),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 4.dp,
                                    bottom = 32.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                listItems(vaultItems, key = { it.id }) { item ->
                                    VaultNoteStyleCard(
                                        item = item,
                                        metaNote = vaultNoteMetaMap[item.id],
                                        isDark = isDark,
                                        isArabic = isArabic,
                                        onClick = { previewItem = item },
                                        onMoveToNotesClick = { onMoveToNotes(item) },
                                        onExportClick = { launchExport(item) },
                                        onDeleteClick = { pendingDeleteItem = item }
                                    )
                                }
                            }
                        }

                        NotesViewMode.GALLERY -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 158.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("vault_notes_gallery"),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 4.dp,
                                    bottom = 32.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                gridItems(vaultItems, key = { it.id }) { item ->
                                    VaultGalleryCard(
                                        item = item,
                                        metaNote = vaultNoteMetaMap[item.id],
                                        isDark = isDark,
                                        isArabic = isArabic,
                                        onClick = { previewItem = item },
                                        onMoveToNotesClick = { onMoveToNotes(item) },
                                        onExportClick = { launchExport(item) },
                                        onDeleteClick = { pendingDeleteItem = item }
                                    )
                                }
                            }
                        }

                        NotesViewMode.MAGAZINE -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("vault_notes_magazine"),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 4.dp,
                                    bottom = 32.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                itemsIndexed(vaultItems, key = { _, it -> it.id }) { index, item ->
                                    VaultMagazineCard(
                                        item = item,
                                        metaNote = vaultNoteMetaMap[item.id],
                                        issueIndex = index,
                                        isLeadFeature = index == 0,
                                        isDark = isDark,
                                        isArabic = isArabic,
                                        onClick = { previewItem = item },
                                        onMoveToNotesClick = { onMoveToNotes(item) },
                                        onExportClick = { launchExport(item) },
                                        onDeleteClick = { pendingDeleteItem = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) { data ->
                LiquidGlassCard(
                    isDark = effectiveDark,
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
    }

    // Detail Preview Dialog when tapping any Vault card (includes Audio playback, Edit & Move back)
    previewItem?.let { rawPreviewItem ->
        val item = vaultItems.firstOrNull { it.id == rawPreviewItem.id } ?: rawPreviewItem
        val metaNote = vaultNoteMetaMap[item.id]
        val isAudio = AudioCloudPolicy.isAudioFile(item.mimeType, item.fileName) ||
            ((metaNote?.audioDurationSec ?: 0) > 0 && item.localFilePath.isNotBlank())
        val isEditableNoteOrAudio = isAudio ||
            metaNote != null ||
            item.mimeType.startsWith("text/") ||
            item.literalContent.isNotEmpty() ||
            item.localFilePath.isBlank()
        val checklistItems = remember(metaNote?.checklistJson) {
            metaNote?.let { ChecklistItem.jsonToList(it.checklistJson) }.orEmpty()
        }
        val pages = remember(metaNote?.content, metaNote?.pagesJson, item.literalContent) {
            if (metaNote != null) {
                NotePagesCodec.decodePages(metaNote.content, metaNote.pagesJson)
            } else {
                listOf(item.literalContent)
            }
        }
        val isIntegrityValid = remember(item.id, item.sha256, item.localFilePath) {
            LocalDeviceStorageManager.verifyVaultItemIntegrity(item)
        }
        val titleText = item.title.ifBlank {
            item.fileName.ifBlank { tr("عنصر Vault", "Élément Vault", "Vault Item", "Aferdis n Vault") }
        }

        Dialog(
            onDismissRequest = {
                playbackController.stop()
                previewItem = null
            }
        ) {
            LiquidGlassCard(
                isDark = effectiveDark,
                tint = if (isAudio) GlassTint.ROSE else GlassTint.AMETHYST,
                shape = RoundedCornerShape(26.dp),
                borderWidth = 1.4.dp,
                customFill = activePreset.surface.copy(alpha = 0.96f),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .testTag("vault_preview_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = titleText,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    playbackController.stop()
                                    previewItem = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = tr("إغلاق", "Fermer", "Close", "Mdel"),
                                tint = metaColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.createdAt)),
                            fontSize = 11.5.sp,
                            color = metaColor
                        )
                        Text(
                            text = if (isIntegrityValid) {
                                if (isArabic) "محفوظ في الخزنة ✓" else "Vault Verified ✓"
                            } else {
                                if (isArabic) "نسخة محلية" else "Local Copy"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                    }

                    if (isAudio && item.localFilePath.isNotBlank()) {
                        AudioPlayerCard(
                            trackId = "vault_preview_${item.id}",
                            title = titleText,
                            filePathOrUri = item.localFilePath,
                            durationSec = metaNote?.audioDurationSec ?: 0,
                            byteSize = AudioCloudPolicy.effectiveAudioByteSize(item.byteSize, item.localFilePath),
                            playbackController = playbackController,
                            isDark = effectiveDark,
                            isArabic = isArabic
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (pages.size <= 1) {
                            val single = pages.firstOrNull().orEmpty()
                            if (single.isNotBlank()) {
                                Text(
                                    text = single,
                                    fontSize = 14.sp,
                                    color = textColor,
                                    lineHeight = 21.sp
                                )
                            }
                        } else {
                            pages.forEachIndexed { idx, p ->
                                if (p.isNotBlank()) {
                                    Text(
                                        text = if (isArabic) "--- صفحة ${idx + 1} ---" else "--- Page ${idx + 1} ---",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC084FC)
                                    )
                                    Text(
                                        text = p,
                                        fontSize = 14.sp,
                                        color = textColor,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        if (checklistItems.isNotEmpty()) {
                            checklistItems.forEach { chk ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (chk.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = Color(0xFFC084FC),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = chk.text,
                                        fontSize = 13.sp,
                                        color = if (chk.isDone) metaColor else textColor,
                                        textDecoration = if (chk.isDone) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }
                            }
                        }

                        if (item.fileName.isNotEmpty()) {
                            val sizeKb = item.byteSize / 1024f
                            val sizeLabel = if (sizeKb >= 1024f) {
                                String.format(Locale.US, "%.2f MB", sizeKb / 1024f)
                            } else {
                                String.format(Locale.US, "%.1f KB", sizeKb.coerceAtLeast(0.1f))
                            }
                            Text(
                                text = "${item.fileName} • $sizeLabel",
                                fontSize = 12.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Preview Actions: Edit inside Vault, Move back to Notes/Audio, Export, Delete
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isEditableNoteOrAudio) {
                                Button(
                                    onClick = {
                                        playbackController.stop()
                                        previewItem = null
                                        editingTarget = item to metaNote
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF8B5CF6),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("vault_preview_edit_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAudio) {
                                            tr("تعديل والصوت", "Modifier", "Edit / Voice", "Ẓreg")
                                        } else {
                                            tr("تعديل", "Modifier", "Edit", "Ẓreg")
                                        },
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    playbackController.stop()
                                    previewItem = null
                                    onMoveToNotes(item)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("vault_preview_move_to_notes")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAudio) {
                                        tr("للتسجيلات", "Enregistrements", "To Audio", "I imesli")
                                    } else {
                                        tr("للمسودات", "Vers Notes", "To Notes", "Ɣer tazmilt")
                                    },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    playbackController.stop()
                                    previewItem = null
                                    pendingExportItem = item
                                    val suggested = item.fileName.ifBlank {
                                        (item.title.ifBlank { "vault_note_${item.createdAt}" }) + ".txt"
                                    }
                                    exportFileLauncher.launch(suggested)
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = tr("تصدير", "Exporter", "Export", "Sifeḍ"),
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr("تصدير", "Exporter", "Export", "Sifeḍ"),
                                    color = textColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    playbackController.stop()
                                    previewItem = null
                                    pendingDeleteItem = item
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = tr("حذف", "Supprimer", "Delete", "Kkes"),
                                    tint = Color(0xFFF43F5E),
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr("حذف", "Supprimer", "Delete", "Kkes"),
                                    color = Color(0xFFF43F5E),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingTarget?.let { (editItem, editMeta) ->
        VaultNoteEditorSheet(
            existingItem = editItem,
            existingMeta = editMeta,
            isDark = effectiveDark,
            isArabic = isArabic,
            isPremium = isPremium,
            onRequestPremiumUpgrade = onRequestPremiumUpgrade,
            onDismiss = { editingTarget = null },
            onSave = { title, content, pagesJson, category, colorPreset, backgroundId, customColorArgb, isPinned, checklist, audioDurationSec, audioFilePath, audioByteSize ->
                if (editItem != null) {
                    onUpdateVaultNote(
                        editItem,
                        editMeta,
                        title,
                        content,
                        pagesJson,
                        category,
                        colorPreset,
                        backgroundId,
                        customColorArgb,
                        isPinned,
                        checklist,
                        audioDurationSec,
                        audioFilePath,
                        audioByteSize
                    )
                } else {
                    onCreateRichNote(
                        title,
                        content,
                        pagesJson,
                        category,
                        colorPreset,
                        backgroundId,
                        customColorArgb,
                        isPinned,
                        checklist,
                        audioDurationSec,
                        audioFilePath,
                        audioByteSize
                    )
                }
                editingTarget = null
            }
        )
    }

    if (showVoiceRecorderInVault) {
        val voiceTitlePrefix = tr("نوتة صوتية", "Note vocale", "Voice Note", "Imesli")
        VoiceRecorderDialog(
            isDark = effectiveDark,
            isArabic = isArabic,
            onDismiss = { showVoiceRecorderInVault = false },
            onSaveVoiceNote = { durSec, path, size ->
                val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                val voiceTitle = "$voiceTitlePrefix $stamp"
                onCreateRichNote(
                    voiceTitle,
                    "",
                    "",
                    "Audio",
                    GlassTint.ROSE.name,
                    "NONE",
                    0L,
                    false,
                    emptyList(),
                    durSec,
                    path,
                    size
                )
                showVoiceRecorderInVault = false
            }
        )
    }

    pendingDeleteItem?.let { itemToDelete ->
        val displayTitle = itemToDelete.title.ifBlank {
            itemToDelete.fileName.ifBlank { tr("عنصر Vault", "Élément Vault", "Vault Item", "Aferdis n Vault") }
        }
        Dialog(onDismissRequest = { pendingDeleteItem = null }) {
            LiquidGlassCard(
                isDark = effectiveDark,
                tint = GlassTint.ROSE,
                shape = RoundedCornerShape(28.dp),
                borderWidth = 1.4.dp,
                customFill = activePreset.surface.copy(alpha = 0.94f),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .testTag("vault_delete_confirm_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF43F5E).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = tr(
                                "حذف من الـ Vault",
                                "Supprimer du Vault",
                                "Delete from Vault",
                                "Kkes seg Vault"
                            ),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    Text(
                        text = tr(
                            "هل تريد حذف \"$displayTitle\" نهائياً من الـ Vault ومن ذاكرة الجهاز؟ لا يمكن التراجع عن هذا الإجراء.",
                            "Supprimer définitivement \"$displayTitle\" du Vault et de l'appareil ? Cette action est irréversible.",
                            "Permanently delete \"$displayTitle\" from Vault and device storage? This cannot be undone.",
                            "Ad tekkseḍ \"$displayTitle\" i lebda seg Vault akked tkatut n yibenk?"
                        ),
                        fontSize = 13.sp,
                        color = metaColor,
                        lineHeight = 19.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { pendingDeleteItem = null },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("vault_delete_cancel_button")
                        ) {
                            Text(
                                text = tr("إلغاء", "Annuler", "Cancel", "Sefsex"),
                                color = metaColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                playbackController.stop()
                                onDeleteItem(itemToDelete)
                                pendingDeleteItem = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE11D48),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("vault_delete_confirm_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr("حذف", "Supprimer", "Delete", "Kkes"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
