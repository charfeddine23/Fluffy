package com.charfeddine.fluffy.scanner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import coil.compose.rememberAsyncImagePainter
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
import com.charfeddine.fluffy.scanner.ui.theme.iosLiquidGlass
import com.charfeddine.fluffy.scanner.ui.theme.scannerGlassSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
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
    val safePageIndex = activePageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val currentPage: ScannedPageEntity? = pages.getOrNull(safePageIndex)

    var activeTool by remember { mutableStateOf(AnnotationTool.NONE) }
    var penColorArgb by remember { mutableLongStateOf(0xFF0EA5E9L) }
    var redactionBlack by remember { mutableStateOf(true) }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showTagsDialog by remember { mutableStateOf(false) }
    var showPagesSheet by remember { mutableStateOf(false) }
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
    val activeTags = remember(doc.tagsCsv) {
        doc.tagsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    var pageBitmap by remember(currentPage?.id, currentPage?.processedImagePath) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(currentPage?.id, currentPage?.processedImagePath, doc.isLocked) {
        val path = currentPage?.processedImagePath ?: return@LaunchedEffect
        pageBitmap = withContext(Dispatchers.IO) {
            scannerViewModel.repository.decodePageBitmap(path)
        }
    }

    ScannerDarkLiquidBackdrop {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Expansive Document Viewing & Editing Stage (Edge-to-Edge Breathing Room)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(top = 66.dp, bottom = 92.dp, start = 10.dp, end = 10.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF060A15).copy(alpha = 0.65f))
                    .border(
                        width = if (activeTool != AnnotationTool.NONE) 1.2.dp else 0.75.dp,
                        brush = if (activeTool != AnnotationTool.NONE) {
                            ScannerGlassPalette.NeonBorderBrush
                        } else {
                            ScannerGlassPalette.SubtleGlassBorder
                        },
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clipToBounds()
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = pageBitmap
                val p = currentPage
                if (bmp == null || p == null) {
                    CircularProgressIndicator(color = ScannerGlassPalette.CyanGlow)
                } else {
                    InteractiveAnnotatedPageCanvas(
                        pageId = p.id,
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

                // Multi-page Quick Navigation Pill floating on top-end of canvas when > 1 page
                if (pages.size > 1 && activeTool == AnnotationTool.NONE) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .iosLiquidGlass(CircleShape, fillAlpha = 0.78f)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { if (safePageIndex > 0) activePageIndex = safePageIndex - 1 },
                            enabled = safePageIndex > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBackIosNew,
                                contentDescription = if (isArabic) "السابق" else "Previous page",
                                tint = if (safePageIndex > 0) ScannerGlassPalette.TextPrimary else ScannerGlassPalette.TextSecondary.copy(alpha = 0.35f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "${safePageIndex + 1} / ${pages.size}",
                            color = ScannerGlassPalette.EdgeOverlayCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            modifier = Modifier
                                .clickable { showPagesSheet = true }
                                .padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = { if (safePageIndex < pages.lastIndex) activePageIndex = safePageIndex + 1 },
                            enabled = safePageIndex < pages.lastIndex,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowForwardIos,
                                contentDescription = if (isArabic) "التالي" else "Next page",
                                tint = if (safePageIndex < pages.lastIndex) ScannerGlassPalette.TextPrimary else ScannerGlassPalette.TextSecondary.copy(alpha = 0.35f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Selected Placed Signature Floating Resize / Rotate / Delete Pill
                val selPlacement = activeSignatures.firstOrNull { it.placementId == activeSignaturePlacementId }
                if (selPlacement != null && p != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .shadow(14.dp, CircleShape)
                            .iosLiquidGlass(CircleShape, glowAccent = true, fillAlpha = 0.85f)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "S-",
                            color = ScannerGlassPalette.EdgeOverlayCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    val next = activeSignatures.map {
                                        if (it.placementId == selPlacement.placementId) it.copy(scale = (it.scale - 0.15f).coerceAtLeast(0.45f)) else it
                                    }
                                    scannerViewModel.updatePageMarkups(p, activeAnnotations, next, activeWatermark)
                                }
                                .padding(6.dp)
                        )
                        Text(
                            "S+",
                            color = ScannerGlassPalette.EdgeOverlayCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    val next = activeSignatures.map {
                                        if (it.placementId == selPlacement.placementId) it.copy(scale = (it.scale + 0.15f).coerceAtMost(2.5f)) else it
                                    }
                                    scannerViewModel.updatePageMarkups(p, activeAnnotations, next, activeWatermark)
                                }
                                .padding(6.dp)
                        )
                        Text(
                            "↻15°",
                            color = ScannerGlassPalette.VioletGlow,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    val next = activeSignatures.map {
                                        if (it.placementId == selPlacement.placementId) it.copy(rotationDeg = (it.rotationDeg + 15f) % 360f) else it
                                    }
                                    scannerViewModel.updatePageMarkups(p, activeAnnotations, next, activeWatermark)
                                }
                                .padding(6.dp)
                        )
                        IconButton(
                            onClick = {
                                val next = activeSignatures.filterNot { it.placementId == selPlacement.placementId }
                                activeSignaturePlacementId = null
                                scannerViewModel.updatePageMarkups(p, activeAnnotations, next, activeWatermark)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = if (isArabic) "حذف التوقيع" else "Remove signature",
                                tint = ScannerGlassPalette.RoseGlow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (isBusy) {
                    CircularProgressIndicator(
                        color = ScannerGlassPalette.EdgeOverlayCyan,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // 2. FLOATING iOS 27 LIQUID GLASS TOP HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Orb
                IconButton(
                    onClick = onBackToHub,
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(14.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                        .iosLiquidGlass(CircleShape, fillAlpha = 0.72f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isArabic) "رجوع" else "Back",
                        tint = ScannerGlassPalette.TextPrimary
                    )
                }

                // Generous Center Title & Status Glass Pill (No cramped squishing!)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .shadow(14.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                        .iosLiquidGlass(CircleShape, fillAlpha = 0.68f)
                        .clickable { showRenameDialog = true }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isArabic) "${pages.size} صفحة" else "${pages.size} page${if (pages.size == 1) "" else "s"}",
                                color = ScannerGlassPalette.TextSecondary,
                                fontSize = 11.sp
                            )
                            if (doc.isStarred) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = ScannerGlassPalette.AmberGlow,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            if (doc.isLocked) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = ScannerGlassPalette.EmeraldGlow,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            if (activeTags.isNotEmpty()) {
                                Text(
                                    text = "• ${activeTags.first()}${if (activeTags.size > 1) " +${activeTags.size - 1}" else ""}",
                                    color = ScannerGlassPalette.EdgeOverlayCyan,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = if (isArabic) "إعادة التسمية" else "Rename",
                        tint = ScannerGlassPalette.TextSecondary.copy(alpha = 0.75f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Right Twin Utility Liquid Glass Capsule (Share + Overflow Menu)
                Box {
                    Row(
                        modifier = Modifier
                            .height(48.dp)
                            .shadow(14.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                            .iosLiquidGlass(CircleShape, glowAccent = true, fillAlpha = 0.74f)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showExportSheet = true },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("doc_export_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = if (isArabic) "تصدير ومشاركة" else "Export & Share",
                                tint = ScannerGlassPalette.EdgeOverlayCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(0.8.dp)
                                .height(20.dp)
                                .background(Color.White.copy(alpha = 0.16f))
                        )
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("doc_more_actions_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = if (isArabic) "المزيد" else "More options",
                                tint = ScannerGlassPalette.TextPrimary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        containerColor = ScannerGlassPalette.MidnightNavy
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = if (doc.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = ScannerGlassPalette.AmberGlow
                                )
                            },
                            text = {
                                Text(
                                    text = if (doc.isStarred) {
                                        if (isArabic) "إزالة من المفضلة" else "Remove Star"
                                    } else {
                                        if (isArabic) "تمييز بنجمة" else "Add to Starred"
                                    },
                                    color = ScannerGlassPalette.TextPrimary
                                )
                            },
                            onClick = {
                                scannerViewModel.toggleStarred(doc)
                                showMoreMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = if (doc.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = ScannerGlassPalette.EmeraldGlow
                                )
                            },
                            text = {
                                Text(
                                    text = if (doc.isLocked) {
                                        if (isArabic) "فتح قفل المستند" else "Unlock Document"
                                    } else {
                                        if (isArabic) "قفل وتشفير AES-256" else "Lock (AES-256 Vault)"
                                    },
                                    color = ScannerGlassPalette.TextPrimary
                                )
                            },
                            onClick = {
                                scannerViewModel.toggleDocumentLock(doc.id, !doc.isLocked, isArabic)
                                showMoreMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Label,
                                    contentDescription = null,
                                    tint = ScannerGlassPalette.EdgeOverlayCyan
                                )
                            },
                            text = {
                                Text(
                                    text = if (isArabic) "إدارة الوسوم (${activeTags.size})" else "Manage Tags (${activeTags.size})",
                                    color = ScannerGlassPalette.TextPrimary
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                showTagsDialog = true
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = ScannerGlassPalette.VioletGlow
                                )
                            },
                            text = {
                                Text(
                                    text = if (isArabic) "ترتيب وإدارة الصفحات" else "Manage Pages",
                                    color = ScannerGlassPalette.TextPrimary
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                showPagesSheet = true
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = ScannerGlassPalette.TextSecondary
                                )
                            },
                            text = {
                                Text(
                                    text = if (isArabic) "إعادة تسمية" else "Rename",
                                    color = ScannerGlassPalette.TextPrimary
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                showRenameDialog = true
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = ScannerGlassPalette.RoseGlow
                                )
                            },
                            text = {
                                Text(
                                    text = if (isArabic) "نقل إلى سلة المهملات" else "Move to Trash",
                                    color = ScannerGlassPalette.RoseGlow
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                scannerViewModel.moveToTrash(doc.id, isArabic)
                            }
                        )
                    }
                }
            }

            // 3. FLOATING BOTTOM iOS 27 LIQUID GLASS DECK + DETACHED `+` ORB
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Contextual Floating Secondary Markup Pill (ONLY appears when Markup is active!)
                AnimatedVisibility(
                    visible = activeTool != AnnotationTool.NONE && currentPage != null,
                    enter = fadeIn(spring()) + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut(spring()) + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    currentPage?.let { pg ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(18.dp, RoundedCornerShape(24.dp))
                                .iosLiquidGlass(RoundedCornerShape(24.dp), glowAccent = true, fillAlpha = 0.82f)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Top row: Pen / Highlight / Redact switcher + Undo + Done
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    MarkupSubChip(
                                        icon = Icons.Default.BorderColor,
                                        label = if (isArabic) "قلم" else "Pen",
                                        selected = activeTool == AnnotationTool.PEN,
                                        onClick = { activeTool = AnnotationTool.PEN }
                                    )
                                    MarkupSubChip(
                                        icon = Icons.Default.Highlight,
                                        label = if (isArabic) "تظليل" else "Highlight",
                                        selected = activeTool == AnnotationTool.HIGHLIGHTER,
                                        onClick = {
                                            activeTool = AnnotationTool.HIGHLIGHTER
                                            penColorArgb = 0xFFFDE047L
                                        }
                                    )
                                    MarkupSubChip(
                                        icon = Icons.Default.Rectangle,
                                        label = if (isArabic) "طمس" else "Redact",
                                        selected = activeTool == AnnotationTool.REDACTION,
                                        onClick = { activeTool = AnnotationTool.REDACTION }
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            val nextAnn = if (activeTool == AnnotationTool.REDACTION && activeAnnotations.redactions.isNotEmpty()) {
                                                activeAnnotations.copy(redactions = activeAnnotations.redactions.dropLast(1))
                                            } else if (activeAnnotations.strokes.isNotEmpty()) {
                                                activeAnnotations.copy(strokes = activeAnnotations.strokes.dropLast(1))
                                            } else activeAnnotations
                                            scannerViewModel.updatePageMarkups(pg, nextAnn, activeSignatures, activeWatermark)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = ScannerGlassPalette.AmberGlow, modifier = Modifier.size(18.dp))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(ScannerGlassPalette.EdgeOverlayCyan)
                                            .clickable { activeTool = AnnotationTool.NONE }
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = if (isArabic) "تم" else "Done",
                                            tint = ScannerGlassPalette.DeepObsidian,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }

                            // Bottom row inside sub-pill: Color Swatches or Black/White Redaction toggle
                            if (activeTool == AnnotationTool.REDACTION) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.Black)
                                            .border(1.5.dp, if (redactionBlack) ScannerGlassPalette.EdgeOverlayCyan else Color.White.copy(alpha = 0.2f), CircleShape)
                                            .clickable { redactionBlack = true }
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text(if (isArabic) "طمس أسود" else "Black Redact", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(1.5.dp, if (!redactionBlack) ScannerGlassPalette.EdgeOverlayCyan else Color.Transparent, CircleShape)
                                            .clickable { redactionBlack = false }
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text(if (isArabic) "طمس أبيض" else "White Redact", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val palette = if (activeTool == AnnotationTool.HIGHLIGHTER) {
                                        listOf(0xFFFDE047L, 0xFF34D399L, 0xFF38BDF8L, 0xFFF472B6L)
                                    } else {
                                        listOf(0xFF0EA5E9L, 0xFFEF4444L, 0xFF10B981L, 0xFF0F172AL)
                                    }
                                    palette.forEach { argb ->
                                        val isPicked = penColorArgb == argb
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color(argb.toInt()))
                                                .border(
                                                    if (isPicked) 2.2.dp else 1.dp,
                                                    if (isPicked) Color.White else Color.White.copy(alpha = 0.25f),
                                                    CircleShape
                                                )
                                                .clickable { penColorArgb = argb }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Signature iOS 27 Floating Glass Pill Bar + Detached Circular `+` Action Orb
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .shadow(20.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                            .iosLiquidGlass(CircleShape, fillAlpha = 0.74f)
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidDockItem(
                            icon = Icons.Default.BorderColor,
                            label = if (isArabic) "تحرير" else "Markup",
                            active = activeTool != AnnotationTool.NONE,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeTool = if (activeTool == AnnotationTool.NONE) AnnotationTool.PEN else AnnotationTool.NONE
                            }
                        )
                        LiquidDockItem(
                            icon = Icons.Default.Draw,
                            label = if (isArabic) "توقيع" else "Sign",
                            active = showSignatureDialog,
                            modifier = Modifier.weight(1f),
                            onClick = { showSignatureDialog = true }
                        )
                        LiquidDockItem(
                            icon = Icons.Default.WaterDrop,
                            label = if (isArabic) "علامة" else "Mark",
                            active = activeWatermark != null,
                            modifier = Modifier.weight(1f),
                            onClick = { showWatermarkDialog = true }
                        )
                        LiquidDockItem(
                            icon = Icons.Default.TextSnippet,
                            label = "OCR",
                            active = showOcrDialog,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                currentPage?.let { pg ->
                                    scannerViewModel.runOfflineOcrOnPage(pg, isArabic) { text ->
                                        ocrResultText = text
                                        if (text.isNotBlank()) showOcrDialog = true
                                    }
                                }
                            }
                        )
                        LiquidDockItem(
                            icon = Icons.Default.Layers,
                            label = if (isArabic) "الصفحات" else "Pages",
                            active = showPagesSheet,
                            modifier = Modifier.weight(1f),
                            onClick = { showPagesSheet = true }
                        )
                    }

                    // Signature Detached iOS 27 Circular `+` Liquid Glass Orb (Add / Scan Page)
                    IconButton(
                        onClick = onAddMorePages,
                        modifier = Modifier
                            .size(62.dp)
                            .shadow(20.dp, CircleShape, ambientColor = ScannerGlassPalette.EdgeOverlayCyan, spotColor = Color.Black)
                            .iosLiquidGlass(CircleShape, glowAccent = true, fillAlpha = 0.80f)
                            .testTag("doc_add_page_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (isArabic) "إضافة صفحة" else "Add Page",
                            tint = ScannerGlassPalette.EdgeOverlayCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
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
                    .iosLiquidGlass(RoundedCornerShape(26.dp), glowAccent = true, fillAlpha = 0.92f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (isArabic) "إعادة تسمية المستند" else "Rename Document",
                    color = ScannerGlassPalette.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
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
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text(if (isArabic) "إلغاء" else "Cancel", color = ScannerGlassPalette.TextSecondary)
                    }
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

    // Tags Management Dialog (Cleanly tucked out of the header)
    if (showTagsDialog) {
        Dialog(onDismissRequest = { showTagsDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosLiquidGlass(RoundedCornerShape(26.dp), glowAccent = true, fillAlpha = 0.92f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isArabic) "وسوم المستند" else "Document Tags",
                    color = ScannerGlassPalette.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScannerTagCatalog.presets.forEach { preset ->
                        val tagColor = Color(preset.colorArgb.toInt())
                        val selected = preset.name in activeTags
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) tagColor.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.06f))
                                .border(
                                    width = if (selected) 1.5.dp else 0.8.dp,
                                    color = if (selected) tagColor else Color.White.copy(alpha = 0.16f),
                                    shape = CircleShape
                                )
                                .clickable { scannerViewModel.toggleTagOnDocument(doc, preset.name) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (selected) "${preset.name} ✓" else preset.name,
                                color = if (selected) tagColor else ScannerGlassPalette.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                        onClick = { showTagsDialog = false }
                    ) {
                        Text(if (isArabic) "تم" else "Done", color = Color(0xFF04101B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Visual Pages Manager Sheet (Reorder, Duplicate, Extract, Delete, Add Page)
    if (showPagesSheet && currentPage != null) {
        DocumentPagesManagerSheet(
            doc = doc,
            pages = pages,
            activePageIndex = safePageIndex,
            isArabic = isArabic,
            onSelectPage = { idx ->
                activePageIndex = idx
                showPagesSheet = false
            },
            onMoveEarlier = { idx ->
                if (idx > 0) {
                    scannerViewModel.movePageOrder(doc.id, idx, idx - 1)
                    activePageIndex = idx - 1
                }
            },
            onMoveLater = { idx ->
                if (idx < pages.lastIndex) {
                    scannerViewModel.movePageOrder(doc.id, idx, idx + 1)
                    activePageIndex = idx + 1
                }
            },
            onDuplicatePage = { pg ->
                scannerViewModel.duplicatePage(doc.id, pg, isArabic)
            },
            onExtractPage = { pg ->
                scannerViewModel.extractPageAsNewDocument(doc, pg, isArabic)
            },
            onDeletePage = { pg ->
                scannerViewModel.deletePage(doc.id, pg.id, isArabic)
                activePageIndex = (safePageIndex - 1).coerceAtLeast(0)
            },
            onAddPage = {
                showPagesSheet = false
                onAddMorePages()
            },
            onDismiss = { showPagesSheet = false }
        )
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
                    .iosLiquidGlass(RoundedCornerShape(26.dp), glowAccent = true, fillAlpha = 0.94f)
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
private fun LiquidDockItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(52.dp)
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .then(
                if (active) {
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.28f),
                                    ScannerGlassPalette.EdgeOverlayPurple.copy(alpha = 0.22f)
                                )
                            ),
                            CircleShape
                        )
                        .border(0.8.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) ScannerGlassPalette.EdgeOverlayCyan else ScannerGlassPalette.TextPrimary.copy(alpha = 0.90f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active) ScannerGlassPalette.EdgeOverlayCyan else ScannerGlassPalette.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MarkupSubChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.22f)
                else Color.White.copy(alpha = 0.06f)
            )
            .border(
                width = 0.9.dp,
                color = if (selected) ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.12f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) ScannerGlassPalette.EdgeOverlayCyan else ScannerGlassPalette.TextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = if (selected) ScannerGlassPalette.EdgeOverlayCyan else ScannerGlassPalette.TextPrimary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentPagesManagerSheet(
    doc: ScannedDocumentEntity,
    pages: List<ScannedPageEntity>,
    activePageIndex: Int,
    isArabic: Boolean,
    onSelectPage: (Int) -> Unit,
    onMoveEarlier: (Int) -> Unit,
    onMoveLater: (Int) -> Unit,
    onDuplicatePage: (ScannedPageEntity) -> Unit,
    onExtractPage: (ScannedPageEntity) -> Unit,
    onDeletePage: (ScannedPageEntity) -> Unit,
    onAddPage: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedPage = pages.getOrNull(activePageIndex)

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "إدارة الصفحات (${pages.size})" else "Pages Manager (${pages.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScannerGlassPalette.TextPrimary
                )
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.16f))
                        .border(1.dp, ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.6f), CircleShape)
                        .clickable(onClick = onAddPage)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = ScannerGlassPalette.EdgeOverlayCyan, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (isArabic) "إضافة صفحة" else "Scan Page",
                        color = ScannerGlassPalette.EdgeOverlayCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(pages, key = { _, pg -> pg.id }) { idx, pg ->
                    val selected = idx == activePageIndex
                    Column(
                        modifier = Modifier
                            .width(86.dp)
                            .iosLiquidGlass(RoundedCornerShape(16.dp), glowAccent = selected, fillAlpha = 0.78f)
                            .clickable { onSelectPage(idx) }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(File(pg.processedImagePath)),
                            contentDescription = "Page ${idx + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(98.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black)
                        )
                        Text(
                            text = if (isArabic) "صفحة ${idx + 1}" else "Page ${idx + 1}",
                            color = if (selected) ScannerGlassPalette.EdgeOverlayCyan else ScannerGlassPalette.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            if (selectedPage != null) {
                Text(
                    text = if (isArabic) "إجراءات الصفحة المحددة (${activePageIndex + 1}):" else "Selected Page (${activePageIndex + 1}) Actions:",
                    color = ScannerGlassPalette.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SheetActionTile(
                        icon = Icons.Default.ArrowBackIosNew,
                        label = if (isArabic) "تقديم" else "Move ←",
                        enabled = activePageIndex > 0,
                        tint = ScannerGlassPalette.TextPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onMoveEarlier(activePageIndex) }
                    )
                    SheetActionTile(
                        icon = Icons.Default.ArrowForwardIos,
                        label = if (isArabic) "تأخير" else "Move →",
                        enabled = activePageIndex < pages.lastIndex,
                        tint = ScannerGlassPalette.TextPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onMoveLater(activePageIndex) }
                    )
                    SheetActionTile(
                        icon = Icons.Default.CopyAll,
                        label = if (isArabic) "تكرار" else "Duplicate",
                        enabled = true,
                        tint = ScannerGlassPalette.CyanGlow,
                        modifier = Modifier.weight(1f),
                        onClick = { onDuplicatePage(selectedPage) }
                    )
                    SheetActionTile(
                        icon = Icons.Default.Output,
                        label = if (isArabic) "استخراج" else "Extract",
                        enabled = true,
                        tint = ScannerGlassPalette.VioletGlow,
                        modifier = Modifier.weight(1f),
                        onClick = { onExtractPage(selectedPage) }
                    )
                    if (pages.size > 1) {
                        SheetActionTile(
                            icon = Icons.Default.DeleteOutline,
                            label = if (isArabic) "حذف" else "Delete",
                            enabled = true,
                            tint = ScannerGlassPalette.RoseGlow,
                            modifier = Modifier.weight(1f),
                            onClick = { onDeletePage(selectedPage) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetActionTile(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(62.dp)
            .iosLiquidGlass(RoundedCornerShape(14.dp), fillAlpha = if (enabled) 0.72f else 0.35f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) tint else tint.copy(alpha = 0.32f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (enabled) ScannerGlassPalette.TextPrimary else ScannerGlassPalette.TextSecondary.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun InteractiveAnnotatedPageCanvas(
    pageId: String,
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
    var zoomScale by remember(pageId) { mutableFloatStateOf(1f) }
    var panOffset by remember(pageId) { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    // Ensure 1:1 coordinate fidelity whenever user activates any markup tool
    LaunchedEffect(activeTool) {
        if (activeTool != AnnotationTool.NONE) {
            zoomScale = 1f
            panOffset = Offset.Zero
        }
    }

    val bw = bitmap.width.toFloat().coerceAtLeast(1f)
    val bh = bitmap.height.toFloat().coerceAtLeast(1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { boxSize = it }
            .then(
                if (activeTool == AnnotationTool.NONE) {
                    Modifier
                        .pointerInput(pageId, bw, bh) {
                            detectTapGestures(
                                onDoubleTap = { tapOffset ->
                                    val stageW = size.width.toFloat().coerceAtLeast(1f)
                                    val stageH = size.height.toFloat().coerceAtLeast(1f)
                                    val stageScale = minOf(stageW / bw, stageH / bh)
                                    val imgW = bw * stageScale
                                    val imgH = bh * stageScale
                                    if (zoomScale > 1.05f) {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    } else {
                                        val oldScale = zoomScale
                                        val targetScale = 2.4f
                                        val zoomFactor = targetScale / oldScale
                                        val center = Offset(stageW / 2f, stageH / 2f)
                                        val focal = tapOffset - center
                                        val candidatePan = (panOffset - focal) * zoomFactor + focal
                                        val maxPanX = ((imgW * targetScale - stageW).coerceAtLeast(0f)) / 2f
                                        val maxPanY = ((imgH * targetScale - stageH).coerceAtLeast(0f)) / 2f
                                        zoomScale = targetScale
                                        panOffset = Offset(
                                            x = candidatePan.x.coerceIn(-maxPanX, maxPanX),
                                            y = candidatePan.y.coerceIn(-maxPanY, maxPanY)
                                        )
                                    }
                                }
                            )
                        }
                        .pointerInput(pageId, bw, bh) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val stageW = size.width.toFloat().coerceAtLeast(1f)
                                val stageH = size.height.toFloat().coerceAtLeast(1f)
                                val stageScale = minOf(stageW / bw, stageH / bh)
                                val imgW = bw * stageScale
                                val imgH = bh * stageScale

                                val oldScale = zoomScale
                                val newScale = (oldScale * zoom).coerceIn(1f, 4f)
                                if (newScale <= 1.01f) {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                } else {
                                    val zoomFactor = newScale / oldScale
                                    val center = Offset(stageW / 2f, stageH / 2f)
                                    val focalFromCenter = centroid - center
                                    val candidatePan = (panOffset - focalFromCenter) * zoomFactor + focalFromCenter + pan
                                    val maxPanX = ((imgW * newScale - stageW).coerceAtLeast(0f)) / 2f
                                    val maxPanY = ((imgH * newScale - stageH).coerceAtLeast(0f)) / 2f
                                    zoomScale = newScale
                                    panOffset = Offset(
                                        x = candidatePan.x.coerceIn(-maxPanX, maxPanX),
                                        y = candidatePan.y.coerceIn(-maxPanY, maxPanY)
                                    )
                                }
                            }
                        }
                } else {
                    Modifier
                }
            )
    ) {
        val cw = boxSize.width.toFloat().coerceAtLeast(1f)
        val ch = boxSize.height.toFloat().coerceAtLeast(1f)
        val scale = minOf(cw / bw, ch / bh)
        val drawW = bw * scale
        val drawH = bh * scale
        val offsetX = (cw - drawW) / 2f
        val offsetY = (ch - drawH) / 2f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffset.x
                    translationY = panOffset.y
                }
        ) {

        fun toNorm(pos: Offset) = PointNorm(
            x = ((pos.x - offsetX) / drawW).coerceIn(0f, 1f),
            y = ((pos.y - offsetY) / drawH).coerceIn(0f, 1f)
        )

        Image(
            bitmap = imageBitmap,
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
                    size = Size((r.right - r.left) * drawW, (r.bottom - r.top) * drawH)
                )
            }
            val rs = draftRedactStart
            val re = draftRedactEnd
            if (rs != null && re != null) {
                drawRect(
                    color = Color(redactionColorArgb.toInt()).copy(alpha = 0.8f),
                    topLeft = Offset(offsetX + min(rs.x, re.x) * drawW, offsetY + min(rs.y, re.y) * drawH),
                    size = Size(abs(re.x - rs.x) * drawW, abs(re.y - rs.y) * drawH)
                )
            }
        }

        // Draggable Placed E-Signatures
        signatures.forEach { placement ->
            val sigBmp = remember(placement.signatureId) { loadSignatureBitmap(placement.signatureId) }
            if (sigBmp != null && boxSize.width > 0) {
                val sigImg = remember(sigBmp) { sigBmp.asImageBitmap() }
                val targetW = drawW * 0.28f * placement.scale
                val aspect = sigBmp.height.toFloat() / sigBmp.width.coerceAtLeast(1).toFloat()
                val targetH = targetW * aspect
                val leftPx = offsetX + placement.centerXNorm * drawW - targetW / 2f
                val topPx = offsetY + placement.centerYNorm * drawH - targetH / 2f
                val isSelected = placement.placementId == activeSignaturePlacementId

                Image(
                    bitmap = sigImg,
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
                        .iosLiquidGlass(RoundedCornerShape(12.dp), glowAccent = !asImages)
                        .clickable { asImages = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("PDF", color = if (!asImages) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextSecondary, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .iosLiquidGlass(RoundedCornerShape(12.dp), glowAccent = asImages)
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
                                .iosLiquidGlass(RoundedCornerShape(12.dp), glowAccent = sel)
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
