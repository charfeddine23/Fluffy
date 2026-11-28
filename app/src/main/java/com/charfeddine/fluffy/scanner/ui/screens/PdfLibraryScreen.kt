package com.charfeddine.fluffy.scanner.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charfeddine.fluffy.scanner.data.local.PdfReadingProgressStore
import com.charfeddine.fluffy.scanner.data.model.DevicePdfEntry
import com.charfeddine.fluffy.scanner.data.model.PdfLibrarySort
import com.charfeddine.fluffy.scanner.data.model.PdfReadingProgress
import com.charfeddine.fluffy.scanner.domain.ScannerViewModel
import com.charfeddine.fluffy.scanner.ui.theme.ScannerDarkLiquidBackdrop
import com.charfeddine.fluffy.scanner.ui.theme.ScannerGlassPalette
import com.charfeddine.fluffy.scanner.ui.theme.formatScannerBytes
import com.charfeddine.fluffy.scanner.ui.theme.iosLiquidGlass
import com.charfeddine.fluffy.scanner.ui.theme.scannerGlassSurface
import com.charfeddine.fluffy.scanner.utils.DevicePdfDiscovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfLibraryScreen(
    scannerViewModel: ScannerViewModel,
    progressStore: PdfReadingProgressStore,
    isArabic: Boolean,
    onOpenPdfReader: (DevicePdfEntry) -> Unit,
    onBackToHub: () -> Unit
) {
    BackHandler { onBackToHub() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allActiveDocs by scannerViewModel.allActiveDocs.collectAsState()
    val progressMap by progressStore.progressMap.collectAsState()
    val sortMode by progressStore.librarySort.collectAsState()

    var pdfEntries by remember { mutableStateOf<List<DevicePdfEntry>>(emptyList()) }
    var isScanning by remember { mutableStateOf(true) }
    var refreshSignal by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var filterTab by remember { mutableIntStateOf(0) } // 0=All, 1=In Progress, 2=Bookmarked
    var showSortMenu by remember { mutableStateOf(false) }

    val thumbCache = remember { mutableStateMapOf<String, Pair<Int, Bitmap?>>() }

    val folderTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            refreshSignal++
            scannerViewModel.postMessage(
                if (isArabic) "تم مسح المجلد وإضافة ملفات PDF ✓" else "Folder linked — scanning PDFs ✓"
            )
        }
    }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val pickedStrings = uris.map { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                uri.toString()
            }
            progressStore.addManualUris(pickedStrings)
            refreshSignal++
            if (uris.size == 1) {
                scope.launch {
                    val entry = withContext(Dispatchers.IO) {
                        DevicePdfDiscovery.readSingleContentPdfEntry(context, uris.first())
                    }
                    if (entry != null) {
                        onOpenPdfReader(entry)
                    }
                }
            }
        }
    }

    LaunchedEffect(refreshSignal, allActiveDocs) {
        isScanning = true
        try {
            val found = DevicePdfDiscovery.discoverAllDevicePdfs(
                context = context,
                progressStore = progressStore,
                scannerDocs = allActiveDocs
            )
            pdfEntries = found
        } finally {
            isScanning = false
        }
    }

    val continueEntry = remember(pdfEntries, progressMap) {
        val recentProg = progressMap.values
            .filter { it.lastReadAtMs > 0L }
            .maxByOrNull { it.lastReadAtMs }
            ?: return@remember null
        val matched = pdfEntries.firstOrNull { it.key == recentProg.pdfKey || it.uriString == recentProg.uriString }
        matched?.let { it to recentProg }
    }

    val filteredEntries = remember(pdfEntries, progressMap, searchQuery, filterTab, sortMode) {
        val q = searchQuery.trim()
        val base = pdfEntries.filter { entry ->
            val prog = progressMap[entry.key]
            val matchesSearch = q.isBlank() ||
                entry.title.contains(q, ignoreCase = true) ||
                entry.fileName.contains(q, ignoreCase = true) ||
                entry.folderLabel.contains(q, ignoreCase = true)
            val matchesTab = when (filterTab) {
                1 -> (prog?.lastReadAtMs ?: 0L) > 0L
                2 -> (prog?.bookmarkedPages?.isNotEmpty() == true)
                else -> true
            }
            matchesSearch && matchesTab
        }

        when (sortMode) {
            PdfLibrarySort.RECENT_READ -> base.sortedWith(
                compareByDescending<DevicePdfEntry> { progressMap[it.key]?.lastReadAtMs ?: 0L }
                    .thenByDescending { it.lastModifiedMs }
            )
            PdfLibrarySort.NEWEST -> base.sortedByDescending { it.lastModifiedMs }
            PdfLibrarySort.NAME_ASC -> base.sortedBy { it.title.lowercase() }
            PdfLibrarySort.SIZE_DESC -> base.sortedByDescending { it.sizeBytes }
        }
    }

    val hasSafTreeGrant = remember(refreshSignal) {
        runCatching {
            context.contentResolver.persistedUriPermissions.any { it.isReadPermission }
        }.getOrDefault(false)
    }

    ScannerDarkLiquidBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 14.dp)
        ) {
            // 1. Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onBackToHub,
                    modifier = Modifier
                        .size(48.dp)
                        .iosLiquidGlass(CircleShape, fillAlpha = 0.72f)
                        .testTag("pdf_library_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isArabic) "رجوع" else "Back",
                        tint = ScannerGlassPalette.TextPrimary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "قارئ ملفات PDF" else "Read PDF",
                        color = ScannerGlassPalette.TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        maxLines = 1
                    )
                    Text(
                        text = if (isArabic) {
                            "${pdfEntries.size} ملف متاح للقراءة فقط"
                        } else {
                            "${pdfEntries.size} PDFs • Read-Only Library"
                        },
                        color = ScannerGlassPalette.TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = { refreshSignal++ },
                    modifier = Modifier
                        .size(48.dp)
                        .iosLiquidGlass(CircleShape, fillAlpha = 0.72f)
                        .testTag("pdf_library_refresh_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (isArabic) "تحديث" else "Refresh",
                        tint = ScannerGlassPalette.CyanGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier
                        .size(48.dp)
                        .iosLiquidGlass(CircleShape, glowAccent = true, fillAlpha = 0.80f)
                        .testTag("pdf_library_pick_pdf_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (isArabic) "فتح ملف PDF" else "Browse PDF",
                        tint = ScannerGlassPalette.VioletGlow,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 2. Search Bar + Sort Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .scannerGlassSurface(CircleShape, glowBorder = searchQuery.isNotBlank())
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ScannerGlassPalette.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (isArabic) "ابحث عن أي ملف PDF..." else "Search all PDFs...",
                                color = ScannerGlassPalette.TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = ScannerGlassPalette.TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(ScannerGlassPalette.CyanGlow),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pdf_library_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isArabic) "مسح البحث" else "Clear search",
                                tint = ScannerGlassPalette.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Box {
                    Row(
                        modifier = Modifier
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .iosLiquidGlass(CircleShape, fillAlpha = 0.72f)
                            .clickable { showSortMenu = true }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = if (isArabic) "فرز" else "Sort",
                            tint = ScannerGlassPalette.VioletGlow,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = if (isArabic) sortMode.labelAr else sortMode.labelEn,
                            color = ScannerGlassPalette.TextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        PdfLibrarySort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(if (isArabic) option.labelAr else option.labelEn) },
                                onClick = {
                                    progressStore.setLibrarySort(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Filter Pills + Scan All Folders Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibraryFilterChip(
                    label = if (isArabic) "الكل (${pdfEntries.size})" else "All (${pdfEntries.size})",
                    selected = filterTab == 0,
                    accent = ScannerGlassPalette.CyanGlow,
                    tag = "pdf_filter_all",
                    onClick = { filterTab = 0 }
                )
                LibraryFilterChip(
                    label = if (isArabic) "قيد القراءة" else "Continue Reading",
                    selected = filterTab == 1,
                    accent = ScannerGlassPalette.VioletGlow,
                    tag = "pdf_filter_reading",
                    onClick = { filterTab = 1 }
                )
                LibraryFilterChip(
                    label = if (isArabic) "صفحات محفوظة" else "Saved Pages",
                    selected = filterTab == 2,
                    accent = ScannerGlassPalette.AmberGlow,
                    tag = "pdf_filter_bookmarked",
                    onClick = { filterTab = 2 }
                )
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .minimumInteractiveComponentSize()
                        .iosLiquidGlass(CircleShape, glowAccent = !hasSafTreeGrant, fillAlpha = 0.72f)
                        .clickable { folderTreeLauncher.launch(null) }
                        .testTag("pdf_library_scan_folders_btn")
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = ScannerGlassPalette.EmeraldGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isArabic) "فحص مجلدات الهاتف" else "Scan Phone Folders",
                        color = ScannerGlassPalette.TextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Main Library List
            if (isScanning && pdfEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = ScannerGlassPalette.VioletGlow)
                        Text(
                            text = if (isArabic) "جاري البحث عن جميع ملفات PDF..." else "Scanning device for PDF files...",
                            color = ScannerGlassPalette.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Resume Hero Card
                    if (continueEntry != null && searchQuery.isBlank() && filterTab == 0) {
                        item(key = "resume_hero") {
                            ContinueReadingHeroCard(
                                entry = continueEntry.first,
                                progress = continueEntry.second,
                                isArabic = isArabic,
                                onClick = { onOpenPdfReader(continueEntry.first) }
                            )
                        }
                    }

                    // Compact Folder Permission Helper Card (zero-permission SAF tree access)
                    if (!hasSafTreeGrant) {
                        item(key = "saf_folder_hint") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .iosLiquidGlass(RoundedCornerShape(18.dp), glowAccent = true, fillAlpha = 0.64f)
                                    .clickable { folderTreeLauncher.launch(null) }
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = ScannerGlassPalette.EmeraldGlow,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) "عرض كل ملفات PDF في أي مجلد" else "Include PDFs from any phone folder",
                                        color = ScannerGlassPalette.TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                    Text(
                                        text = if (isArabic) {
                                            "اضغط لربط مجلد (مثل التنزيلات أو المستندات) لقراءتها جميعاً فوراً"
                                        } else {
                                            "Tap to link Downloads, Documents, or Storage folders safely"
                                        },
                                        color = ScannerGlassPalette.TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = ScannerGlassPalette.EmeraldGlow
                                )
                            }
                        }
                    }

                    if (filteredEntries.isEmpty()) {
                        item(key = "empty_pdf_state") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 28.dp)
                                    .iosLiquidGlass(RoundedCornerShape(24.dp), glowAccent = true, fillAlpha = 0.68f)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = ScannerGlassPalette.VioletGlow,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = if (isArabic) "لا توجد ملفات PDF مطابقة حالياً" else "No matching PDFs found yet",
                                    color = ScannerGlassPalette.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isArabic) {
                                        "يمكنك فحص مجلدات الهاتف أو اختيار ملفات PDF مباشرة للقراءة"
                                    } else {
                                        "Link a phone folder or pick PDF files directly for distraction-free reading"
                                    },
                                    color = ScannerGlassPalette.TextSecondary,
                                    fontSize = 12.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { folderTreeLauncher.launch(null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.EmeraldGlow)
                                    ) {
                                        Text(
                                            text = if (isArabic) "فحص مجلد" else "Scan Folder",
                                            color = ScannerGlassPalette.OnAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Button(
                                        onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.VioletGlow)
                                    ) {
                                        Text(
                                            text = if (isArabic) "اختيار PDF" else "Pick PDF",
                                            color = ScannerGlassPalette.OnAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredEntries, key = { it.key }) { entry ->
                            val cached = thumbCache[entry.key]
                            LaunchedEffect(entry.key) {
                                if (cached == null) {
                                    val loaded = DevicePdfDiscovery.loadPdfMetadataAndThumb(
                                        context = context,
                                        uri = Uri.parse(entry.uriString),
                                        maxThumbSide = 220
                                    )
                                    thumbCache[entry.key] = loaded
                                }
                            }

                            val progress = progressMap[entry.key]
                            val resolvedPages = maxOf(
                                entry.pageCount,
                                progress?.totalPages ?: 0,
                                cached?.first ?: 0
                            )

                            DevicePdfRowCard(
                                entry = entry,
                                pageCount = resolvedPages,
                                thumbnail = cached?.second,
                                progress = progress,
                                isArabic = isArabic,
                                onClick = { onOpenPdfReader(entry.copy(pageCount = resolvedPages)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryFilterChip(
    label: String,
    selected: Boolean,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .minimumInteractiveComponentSize()
            .testTag(tag)
            .iosLiquidGlass(
                shape = CircleShape,
                glowAccent = selected,
                fillAlpha = if (selected) 0.88f else 0.56f
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) accent else ScannerGlassPalette.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun ContinueReadingHeroCard(
    entry: DevicePdfEntry,
    progress: PdfReadingProgress,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val total = maxOf(entry.pageCount, progress.totalPages, 1)
    val pageHuman = (progress.lastPageIndex + 1).coerceIn(1, total)
    val pct = ((pageHuman.toFloat() / total.toFloat()) * 100f).toInt().coerceIn(0, 100)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pdf_resume_hero_btn")
            .iosLiquidGlass(RoundedCornerShape(22.dp), glowAccent = true, fillAlpha = 0.84f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(ScannerGlassPalette.VioletGlow.copy(alpha = 0.22f))
                    .border(1.dp, ScannerGlassPalette.VioletGlow.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = ScannerGlassPalette.VioletGlow,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isArabic) "استكمال القراءة" else "Continue Reading",
                    color = ScannerGlassPalette.VioletGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = entry.title,
                    color = ScannerGlassPalette.TextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (isArabic) "ص $pageHuman/$total" else "P.$pageHuman/$total",
                color = ScannerGlassPalette.CyanGlow,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        LinearProgressIndicator(
            progress = { pct / 100f },
            color = ScannerGlassPalette.VioletGlow,
            trackColor = ScannerGlassPalette.SubtleStroke,
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
private fun DevicePdfRowCard(
    entry: DevicePdfEntry,
    pageCount: Int,
    thumbnail: Bitmap?,
    progress: PdfReadingProgress?,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val thumbImg = remember(thumbnail) { thumbnail?.asImageBitmap() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pdf_library_item_${entry.key}")
            .iosLiquidGlass(RoundedCornerShape(20.dp), glowAccent = (progress?.lastReadAtMs ?: 0L) > 0L, fillAlpha = 0.72f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 50.dp, height = 66.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ScannerGlassPalette.ThumbnailFrame)
                .border(0.9.dp, ScannerGlassPalette.VioletGlow.copy(alpha = 0.42f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbImg != null) {
                Image(
                    bitmap = thumbImg,
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = entry.title,
                    tint = ScannerGlassPalette.VioletGlow,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = entry.title,
                color = ScannerGlassPalette.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = entry.folderLabel,
                    color = ScannerGlassPalette.CyanGlow,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                if (pageCount > 0) {
                    Text("•", color = ScannerGlassPalette.TextSecondary, fontSize = 10.5.sp)
                    Text(
                        text = if (isArabic) "$pageCount ص" else "$pageCount p",
                        color = ScannerGlassPalette.TextSecondary,
                        fontSize = 10.5.sp
                    )
                }
                if (entry.sizeBytes > 0L) {
                    Text("•", color = ScannerGlassPalette.TextSecondary, fontSize = 10.5.sp)
                    Text(
                        text = formatScannerBytes(entry.sizeBytes, isArabic),
                        color = ScannerGlassPalette.TextSecondary,
                        fontSize = 10.5.sp
                    )
                }
            }

            if (progress != null && progress.lastReadAtMs > 0L && pageCount > 0) {
                val cur = (progress.lastPageIndex + 1).coerceIn(1, pageCount)
                val frac = (cur.toFloat() / pageCount.toFloat()).coerceIn(0f, 1f)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { frac },
                        color = ScannerGlassPalette.CyanGlow,
                        trackColor = ScannerGlassPalette.SubtleStroke,
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = "$cur/$pageCount",
                        color = ScannerGlassPalette.CyanGlow,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (progress != null && progress.bookmarkedPages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ScannerGlassPalette.AmberGlow.copy(alpha = 0.16f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = ScannerGlassPalette.AmberGlow,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "${progress.bookmarkedPages.size}",
                    color = ScannerGlassPalette.AmberGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
