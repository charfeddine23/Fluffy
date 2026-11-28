package com.charfeddine.fluffy.scanner.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.charfeddine.fluffy.scanner.data.local.PdfReadingProgressStore
import com.charfeddine.fluffy.scanner.data.model.DevicePdfEntry
import com.charfeddine.fluffy.scanner.data.model.PdfReaderThemeMode
import com.charfeddine.fluffy.scanner.data.model.PdfScrollMode
import com.charfeddine.fluffy.scanner.domain.ScannerViewModel
import com.charfeddine.fluffy.scanner.ui.theme.ScannerGlassPalette
import com.charfeddine.fluffy.scanner.ui.theme.iosLiquidGlass
import com.charfeddine.fluffy.scanner.utils.DevicePdfDiscovery
import com.charfeddine.fluffy.scanner.utils.OfflineOcrAndBarcodeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    pdfEntry: DevicePdfEntry,
    scannerViewModel: ScannerViewModel,
    progressStore: PdfReadingProgressStore,
    isArabic: Boolean,
    onBackToLibrary: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val systemDark = isSystemInDarkTheme()
    val pdfUri = remember(pdfEntry.uriString) { Uri.parse(pdfEntry.uriString) }

    val progressMap by progressStore.progressMap.collectAsState()
    val savedProgress = progressMap[pdfEntry.key]
    val initialSavedPage = remember(pdfEntry.key) {
        (progressStore.getProgress(pdfEntry.key)?.lastPageIndex ?: 0).coerceAtLeast(0)
    }

    var pageCount by remember(pdfEntry.key) {
        mutableIntStateOf(maxOf(pdfEntry.pageCount, savedProgress?.totalPages ?: 0))
    }
    var currentPage by remember(pdfEntry.key) { mutableIntStateOf(initialSavedPage) }
    var isLoadingDoc by remember(pdfEntry.key) { mutableStateOf(pageCount <= 0) }
    var loadError by remember(pdfEntry.key) { mutableStateOf(false) }

    var isFocusMode by remember(pdfEntry.key) { mutableStateOf(false) }
    var keepScreenOn by remember(pdfEntry.key) { mutableStateOf(true) }
    var showViewSettingsSheet by remember { mutableStateOf(false) }
    var showPagesSheet by remember { mutableStateOf(false) }
    var showSaveSheet by remember { mutableStateOf(false) }
    var showJumpPageDialog by remember { mutableStateOf(false) }
    var showOcrSheet by remember { mutableStateOf(false) }
    var extractedOcrText by remember { mutableStateOf("") }
    var isWorkingAction by remember { mutableStateOf(false) }

    var zoomScale by remember(pdfEntry.key) { mutableFloatStateOf(1f) }
    var panOffset by remember(pdfEntry.key) { mutableStateOf(Offset.Zero) }

    val themeMode = savedProgress?.themeMode ?: PdfReaderThemeMode.ADAPTIVE_AUTO
    val scrollMode = savedProgress?.scrollMode ?: PdfScrollMode.VERTICAL_CONTINUOUS
    val rotationDeg = savedProgress?.rotationDeg ?: 0
    val brightnessLevel = savedProgress?.brightnessLevel ?: 1f
    val bookmarkedPages = savedProgress?.bookmarkedPages ?: emptySet()
    val isCurrentBookmarked = currentPage in bookmarkedPages

    // Bounded LRU cache for rendered pages
    val pageBitmapCache = remember(pdfEntry.key) {
        val budgetKb = (Runtime.getRuntime().maxMemory() / 1024L / 10L).toInt().coerceIn(8 * 1024, 28 * 1024)
        object : LruCache<Int, Bitmap>(budgetKb) {
            override fun sizeOf(key: Int, value: Bitmap): Int =
                (value.byteCount / 1024).coerceAtLeast(1)
        }
    }

    suspend fun getOrRenderPage(index: Int, maxWidth: Int = 1480): Bitmap? {
        pageBitmapCache.get(index)?.let { cached ->
            if (!cached.isRecycled) return cached
        }
        val (bmp, total) = DevicePdfDiscovery.renderPdfPageBitmap(
            context = context,
            uri = pdfUri,
            pageIndex = index,
            targetMaxWidth = maxWidth
        )
        if (total > 0 && pageCount != total) {
            pageCount = total
        }
        if (bmp != null && !bmp.isRecycled) {
            pageBitmapCache.put(index, bmp)
        }
        return bmp
    }

    // Resolve total page count on open & immediately validate saved page index
    LaunchedEffect(pdfEntry.key) {
        isLoadingDoc = true
        val (bmp, count) = DevicePdfDiscovery.renderPdfPageBitmap(
            context = context,
            uri = pdfUri,
            pageIndex = initialSavedPage.coerceAtLeast(0),
            targetMaxWidth = 1480
        )
        if (count > 0) {
            pageCount = count
            val safeTarget = initialSavedPage.coerceIn(0, count - 1)
            if (safeTarget == initialSavedPage && bmp != null && !bmp.isRecycled) {
                pageBitmapCache.put(safeTarget, bmp)
            } else {
                bmp?.recycle()
            }
            currentPage = safeTarget
            loadError = false
        } else {
            val (metaCount, _) = DevicePdfDiscovery.loadPdfMetadataAndThumb(context, pdfUri, 200)
            if (metaCount > 0) {
                pageCount = metaCount
                currentPage = initialSavedPage.coerceIn(0, metaCount - 1)
                loadError = false
            } else {
                loadError = true
            }
        }
        isLoadingDoc = false
    }

    // Preload adjacent pages smoothly for lag-free scrolling/flipping
    LaunchedEffect(currentPage, pageCount) {
        if (pageCount <= 0) return@LaunchedEffect
        val neighbors = listOf(currentPage + 1, currentPage - 1, currentPage + 2)
            .filter { it in 0 until pageCount }
        for (idx in neighbors) {
            if (pageBitmapCache.get(idx) == null) {
                withContext(Dispatchers.IO) {
                    getOrRenderPage(idx, 1480)
                }
            }
        }
    }

    // Always keep screen awake while reading if enabled, and restore on exit
    DisposableEffect(keepScreenOn, view) {
        val previous = view.keepScreenOn
        view.keepScreenOn = keepScreenOn
        onDispose {
            view.keepScreenOn = previous
        }
    }

    // Hide/show system bars cleanly in Focus Mode
    DisposableEffect(isFocusMode, view) {
        val activity = context.findActivity()
        val window = activity?.window
        val controller = if (window != null) WindowCompat.getInsetsController(window, view) else null
        if (controller != null) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isFocusMode) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Persist reading progress automatically on every page turn AND when leaving the reader
    val latestCurrentPage by rememberUpdatedState(currentPage)
    val latestPageCount by rememberUpdatedState(pageCount)
    LaunchedEffect(currentPage, pageCount) {
        if (pageCount > 0) {
            progressStore.savePageProgress(pdfEntry, currentPage, pageCount)
        }
    }
    DisposableEffect(pdfEntry.key) {
        onDispose {
            if (latestPageCount > 0) {
                progressStore.savePageProgress(pdfEntry, latestCurrentPage, latestPageCount)
            }
            pageBitmapCache.evictAll()
        }
    }

    BackHandler {
        if (isFocusMode) {
            isFocusMode = false
        } else {
            if (pageCount > 0) {
                progressStore.savePageProgress(pdfEntry, currentPage, pageCount)
            }
            onBackToLibrary()
        }
    }

    // Sample current page bitmap for adaptive backdrop lighting
    var currentPageSampledColor by remember(pdfEntry.key) {
        mutableStateOf(if (systemDark) Color(0xFF0A0D14) else Color(0xFFF3EFE6))
    }
    LaunchedEffect(currentPage, pageCount, themeMode, systemDark) {
        if (themeMode == PdfReaderThemeMode.ADAPTIVE_AUTO && pageCount > 0) {
            val bmp = withContext(Dispatchers.IO) { getOrRenderPage(currentPage, 1480) }
            currentPageSampledColor = if (systemDark) {
                Color(0xFF0B0F17)
            } else {
                DevicePdfDiscovery.sampleAdaptiveBackdropColor(bmp, preferDarkEnvironment = false)
            }
        }
    }

    val targetBackdropColor = when (themeMode) {
        PdfReaderThemeMode.ADAPTIVE_AUTO -> if (isFocusMode) {
            currentPageSampledColor
        } else {
            if (systemDark) Color(0xFF090D16) else Color(0xFF141824)
        }
        PdfReaderThemeMode.PAPER_LIGHT -> Color(0xFFF5F2EA)
        PdfReaderThemeMode.SEPIA_WARM -> Color(0xFFEAD9BA)
        PdfReaderThemeMode.DARK_SLATE -> Color(0xFF0E131D)
        PdfReaderThemeMode.AMOLED_BLACK -> Color(0xFF000000)
    }

    val animatedBackdrop by animateColorAsState(
        targetValue = targetBackdropColor,
        animationSpec = tween(360),
        label = "pdf_reader_adaptive_backdrop"
    )

    val pageColorFilter = remember(themeMode) {
        when (themeMode) {
            PdfReaderThemeMode.SEPIA_WARM -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.96f, 0.04f, 0.00f, 0f, 6f,
                        0.02f, 0.91f, 0.02f, 0f, 0f,
                        0.00f, 0.02f, 0.78f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            PdfReaderThemeMode.DARK_SLATE -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -0.88f, 0f, 0f, 0f, 238f,
                        0f, -0.87f, 0f, 0f, 236f,
                        0f, 0f, -0.84f, 0f, 234f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            PdfReaderThemeMode.AMOLED_BLACK -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -0.96f, 0f, 0f, 0f, 245f,
                        0f, -0.96f, 0f, 0f, 245f,
                        0f, 0f, -0.95f, 0f, 245f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            else -> null
        }
    }

    // List/Pager states initialized cleanly to initialSavedPage
    val verticalListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialSavedPage.coerceAtLeast(0)
    )
    val horizontalPagerState = rememberPagerState(
        initialPage = initialSavedPage.coerceAtLeast(0),
        pageCount = { pageCount.coerceAtLeast(1) }
    )
    var hasSyncedInitialScroll by remember(pdfEntry.key, scrollMode) { mutableStateOf(false) }

    fun jumpToPage(targetIndex: Int, animate: Boolean = false) {
        if (pageCount <= 0) return
        val clamped = targetIndex.coerceIn(0, pageCount - 1)
        zoomScale = 1f
        panOffset = Offset.Zero
        currentPage = clamped
        scope.launch {
            if (scrollMode == PdfScrollMode.VERTICAL_CONTINUOUS) {
                if (animate) verticalListState.animateScrollToItem(clamped)
                else verticalListState.scrollToItem(clamped)
            } else {
                if (animate) horizontalPagerState.animateScrollToPage(clamped)
                else horizontalPagerState.scrollToPage(clamped)
            }
        }
    }

    // Sync initial page when document count arrives & when switching scroll modes
    LaunchedEffect(pageCount, scrollMode) {
        if (pageCount > 0 && !hasSyncedInitialScroll) {
            val target = currentPage.coerceIn(0, pageCount - 1)
            if (scrollMode == PdfScrollMode.VERTICAL_CONTINUOUS) {
                verticalListState.scrollToItem(target)
            } else {
                horizontalPagerState.scrollToPage(target)
            }
            hasSyncedInitialScroll = true
        }
    }

    // Track visible page in vertical scroll mode
    LaunchedEffect(verticalListState, scrollMode, pageCount) {
        if (scrollMode != PdfScrollMode.VERTICAL_CONTINUOUS || pageCount <= 0) return@LaunchedEffect
        snapshotFlow {
            val info = verticalListState.layoutInfo
            val viewportMid = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs((item.offset + item.size / 2) - viewportMid)
            }?.index ?: verticalListState.firstVisibleItemIndex
        }
            .distinctUntilChanged()
            .collect { visibleIdx ->
                if (hasSyncedInitialScroll && visibleIdx in 0 until pageCount) {
                    currentPage = visibleIdx
                }
            }
    }

    // Track settled page in horizontal pager mode
    LaunchedEffect(horizontalPagerState, scrollMode, pageCount) {
        if (scrollMode != PdfScrollMode.HORIZONTAL_PAGER || pageCount <= 0) return@LaunchedEffect
        snapshotFlow { horizontalPagerState.settledPage }
            .distinctUntilChanged()
            .collect { settled ->
                if (hasSyncedInitialScroll && settled in 0 until pageCount) {
                    currentPage = settled
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBackdrop)
            .testTag("pdf_reader_root")
    ) {
        // 1. Main Read-Only PDF Viewport (Supports pinch-to-zoom, double-tap, tap to toggle Focus Mode)
        when {
            isLoadingDoc -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ScannerGlassPalette.CyanGlow)
                }
            }

            loadError || pageCount <= 0 -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(28.dp)
                        .iosLiquidGlass(RoundedCornerShape(24.dp), glowAccent = true, fillAlpha = 0.85f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = ScannerGlassPalette.RoseGlow,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = if (isArabic) "تعذر فتح ملف PDF هذا" else "Unable to open this PDF",
                        color = ScannerGlassPalette.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Button(
                        onClick = onBackToLibrary,
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow)
                    ) {
                        Text(
                            text = if (isArabic) "العودة للمكتبة" else "Back to Library",
                            color = ScannerGlassPalette.OnAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            else -> {
                val latestZoom by rememberUpdatedState(zoomScale)
                val latestPan by rememberUpdatedState(panOffset)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(pdfEntry.key) {
                            detectTapGestures(
                                onTap = { isFocusMode = !isFocusMode },
                                onDoubleTap = {
                                    if (latestZoom > 1.15f) {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    } else {
                                        zoomScale = 2.25f
                                    }
                                }
                            )
                        }
                        .pointerInput(pdfEntry.key) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val pressedCount = event.changes.count { it.pressed }
                                    if (pressedCount >= 2 || latestZoom > 1.03f) {
                                        val zoom = event.calculateZoom()
                                        val pan = event.calculatePan()
                                        val newScale = (latestZoom * zoom).coerceIn(1f, 3.2f)
                                        zoomScale = newScale
                                        if (newScale <= 1.02f) {
                                            panOffset = Offset.Zero
                                        } else {
                                            val maxX = size.width * (newScale - 1f) * 0.52f
                                            val maxY = size.height * (newScale - 1f) * 0.52f
                                            panOffset = Offset(
                                                x = (latestPan.x + pan.x).coerceIn(-maxX, maxX),
                                                y = (latestPan.y + pan.y).coerceIn(-maxY, maxY)
                                            )
                                        }
                                        if (pressedCount >= 2) {
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panOffset.x
                            translationY = panOffset.y
                        }
                ) {
                    val contentTopPad = if (isFocusMode) 12.dp else 82.dp
                    val contentBottomPad = if (isFocusMode) 12.dp else 94.dp

                    if (scrollMode == PdfScrollMode.VERTICAL_CONTINUOUS) {
                        LazyColumn(
                            state = verticalListState,
                            userScrollEnabled = zoomScale <= 1.04f,
                            contentPadding = PaddingValues(
                                top = contentTopPad,
                                bottom = contentBottomPad,
                                start = 10.dp,
                                end = 10.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("pdf_reader_vertical_list")
                        ) {
                            items(pageCount, key = { idx -> "${pdfEntry.key}_v_$idx" }) { pageIdx ->
                                PdfRenderedPageCard(
                                    pdfKey = pdfEntry.key,
                                    pageIndex = pageIdx,
                                    rotationDeg = rotationDeg,
                                    colorFilter = pageColorFilter,
                                    isFocusMode = isFocusMode,
                                    renderPage = { getOrRenderPage(pageIdx, 1480) }
                                )
                            }
                        }
                    } else {
                        HorizontalPager(
                            state = horizontalPagerState,
                            userScrollEnabled = zoomScale <= 1.04f,
                            key = { idx -> "${pdfEntry.key}_h_$idx" },
                            pageSpacing = 12.dp,
                            contentPadding = PaddingValues(
                                top = contentTopPad,
                                bottom = contentBottomPad,
                                start = 10.dp,
                                end = 10.dp
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("pdf_reader_horizontal_pager")
                        ) { pageIdx ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                PdfRenderedPageCard(
                                    pdfKey = pdfEntry.key,
                                    pageIndex = pageIdx,
                                    rotationDeg = rotationDeg,
                                    colorFilter = pageColorFilter,
                                    isFocusMode = isFocusMode,
                                    renderPage = { getOrRenderPage(pageIdx, 1480) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Optional Eye-Comfort Brightness Dimmer Veil (stays non-blocking to touches)
        if (brightnessLevel < 0.99f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1f - brightnessLevel).coerceIn(0f, 0.65f)))
            )
        }

        // 2. Top Floating Liquid Glass Toolbar (Hidden completely in Focus Mode)
        AnimatedVisibility(
            visible = !isFocusMode,
            enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { -it / 2 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(200)) { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("pdf_reader_top_bar"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (pageCount > 0) {
                            progressStore.savePageProgress(pdfEntry, currentPage, pageCount)
                        }
                        onBackToLibrary()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(12.dp, CircleShape)
                        .iosLiquidGlass(CircleShape, fillAlpha = 0.78f)
                        .testTag("pdf_reader_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isArabic) "رجوع" else "Back",
                        tint = ScannerGlassPalette.TextPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .shadow(12.dp, CircleShape)
                        .iosLiquidGlass(CircleShape, fillAlpha = 0.74f)
                        .clickable { showJumpPageDialog = true }
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = pdfEntry.title,
                        color = ScannerGlassPalette.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (pageCount > 0) {
                        val pct = (((currentPage + 1).toFloat() / pageCount.toFloat()) * 100f).roundToInt()
                        Text(
                            text = if (isArabic) {
                                "صفحة ${currentPage + 1} من $pageCount • $pct%"
                            } else {
                                "Page ${currentPage + 1} of $pageCount • $pct%"
                            },
                            color = ScannerGlassPalette.CyanGlow,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                // Bookmark / Save Page toggle
                IconButton(
                    onClick = {
                        if (pageCount > 0) {
                            val updated = progressStore.toggleBookmark(pdfEntry, currentPage, pageCount)
                            val added = currentPage in updated
                            scannerViewModel.postMessage(
                                if (added) {
                                    if (isArabic) "تم حفظ الصفحة ${currentPage + 1} في الإشارات ✓"
                                    else "Saved page ${currentPage + 1} to bookmarks ✓"
                                } else {
                                    if (isArabic) "تمت إزالة الصفحة من الإشارات"
                                    else "Removed page bookmark"
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(12.dp, CircleShape)
                        .iosLiquidGlass(CircleShape, glowAccent = isCurrentBookmarked, fillAlpha = 0.78f)
                        .testTag("pdf_reader_bookmark_btn")
                ) {
                    Icon(
                        imageVector = if (isCurrentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isArabic) "حفظ الصفحة" else "Bookmark page",
                        tint = if (isCurrentBookmarked) ScannerGlassPalette.AmberGlow else ScannerGlassPalette.TextPrimary
                    )
                }

                // Focus Mode Toggle
                IconButton(
                    onClick = { isFocusMode = true },
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(12.dp, CircleShape)
                        .iosLiquidGlass(CircleShape, glowAccent = true, fillAlpha = 0.80f)
                        .testTag("pdf_reader_focus_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = if (isArabic) "وضع التركيز" else "Focus Mode",
                        tint = ScannerGlassPalette.VioletGlow
                    )
                }
            }
        }

        // 3. Bottom Scrubber & Viewing / Saving Controls (Hidden completely in Focus Mode)
        AnimatedVisibility(
            visible = !isFocusMode && pageCount > 0,
            enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 2 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(200)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("pdf_reader_bottom_bar"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pageCount > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .iosLiquidGlass(CircleShape, fillAlpha = 0.78f)
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${currentPage + 1} / $pageCount",
                            color = ScannerGlassPalette.CyanGlow,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showJumpPageDialog = true }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("pdf_reader_page_pill")
                        )
                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = { valFloat ->
                                val idx = valFloat.roundToInt().coerceIn(0, pageCount - 1)
                                if (idx != currentPage) {
                                    jumpToPage(idx, animate = false)
                                }
                            },
                            valueRange = 0f..(pageCount - 1).toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = ScannerGlassPalette.VioletGlow,
                                activeTrackColor = ScannerGlassPalette.VioletGlow,
                                inactiveTrackColor = ScannerGlassPalette.SubtleStroke
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pdf_reader_scrubber")
                        )
                    }
                }

                // Bottom Liquid Glass Action Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(16.dp, CircleShape)
                        .iosLiquidGlass(CircleShape, glowAccent = true, fillAlpha = 0.80f)
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReaderDockActionItem(
                        icon = Icons.Default.Tune,
                        label = if (isArabic) "العرض" else "View",
                        tint = ScannerGlassPalette.CyanGlow,
                        tag = "pdf_reader_view_btn",
                        onClick = { showViewSettingsSheet = true }
                    )
                    ReaderDockActionItem(
                        icon = if (scrollMode == PdfScrollMode.VERTICAL_CONTINUOUS) Icons.Default.SwapHoriz else Icons.Default.SwapVert,
                        label = if (scrollMode == PdfScrollMode.VERTICAL_CONTINUOUS) {
                            if (isArabic) "تقليب" else "Flip"
                        } else {
                            if (isArabic) "عمودي" else "Scroll"
                        },
                        tint = ScannerGlassPalette.VioletGlow,
                        tag = "pdf_reader_scroll_toggle_btn",
                        onClick = {
                            val nextScroll = if (scrollMode == PdfScrollMode.VERTICAL_CONTINUOUS) {
                                PdfScrollMode.HORIZONTAL_PAGER
                            } else {
                                PdfScrollMode.VERTICAL_CONTINUOUS
                            }
                            progressStore.updateReaderSettings(pdfEntry, scrollMode = nextScroll)
                        }
                    )
                    ReaderDockActionItem(
                        icon = Icons.Default.GridView,
                        label = if (isArabic) "الصفحات" else "Pages",
                        tint = ScannerGlassPalette.AmberGlow,
                        tag = "pdf_reader_pages_btn",
                        onClick = { showPagesSheet = true }
                    )
                    ReaderDockActionItem(
                        icon = Icons.Default.FileDownload,
                        label = if (isArabic) "حفظ" else "Save",
                        tint = ScannerGlassPalette.EmeraldGlow,
                        tag = "pdf_reader_save_btn",
                        onClick = { showSaveSheet = true }
                    )
                    ReaderDockActionItem(
                        icon = Icons.AutoMirrored.Filled.RotateRight,
                        label = if (isArabic) "تدوير" else "Rotate",
                        tint = ScannerGlassPalette.TextPrimary,
                        tag = "pdf_reader_rotate_btn",
                        onClick = {
                            progressStore.updateReaderSettings(
                                pdfEntry,
                                rotationDeg = (rotationDeg + 90) % 360
                            )
                        }
                    )
                }
            }
        }

        if (isWorkingAction) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ScannerGlassPalette.CyanGlow)
            }
        }
    }

    // View Modes & Reading Comfort Bottom Sheet
    if (showViewSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showViewSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ScannerGlassPalette.FrostedElevated.copy(alpha = 0.97f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isArabic) "أوضاع العرض والراحة البصرية" else "Display & Reading Comfort",
                    color = ScannerGlassPalette.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )

                // Reading Theme Pills
                Text(
                    text = if (isArabic) "خلفية وإضاءة القراءة" else "Reading Theme",
                    color = ScannerGlassPalette.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PdfReaderThemeMode.entries.forEach { mode ->
                        val selected = themeMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("pdf_theme_${mode.id}")
                                .iosLiquidGlass(
                                    shape = RoundedCornerShape(14.dp),
                                    glowAccent = selected,
                                    fillAlpha = if (selected) 0.88f else 0.56f
                                )
                                .clickable {
                                    progressStore.updateReaderSettings(pdfEntry, themeMode = mode)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) mode.labelAr else mode.labelEn,
                                color = if (selected) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Scroll Orientation Toggle
                Text(
                    text = if (isArabic) "طريقة التمرير" else "Reading Layout",
                    color = ScannerGlassPalette.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PdfScrollMode.entries.forEach { mode ->
                        val selected = scrollMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pdf_scroll_${mode.id}")
                                .iosLiquidGlass(
                                    shape = RoundedCornerShape(16.dp),
                                    glowAccent = selected,
                                    fillAlpha = if (selected) 0.88f else 0.58f
                                )
                                .clickable {
                                    progressStore.updateReaderSettings(pdfEntry, scrollMode = mode)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) mode.labelAr else mode.labelEn,
                                color = if (selected) ScannerGlassPalette.VioletGlow else ScannerGlassPalette.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Dimmer / Brightness Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Brightness6,
                        contentDescription = null,
                        tint = ScannerGlassPalette.AmberGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isArabic) "سطوع القراءة" else "Dimmer",
                        color = ScannerGlassPalette.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = brightnessLevel,
                        onValueChange = {
                            progressStore.updateReaderSettings(pdfEntry, brightnessLevel = it)
                        },
                        valueRange = 0.35f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = ScannerGlassPalette.AmberGlow,
                            activeTrackColor = ScannerGlassPalette.AmberGlow
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Keep Screen On Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = ScannerGlassPalette.EmeraldGlow,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isArabic) "إبقاء الشاشة مضاءة أثناء القراءة" else "Keep screen awake while reading",
                            color = ScannerGlassPalette.TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Switch(
                        checked = keepScreenOn,
                        onCheckedChange = { keepScreenOn = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = ScannerGlassPalette.EmeraldGlow)
                    )
                }

                Button(
                    onClick = {
                        showViewSettingsSheet = false
                        isFocusMode = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.VioletGlow),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = ScannerGlassPalette.OnAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "تفعيل وضع التركيز التام" else "Enter Focus Mode",
                        color = ScannerGlassPalette.OnAccent,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Pages Overview & Saved/Bookmarked Pages Sheet
    if (showPagesSheet) {
        var sheetTab by remember { mutableIntStateOf(0) } // 0=All Pages, 1=Bookmarked
        ModalBottomSheet(
            onDismissRequest = { showPagesSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ScannerGlassPalette.FrostedElevated.copy(alpha = 0.97f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 540.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .iosLiquidGlass(CircleShape, glowAccent = sheetTab == 0, fillAlpha = if (sheetTab == 0) 0.86f else 0.56f)
                            .clickable { sheetTab = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "كل الصفحات ($pageCount)" else "All Pages ($pageCount)",
                            color = if (sheetTab == 0) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .iosLiquidGlass(CircleShape, glowAccent = sheetTab == 1, fillAlpha = if (sheetTab == 1) 0.86f else 0.56f)
                            .clickable { sheetTab = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "المحفوظة (${bookmarkedPages.size})" else "Saved (${bookmarkedPages.size})",
                            color = if (sheetTab == 1) ScannerGlassPalette.AmberGlow else ScannerGlassPalette.TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }

                val targetIndices = if (sheetTab == 0) {
                    (0 until pageCount).toList()
                } else {
                    bookmarkedPages.sorted().filter { it in 0 until pageCount }
                }

                if (targetIndices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "لم تحفظ أي صفحة في الإشارات بعد" else "No bookmarked pages yet — tap Bookmark while reading",
                            color = ScannerGlassPalette.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(targetIndices, key = { it }) { idx ->
                            val isSelected = idx == currentPage
                            val isSaved = idx in bookmarkedPages
                            val thumbBmp by produceState<Bitmap?>(initialValue = null, key1 = pdfEntry.key, key2 = idx) {
                                val (bmp, _) = DevicePdfDiscovery.renderPdfPageBitmap(context, pdfUri, idx, 220)
                                value = bmp
                            }
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ScannerGlassPalette.FrostedCardDark)
                                    .border(
                                        width = if (isSelected) 1.6.dp else 0.8.dp,
                                        color = if (isSelected) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.SubtleStroke,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        showPagesSheet = false
                                        jumpToPage(idx, animate = false)
                                    }
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.72f)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    thumbBmp?.let {
                                        Image(
                                            bitmap = it.asImageBitmap(),
                                            contentDescription = "Page ${idx + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    if (isSaved) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = null,
                                            tint = ScannerGlassPalette.AmberGlow,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${idx + 1}",
                                    color = if (isSelected) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (sheetTab == 1 && targetIndices.isNotEmpty()) {
                        Button(
                            onClick = {
                                showPagesSheet = false
                                isWorkingAction = true
                                scope.launch {
                                    try {
                                        val savedPath = DevicePdfDiscovery.savePdfPagesAsStandalonePdf(
                                            context = context,
                                            uri = pdfUri,
                                            pageIndices = targetIndices,
                                            titleBase = pdfEntry.title
                                        )
                                        scannerViewModel.postMessage(
                                            if (savedPath != null) {
                                                if (isArabic) "تم حفظ الصفحات المحفوظة كملف PDF ✓" else "Saved bookmarked pages to PDF ✓"
                                            } else {
                                                if (isArabic) "تعذر حفظ الملف" else "Unable to save PDF"
                                            }
                                        )
                                    } finally {
                                        isWorkingAction = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.AmberGlow),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = if (isArabic) "تصدير كل الصفحات المحفوظة كـ PDF" else "Export Saved Pages as PDF",
                                color = ScannerGlassPalette.OnAccent,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Save / Export Page Tools Sheet
    if (showSaveSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSaveSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ScannerGlassPalette.FrostedElevated.copy(alpha = 0.97f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isArabic) {
                        "حفظ ومشاركة الصفحة (${currentPage + 1})"
                    } else {
                        "Save & Export Page ${currentPage + 1}"
                    },
                    color = ScannerGlassPalette.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )

                SavePageActionRow(
                    icon = Icons.Default.Image,
                    title = if (isArabic) "حفظ الصفحة الحالية كصورة (JPG)" else "Save Current Page as JPG (Gallery)",
                    accent = ScannerGlassPalette.CyanGlow,
                    tag = "pdf_save_page_jpg",
                    onClick = {
                        showSaveSheet = false
                        isWorkingAction = true
                        scope.launch {
                            try {
                                val res = DevicePdfDiscovery.savePdfPageAsImageToGallery(
                                    context, pdfUri, currentPage, pdfEntry.title, asPng = false
                                )
                                scannerViewModel.postMessage(
                                    if (res != null) {
                                        if (isArabic) "تم حفظ الصفحة في المعرض ✓" else "Saved page to Gallery ✓"
                                    } else {
                                        if (isArabic) "تعذر حفظ الصورة" else "Unable to save image"
                                    }
                                )
                            } finally {
                                isWorkingAction = false
                            }
                        }
                    }
                )

                SavePageActionRow(
                    icon = Icons.Default.Image,
                    title = if (isArabic) "حفظ الصفحة بدقة عالية (PNG)" else "Save Current Page as Lossless PNG",
                    accent = ScannerGlassPalette.VioletGlow,
                    tag = "pdf_save_page_png",
                    onClick = {
                        showSaveSheet = false
                        isWorkingAction = true
                        scope.launch {
                            try {
                                val res = DevicePdfDiscovery.savePdfPageAsImageToGallery(
                                    context, pdfUri, currentPage, pdfEntry.title, asPng = true
                                )
                                scannerViewModel.postMessage(
                                    if (res != null) {
                                        if (isArabic) "تم حفظ PNG في المعرض ✓" else "Saved PNG to Gallery ✓"
                                    } else {
                                        if (isArabic) "تعذر حفظ الصورة" else "Unable to save image"
                                    }
                                )
                            } finally {
                                isWorkingAction = false
                            }
                        }
                    }
                )

                SavePageActionRow(
                    icon = Icons.Default.PictureAsPdf,
                    title = if (isArabic) "استخراج الصفحة الحالية كملف PDF" else "Save Current Page as Standalone PDF",
                    accent = ScannerGlassPalette.EmeraldGlow,
                    tag = "pdf_save_page_pdf",
                    onClick = {
                        showSaveSheet = false
                        isWorkingAction = true
                        scope.launch {
                            try {
                                val res = DevicePdfDiscovery.savePdfPagesAsStandalonePdf(
                                    context, pdfUri, listOf(currentPage), pdfEntry.title
                                )
                                scannerViewModel.postMessage(
                                    if (res != null) {
                                        if (isArabic) "تم حفظ الصفحة في Downloads/FluffyScans ✓" else "Saved PDF to Downloads/FluffyScans ✓"
                                    } else {
                                        if (isArabic) "تعذر حفظ الصفحة" else "Unable to save PDF"
                                    }
                                )
                            } finally {
                                isWorkingAction = false
                            }
                        }
                    }
                )

                if (bookmarkedPages.isNotEmpty()) {
                    SavePageActionRow(
                        icon = Icons.Default.Bookmark,
                        title = if (isArabic) {
                            "حفظ كل الصفحات المرجعية (${bookmarkedPages.size}) كـ PDF"
                        } else {
                            "Save All ${bookmarkedPages.size} Bookmarked Pages as PDF"
                        },
                        accent = ScannerGlassPalette.AmberGlow,
                        tag = "pdf_save_bookmarks_pdf",
                        onClick = {
                            showSaveSheet = false
                            isWorkingAction = true
                            scope.launch {
                                try {
                                    val res = DevicePdfDiscovery.savePdfPagesAsStandalonePdf(
                                        context, pdfUri, bookmarkedPages.toList(), pdfEntry.title
                                    )
                                    scannerViewModel.postMessage(
                                        if (res != null) {
                                            if (isArabic) "تم تصدير الصفحات المحفوظة ✓" else "Exported bookmarked pages ✓"
                                        } else {
                                            if (isArabic) "تعذر التصدير" else "Unable to export PDF"
                                        }
                                    )
                                } finally {
                                    isWorkingAction = false
                                }
                            }
                        }
                    )
                }

                SavePageActionRow(
                    icon = Icons.Default.TextSnippet,
                    title = if (isArabic) "استخراج ونسخ نص الصفحة (OCR)" else "Extract & Copy Page Text (OCR)",
                    accent = ScannerGlassPalette.CyanGlow,
                    tag = "pdf_extract_page_ocr",
                    onClick = {
                        showSaveSheet = false
                        isWorkingAction = true
                        scope.launch {
                            try {
                                val bmp = getOrRenderPage(currentPage, 1600)
                                val ocr = if (bmp != null) {
                                    withContext(Dispatchers.Default) {
                                        OfflineOcrAndBarcodeEngine.extractTextOffline(bmp).fullText
                                    }
                                } else ""
                                if (ocr.isBlank()) {
                                    scannerViewModel.postMessage(
                                        if (isArabic) "لم يتم العثور على نص واضح" else "No readable text detected on this page"
                                    )
                                } else {
                                    extractedOcrText = ocr
                                    showOcrSheet = true
                                }
                            } finally {
                                isWorkingAction = false
                            }
                        }
                    }
                )

                SavePageActionRow(
                    icon = Icons.Default.Share,
                    title = if (isArabic) "مشاركة هذه الصفحة" else "Share Current Page",
                    accent = ScannerGlassPalette.TextPrimary,
                    tag = "pdf_share_page",
                    onClick = {
                        showSaveSheet = false
                        isWorkingAction = true
                        scope.launch {
                            try {
                                DevicePdfDiscovery.shareSinglePdfPageImage(
                                    context = context,
                                    uri = pdfUri,
                                    pageIndex = currentPage,
                                    titleBase = pdfEntry.title,
                                    chooserTitle = if (isArabic) "مشاركة الصفحة" else "Share Page"
                                )
                            } finally {
                                isWorkingAction = false
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Go To Page Dialog
    if (showJumpPageDialog) {
        var pageInput by remember(currentPage) { mutableStateOf("${currentPage + 1}") }
        Dialog(onDismissRequest = { showJumpPageDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosLiquidGlass(RoundedCornerShape(24.dp), glowAccent = true, fillAlpha = 0.94f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "الانتقال إلى صفحة" else "Go to Page",
                    color = ScannerGlassPalette.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { next ->
                        if (next.all { it.isDigit() }) pageInput = next
                    },
                    label = { Text("1 .. ${pageCount.coerceAtLeast(1)}") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScannerGlassPalette.VioletGlow,
                        unfocusedBorderColor = ScannerGlassPalette.SubtleStroke,
                        focusedTextColor = ScannerGlassPalette.TextPrimary,
                        unfocusedTextColor = ScannerGlassPalette.TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showJumpPageDialog = false }) {
                        Text(if (isArabic) "إلغاء" else "Cancel", color = ScannerGlassPalette.TextSecondary)
                    }
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.VioletGlow),
                        onClick = {
                            val parsed = pageInput.toIntOrNull()
                            if (parsed != null && pageCount > 0) {
                                jumpToPage((parsed - 1).coerceIn(0, pageCount - 1), animate = false)
                            }
                            showJumpPageDialog = false
                        }
                    ) {
                        Text(if (isArabic) "انتقال" else "Jump", color = ScannerGlassPalette.OnAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // OCR Copy Dialog
    if (showOcrSheet) {
        Dialog(onDismissRequest = { showOcrSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosLiquidGlass(RoundedCornerShape(24.dp), glowAccent = true, fillAlpha = 0.94f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "نص الصفحة ${currentPage + 1}" else "Page ${currentPage + 1} Text",
                    color = ScannerGlassPalette.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState())
                        .clip(RoundedCornerShape(14.dp))
                        .background(ScannerGlassPalette.FrostedCardDark)
                        .padding(12.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = extractedOcrText,
                            color = ScannerGlassPalette.TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showOcrSheet = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isArabic) "إغلاق" else "Close", color = ScannerGlassPalette.TextSecondary)
                    }
                    Button(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("PDF Page Text", extractedOcrText))
                            scannerViewModel.postMessage(if (isArabic) "تم نسخ النص ✓" else "Text copied ✓")
                            showOcrSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ScannerGlassPalette.OnAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "نسخ" else "Copy", color = ScannerGlassPalette.OnAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfRenderedPageCard(
    pdfKey: String,
    pageIndex: Int,
    rotationDeg: Int,
    colorFilter: ColorFilter?,
    isFocusMode: Boolean,
    renderPage: suspend () -> Bitmap?
) {
    val rendered by produceState<Bitmap?>(initialValue = null, key1 = pdfKey, key2 = pageIndex) {
        value = renderPage()
    }
    val img = remember(rendered) {
        rendered?.takeIf { !it.isRecycled }?.asImageBitmap()
    }

    val cardRadius = if (isFocusMode) 8.dp else 14.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (img != null) {
                    val isQuarterTurn = (rotationDeg % 180) != 0
                    val ratio = if (isQuarterTurn) {
                        img.height.toFloat() / img.width.coerceAtLeast(1).toFloat()
                    } else {
                        img.width.toFloat() / img.height.coerceAtLeast(1).toFloat()
                    }.coerceIn(0.45f, 2.2f)
                    Modifier.aspectRatio(ratio)
                } else {
                    Modifier.height(420.dp)
                }
            )
            .shadow(if (isFocusMode) 2.dp else 10.dp, RoundedCornerShape(cardRadius))
            .clip(RoundedCornerShape(cardRadius))
            .background(Color.White.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center
    ) {
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = "PDF Page ${pageIndex + 1}",
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = rotationDeg.toFloat()
                    }
            )
        } else {
            CircularProgressIndicator(
                color = ScannerGlassPalette.CyanGlow,
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp
            )
        }
    }
}

@Composable
private fun ReaderDockActionItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(tag)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = ScannerGlassPalette.TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SavePageActionRow(
    icon: ImageVector,
    title: String,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag(tag)
            .iosLiquidGlass(RoundedCornerShape(16.dp), glowAccent = false, fillAlpha = 0.72f)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            color = ScannerGlassPalette.TextPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
