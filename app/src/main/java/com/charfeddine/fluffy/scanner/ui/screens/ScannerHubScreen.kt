package com.charfeddine.fluffy.scanner.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.charfeddine.fluffy.scanner.data.model.ScannedDocumentEntity
import com.charfeddine.fluffy.scanner.data.model.ScannerFolderEntity
import com.charfeddine.fluffy.scanner.data.model.ScannerTagCatalog
import com.charfeddine.fluffy.scanner.data.model.SortOption
import com.charfeddine.fluffy.scanner.domain.ScannerViewModel
import com.charfeddine.fluffy.scanner.ui.components.PdfToolsSheet
import com.charfeddine.fluffy.scanner.ui.theme.ScannerDarkLiquidBackdrop
import com.charfeddine.fluffy.scanner.ui.theme.ScannerGlassPalette
import com.charfeddine.fluffy.scanner.ui.theme.ScannerLaserIcon
import com.charfeddine.fluffy.scanner.ui.theme.formatScannerBytes
import com.charfeddine.fluffy.scanner.ui.theme.scannerGlassSurface
import java.io.File

@Composable
fun ScannerHubScreen(
    scannerViewModel: ScannerViewModel,
    isArabic: Boolean,
    onLaunchSmartScan: () -> Unit = { scannerViewModel.openCameraScanner(appendToDocId = null) },
    onRequestProtectedAccess: (targetId: String, onUnlocked: () -> Unit) -> Unit,
    onExitEcosystem: () -> Unit
) {
    val currentFolderId by scannerViewModel.currentFolderId.collectAsState()
    val allFolders by scannerViewModel.allFolders.collectAsState()
    val visibleDocs by scannerViewModel.visibleDocuments.collectAsState()
    val trashedDocs by scannerViewModel.trashedDocs.collectAsState()
    val searchQuery by scannerViewModel.searchQuery.collectAsState()
    val selectedTag by scannerViewModel.selectedTagFilter.collectAsState()
    val onlyStarred by scannerViewModel.onlyStarredFilter.collectAsState()
    val showTrash by scannerViewModel.showTrashView.collectAsState()
    val sortOption by scannerViewModel.sortOption.collectAsState()
    val isGridView by scannerViewModel.isGridView.collectAsState()
    val unlockedIds by scannerViewModel.unlockedProtectedIds.collectAsState()

    BackHandler {
        when {
            showTrash -> scannerViewModel.showTrashView.value = false
            currentFolderId != null -> {
                val parent = allFolders.firstOrNull { it.id == currentFolderId }?.parentFolderId
                scannerViewModel.currentFolderId.value = parent
            }
            else -> onExitEcosystem()
        }
    }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showTagMenu by remember { mutableStateOf(false) }
    var showPdfToolsSheet by remember { mutableStateOf(false) }

    val galleryImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scannerViewModel.importGalleryUris(uris, appendToDocId = null, isArabic = isArabic)
        }
    }

    val childFolders = remember(allFolders, currentFolderId) {
        allFolders.filter { it.parentFolderId == currentFolderId }
    }

    val currentFolderObj = remember(allFolders, currentFolderId) {
        allFolders.firstOrNull { it.id == currentFolderId }
    }

    ScannerDarkLiquidBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Clean Minimal Top Bar (Back + Title + View Toggle + Trash)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                when {
                                    showTrash -> scannerViewModel.showTrashView.value = false
                                    currentFolderId != null -> {
                                        val parent = allFolders.firstOrNull { it.id == currentFolderId }?.parentFolderId
                                        scannerViewModel.currentFolderId.value = parent
                                    }
                                    else -> onExitEcosystem()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("scanner_hub_back_btn")
                                .scannerGlassSurface(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isArabic) "رجوع" else "Back",
                                tint = ScannerGlassPalette.TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    showTrash -> if (isArabic) "سلة المهملات" else "Recycle Bin"
                                    currentFolderObj != null -> currentFolderObj.name
                                    else -> "Doc Scanner"
                                },
                                color = ScannerGlassPalette.TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitleText = when {
                                showTrash -> if (isArabic) "تُحذف تلقائياً بعد 30 يوماً" else "Auto-deletes after 30 days"
                                currentFolderObj != null -> if (isArabic) "مجلد خاص" else "Folder"
                                else -> null
                            }
                            if (subtitleText != null) {
                                Text(
                                    text = subtitleText,
                                    color = ScannerGlassPalette.TextSecondary,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!showTrash) {
                            IconButton(
                                onClick = { scannerViewModel.openCameraScanner(appendToDocId = null) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("hub_pro_camera_btn")
                                    .scannerGlassSurface(CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = if (isArabic) "كاميرا Pro / QR / بطاقة" else "Pro Camera / QR / ID",
                                    tint = ScannerGlassPalette.CyanGlow
                                )
                            }

                            IconButton(
                                onClick = { scannerViewModel.isGridView.value = !isGridView },
                                modifier = Modifier
                                    .size(48.dp)
                                    .scannerGlassSurface(CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                    contentDescription = if (isArabic) "تغيير العرض" else "Toggle View",
                                    tint = ScannerGlassPalette.TextPrimary
                                )
                            }
                        }

                        IconButton(
                            onClick = { scannerViewModel.showTrashView.value = !showTrash },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("hub_trash_toggle_btn")
                                .scannerGlassSurface(CircleShape, glowBorder = showTrash)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = if (isArabic) "سلة المهملات" else "Recycle Bin",
                                tint = if (showTrash) ScannerGlassPalette.RoseGlow else ScannerGlassPalette.TextSecondary
                            )
                        }
                    }
                }

                if (showTrash) {
                    // Clean Recycle Bin View
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (trashedDocs.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 48.dp)
                                        .scannerGlassSurface(RoundedCornerShape(24.dp))
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = ScannerGlassPalette.TextSecondary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "سلة المهملات فارغة" else "Recycle Bin is empty",
                                        color = ScannerGlassPalette.TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        } else {
                            items(trashedDocs, key = { it.id }) { doc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scannerGlassSurface(RoundedCornerShape(18.dp))
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = doc.title,
                                            color = ScannerGlassPalette.TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isArabic) "${doc.pageCount} صفحة • ${formatScannerBytes(doc.totalSizeBytes, true)}" else "${doc.pageCount} pages • ${formatScannerBytes(doc.totalSizeBytes, false)}",
                                            color = ScannerGlassPalette.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { scannerViewModel.restoreFromTrash(doc.id) }) {
                                            Icon(Icons.Default.RestoreFromTrash, contentDescription = "Restore", tint = ScannerGlassPalette.EmeraldGlow)
                                        }
                                        IconButton(onClick = { scannerViewModel.deletePermanently(doc.id) }) {
                                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete Permanently", tint = ScannerGlassPalette.RoseGlow)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 2. Clean Search Bar (full width pill)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { scannerViewModel.searchQuery.value = it },
                        placeholder = {
                            Text(
                                text = if (isArabic) "ابحث في المستندات أو نصوص OCR..." else "Search documents or OCR text...",
                                fontSize = 13.sp,
                                color = ScannerGlassPalette.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = ScannerGlassPalette.CyanGlow
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { scannerViewModel.searchQuery.value = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (isArabic) "مسح البحث" else "Clear search",
                                        tint = ScannerGlassPalette.TextSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScannerGlassPalette.CyanGlow.copy(alpha = 0.7f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                            focusedContainerColor = ScannerGlassPalette.FrostedCardDark,
                            unfocusedContainerColor = ScannerGlassPalette.FrostedCardDark,
                            focusedTextColor = ScannerGlassPalette.TextPrimary,
                            unfocusedTextColor = ScannerGlassPalette.TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Structured Scrollable Content
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Quick Actions Bento Row (shown cleanly at top when not searching)
                        if (searchQuery.isBlank() && currentFolderId == null) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HubQuickActionTile(
                                        icon = Icons.Default.CameraAlt,
                                        label = if (isArabic) "تصوير مستند" else "Scan",
                                        accentColor = ScannerGlassPalette.CyanGlow,
                                        highlighted = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hub_open_camera_scanner_btn"),
                                        onClick = onLaunchSmartScan
                                    )
                                    HubQuickActionTile(
                                        icon = Icons.Default.PhotoLibrary,
                                        label = if (isArabic) "من المعرض" else "Import",
                                        accentColor = ScannerGlassPalette.VioletGlow,
                                        highlighted = false,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hub_import_gallery_btn"),
                                        onClick = {
                                            galleryImportLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    )
                                    HubQuickActionTile(
                                        icon = Icons.Default.PictureAsPdf,
                                        label = if (isArabic) "أدوات PDF" else "PDF Tools",
                                        accentColor = ScannerGlassPalette.EmeraldGlow,
                                        highlighted = false,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hub_pdf_tools_btn"),
                                        onClick = { showPdfToolsSheet = true }
                                    )
                                }
                            }
                        }

                        // Compact Segmented Filter Bar (All / Starred / Tag Dropdown)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChipPill(
                                    label = if (isArabic) "الكل" else "All",
                                    selected = !onlyStarred && selectedTag == null,
                                    accentColor = ScannerGlassPalette.CyanGlow,
                                    onClick = {
                                        scannerViewModel.onlyStarredFilter.value = false
                                        scannerViewModel.selectedTagFilter.value = null
                                    }
                                )

                                FilterChipPill(
                                    label = if (isArabic) "المميزة" else "Starred",
                                    icon = if (onlyStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                    selected = onlyStarred,
                                    accentColor = ScannerGlassPalette.AmberGlow,
                                    onClick = { scannerViewModel.onlyStarredFilter.value = !onlyStarred }
                                )

                                Box {
                                    val activePreset = ScannerTagCatalog.presets.firstOrNull { it.name == selectedTag }
                                    val tagAccent = activePreset?.let { Color(it.colorArgb.toInt()) } ?: ScannerGlassPalette.VioletGlow
                                    Row(
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .clip(CircleShape)
                                            .scannerGlassSurface(CircleShape, glowBorder = selectedTag != null)
                                            .clickable { showTagMenu = true }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        if (activePreset != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(tagAccent)
                                            )
                                        }
                                        Text(
                                            text = selectedTag ?: if (isArabic) "الوسوم" else "Tags",
                                            color = if (selectedTag != null) tagAccent else ScannerGlassPalette.TextSecondary,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = ScannerGlassPalette.TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showTagMenu,
                                        onDismissRequest = { showTagMenu = false },
                                        containerColor = ScannerGlassPalette.MidnightNavy
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (isArabic) "كل الوسوم" else "All Tags",
                                                    color = if (selectedTag == null) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextPrimary,
                                                    fontWeight = if (selectedTag == null) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                scannerViewModel.selectedTagFilter.value = null
                                                showTagMenu = false
                                            }
                                        )
                                        ScannerTagCatalog.presets.forEach { preset ->
                                            val dotColor = Color(preset.colorArgb.toInt())
                                            val isSelected = selectedTag == preset.name
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(dotColor)
                                                    )
                                                },
                                                text = {
                                                    Text(
                                                        text = preset.name,
                                                        color = if (isSelected) dotColor else ScannerGlassPalette.TextPrimary,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    scannerViewModel.selectedTagFilter.value = preset.name
                                                    showTagMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Folders Section (Clean Header + Horizontal Cards)
                        if (searchQuery.isBlank() && selectedTag == null && !onlyStarred) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isArabic) "المجلدات" else "Folders",
                                            color = ScannerGlassPalette.TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Row(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable { showCreateFolderDialog = true }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CreateNewFolder,
                                                contentDescription = null,
                                                tint = ScannerGlassPalette.CyanGlow,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (isArabic) "مجلد جديد" else "New Folder",
                                                color = ScannerGlassPalette.CyanGlow,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    if (childFolders.isNotEmpty()) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(childFolders, key = { it.id }) { folder ->
                                                FolderCard(
                                                    folder = folder,
                                                    onClick = {
                                                        if (folder.isLocked && folder.id !in unlockedIds) {
                                                            onRequestProtectedAccess(folder.id) {
                                                                scannerViewModel.markProtectedUnlocked(folder.id)
                                                                scannerViewModel.currentFolderId.value = folder.id
                                                            }
                                                        } else {
                                                            scannerViewModel.currentFolderId.value = folder.id
                                                        }
                                                    },
                                                    onToggleLock = {
                                                        if (folder.isLocked && folder.id !in unlockedIds) {
                                                            onRequestProtectedAccess(folder.id) {
                                                                scannerViewModel.markProtectedUnlocked(folder.id)
                                                                scannerViewModel.toggleFolderLock(folder, false, isArabic)
                                                            }
                                                        } else {
                                                            scannerViewModel.toggleFolderLock(folder, !folder.isLocked, isArabic)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Documents Section Header + Sort Picker
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isArabic) "المستندات (${visibleDocs.size})" else "Documents (${visibleDocs.size})",
                                    color = ScannerGlassPalette.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                Box {
                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .scannerGlassSurface(CircleShape)
                                            .clickable { showSortMenu = true }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sort,
                                            contentDescription = "Sort",
                                            tint = ScannerGlassPalette.VioletGlow,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (isArabic) sortOption.labelAr else sortOption.labelEn,
                                            color = ScannerGlassPalette.TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false },
                                        containerColor = ScannerGlassPalette.MidnightNavy
                                    ) {
                                        SortOption.entries.forEach { opt ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = if (isArabic) opt.labelAr else opt.labelEn,
                                                        color = if (sortOption == opt) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextPrimary,
                                                        fontWeight = if (sortOption == opt) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    scannerViewModel.sortOption.value = opt
                                                    showSortMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Documents Body: Empty State, Clean 2-Col Rows, or List Cards
                        if (visibleDocs.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scannerGlassSurface(RoundedCornerShape(24.dp))
                                        .padding(horizontal = 24.dp, vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ScannerLaserIcon(modifier = Modifier.size(56.dp))
                                    Text(
                                        text = if (isArabic) "لا توجد مستندات بعد" else "No Documents Yet",
                                        color = ScannerGlassPalette.TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isArabic) {
                                            "صوّر المستندات والبطاقات والكتب أو استوردها من المعرض للبدء."
                                        } else {
                                            "Scan receipts, ID cards, contracts, or import photos from your gallery."
                                        },
                                        color = ScannerGlassPalette.TextSecondary,
                                        fontSize = 12.5.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else if (isGridView) {
                            val rows = visibleDocs.chunked(2)
                            items(rows, key = { chunk -> chunk.first().id }) { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { doc ->
                                        DocumentGridCard(
                                            doc = doc,
                                            isArabic = isArabic,
                                            isUnlocked = !doc.isLocked || doc.id in unlockedIds,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                if (doc.isLocked && doc.id !in unlockedIds) {
                                                    onRequestProtectedAccess(doc.id) {
                                                        scannerViewModel.markProtectedUnlocked(doc.id)
                                                        scannerViewModel.openDocumentDetail(doc.id)
                                                    }
                                                } else {
                                                    scannerViewModel.openDocumentDetail(doc.id)
                                                }
                                            },
                                            onToggleStar = { scannerViewModel.toggleStarred(doc) }
                                        )
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            items(visibleDocs, key = { it.id }) { doc ->
                                DocumentListCard(
                                    doc = doc,
                                    isArabic = isArabic,
                                    isUnlocked = !doc.isLocked || doc.id in unlockedIds,
                                    onClick = {
                                        if (doc.isLocked && doc.id !in unlockedIds) {
                                            onRequestProtectedAccess(doc.id) {
                                                scannerViewModel.markProtectedUnlocked(doc.id)
                                                scannerViewModel.openDocumentDetail(doc.id)
                                            }
                                        } else {
                                            scannerViewModel.openDocumentDetail(doc.id)
                                        }
                                    },
                                    onToggleStar = { scannerViewModel.toggleStarred(doc) }
                                )
                            }
                        }
                    }
                }
            }

            // Single Sleek Floating Scan FAB in corner when inside a subfolder or scrolling docs
            if (!showTrash && (currentFolderId != null || visibleDocs.isNotEmpty())) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .height(54.dp)
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(ScannerGlassPalette.CyanGlow, ScannerGlassPalette.VioletGlow)
                            )
                        )
                        .clickable { onLaunchSmartScan() }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF04101B))
                    Text(
                        text = if (isArabic) "مسح" else "Scan",
                        color = Color(0xFF04101B),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        var folderColor by remember { mutableLongStateOf(0xFF22D3EEL) }
        Dialog(onDismissRequest = { showCreateFolderDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scannerGlassSurface(RoundedCornerShape(24.dp), glowBorder = true, fillAlpha = 0.95f)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "إنشاء مجلد جديد" else "Create New Folder",
                    color = ScannerGlassPalette.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(if (isArabic) "اسم المجلد" else "Folder Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScannerGlassPalette.CyanGlow,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                        focusedTextColor = ScannerGlassPalette.TextPrimary,
                        unfocusedTextColor = ScannerGlassPalette.TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(0xFF22D3EEL, 0xFFA855F7L, 0xFF34D399L, 0xFFF59E0BL, 0xFFF43F5EL).forEach { argb ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(argb.toInt()))
                                .border(if (folderColor == argb) 2.5.dp else 0.dp, Color.White, CircleShape)
                                .clickable { folderColor = argb }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showCreateFolderDialog = false }) {
                        Text(if (isArabic) "إلغاء" else "Cancel", color = ScannerGlassPalette.TextSecondary)
                    }
                    Button(
                        enabled = folderName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                        onClick = {
                            scannerViewModel.createFolder(folderName, folderColor, isArabic)
                            showCreateFolderDialog = false
                        }
                    ) {
                        Text(if (isArabic) "إنشاء" else "Create", color = Color(0xFF04101B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // PDF Utility Tools Sheet
    PdfToolsSheet(
        visible = showPdfToolsSheet,
        isArabic = isArabic,
        onMergePdfs = { uris, outName -> scannerViewModel.mergePdfs(uris, outName, isArabic) },
        onSplitPdf = { uri, outName -> scannerViewModel.splitPdf(uri, outName, isArabic) },
        onCompressPdf = { uri, quality, outName -> scannerViewModel.compressPdf(uri, quality, outName, isArabic) },
        onProtectPdf = { uri, pwd, outName -> scannerViewModel.passwordProtectPdf(uri, pwd, outName, isArabic) },
        onDismiss = { showPdfToolsSheet = false }
    )
}

@Composable
private fun HubQuickActionTile(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(84.dp)
            .minimumInteractiveComponentSize()
            .scannerGlassSurface(RoundedCornerShape(20.dp), glowBorder = highlighted)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = ScannerGlassPalette.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .background(if (selected) accentColor.copy(alpha = 0.18f) else ScannerGlassPalette.FrostedCardDark)
            .border(
                width = 1.dp,
                color = if (selected) accentColor.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.12f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accentColor else ScannerGlassPalette.TextSecondary,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = label,
            color = if (selected) accentColor else ScannerGlassPalette.TextSecondary,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun FolderCard(
    folder: ScannerFolderEntity,
    onClick: () -> Unit,
    onToggleLock: () -> Unit
) {
    val color = Color(folder.colorHex.toInt())
    Row(
        modifier = Modifier
            .width(156.dp)
            .minimumInteractiveComponentSize()
            .scannerGlassSurface(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Text(
                text = folder.name,
                color = ScannerGlassPalette.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onToggleLock,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = if (folder.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Lock folder",
                tint = if (folder.isLocked) ScannerGlassPalette.EmeraldGlow else ScannerGlassPalette.TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun DocumentGridCard(
    doc: ScannedDocumentEntity,
    isArabic: Boolean,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onToggleStar: () -> Unit
) {
    Column(
        modifier = modifier
            .scannerGlassSurface(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A1224)),
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = ScannerGlassPalette.EmeraldGlow, modifier = Modifier.size(36.dp))
            } else if (doc.coverThumbnailPath.isNotBlank() && File(doc.coverThumbnailPath).exists()) {
                Image(
                    painter = rememberAsyncImagePainter(File(doc.coverThumbnailPath)),
                    contentDescription = doc.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ScannerLaserIcon(modifier = Modifier.size(40.dp))
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("${doc.pageCount}p", color = ScannerGlassPalette.CyanGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    color = ScannerGlassPalette.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatScannerBytes(doc.totalSizeBytes, isArabic),
                    color = ScannerGlassPalette.TextSecondary,
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onToggleStar, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (doc.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Star",
                    tint = if (doc.isStarred) ScannerGlassPalette.AmberGlow else ScannerGlassPalette.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DocumentListCard(
    doc: ScannedDocumentEntity,
    isArabic: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    onToggleStar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scannerGlassSurface(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A1224)),
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = ScannerGlassPalette.EmeraldGlow)
            } else if (doc.coverThumbnailPath.isNotBlank() && File(doc.coverThumbnailPath).exists()) {
                Image(
                    painter = rememberAsyncImagePainter(File(doc.coverThumbnailPath)),
                    contentDescription = doc.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ScannerLaserIcon(modifier = Modifier.size(28.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.title,
                color = ScannerGlassPalette.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isArabic) "${doc.pageCount} صفحات • ${formatScannerBytes(doc.totalSizeBytes, true)}" else "${doc.pageCount} pages • ${formatScannerBytes(doc.totalSizeBytes, false)}",
                color = ScannerGlassPalette.TextSecondary,
                fontSize = 11.5.sp
            )
            if (doc.tagsCsv.isNotBlank()) {
                Text(
                    text = doc.tagsCsv.replace(",", " • "),
                    color = ScannerGlassPalette.CyanGlow,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onToggleStar) {
            Icon(
                imageVector = if (doc.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Star",
                tint = if (doc.isStarred) ScannerGlassPalette.AmberGlow else ScannerGlassPalette.TextSecondary
            )
        }
    }
}
