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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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
                                .size(46.dp)
                                .testTag("scanner_hub_back_btn")
                                .scannerGlassSurface(CircleShape)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isArabic) "رجوع" else "Back",
                                tint = ScannerGlassPalette.TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        ScannerLaserIcon(modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    showTrash -> if (isArabic) "سلة المهملات (30 يوماً)" else "Recycle Bin (30 Days)"
                                    currentFolderObj != null -> currentFolderObj.name
                                    else -> if (isArabic) "ماسح Fluffy الاحترافي" else "Fluffy Doc Scanner"
                                },
                                color = ScannerGlassPalette.TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isArabic) "محلي 100% • PDF & OCR مشفّر" else "100% Offline • PDF, OCR & Vault",
                                color = ScannerGlassPalette.CyanGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // PDF Tools Drawer trigger
                        IconButton(
                            onClick = { showPdfToolsSheet = true },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("hub_pdf_tools_btn")
                                .scannerGlassSurface(CircleShape, glowBorder = true)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = "PDF Tools", tint = ScannerGlassPalette.CyanGlow)
                        }

                        // Recycle Bin toggle
                        IconButton(
                            onClick = { scannerViewModel.showTrashView.value = !showTrash },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("hub_trash_toggle_btn")
                                .scannerGlassSurface(CircleShape, glowBorder = showTrash)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Trash",
                                tint = if (showTrash) ScannerGlassPalette.RoseGlow else ScannerGlassPalette.TextSecondary
                            )
                        }

                        // Grid / List Toggle
                        IconButton(
                            onClick = { scannerViewModel.isGridView.value = !isGridView },
                            modifier = Modifier.size(44.dp).scannerGlassSurface(CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                contentDescription = "View Mode",
                                tint = ScannerGlassPalette.TextPrimary
                            )
                        }
                    }
                }

                if (!showTrash) {
                    // Smart Search Bar (Title + Tags + Extracted OCR Text) + Sort Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { scannerViewModel.searchQuery.value = it },
                            placeholder = {
                                Text(
                                    if (isArabic) "ابحث بالعنوان أو الوسم أو نص OCR..." else "Search title, tags, or OCR text...",
                                    fontSize = 12.5.sp,
                                    color = ScannerGlassPalette.TextSecondary
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ScannerGlassPalette.CyanGlow) },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ScannerGlassPalette.CyanGlow,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.20f),
                                focusedContainerColor = ScannerGlassPalette.FrostedCardDark,
                                unfocusedContainerColor = ScannerGlassPalette.FrostedCardDark,
                                focusedTextColor = ScannerGlassPalette.TextPrimary,
                                unfocusedTextColor = ScannerGlassPalette.TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier
                                    .size(52.dp)
                                    .scannerGlassSurface(RoundedCornerShape(16.dp))
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort", tint = ScannerGlassPalette.VioletGlow)
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

                    // Filter Strip: Starred + Color-Coded Tag Pills + New Folder Button
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .scannerGlassSurface(CircleShape, glowBorder = onlyStarred)
                                    .clickable { scannerViewModel.onlyStarredFilter.value = !onlyStarred }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (onlyStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = ScannerGlassPalette.AmberGlow,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isArabic) "المميزة" else "Starred",
                                    color = if (onlyStarred) ScannerGlassPalette.AmberGlow else ScannerGlassPalette.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        items(ScannerTagCatalog.presets) { preset ->
                            val selected = selectedTag == preset.name
                            val col = Color(preset.colorArgb.toInt())
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) col.copy(alpha = 0.28f) else ScannerGlassPalette.FrostedCardDark)
                                    .border(1.dp, if (selected) col else Color.White.copy(alpha = 0.16f), CircleShape)
                                    .clickable {
                                        scannerViewModel.selectedTagFilter.value = if (selected) null else preset.name
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = preset.name,
                                    color = if (selected) col else ScannerGlassPalette.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .scannerGlassSurface(CircleShape)
                                    .clickable { showCreateFolderDialog = true }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = ScannerGlassPalette.CyanGlow, modifier = Modifier.size(15.dp))
                                Text(if (isArabic) "مجلد جديد" else "New Folder", color = ScannerGlassPalette.CyanGlow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Subfolders Strip
                    if (childFolders.isNotEmpty() && searchQuery.isBlank() && selectedTag == null && !onlyStarred) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(childFolders, key = { it.id }) { folder ->
                                FolderCardPill(
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

                    // Documents Grid or List
                    if (visibleDocs.isEmpty() && childFolders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scannerGlassSurface(RoundedCornerShape(26.dp), glowBorder = true)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ScannerLaserIcon(modifier = Modifier.size(64.dp))
                                Text(
                                    text = if (isArabic) "لا توجد مستندات ممسوحة بعد" else "No Scanned Documents Yet",
                                    color = ScannerGlassPalette.TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isArabic) {
                                        "اضغط على زر التصوير بالأسفل لمسح الفواتير، العقود، البطاقات، أو الكتب محلياً 100%."
                                    } else {
                                        "Tap Scan Document below to scan receipts, ID cards, contracts, or books 100% offline."
                                    },
                                    color = ScannerGlassPalette.TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(visibleDocs, key = { it.id }) { doc ->
                                DocumentGridCard(
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
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
                } else {
                    // Recycle Bin (30-Day Retention)
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (trashedDocs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scannerGlassSurface(RoundedCornerShape(22.dp))
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isArabic) "سلة المهملات فارغة" else "Recycle Bin is empty",
                                        color = ScannerGlassPalette.TextSecondary
                                    )
                                }
                            }
                        } else {
                            items(trashedDocs, key = { it.id }) { doc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scannerGlassSurface(RoundedCornerShape(18.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(doc.title, color = ScannerGlassPalette.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = if (isArabic) "${doc.pageCount} صفحة • ${formatScannerBytes(doc.totalSizeBytes, true)}" else "${doc.pageCount} pages • ${formatScannerBytes(doc.totalSizeBytes, false)}",
                                            color = ScannerGlassPalette.TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                }
            }

            // Bottom Floating Action Dock (Scan Document + Gallery Import)
            if (!showTrash) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            galleryImportLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("hub_import_gallery_btn")
                            .scannerGlassSurface(CircleShape, glowBorder = true)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = if (isArabic) "استيراد صورة" else "Import Photo",
                            tint = ScannerGlassPalette.VioletGlow
                        )
                    }

                    Row(
                        modifier = Modifier
                            .height(58.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("hub_open_camera_scanner_btn")
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ScannerGlassPalette.CyanGlow, ScannerGlassPalette.VioletGlow)
                                )
                            )
                            .clickable { scannerViewModel.openCameraScanner(appendToDocId = null) }
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF04101B))
                        Text(
                            text = if (isArabic) "تصوير مستند" else "Scan Document",
                            color = Color(0xFF04101B),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
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
                Text(if (isArabic) "إنشاء مجلد جديد" else "Create New Folder", color = ScannerGlassPalette.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
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

    // PDF Utility Tools Sheet (Merge, Split, Compress, Password-Protect)
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
private fun FolderCardPill(
    folder: ScannerFolderEntity,
    onClick: () -> Unit,
    onToggleLock: () -> Unit
) {
    val color = Color(folder.colorHex.toInt())
    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .scannerGlassSurface(RoundedCornerShape(16.dp), glowBorder = folder.isLocked)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Folder, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(folder.name, color = ScannerGlassPalette.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Lock folder",
            tint = if (folder.isLocked) ScannerGlassPalette.EmeraldGlow else ScannerGlassPalette.TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier
                .size(16.dp)
                .clickable(onClick = onToggleLock)
        )
    }
}

@Composable
private fun DocumentGridCard(
    doc: ScannedDocumentEntity,
    isArabic: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    onToggleStar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scannerGlassSurface(RoundedCornerShape(20.dp), glowBorder = doc.isStarred || doc.isLocked)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A1224)),
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = ScannerGlassPalette.EmeraldGlow, modifier = Modifier.size(38.dp))
            } else if (doc.coverThumbnailPath.isNotBlank() && File(doc.coverThumbnailPath).exists()) {
                Image(
                    painter = rememberAsyncImagePainter(File(doc.coverThumbnailPath)),
                    contentDescription = doc.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ScannerLaserIcon(modifier = Modifier.size(44.dp))
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
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
                Text(doc.title, color = ScannerGlassPalette.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatScannerBytes(doc.totalSizeBytes, isArabic), color = ScannerGlassPalette.TextSecondary, fontSize = 11.sp)
            }
            IconButton(onClick = onToggleStar, modifier = Modifier.size(30.dp)) {
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
            .scannerGlassSurface(RoundedCornerShape(18.dp), glowBorder = doc.isStarred || doc.isLocked)
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
            Text(doc.title, color = ScannerGlassPalette.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = if (isArabic) "${doc.pageCount} صفحات • ${formatScannerBytes(doc.totalSizeBytes, true)}" else "${doc.pageCount} pages • ${formatScannerBytes(doc.totalSizeBytes, false)}",
                color = ScannerGlassPalette.TextSecondary,
                fontSize = 11.5.sp
            )
            if (doc.tagsCsv.isNotBlank()) {
                Text(doc.tagsCsv.replace(",", " • "), color = ScannerGlassPalette.CyanGlow, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
