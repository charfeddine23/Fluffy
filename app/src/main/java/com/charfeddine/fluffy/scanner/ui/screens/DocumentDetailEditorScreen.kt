package com.charfeddine.fluffy.scanner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Crop
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot
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
    onBackToHub: () -> Unit,
    onSwitchDocument: ((ScannedDocumentEntity, Int) -> Unit)? = null
) {
    val handleBackExit: () -> Unit = {
        scannerViewModel.autoSaveModifiedDocumentOnExit(documentId, isArabic)
        onBackToHub()
    }
    BackHandler { handleBackExit() }

    val context = LocalContext.current
    val docState by scannerViewModel.observeDocument(documentId).collectAsState(initial = null)
    val pages by scannerViewModel.observePages(documentId).collectAsState(initial = emptyList())
    val visibleDocs by scannerViewModel.visibleDocuments.collectAsState()
    val allActiveDocs by scannerViewModel.allActiveDocs.collectAsState()
    val savedSignatures by scannerViewModel.savedSignatures.collectAsState()
    val isBusy by scannerViewModel.isBusy.collectAsState()

    val doc = docState ?: return
    val navDocs = remember(visibleDocs, allActiveDocs, documentId) {
        if (visibleDocs.any { it.id == documentId }) visibleDocs else allActiveDocs
    }
    val currentDocNavIndex = navDocs.indexOfFirst { it.id == documentId }

    var activePageIndex by remember(documentId) { mutableIntStateOf(0) }
    val safePageIndex = activePageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val currentPage: ScannedPageEntity? = pages.getOrNull(safePageIndex)

    var activeTool by remember(documentId) { mutableStateOf(AnnotationTool.NONE) }
    var penColorArgb by remember { mutableLongStateOf(0xFF0EA5E9L) }
    var redactionBlack by remember { mutableStateOf(true) }

    var showMoreMenu by remember(documentId) { mutableStateOf(false) }
    var showRenameDialog by remember(documentId) { mutableStateOf(false) }
    var showTagsDialog by remember(documentId) { mutableStateOf(false) }
    var showPagesSheet by remember(documentId) { mutableStateOf(false) }
    var showSignatureDialog by remember(documentId) { mutableStateOf(false) }
    var showWatermarkDialog by remember(documentId) { mutableStateOf(false) }
    var showOcrDialog by remember(documentId) { mutableStateOf(false) }
    var ocrResultText by remember(documentId) { mutableStateOf("") }
    var showExportSheet by remember(documentId) { mutableStateOf(false) }

    var activeSignaturePlacementId by remember(documentId) { mutableStateOf<String?>(null) }

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

    val pageBitmapCache = remember(doc.id, doc.isLocked) { mutableStateMapOf<String, Bitmap>() }
    val pageBitmapPathCache = remember(doc.id, doc.isLocked) { mutableStateMapOf<String, String>() }
    LaunchedEffect(currentPage?.id, currentPage?.processedImagePath, doc.isLocked, pages, safePageIndex) {
        val curr = currentPage ?: return@LaunchedEffect
        if (pageBitmapCache[curr.id] == null || pageBitmapPathCache[curr.id] != curr.processedImagePath) {
            val decoded = withContext(Dispatchers.IO) {
                scannerViewModel.repository.decodePageBitmap(curr.processedImagePath)
            }
            if (decoded != null) {
                pageBitmapCache[curr.id] = decoded
                pageBitmapPathCache[curr.id] = curr.processedImagePath
            }
        }
        val activePageIds = pages.map { it.id }.toSet()
        pageBitmapCache.keys.toList().filter { it !in activePageIds }.forEach {
            pageBitmapCache.remove(it)
            pageBitmapPathCache.remove(it)
        }
        val neighbors = listOfNotNull(
            pages.getOrNull(safePageIndex + 1),
            pages.getOrNull(safePageIndex - 1)
        )
        for (neighbor in neighbors) {
            if (pageBitmapCache[neighbor.id] == null || pageBitmapPathCache[neighbor.id] != neighbor.processedImagePath) {
                val nBmp = withContext(Dispatchers.IO) {
                    scannerViewModel.repository.decodePageBitmap(neighbor.processedImagePath)
                }
                if (nBmp != null) {
                    pageBitmapCache[neighbor.id] = nBmp
                    pageBitmapPathCache[neighbor.id] = neighbor.processedImagePath
                }
            }
        }
        if (pageBitmapCache.size > 6) {
            val keepIds = (-2..2).mapNotNull { offset -> pages.getOrNull(safePageIndex + offset)?.id }.toSet()
            pageBitmapCache.keys.toList().filter { it !in keepIds }.forEach {
                pageBitmapCache.remove(it)
                pageBitmapPathCache.remove(it)
            }
        }
    }

    val density = LocalDensity.current
    val uiScope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val stageDragAnim = remember(documentId) { Animatable(0f) }
    val stageVerticalDragAnim = remember(documentId) { Animatable(0f) }
    var lastNavDirection by remember(documentId) { mutableIntStateOf(0) }
    val swipeThresholdPx = with(density) { 52.dp.toPx() }

    fun navigatePageByDelta(delta: Int) {
        val target = safePageIndex + delta
        if (target in pages.indices) {
            lastNavDirection = delta
            activeSignaturePlacementId = null
            activePageIndex = target
        }
    }

    fun navigateDocumentByDelta(delta: Int) {
        if (currentDocNavIndex < 0) return
        val targetDoc = navDocs.getOrNull(currentDocNavIndex + delta) ?: return
        activeSignaturePlacementId = null
        if (onSwitchDocument != null) {
            onSwitchDocument(targetDoc, delta)
        } else {
            scannerViewModel.autoSaveModifiedDocumentOnExit(documentId, isArabic)
            scannerViewModel.openDocumentDetail(targetDoc.id, transitionDirection = delta)
        }
    }

    val canSwipePages = pages.size > 1 && activeTool == AnnotationTool.NONE
    val canSwipeDocuments = navDocs.size > 1 && currentDocNavIndex >= 0 && activeTool == AnnotationTool.NONE
    val onStageSwipeDragDelta: (Float) -> Unit = { totalDx ->
        val isNextDrag = if (isArabic) totalDx > 0f else totalDx < 0f
        val canMoveInDir = if (isNextDrag) safePageIndex < pages.lastIndex else safePageIndex > 0
        val targetDx = if (canMoveInDir) totalDx * 0.58f else totalDx * 0.16f
        uiScope.launch { stageDragAnim.snapTo(targetDx) }
    }
    val onStageSwipeRelease: (Float, Float) -> Unit = { totalDx, totalDy ->
        uiScope.launch {
            stageDragAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 440f)
            )
        }
        if (abs(totalDx) >= swipeThresholdPx && abs(totalDx) > abs(totalDy) * 1.15f) {
            val delta = if (isArabic) {
                if (totalDx > 0f) 1 else -1
            } else {
                if (totalDx < 0f) 1 else -1
            }
            navigatePageByDelta(delta)
        }
    }
    val onStageVerticalSwipeDragDelta: (Float) -> Unit = { totalDy ->
        val isNextDocDrag = totalDy < 0f
        val canMoveInDir = if (isNextDocDrag) currentDocNavIndex < navDocs.lastIndex else currentDocNavIndex > 0
        val targetDy = if (canMoveInDir) totalDy * 0.52f else totalDy * 0.16f
        uiScope.launch { stageVerticalDragAnim.snapTo(targetDy) }
    }
    val onStageVerticalSwipeRelease: (Float, Float) -> Unit = { totalDx, totalDy ->
        uiScope.launch {
            stageVerticalDragAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 440f)
            )
        }
        if (abs(totalDy) >= swipeThresholdPx && abs(totalDy) > abs(totalDx) * 1.15f) {
            val delta = if (totalDy < 0f) 1 else -1
            navigateDocumentByDelta(delta)
        }
    }

    val stageBottomBreathing by animateDpAsState(
        targetValue = if (activeTool != AnnotationTool.NONE) 186.dp else 90.dp,
        animationSpec = tween(220),
        label = "detail_stage_bottom_breathing"
    )

    ScannerDarkLiquidBackdrop {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Expansive Document Viewing & Editing Stage (Balanced 14-16dp Breathing Room)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(top = 72.dp, bottom = stageBottomBreathing, start = 12.dp, end = 12.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(ScannerGlassPalette.StageSurface)
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
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = safePageIndex,
                        transitionSpec = {
                            val dir = (targetState - initialState).coerceIn(-1, 1)
                            val visualDir = if (layoutDirection == LayoutDirection.Rtl) -dir else dir
                            if (visualDir == 0) {
                                (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)))
                                    .togetherWith(fadeOut(tween(180)))
                            } else {
                                (slideInHorizontally(
                                    animationSpec = tween(260),
                                    initialOffsetX = { fullWidth -> (visualDir * fullWidth * 0.42f).roundToInt() }
                                ) + fadeIn(tween(230)) + scaleIn(initialScale = 0.95f, animationSpec = tween(260)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = tween(240),
                                            targetOffsetX = { fullWidth -> (-visualDir * fullWidth * 0.35f).roundToInt() }
                                        ) + fadeOut(tween(190)) + scaleOut(targetScale = 0.94f, animationSpec = tween(240))
                                    )
                            }
                        },
                        label = "detail_stage_page_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { targetIdx ->
                        val targetPage = pages.getOrNull(targetIdx)
                        val targetBmp = targetPage?.let { pageBitmapCache[it.id] }
                        if (targetBmp == null || targetPage == null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = ScannerGlassPalette.CyanGlow)
                            }
                        } else {
                            val dragPx = stageDragAnim.value
                            val vDragPx = stageVerticalDragAnim.value
                            val dragFraction = (maxOf(abs(dragPx), abs(vDragPx)) / 900f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationX = dragPx
                                        translationY = vDragPx
                                        val parallaxScale = 1f - dragFraction * 0.045f
                                        scaleX = parallaxScale
                                        scaleY = parallaxScale
                                        alpha = 1f - dragFraction * 0.14f
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                InteractiveAnnotatedPageCanvas(
                                    pageId = targetPage.id,
                                    bitmap = targetBmp,
                                    annotations = PageAnnotations.fromJson(targetPage.annotationsJson),
                                    signatures = SignaturePlacement.listFromJson(targetPage.signaturesJson),
                                    watermark = WatermarkConfig.fromJson(targetPage.watermarkJson),
                                    activeTool = if (targetIdx == safePageIndex) activeTool else AnnotationTool.NONE,
                                    penColorArgb = penColorArgb,
                                    redactionColorArgb = if (redactionBlack) 0xFF000000L else 0xFFFFFFFFL,
                                    activeSignaturePlacementId = activeSignaturePlacementId,
                                    canSwipePages = canSwipePages,
                                    canSwipeDocuments = canSwipeDocuments,
                                    loadSignatureBitmap = { sigId ->
                                        savedSignatures.firstOrNull { it.id == sigId }?.let {
                                            scannerViewModel.repository.loadEncryptedSignatureBitmap(it)
                                        }
                                    },
                                    onSelectSignature = { activeSignaturePlacementId = it },
                                    onAnnotationsChanged = { updatedAnn ->
                                        scannerViewModel.updatePageMarkups(targetPage, updatedAnn, activeSignatures, activeWatermark)
                                    },
                                    onSignaturesChanged = { updatedSigs ->
                                        scannerViewModel.updatePageMarkups(targetPage, activeAnnotations, updatedSigs, activeWatermark)
                                    },
                                    onSwipeDragDelta = onStageSwipeDragDelta,
                                    onSwipeRelease = onStageSwipeRelease,
                                    onVerticalSwipeDragDelta = onStageVerticalSwipeDragDelta,
                                    onVerticalSwipeRelease = onStageVerticalSwipeRelease
                                )
                            }
                        }
                    }

                    val vDragVal = stageVerticalDragAnim.value
                    if (canSwipeDocuments && abs(vDragVal) > swipeThresholdPx * 0.25f) {
                        val previewDelta = if (vDragVal < 0f) 1 else -1
                        val adjacentDoc = navDocs.getOrNull(currentDocNavIndex + previewDelta)
                        if (adjacentDoc != null) {
                            Row(
                                modifier = Modifier
                                    .align(if (previewDelta > 0) Alignment.BottomCenter else Alignment.TopCenter)
                                    .padding(vertical = 10.dp, horizontal = 12.dp)
                                    .shadow(12.dp, CircleShape)
                                    .iosLiquidGlass(CircleShape, glowAccent = true, fillAlpha = 0.88f)
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (previewDelta > 0) {
                                        if (isArabic) "↑ التالي: ${adjacentDoc.title}" else "↑ Next: ${adjacentDoc.title}"
                                    } else {
                                        if (isArabic) "↓ السابق: ${adjacentDoc.title}" else "↓ Prev: ${adjacentDoc.title}"
                                    },
                                    color = ScannerGlassPalette.EdgeOverlayCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 240.dp)
                                )
                            }
                        }
                    }

                    // Selected Placed Signature Floating Resize / Rotate / Delete Pill (TopCenter, never collides)
                    val p = currentPage
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

                // Non-overlapping Multi-page Quick Navigation Pill docked cleanly below the canvas
                if (pages.size > 1 && activeTool == AnnotationTool.NONE) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .iosLiquidGlass(CircleShape, fillAlpha = 0.80f)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { navigatePageByDelta(-1) },
                            enabled = safePageIndex > 0,
                            modifier = Modifier.size(34.dp)
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
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showPagesSheet = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        IconButton(
                            onClick = { navigatePageByDelta(1) },
                            enabled = safePageIndex < pages.lastIndex,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowForwardIos,
                                contentDescription = if (isArabic) "التالي" else "Next page",
                                tint = if (safePageIndex < pages.lastIndex) ScannerGlassPalette.TextPrimary else ScannerGlassPalette.TextSecondary.copy(alpha = 0.35f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }

            // 2. FLOATING iOS 27 LIQUID GLASS TOP HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Orb
                IconButton(
                    onClick = handleBackExit,
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

                // Generous Center Title & Status Glass Pill (Clean gap before edit pencil)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .shadow(14.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                        .iosLiquidGlass(CircleShape, fillAlpha = 0.68f)
                        .clickable { showRenameDialog = true }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        if (doc.isStarred || doc.isLocked || activeTags.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
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
                                        text = "${activeTags.first()}${if (activeTags.size > 1) " +${activeTags.size - 1}" else ""}",
                                        color = ScannerGlassPalette.EdgeOverlayCyan,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = if (isArabic) "إعادة التسمية" else "Rename",
                        tint = ScannerGlassPalette.TextSecondary.copy(alpha = 0.78f),
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(14.dp)
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
                                .minimumInteractiveComponentSize()
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
                                .background(ScannerGlassPalette.SubtleDivider)
                        )
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier
                                .size(44.dp)
                                .minimumInteractiveComponentSize()
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
                                    imageVector = Icons.Default.Crop,
                                    contentDescription = null,
                                    tint = ScannerGlassPalette.CyanGlow
                                )
                            },
                            text = {
                                Text(
                                    text = if (isArabic) "تعديل الحواف والفلاتر" else "Edit Edges & Filter",
                                    color = ScannerGlassPalette.TextPrimary
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                activeTool = AnnotationTool.NONE
                                currentPage?.let { pg ->
                                    scannerViewModel.openPageCropEditor(doc.id, pg)
                                }
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
                        HorizontalDivider(color = ScannerGlassPalette.SubtleDivider)
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
                                            tint = ScannerGlassPalette.OnAccent,
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
                                                    if (isPicked) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.SubtleStroke,
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
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .shadow(20.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                            .iosLiquidGlass(CircleShape, fillAlpha = 0.74f)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val useEvenDistribution = maxWidth >= 430.dp
                        val dockModifier = if (useEvenDistribution) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 4.dp)
                        }
                        Row(
                            modifier = dockModifier,
                            horizontalArrangement = if (useEvenDistribution) {
                                Arrangement.SpaceEvenly
                            } else {
                                Arrangement.spacedBy(12.dp)
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val itemMod = if (useEvenDistribution) {
                                Modifier.weight(1f)
                            } else {
                                Modifier.widthIn(min = 52.dp)
                            }
                            LiquidDockItem(
                                icon = Icons.Default.Crop,
                                label = if (isArabic) "تعديل" else "Edit",
                                active = false,
                                modifier = itemMod.testTag("doc_edit_page_btn"),
                                onClick = {
                                    activeTool = AnnotationTool.NONE
                                    currentPage?.let { pg ->
                                        scannerViewModel.openPageCropEditor(doc.id, pg)
                                    }
                                }
                            )
                            LiquidDockItem(
                                icon = Icons.Default.BorderColor,
                                label = if (isArabic) "تأشير" else "Markup",
                                active = activeTool != AnnotationTool.NONE,
                                modifier = itemMod.testTag("doc_markup_btn"),
                                onClick = {
                                    activeTool = if (activeTool == AnnotationTool.NONE) AnnotationTool.PEN else AnnotationTool.NONE
                                }
                            )
                            LiquidDockItem(
                                icon = Icons.Default.Draw,
                                label = if (isArabic) "توقيع" else "Sign",
                                active = showSignatureDialog,
                                modifier = itemMod.testTag("doc_sign_btn"),
                                onClick = { showSignatureDialog = true }
                            )
                            LiquidDockItem(
                                icon = Icons.Default.WaterDrop,
                                label = if (isArabic) "علامة" else "Mark",
                                active = activeWatermark != null,
                                modifier = itemMod.testTag("doc_watermark_btn"),
                                onClick = { showWatermarkDialog = true }
                            )
                            LiquidDockItem(
                                icon = Icons.Default.TextSnippet,
                                label = "OCR",
                                active = showOcrDialog,
                                modifier = itemMod.testTag("doc_ocr_btn"),
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
                                modifier = itemMod.testTag("doc_pages_btn"),
                                onClick = { showPagesSheet = true }
                            )
                        }
                    }

                    // Signature Detached iOS 27 Circular `+` Liquid Glass Orb (Add / Scan Page)
                    IconButton(
                        onClick = onAddMorePages,
                        modifier = Modifier
                            .size(58.dp)
                            .shadow(20.dp, CircleShape, ambientColor = ScannerGlassPalette.EdgeOverlayCyan, spotColor = Color.Black)
                            .iosLiquidGlass(CircleShape, glowAccent = true, fillAlpha = 0.80f)
                            .testTag("doc_add_page_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (isArabic) "إضافة صفحة" else "Add Page",
                            tint = ScannerGlassPalette.EdgeOverlayCyan,
                            modifier = Modifier.size(25.dp)
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
                        unfocusedBorderColor = ScannerGlassPalette.SubtleStroke,
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
                        Text(if (isArabic) "حفظ" else "Save", color = ScannerGlassPalette.OnAccent, fontWeight = FontWeight.Bold)
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
                                .background(if (selected) tagColor.copy(alpha = 0.24f) else ScannerGlassPalette.SubtleChipBg)
                                .border(
                                    width = if (selected) 1.5.dp else 0.8.dp,
                                    color = if (selected) tagColor else ScannerGlassPalette.SubtleStroke,
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
                        Text(if (isArabic) "تم" else "Done", color = ScannerGlassPalette.OnAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Visual Pages Manager Sheet (Edit Edges, Reorder, Duplicate, Extract, Delete, Add Page)
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
            onEditPage = { pg ->
                showPagesSheet = false
                activeTool = AnnotationTool.NONE
                scannerViewModel.openPageCropEditor(doc.id, pg)
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
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ScannerGlassPalette.OnAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "نسخ الكل" else "Copy All", color = ScannerGlassPalette.OnAccent, fontWeight = FontWeight.Bold)
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
            .defaultMinSize(minWidth = 52.dp)
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
                        .border(0.8.dp, ScannerGlassPalette.SubtleStroke, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) ScannerGlassPalette.EdgeOverlayCyan else ScannerGlassPalette.TextPrimary.copy(alpha = 0.90f),
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active) ScannerGlassPalette.EdgeOverlayCyan else ScannerGlassPalette.TextSecondary,
            maxLines = 1,
            softWrap = false,
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
                else ScannerGlassPalette.SubtleChipBg
            )
            .border(
                width = 0.9.dp,
                color = if (selected) ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.75f) else ScannerGlassPalette.SubtleStroke,
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
    onEditPage: (ScannedPageEntity) -> Unit,
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
                                .background(ScannerGlassPalette.ThumbnailFrame)
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SheetActionTile(
                            icon = Icons.Default.Crop,
                            label = if (isArabic) "تعديل الحواف" else "Edit Edges",
                            enabled = true,
                            tint = ScannerGlassPalette.CyanGlow,
                            modifier = Modifier.weight(1f).testTag("pages_sheet_edit_btn"),
                            onClick = { onEditPage(selectedPage) }
                        )
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
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
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
    canSwipePages: Boolean,
    canSwipeDocuments: Boolean,
    loadSignatureBitmap: (String) -> Bitmap?,
    onSelectSignature: (String?) -> Unit,
    onAnnotationsChanged: (PageAnnotations) -> Unit,
    onSignaturesChanged: (List<SignaturePlacement>) -> Unit,
    onSwipeDragDelta: (Float) -> Unit,
    onSwipeRelease: (Float, Float) -> Unit,
    onVerticalSwipeDragDelta: (Float) -> Unit,
    onVerticalSwipeRelease: (Float, Float) -> Unit
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var draftPoints by remember { mutableStateOf<List<PointNorm>>(emptyList()) }
    var draftRedactStart by remember { mutableStateOf<PointNorm?>(null) }
    var draftRedactEnd by remember { mutableStateOf<PointNorm?>(null) }
    var zoomScale by remember(pageId) { mutableFloatStateOf(1f) }
    var panOffset by remember(pageId) { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    val latestZoomScale by rememberUpdatedState(zoomScale)
    val latestPanOffset by rememberUpdatedState(panOffset)
    val latestCanSwipePages by rememberUpdatedState(canSwipePages)
    val latestCanSwipeDocuments by rememberUpdatedState(canSwipeDocuments)
    val latestOnSwipeDragDelta by rememberUpdatedState(onSwipeDragDelta)
    val latestOnSwipeRelease by rememberUpdatedState(onSwipeRelease)
    val latestOnVerticalSwipeDragDelta by rememberUpdatedState(onVerticalSwipeDragDelta)
    val latestOnVerticalSwipeRelease by rememberUpdatedState(onVerticalSwipeRelease)

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
                            val swipeSlopPx = viewConfiguration.touchSlop
                            awaitEachGesture {
                                val firstDown = awaitFirstDown(requireUnconsumed = false)
                                val downPos = firstDown.position
                                var wasMultiTouch = false
                                var swipeAxis = 0 // 0 = undecided, 1 = horizontal pages, 2 = vertical documents
                                var swipeDecidedNonSwipe = false
                                var totalDx = 0f
                                var totalDy = 0f

                                do {
                                    val event = awaitPointerEvent()
                                    val pressedCount = event.changes.count { it.pressed }
                                    if (pressedCount >= 2) {
                                        wasMultiTouch = true
                                        if (swipeAxis == 1) {
                                            swipeAxis = 0
                                            latestOnSwipeDragDelta(0f)
                                        } else if (swipeAxis == 2) {
                                            swipeAxis = 0
                                            latestOnVerticalSwipeDragDelta(0f)
                                        }
                                        val zoom = event.calculateZoom()
                                        val pan = event.calculatePan()
                                        val centroid = event.calculateCentroid(useCurrent = false)
                                        val stageW = size.width.toFloat().coerceAtLeast(1f)
                                        val stageH = size.height.toFloat().coerceAtLeast(1f)
                                        val stageScale = minOf(stageW / bw, stageH / bh)
                                        val imgW = bw * stageScale
                                        val imgH = bh * stageScale

                                        val oldScale = latestZoomScale
                                        val newScale = (oldScale * zoom).coerceIn(1f, 4f)
                                        if (newScale <= 1.01f) {
                                            zoomScale = 1f
                                            panOffset = Offset.Zero
                                        } else {
                                            val zoomFactor = newScale / oldScale
                                            val center = Offset(stageW / 2f, stageH / 2f)
                                            val focalFromCenter = centroid - center
                                            val candidatePan = (latestPanOffset - focalFromCenter) * zoomFactor + focalFromCenter + pan
                                            val maxPanX = ((imgW * newScale - stageW).coerceAtLeast(0f)) / 2f
                                            val maxPanY = ((imgH * newScale - stageH).coerceAtLeast(0f)) / 2f
                                            zoomScale = newScale
                                            panOffset = Offset(
                                                x = candidatePan.x.coerceIn(-maxPanX, maxPanX),
                                                y = candidatePan.y.coerceIn(-maxPanY, maxPanY)
                                            )
                                        }
                                        event.changes.forEach { it.consume() }
                                    } else if (pressedCount == 1) {
                                        val change = event.changes.firstOrNull { it.pressed } ?: break
                                        // Respect child interactions (e.g. dragging a placed signature)
                                        if (change.isConsumed) {
                                            swipeDecidedNonSwipe = true
                                            if (swipeAxis == 1) {
                                                swipeAxis = 0
                                                latestOnSwipeDragDelta(0f)
                                            } else if (swipeAxis == 2) {
                                                swipeAxis = 0
                                                latestOnVerticalSwipeDragDelta(0f)
                                            }
                                            continue
                                        }
                                        if (latestZoomScale > 1.02f) {
                                            val pan = event.calculatePan()
                                            if (pan != Offset.Zero) {
                                                val stageW = size.width.toFloat().coerceAtLeast(1f)
                                                val stageH = size.height.toFloat().coerceAtLeast(1f)
                                                val stageScale = minOf(stageW / bw, stageH / bh)
                                                val imgW = bw * stageScale
                                                val imgH = bh * stageScale
                                                val maxPanX = ((imgW * latestZoomScale - stageW).coerceAtLeast(0f)) / 2f
                                                val maxPanY = ((imgH * latestZoomScale - stageH).coerceAtLeast(0f)) / 2f
                                                val candidatePan = latestPanOffset + pan
                                                panOffset = Offset(
                                                    x = candidatePan.x.coerceIn(-maxPanX, maxPanX),
                                                    y = candidatePan.y.coerceIn(-maxPanY, maxPanY)
                                                )
                                                change.consume()
                                            }
                                        } else if ((latestCanSwipePages || latestCanSwipeDocuments) && !wasMultiTouch && !swipeDecidedNonSwipe) {
                                            totalDx = change.position.x - downPos.x
                                            totalDy = change.position.y - downPos.y
                                            if (swipeAxis == 0) {
                                                val dist = hypot(totalDx.toDouble(), totalDy.toDouble()).toFloat()
                                                if (dist > swipeSlopPx) {
                                                    when {
                                                        latestCanSwipePages && abs(totalDx) > abs(totalDy) * 1.15f -> {
                                                            swipeAxis = 1
                                                            change.consume()
                                                            latestOnSwipeDragDelta(totalDx)
                                                        }
                                                        latestCanSwipeDocuments && abs(totalDy) > abs(totalDx) * 1.15f -> {
                                                            swipeAxis = 2
                                                            change.consume()
                                                            latestOnVerticalSwipeDragDelta(totalDy)
                                                        }
                                                        else -> {
                                                            swipeDecidedNonSwipe = true
                                                        }
                                                    }
                                                }
                                            } else if (swipeAxis == 1) {
                                                change.consume()
                                                latestOnSwipeDragDelta(totalDx)
                                            } else if (swipeAxis == 2) {
                                                change.consume()
                                                latestOnVerticalSwipeDragDelta(totalDy)
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                when (swipeAxis) {
                                    1 -> latestOnSwipeRelease(totalDx, totalDy)
                                    2 -> latestOnVerticalSwipeRelease(totalDx, totalDy)
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
            val sigBmp by produceState<Bitmap?>(initialValue = null, placement.signatureId) {
                value = withContext(Dispatchers.IO) {
                    loadSignatureBitmap(placement.signatureId)
                }
            }
            val loadedSigBmp = sigBmp
            if (loadedSigBmp != null && boxSize.width > 0) {
                val sigImg = remember(loadedSigBmp) { loadedSigBmp.asImageBitmap() }
                val targetW = drawW * 0.28f * placement.scale
                val aspect = loadedSigBmp.height.toFloat() / loadedSigBmp.width.coerceAtLeast(1).toFloat()
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
                            unfocusedBorderColor = ScannerGlassPalette.SubtleStroke,
                            focusedTextColor = ScannerGlassPalette.TextPrimary,
                            unfocusedTextColor = ScannerGlassPalette.TextPrimary,
                            focusedLabelColor = ScannerGlassPalette.AmberGlow,
                            unfocusedLabelColor = ScannerGlassPalette.TextSecondary
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
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = ScannerGlassPalette.OnAccent)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isArabic) "حفظ في الهاتف" else "Save Downloads", color = ScannerGlassPalette.OnAccent, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
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
