package com.charfeddine.fluffy.scanner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.charfeddine.fluffy.scanner.data.model.AnnotationTool
import com.charfeddine.fluffy.scanner.data.model.InkStroke
import com.charfeddine.fluffy.scanner.data.model.PageAnnotations
import com.charfeddine.fluffy.scanner.data.model.PdfPageSizePreset
import com.charfeddine.fluffy.scanner.data.model.PointNorm
import com.charfeddine.fluffy.scanner.data.model.RedactionRect
import com.charfeddine.fluffy.scanner.data.model.ScannedDocumentEntity
import com.charfeddine.fluffy.scanner.data.model.ScannedPageEntity
import com.charfeddine.fluffy.scanner.data.model.ScannerTagCatalog
import com.charfeddine.fluffy.scanner.data.model.SignaturePlacement
import com.charfeddine.fluffy.scanner.data.model.WatermarkConfig
import com.charfeddine.fluffy.scanner.domain.ScannerViewModel
import com.charfeddine.fluffy.scanner.ui.components.SignatureStudioDialog
import com.charfeddine.fluffy.scanner.ui.components.WatermarkConfigDialog
import com.charfeddine.fluffy.scanner.ui.theme.ScannerDarkLiquidBackdrop
import com.charfeddine.fluffy.scanner.ui.theme.ScannerGlassPalette
import com.charfeddine.fluffy.scanner.ui.theme.scannerGlassSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun DocumentDetailEditorScreen(
    scannerViewModel: ScannerViewModel,
    documentId: String,
    isArabic: Boolean,
    onAddMorePages: () -> Unit,
    onBackToHub: () -> Unit
) {
    BackHandler { onBackToHub() }

    val context = LocalContext.current
    val docState by scannerViewModel.observeDocument(documentId).collectAsState(initial = null)
    val pages by scannerViewModel.observePages(documentId).collectAsState(initial = emptyList())
    val savedSignatures by scannerViewModel.savedSignatures.collectAsState()
    val isBusy by scannerViewModel.isBusy.collectAsState()

    val doc = docState ?: return
    var activePageIndex by remember { mutableIntStateOf(0) }
    val currentPage: ScannedPageEntity? = pages.getOrNull(activePageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0)))

    var activeTool by remember { mutableStateOf(AnnotationTool.NONE) }
    var penColorArgb by remember { mutableLongStateOf(0xFF0EA5E9L) }
    var redactionBlack by remember { mutableStateOf(true) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showWatermarkDialog by remember { mutableStateOf(false) }
    var showOcrDialog by remember { mutableStateOf(false) }
    var ocrResultText by remember { mutableStateOf("") }
    var showExportSheet by remember { mutableStateOf(false) }

    var activeSignaturePlacementId by remember { mutableStateOf<String?>(null) }

    val activeAnnotations = remember(currentPage?.annotationsJson) {
        PageAnnotations.fromJson(currentPage?.annotationsJson)
    }
    val activeSignatures = remember(currentPage?.signaturesJson) {
        SignaturePlacement.listFromJson(currentPage?.signaturesJson)
    }
    val activeWatermark = remember(currentPage?.watermarkJson) {
        WatermarkConfig.fromJson(currentPage?.watermarkJson)
    }

    var pageBitmap by remember(currentPage?.id, currentPage?.processedImagePath) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(currentPage?.id, currentPage?.processedImagePath, doc.isLocked) {
        val path = currentPage?.processedImagePath ?: return@LaunchedEffect
        pageBitmap = withContext(Dispatchers.IO) {
            scannerViewModel.repository.decodePageBitmap(path)
        }
    }

    ScannerDarkLiquidBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBackToHub,
                        modifier = Modifier
                            .size(46.dp)
                            .scannerGlassSurface(CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back",
                            tint = ScannerGlassPalette.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showRenameDialog = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = doc.title,
                                color = ScannerGlassPalette.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(Icons.Default.Edit, contentDescription = null, tint = ScannerGlassPalette.CyanGlow, modifier = Modifier.size(13.dp))
                        }
                        Text(
                            text = if (isArabic) "${pages.size} صفحات" else "${pages.size} page(s)",
                            color = ScannerGlassPalette.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { scannerViewModel.toggleStarred(doc) },
                        modifier = Modifier.size(44.dp).scannerGlassSurface(CircleShape)
                    ) {
                        Icon(
                            imageVector = if (doc.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star",
                            tint = if (doc.isStarred) ScannerGlassPalette.AmberGlow else ScannerGlassPalette.TextSecondary
                        )
                    }
                    IconButton(
                        onClick = { scannerViewModel.toggleDocumentLock(doc.id, !doc.isLocked, isArabic) },
                        modifier = Modifier.size(44.dp).scannerGlassSurface(CircleShape, glowBorder = doc.isLocked)
                    ) {
                        Icon(
                            imageVector = if (doc.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock",
                            tint = if (doc.isLocked) ScannerGlassPalette.EmeraldGlow else ScannerGlassPalette.TextSecondary
                        )
                    }
                    IconButton(
                        onClick = { showExportSheet = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("doc_export_btn")
                            .scannerGlassSurface(CircleShape, glowBorder = true)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export/Share", tint = ScannerGlassPalette.CyanGlow)
                    }
                    IconButton(
                        onClick = { scannerViewModel.moveToTrash(doc.id, isArabic) },
                        modifier = Modifier.size(44.dp).scannerGlassSurface(CircleShape)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Trash", tint = ScannerGlassPalette.RoseGlow)
                    }
                }
            }

            // Compact Active Tags + "+ Tag" Dropdown Selector
            var showTagsDropdown by remember { mutableStateOf(false) }
            val activeTags = remember(doc.tagsCsv) {
                doc.tagsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .scannerGlassSurface(CircleShape)
                                .clickable { showTagsDropdown = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = null,
                                tint = ScannerGlassPalette.CyanGlow,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isArabic) "الوسوم" else "Tags",
                                color = ScannerGlassPalette.TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        DropdownMenu(
                            expanded = showTagsDropdown,
                            onDismissRequest = { showTagsDropdown = false },
                            containerColor = ScannerGlassPalette.MidnightNavy
                        ) {
                            ScannerTagCatalog.presets.forEach { preset ->
                                val tagColor = Color(preset.colorArgb.toInt())
                                val selected = preset.name in activeTags
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(tagColor)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = if (selected) "${preset.name} ✓" else preset.name,
                                            color = if (selected) tagColor else ScannerGlassPalette.TextPrimary,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        scannerViewModel.toggleTagOnDocument(doc, preset.name)
                                    }
                                )
                            }
                        }
                    }
                }

                itemsIndexed(activeTags) { _, tagName ->
                    val preset = ScannerTagCatalog.presets.firstOrNull { it.name == tagName }
                    val tagColor = preset?.let { Color(it.colorArgb.toInt()) } ?: ScannerGlassPalette.CyanGlow
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(tagColor.copy(alpha = 0.20f))
                            .border(1.dp, tagColor.copy(alpha = 0.65f), CircleShape)
                            .clickable { scannerViewModel.toggleTagOnDocument(doc, tagName) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$tagName ×",
                            color = tagColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Main Interactive Page Viewer & Markup Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .scannerGlassSurface(RoundedCornerShape(22.dp), glowBorder = activeTool != AnnotationTool.NONE)
                    .clipToBounds()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = pageBitmap
                val p = currentPage
                if (bmp == null || p == null) {
                    CircularProgressIndicator(color = ScannerGlassPalette.CyanGlow)
                } else {
                    InteractiveAnnotatedPageCanvas(
                        bitmap = bmp,
                        annotations = activeAnnotations,
                        signatures = activeSignatures,
                        watermark = activeWatermark,
                        activeTool = activeTool,
                        penColorArgb = penColorArgb,
                        redactionColorArgb = if (redactionBlack) 0xFF000000L else 0xFFFFFFFFL,
                        activeSignaturePlacementId = activeSignaturePlacementId,
                        loadSignatureBitmap = { sigId ->
                            savedSignatures.firstOrNull { it.id == sigId }?.let {
                                scannerViewModel.repository.loadEncryptedSignatureBitmap(it)
                            }
                        },
                        onSelectSignature = { activeSignaturePlacementId = it },
                        onAnnotationsChanged = { updatedAnn ->
                            scannerViewModel.updatePageMarkups(p, updatedAnn, activeSignatures, activeWatermark)
                        },
                        onSignaturesChanged = { updatedSigs ->
                            scannerViewModel.updatePageMarkups(p, activeAnnotations, updatedSigs, activeWatermark)
                        }
                    )
                }

                // Selected Placed Signature Floating Resize / Rotate / Delete Controls
                val selPlacement = activeSignatures.firstOrNull { it.placementId == activeSignaturePlacementId }
                if (selPlacement != null && p != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp)
                            .scannerGlassSurface(CircleShape, glowBorder = true)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("S-", color = ScannerGlassPalette.CyanGlow, fontWeight = FontWeight.Bold, modifier = Modifier
                            .clickable {
                                val next = activeSignatures.map {
                                    if (it.placementId == selPlacement.placementId) it.copy(scale = (it.scale - 0.15f).coerceAtLeast(0.45f)) else it
                                }
                                scannerViewModel.updatePageMarkups(p, activeAnnotations, next, activeWatermark)
                            }
                            .padding(6.dp))
                        Text("S+", color = ScannerGlassPalette.CyanGlow, fontWeight = FontWeight.Bold, modifier = Modifier
                            .clickable {
                                val next = activeSignatures.map {
                                    if (it.placementId == selPlacement.placementId) it.copy(scale = (it.scale + 0.15f).coerceAtMost(2.5f)) else it
                                }
                                scannerViewModel.updatePageMarkups(p, activeAnnotations, next, activeWatermark)
                            }
                            .padding(6.dp))
                        Text("↻15°", color = ScannerGlassPalette.VioletGlow, fontWeight = FontWeight.Bold, modifier = Modifier
                            .clickable {
                                val next = activeSignatures.map {
                                    if (it.placementId == selPlacement.placementId) it.copy(rotationDeg = (it.rotationDeg + 15f) % 360f) else it
                                }
                                scannerViewModel.updatePageMarkups(p, activeAnnotations, next, activeWatermark)
                            }
                            .padding(6.dp))
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Remove signature",
                            tint = ScannerGlassPalette.RoseGlow,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    val next = activeSignatures.filterNot { it.placementId == selPlacement.placementId }
                                    activeSignaturePlacementId = null
                                    scannerViewModel.updatePageMarkups(p, activeAnnotations, next, activeWatermark)
                                }
                        )
                    }
                }

                if (isBusy) {
                    CircularProgressIndicator(
                        color = ScannerGlassPalette.CyanGlow,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Page Management Strip (Reorder L/R, Duplicate, Extract, Delete, + Add Page)
            if (currentPage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scannerGlassSurface(RoundedCornerShape(18.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(pages, key = { _, pg -> pg.id }) { idx, _ ->
                            val selected = idx == activePageIndex
                            Box(
                                modifier = Modifier
                                    .size(width = 38.dp, height = 44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) ScannerGlassPalette.CyanGlow else Color.White.copy(alpha = 0.08f))
                                    .clickable { activePageIndex = idx },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    color = if (selected) Color(0xFF04101B) else ScannerGlassPalette.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Reorder Move Left / Right
                    IconButton(
                        onClick = {
                            if (activePageIndex > 0) {
                                scannerViewModel.movePageOrder(doc.id, activePageIndex, activePageIndex - 1)
                                activePageIndex--
                            }
                        },
                        enabled = activePageIndex > 0,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Move Earlier", tint = ScannerGlassPalette.TextPrimary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(
                        onClick = {
                            if (activePageIndex < pages.lastIndex) {
                                scannerViewModel.movePageOrder(doc.id, activePageIndex, activePageIndex + 1)
                                activePageIndex++
                            }
                        },
                        enabled = activePageIndex < pages.lastIndex,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "Move Later", tint = ScannerGlassPalette.TextPrimary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(
                        onClick = { scannerViewModel.duplicatePage(doc.id, currentPage, isArabic) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.CopyAll, contentDescription = "Duplicate Page", tint = ScannerGlassPalette.CyanGlow, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { scannerViewModel.extractPageAsNewDocument(doc, currentPage, isArabic) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Output, contentDescription = "Extract Page", tint = ScannerGlassPalette.VioletGlow, modifier = Modifier.size(18.dp))
                    }
                    if (pages.size > 1) {
                        IconButton(
                            onClick = {
                                scannerViewModel.deletePage(doc.id, currentPage.id, isArabic)
                                activePageIndex = (activePageIndex - 1).coerceAtLeast(0)
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Page", tint = ScannerGlassPalette.RoseGlow, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(
                        onClick = onAddMorePages,
                        modifier = Modifier
                            .size(36.dp)
                            .scannerGlassSurface(CircleShape, glowBorder = true)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Page", tint = ScannerGlassPalette.CyanGlow, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Annotation color / undo strip when Pen, Highlighter, or Redaction active
            if (activeTool != AnnotationTool.NONE && currentPage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scannerGlassSurface(CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeTool == AnnotationTool.REDACTION) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (redactionBlack) Color.Black else Color.DarkGray)
                                    .border(1.5.dp, if (redactionBlack) ScannerGlassPalette.CyanGlow else Color.Transparent, CircleShape)
                                    .clickable { redactionBlack = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(if (isArabic) "طمس أسود" else "Black Redact", color = Color.White, fontSize = 11.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.5.dp, if (!redactionBlack) ScannerGlassPalette.CyanGlow else Color.Transparent, CircleShape)
                                    .clickable { redactionBlack = false }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(if (isArabic) "طمس أبيض" else "White Redact", color = Color.Black, fontSize = 11.sp)
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val palette = if (activeTool == AnnotationTool.HIGHLIGHTER) {
                                listOf(0xFFFDE047L, 0xFF34D399L, 0xFF38BDF8L, 0xFFF472B6L)
                            } else {
                                listOf(0xFF0EA5E9L, 0xFFEF4444L, 0xFF10B981L, 0xFF0F172AL)
                            }
                            palette.forEach { argb ->
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(argb.toInt()))
                                        .border(
                                            if (penColorArgb == argb) 2.dp else 1.dp,
                                            if (penColorArgb == argb) Color.White else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { penColorArgb = argb }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            val nextAnn = if (activeTool == AnnotationTool.REDACTION && activeAnnotations.redactions.isNotEmpty()) {
                                activeAnnotations.copy(redactions = activeAnnotations.redactions.dropLast(1))
                            } else if (activeAnnotations.strokes.isNotEmpty()) {
                                activeAnnotations.copy(strokes = activeAnnotations.strokes.dropLast(1))
                            } else activeAnnotations
                            scannerViewModel.updatePageMarkups(currentPage, nextAnn, activeSignatures, activeWatermark)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = ScannerGlassPalette.AmberGlow, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Bottom Markup & OCR Dock
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .scannerGlassSurface(RoundedCornerShape(22.dp), glowBorder = true)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolDockPill(
                    icon = Icons.Default.BorderColor,
                    label = if (isArabic) "قلم" else "Pen",
                    active = activeTool == AnnotationTool.PEN,
                    onClick = {
                        activeTool = if (activeTool == AnnotationTool.PEN) AnnotationTool.NONE else AnnotationTool.PEN
                    }
                )
                ToolDockPill(
                    icon = Icons.Default.Highlight,
                    label = if (isArabic) "تظليل" else "Highlight",
                    active = activeTool == AnnotationTool.HIGHLIGHTER,
                    onClick = {
                        activeTool = if (activeTool == AnnotationTool.HIGHLIGHTER) AnnotationTool.NONE else AnnotationTool.HIGHLIGHTER
                        if (activeTool == AnnotationTool.HIGHLIGHTER) penColorArgb = 0xFFFDE047L
                    }
                )
                ToolDockPill(
                    icon = Icons.Default.Rectangle,
                    label = if (isArabic) "طمس" else "Redact",
                    active = activeTool == AnnotationTool.REDACTION,
                    onClick = {
                        activeTool = if (activeTool == AnnotationTool.REDACTION) AnnotationTool.NONE else AnnotationTool.REDACTION
                    }
                )
                ToolDockPill(
                    icon = Icons.Default.Draw,
                    label = if (isArabic) "توقيع" else "Sign",
                    active = showSignatureDialog,
                    onClick = { showSignatureDialog = true }
                )
                ToolDockPill(
                    icon = Icons.Default.WaterDrop,
                    label = if (isArabic) "علامة" else "Watermark",
                    active = activeWatermark != null,
                    onClick = { showWatermarkDialog = true }
                )
                ToolDockPill(
                    icon = Icons.Default.TextSnippet,
                    label = "OCR",
                    active = showOcrDialog,
                    onClick = {
                        currentPage?.let { pg ->
                            scannerViewModel.runOfflineOcrOnPage(pg, isArabic) { text ->
                                ocrResultText = text
                                if (text.isNotBlank()) showOcrDialog = true
                            }
                        }
                    }
                )
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        var newName by remember(doc.title) { mutableStateOf(doc.title) }
        Dialog(onDismissRequest = { showRenameDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scannerGlassSurface(RoundedCornerShape(22.dp), glowBorder = true, fillAlpha = 0.94f)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(if (isArabic) "إعادة تسمية المستند" else "Rename Document", color = ScannerGlassPalette.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScannerGlassPalette.CyanGlow,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                        focusedTextColor = ScannerGlassPalette.TextPrimary,
                        unfocusedTextColor = ScannerGlassPalette.TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showRenameDialog = false }) { Text(if (isArabic) "إلغاء" else "Cancel", color = ScannerGlassPalette.TextSecondary) }
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                        onClick = {
                            scannerViewModel.renameDocument(doc, newName)
                            showRenameDialog = false
                        }
                    ) {
                        Text(if (isArabic) "حفظ" else "Save", color = Color(0xFF04101B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // E-Signature Studio Dialog
    SignatureStudioDialog(
        visible = showSignatureDialog,
        isArabic = isArabic,
        savedSignatures = savedSignatures,
        loadSignatureBitmap = { sig -> scannerViewModel.repository.loadEncryptedSignatureBitmap(sig) },
        onSelectSavedSignature = { sig ->
            currentPage?.let { p ->
                val placed = SignaturePlacement(signatureId = sig.id)
                activeSignaturePlacementId = placed.placementId
                scannerViewModel.updatePageMarkups(p, activeAnnotations, activeSignatures + placed, activeWatermark)
            }
        },
        onDeleteSignature = { scannerViewModel.deleteSignature(it) },
        onCreateSignature = { name, bmp, colorArgb ->
            scannerViewModel.saveNewSignature(name, bmp, colorArgb, isArabic)
        },
        onDismiss = { showSignatureDialog = false }
    )

    // Watermark Dialog
    WatermarkConfigDialog(
        visible = showWatermarkDialog,
        initial = activeWatermark,
        isArabic = isArabic,
        onSave = { wm ->
            currentPage?.let { p ->
                scannerViewModel.updatePageMarkups(p, activeAnnotations, activeSignatures, wm)
            }
        },
        onDismiss = { showWatermarkDialog = false }
    )

    // Offline OCR Result Dialog (Selectable & One-Tap Copy)
    if (showOcrDialog) {
        Dialog(onDismissRequest = { showOcrDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scannerGlassSurface(RoundedCornerShape(24.dp), glowBorder = true, fillAlpha = 0.95f)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "النص المستخرج (Offline OCR)" else "Extracted OCR Text (Offline)",
                    color = ScannerGlassPalette.CyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .scannerGlassSurface(RoundedCornerShape(14.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    SelectionContainer {
                        Text(text = ocrResultText, color = ScannerGlassPalette.TextPrimary, fontSize = 13.5.sp)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { showOcrDialog = false }) {
                        Text(if (isArabic) "إغلاق" else "Close", color = ScannerGlassPalette.TextSecondary)
                    }
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("OCR Text", ocrResultText))
                            Toast.makeText(context, if (isArabic) "تم نسخ النص ✓" else "Text copied ✓", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF04101B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "نسخ الكل" else "Copy All", color = Color(0xFF04101B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Export & Share Modal Sheet
    if (showExportSheet) {
        DocumentExportSheet(
            doc = doc,
            isArabic = isArabic,
            onExport = { preset, asImages, searchable, pwd, shareSheet ->
                scannerViewModel.exportDocument(doc, preset, asImages, searchable, pwd, shareSheet, isArabic)
                showExportSheet = false
            },
            onDismiss = { showExportSheet = false }
        )
    }
}

@Composable
private fun ToolDockPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) ScannerGlassPalette.CyanGlow.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextPrimary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (active) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun InteractiveAnnotatedPageCanvas(
    bitmap: Bitmap,
    annotations: PageAnnotations,
    signatures: List<SignaturePlacement>,
    watermark: WatermarkConfig?,
    activeTool: AnnotationTool,
    penColorArgb: Long,
    redactionColorArgb: Long,
    activeSignaturePlacementId: String?,
    loadSignatureBitmap: (String) -> Bitmap?,
    onSelectSignature: (String?) -> Unit,
    onAnnotationsChanged: (PageAnnotations) -> Unit,
    onSignaturesChanged: (List<SignaturePlacement>) -> Unit
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var draftPoints by remember { mutableStateOf<List<PointNorm>>(emptyList()) }
    var draftRedactStart by remember { mutableStateOf<PointNorm?>(null) }
    var draftRedactEnd by remember { mutableStateOf<PointNorm?>(null) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
    ) {
        val bw = bitmap.width.toFloat().coerceAtLeast(1f)
        val bh = bitmap.height.toFloat().coerceAtLeast(1f)
        val cw = boxSize.width.toFloat().coerceAtLeast(1f)
        val ch = boxSize.height.toFloat().coerceAtLeast(1f)
        val scale = minOf(cw / bw, ch / bh)
        val drawW = bw * scale
        val drawH = bh * scale
        val offsetX = (cw - drawW) / 2f
        val offsetY = (ch - drawH) / 2f

        fun toNorm(pos: Offset) = PointNorm(
            x = ((pos.x - offsetX) / drawW).coerceIn(0f, 1f),
            y = ((pos.y - offsetY) / drawH).coerceIn(0f, 1f)
        )

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Document page",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeTool, annotations, penColorArgb, redactionColorArgb, boxSize) {
                    if (activeTool == AnnotationTool.NONE) return@pointerInput
                    detectDragGestures(
                        onDragStart = { pos ->
                            val norm = toNorm(pos)
                            if (activeTool == AnnotationTool.REDACTION) {
                                draftRedactStart = norm
                                draftRedactEnd = norm
                            } else {
                                draftPoints = listOf(norm)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val norm = toNorm(change.position)
                            if (activeTool == AnnotationTool.REDACTION) {
                                draftRedactEnd = norm
                            } else {
                                draftPoints = draftPoints + norm
                            }
                        },
                        onDragEnd = {
                            if (activeTool == AnnotationTool.REDACTION) {
                                val s = draftRedactStart
                                val e = draftRedactEnd
                                if (s != null && e != null && abs(e.x - s.x) > 0.02f && abs(e.y - s.y) > 0.02f) {
                                    val rect = RedactionRect(
                                        left = min(s.x, e.x),
                                        top = min(s.y, e.y),
                                        right = max(s.x, e.x),
                                        bottom = max(s.y, e.y),
                                        colorArgb = redactionColorArgb
                                    )
                                    onAnnotationsChanged(annotations.copy(redactions = annotations.redactions + rect))
                                }
                                draftRedactStart = null
                                draftRedactEnd = null
                            } else if (draftPoints.size >= 2) {
                                val stroke = InkStroke(
                                    points = draftPoints,
                                    colorArgb = penColorArgb,
                                    strokeWidthNorm = if (activeTool == AnnotationTool.HIGHLIGHTER) 0.024f else 0.007f,
                                    isHighlighter = activeTool == AnnotationTool.HIGHLIGHTER
                                )
                                onAnnotationsChanged(annotations.copy(strokes = annotations.strokes + stroke))
                                draftPoints = emptyList()
                            }
                        },
                        onDragCancel = {
                            draftPoints = emptyList()
                            draftRedactStart = null
                            draftRedactEnd = null
                        }
                    )
                }
        ) {
            fun drawStroke(pts: List<PointNorm>, colorArgb: Long, widthNorm: Float, hl: Boolean) {
                if (pts.size < 2) return
                val path = Path().apply {
                    pts.forEachIndexed { idx, p ->
                        val px = offsetX + p.x * drawW
                        val py = offsetY + p.y * drawH
                        if (idx == 0) moveTo(px, py) else lineTo(px, py)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(colorArgb.toInt()).copy(alpha = if (hl) 0.38f else 1f),
                    style = Stroke(
                        width = (widthNorm * min(drawW, drawH)).coerceAtLeast(3f),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            annotations.strokes.forEach { s -> drawStroke(s.points, s.colorArgb, s.strokeWidthNorm, s.isHighlighter) }
            if (draftPoints.size >= 2) {
                drawStroke(
                    draftPoints,
                    penColorArgb,
                    if (activeTool == AnnotationTool.HIGHLIGHTER) 0.024f else 0.007f,
                    activeTool == AnnotationTool.HIGHLIGHTER
                )
            }

            annotations.redactions.forEach { r ->
                drawRect(
                    color = Color(r.colorArgb.toInt()),
                    topLeft = Offset(offsetX + r.left * drawW, offsetY + r.top * drawH),
                    size = androidx.compose.ui.geometry.Size((r.right - r.left) * drawW, (r.bottom - r.top) * drawH)
                )
            }
            val rs = draftRedactStart
            val re = draftRedactEnd
            if (rs != null && re != null) {
                drawRect(
                    color = Color(redactionColorArgb.toInt()).copy(alpha = 0.8f),
                    topLeft = Offset(offsetX + min(rs.x, re.x) * drawW, offsetY + min(rs.y, re.y) * drawH),
                    size = androidx.compose.ui.geometry.Size(abs(re.x - rs.x) * drawW, abs(re.y - rs.y) * drawH)
                )
            }
        }

        // Draggable Placed E-Signatures
        signatures.forEach { placement ->
            val sigBmp = remember(placement.signatureId) { loadSignatureBitmap(placement.signatureId) }
            if (sigBmp != null && boxSize.width > 0) {
                val targetW = drawW * 0.28f * placement.scale
                val aspect = sigBmp.height.toFloat() / sigBmp.width.coerceAtLeast(1).toFloat()
                val targetH = targetW * aspect
                val leftPx = offsetX + placement.centerXNorm * drawW - targetW / 2f
                val topPx = offsetY + placement.centerYNorm * drawH - targetH / 2f
                val isSelected = placement.placementId == activeSignaturePlacementId

                Image(
                    bitmap = sigBmp.asImageBitmap(),
                    contentDescription = "Placed Signature",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                        .size(
                            width = with(density) { targetW.toDp() },
                            height = with(density) { targetH.toDp() }
                        )
                        .graphicsLayer { rotationZ = placement.rotationDeg }
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) ScannerGlassPalette.CyanGlow else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectSignature(placement.placementId) }
                        .pointerInput(placement.placementId, signatures) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                onSelectSignature(placement.placementId)
                                val dx = drag.x / drawW
                                val dy = drag.y / drawH
                                val updated = signatures.map {
                                    if (it.placementId == placement.placementId) {
                                        it.copy(
                                            centerXNorm = (it.centerXNorm + dx).coerceIn(0.08f, 0.92f),
                                            centerYNorm = (it.centerYNorm + dy).coerceIn(0.08f, 0.92f)
                                        )
                                    } else it
                                }
                                onSignaturesChanged(updated)
                            }
                        }
                )
            }
        }

        // Custom Watermark Preview Overlay
        if (watermark != null && watermark.text.isNotBlank()) {
            Text(
                text = watermark.text,
                color = Color(watermark.colorArgb.toInt()).copy(alpha = watermark.opacity),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .rotate(watermark.angleDeg)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentExportSheet(
    doc: ScannedDocumentEntity,
    isArabic: Boolean,
    onExport: (PdfPageSizePreset, asImages: Boolean, searchable: Boolean, password: String?, shareSheet: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var asImages by remember { mutableStateOf(false) }
    var pageSize by remember { mutableStateOf(PdfPageSizePreset.A4) }
    var searchableOcr by remember { mutableStateOf(true) }
    var usePassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ScannerGlassPalette.MidnightNavy,
        contentColor = ScannerGlassPalette.TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isArabic) "تصدير ومشاركة المستند" else "Export & Share Document",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ScannerGlassPalette.TextPrimary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .scannerGlassSurface(RoundedCornerShape(12.dp), glowBorder = !asImages)
                        .clickable { asImages = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("PDF", color = if (!asImages) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextSecondary, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .scannerGlassSurface(RoundedCornerShape(12.dp), glowBorder = asImages)
                        .clickable { asImages = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("JPG / Images", color = if (asImages) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextSecondary, fontWeight = FontWeight.Bold)
                }
            }

            if (!asImages) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PdfPageSizePreset.entries.forEach { preset ->
                        val sel = pageSize == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .scannerGlassSurface(RoundedCornerShape(12.dp), glowBorder = sel)
                                .clickable { pageSize = preset }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) preset.labelAr else preset.labelEn,
                                color = if (sel) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "PDF قابل للبحث (دمج نص OCR)" else "Searchable PDF (Embed OCR Text)",
                        color = ScannerGlassPalette.TextPrimary,
                        fontSize = 13.sp
                    )
                    Switch(
                        checked = searchableOcr,
                        onCheckedChange = { searchableOcr = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = ScannerGlassPalette.CyanGlow)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "حماية ملف PDF بكلمة مرور" else "Password-Protect Exported PDF",
                        color = ScannerGlassPalette.TextPrimary,
                        fontSize = 13.sp
                    )
                    Switch(
                        checked = usePassword,
                        onCheckedChange = { usePassword = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = ScannerGlassPalette.AmberGlow)
                    )
                }

                if (usePassword) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (isArabic) "كلمة المرور" else "PDF Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScannerGlassPalette.AmberGlow,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedTextColor = ScannerGlassPalette.TextPrimary,
                            unfocusedTextColor = ScannerGlassPalette.TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        onExport(pageSize, asImages, searchableOcr, password.takeIf { usePassword && it.isNotBlank() }, false)
                    },
                    enabled = !usePassword || password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFF04101B))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isArabic) "حفظ في الهاتف" else "Save Downloads", color = Color(0xFF04101B), fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
                OutlinedButton(
                    onClick = {
                        onExport(pageSize, asImages, searchableOcr, password.takeIf { usePassword && it.isNotBlank() }, true)
                    },
                    enabled = !usePassword || password.isNotBlank(),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = ScannerGlassPalette.VioletGlow)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isArabic) "مشاركة" else "Share Sheet", color = ScannerGlassPalette.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }
        }
    }
}
