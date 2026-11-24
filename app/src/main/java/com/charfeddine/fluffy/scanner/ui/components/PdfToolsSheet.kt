package com.charfeddine.fluffy.scanner.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charfeddine.fluffy.scanner.data.model.CompressionQuality
import com.charfeddine.fluffy.scanner.ui.theme.ScannerDarkLiquidBackdrop
import com.charfeddine.fluffy.scanner.ui.theme.ScannerGlassPalette
import com.charfeddine.fluffy.scanner.ui.theme.formatScannerBytes
import com.charfeddine.fluffy.scanner.ui.theme.iosLiquidGlass
import com.charfeddine.fluffy.scanner.utils.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class PdfUriMetadata(
    val displayName: String,
    val sizeBytes: Long,
    val pageCount: Int,
    val previewBitmap: Bitmap?
)

private data class PdfToolDescriptor(
    val index: Int,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun PdfToolsSheet(
    visible: Boolean,
    isArabic: Boolean,
    onMergePdfs: (List<Uri>, String) -> Unit,
    onSplitPdf: (Uri, String) -> Unit,
    onCompressPdf: (Uri, CompressionQuality, String) -> Unit,
    onProtectPdf: (Uri, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) +
            slideInVertically(animationSpec = spring(dampingRatio = 0.86f, stiffness = 380f)) { it / 10 },
        exit = fadeOut(animationSpec = tween(180)) +
            slideOutVertically(animationSpec = tween(180)) { it / 12 }
    ) {
        BackHandler(enabled = visible) { onDismiss() }

        val context = LocalContext.current
        var activeTool by remember { mutableIntStateOf(0) } // 0=Merge, 1=Split, 2=Compress, 3=Protect

        var mergeUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
        var splitUri by remember { mutableStateOf<Uri?>(null) }
        var compressUri by remember { mutableStateOf<Uri?>(null) }
        var selectedQuality by remember { mutableStateOf(CompressionQuality.MEDIUM) }
        var protectUri by remember { mutableStateOf<Uri?>(null) }
        var protectPassword by remember { mutableStateOf("") }
        var showPassword by remember { mutableStateOf(false) }
        var outputTitle by remember { mutableStateOf("") }

        val metadataCache = remember { mutableStateMapOf<String, PdfUriMetadata>() }

        val allActiveUris = remember(mergeUris, splitUri, compressUri, protectUri) {
            buildList {
                addAll(mergeUris)
                splitUri?.let { add(it) }
                compressUri?.let { add(it) }
                protectUri?.let { add(it) }
            }.distinctBy { it.toString() }
        }

        LaunchedEffect(allActiveUris) {
            allActiveUris.forEach { uri ->
                val key = uri.toString()
                if (!metadataCache.containsKey(key)) {
                    val loaded = withContext(Dispatchers.IO) {
                        readPdfUriMetadata(context, uri)
                    }
                    metadataCache[key] = loaded
                }
            }
        }

        val multiPdfPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            if (uris.isNotEmpty()) {
                val combined = (mergeUris + uris).distinctBy { it.toString() }
                mergeUris = combined
            }
        }

        val singlePdfPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                when (activeTool) {
                    1 -> splitUri = uri
                    2 -> compressUri = uri
                    3 -> protectUri = uri
                }
            }
        }

        val cyanGlow = ScannerGlassPalette.CyanGlow
        val violetGlow = ScannerGlassPalette.VioletGlow
        val emeraldGlow = ScannerGlassPalette.EmeraldGlow
        val amberGlow = ScannerGlassPalette.AmberGlow

        val tools = remember(cyanGlow, violetGlow, emeraldGlow, amberGlow) {
            listOf(
                PdfToolDescriptor(
                    index = 0,
                    titleAr = "دمج الملفات",
                    titleEn = "Merge PDFs",
                    subtitleAr = "تجميع وترتيب عدة ملفات",
                    subtitleEn = "Combine & reorder files",
                    icon = Icons.Default.MergeType,
                    accent = cyanGlow
                ),
                PdfToolDescriptor(
                    index = 1,
                    titleAr = "تقسيم الصفحات",
                    titleEn = "Split Pages",
                    subtitleAr = "استخراج كل صفحة بملف",
                    subtitleEn = "Extract individual pages",
                    icon = Icons.AutoMirrored.Filled.CallSplit,
                    accent = violetGlow
                ),
                PdfToolDescriptor(
                    index = 2,
                    titleAr = "ضغط الحجم",
                    titleEn = "Compress",
                    subtitleAr = "تقليص الحجم مع معاينة",
                    subtitleEn = "Shrink size cleanly",
                    icon = Icons.Default.Compress,
                    accent = emeraldGlow
                ),
                PdfToolDescriptor(
                    index = 3,
                    titleAr = "حماية وتشفير",
                    titleEn = "Protect PDF",
                    subtitleAr = "قفل المستند بكلمة مرور",
                    subtitleEn = "Lock with password",
                    icon = Icons.Default.Lock,
                    accent = amberGlow
                )
            )
        }

        val activeDescriptor = tools[activeTool.coerceIn(0, 3)]
        val hasCurrentSelection = when (activeTool) {
            0 -> mergeUris.isNotEmpty() || outputTitle.isNotBlank()
            1 -> splitUri != null || outputTitle.isNotBlank()
            2 -> compressUri != null || outputTitle.isNotBlank()
            else -> protectUri != null || protectPassword.isNotBlank() || outputTitle.isNotBlank()
        }

        fun clearActiveWorkspace() {
            outputTitle = ""
            when (activeTool) {
                0 -> mergeUris = emptyList()
                1 -> splitUri = null
                2 -> compressUri = null
                3 -> {
                    protectUri = null
                    protectPassword = ""
                }
            }
        }

        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = activeDescriptor.accent,
            unfocusedBorderColor = ScannerGlassPalette.SubtleStroke,
            focusedContainerColor = ScannerGlassPalette.InputContainer,
            unfocusedContainerColor = ScannerGlassPalette.InputContainer.copy(alpha = 0.78f),
            focusedTextColor = ScannerGlassPalette.TextPrimary,
            unfocusedTextColor = ScannerGlassPalette.TextPrimary,
            focusedLabelColor = activeDescriptor.accent,
            unfocusedLabelColor = ScannerGlassPalette.TextSecondary,
            cursorColor = activeDescriptor.accent
        )

        ScannerDarkLiquidBackdrop(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            // Dynamic ambient liquid glow tuned to the active PDF tool
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeDescriptor.accent.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.78f, size.height * 0.12f),
                        radius = size.width * 0.72f
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
            ) {
                // 1. iOS 27 Floating Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("pdf_tools_close_btn")
                                .iosLiquidGlass(shape = CircleShape, glowAccent = false, fillAlpha = 0.76f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isArabic) "رجوع" else "Back",
                                tint = ScannerGlassPalette.TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (isArabic) "استوديو أدوات PDF" else "PDF Utility Studio",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ScannerGlassPalette.TextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(activeDescriptor.accent)
                                )
                                Text(
                                    text = if (isArabic) "معالجة محلية فائقة السرعة" else "100% On-Device PDF Engine",
                                    fontSize = 11.5.sp,
                                    color = ScannerGlassPalette.TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = hasCurrentSelection) {
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .iosLiquidGlass(shape = CircleShape, glowAccent = false, fillAlpha = 0.72f)
                                .clickable { clearActiveWorkspace() }
                                .padding(horizontal = 13.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "مسح" else "Reset",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ScannerGlassPalette.TextSecondary
                            )
                        }
                    }
                }

                // 2. Scrollable Tool Deck + Dedicated Glass Workspace
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 2x2 iOS 27 Liquid Glass Tool Bento Grid
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PdfToolBentoCard(
                                tool = tools[0],
                                selected = activeTool == 0,
                                isArabic = isArabic,
                                modifier = Modifier.weight(1f),
                                onClick = { activeTool = 0 }
                            )
                            PdfToolBentoCard(
                                tool = tools[1],
                                selected = activeTool == 1,
                                isArabic = isArabic,
                                modifier = Modifier.weight(1f),
                                onClick = { activeTool = 1 }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PdfToolBentoCard(
                                tool = tools[2],
                                selected = activeTool == 2,
                                isArabic = isArabic,
                                modifier = Modifier.weight(1f),
                                onClick = { activeTool = 2 }
                            )
                            PdfToolBentoCard(
                                tool = tools[3],
                                selected = activeTool == 3,
                                isArabic = isArabic,
                                modifier = Modifier.weight(1f),
                                onClick = { activeTool = 3 }
                            )
                        }
                    }

                    // Active Tool Workspace
                    AnimatedContent(
                        targetState = activeTool,
                        transitionSpec = {
                            (fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 18 }) togetherWith
                                (fadeOut(tween(140)) + slideOutVertically(tween(140)) { -it / 24 })
                        },
                        label = "pdf_tool_workspace"
                    ) { toolIndex ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            when (toolIndex) {
                                0 -> MergeWorkspaceSection(
                                    isArabic = isArabic,
                                    mergeUris = mergeUris,
                                    metadataCache = metadataCache,
                                    outputTitle = outputTitle,
                                    textFieldColors = textFieldColors,
                                    onPickFiles = { multiPdfPicker.launch(arrayOf("application/pdf")) },
                                    onMoveUp = { idx ->
                                        if (idx > 0) {
                                            val copy = mergeUris.toMutableList()
                                            val item = copy.removeAt(idx)
                                            copy.add(idx - 1, item)
                                            mergeUris = copy
                                        }
                                    },
                                    onMoveDown = { idx ->
                                        if (idx < mergeUris.lastIndex) {
                                            val copy = mergeUris.toMutableList()
                                            val item = copy.removeAt(idx)
                                            copy.add(idx + 1, item)
                                            mergeUris = copy
                                        }
                                    },
                                    onRemove = { idx ->
                                        mergeUris = mergeUris.toMutableList().apply { removeAt(idx) }
                                    },
                                    onOutputTitleChange = { outputTitle = it }
                                )

                                1 -> SplitWorkspaceSection(
                                    isArabic = isArabic,
                                    splitUri = splitUri,
                                    meta = splitUri?.let { metadataCache[it.toString()] },
                                    outputTitle = outputTitle,
                                    textFieldColors = textFieldColors,
                                    onPickFile = { singlePdfPicker.launch(arrayOf("application/pdf")) },
                                    onOutputTitleChange = { outputTitle = it }
                                )

                                2 -> CompressWorkspaceSection(
                                    isArabic = isArabic,
                                    compressUri = compressUri,
                                    meta = compressUri?.let { metadataCache[it.toString()] },
                                    selectedQuality = selectedQuality,
                                    outputTitle = outputTitle,
                                    textFieldColors = textFieldColors,
                                    onPickFile = { singlePdfPicker.launch(arrayOf("application/pdf")) },
                                    onSelectQuality = { selectedQuality = it },
                                    onOutputTitleChange = { outputTitle = it }
                                )

                                else -> ProtectWorkspaceSection(
                                    isArabic = isArabic,
                                    protectUri = protectUri,
                                    meta = protectUri?.let { metadataCache[it.toString()] },
                                    password = protectPassword,
                                    showPassword = showPassword,
                                    outputTitle = outputTitle,
                                    textFieldColors = textFieldColors,
                                    onPickFile = { singlePdfPicker.launch(arrayOf("application/pdf")) },
                                    onPasswordChange = { protectPassword = it },
                                    onTogglePasswordVisibility = { showPassword = !showPassword },
                                    onOutputTitleChange = { outputTitle = it }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 3. Docked iOS 27 Liquid Glass Action Bar
                val isActionEnabled = when (activeTool) {
                    0 -> mergeUris.size >= 2
                    1 -> splitUri != null
                    2 -> compressUri != null
                    else -> protectUri != null && protectPassword.isNotBlank()
                }
                val actionTestTag = when (activeTool) {
                    0 -> "pdf_tool_merge_action"
                    1 -> "pdf_tool_split_action"
                    2 -> "pdf_tool_compress_action"
                    else -> "pdf_tool_protect_action"
                }
                val totalMergePages = mergeUris.sumOf { metadataCache[it.toString()]?.pageCount ?: 0 }
                val actionLabel = when (activeTool) {
                    0 -> if (isArabic) {
                        if (mergeUris.size >= 2) {
                            if (totalMergePages > 0) "دمج ${mergeUris.size} ملفات ($totalMergePages صفحة)"
                            else "دمج ${mergeUris.size} ملفات الآن"
                        } else {
                            "اختر ملفين على الأقل للدمج"
                        }
                    } else {
                        if (mergeUris.size >= 2) {
                            if (totalMergePages > 0) "Merge ${mergeUris.size} PDFs ($totalMergePages pages)"
                            else "Merge ${mergeUris.size} PDFs Now"
                        } else {
                            "Select 2+ PDFs to Merge"
                        }
                    }
                    1 -> if (isArabic) "تقسيم إلى صفحات منفصلة" else "Split Into Separate PDFs"
                    2 -> if (isArabic) "ضغط وتصدير الملف" else "Compress & Export PDF"
                    else -> if (isArabic) "تشفير وحفظ الملف المحمي" else "Encrypt & Save Protected PDF"
                }

                val buttonTextColor = ScannerGlassPalette.contrastOn(activeDescriptor.accent)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .iosLiquidGlass(
                            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                            glowAccent = isActionEnabled,
                            fillAlpha = 0.88f
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = activeDescriptor.accent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isArabic) "الحفظ التلقائي: Downloads/FluffyScans" else "Saves to Downloads/FluffyScans",
                                fontSize = 11.5.sp,
                                color = ScannerGlassPalette.TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Button(
                        onClick = {
                            when (activeTool) {
                                0 -> {
                                    onMergePdfs(mergeUris, outputTitle)
                                    onDismiss()
                                }
                                1 -> splitUri?.let {
                                    onSplitPdf(it, outputTitle)
                                    onDismiss()
                                }
                                2 -> compressUri?.let {
                                    onCompressPdf(it, selectedQuality, outputTitle)
                                    onDismiss()
                                }
                                else -> protectUri?.let {
                                    onProtectPdf(it, protectPassword, outputTitle)
                                    onDismiss()
                                }
                            }
                        },
                        enabled = isActionEnabled,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeDescriptor.accent,
                            disabledContainerColor = ScannerGlassPalette.SubtleChipBg,
                            contentColor = buttonTextColor,
                            disabledContentColor = ScannerGlassPalette.TextSecondary.copy(alpha = 0.65f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag(actionTestTag)
                    ) {
                        Icon(
                            imageVector = activeDescriptor.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = actionLabel,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfToolBentoCard(
    tool: PdfToolDescriptor,
    selected: Boolean,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .height(82.dp)
            .minimumInteractiveComponentSize()
            .testTag("pdf_tool_tab_${tool.index}")
            .iosLiquidGlass(
                shape = cardShape,
                glowAccent = selected,
                fillAlpha = if (selected) 0.86f else 0.56f
            )
            .then(
                if (selected) {
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                tool.accent.copy(alpha = 0.22f),
                                tool.accent.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        ),
                        shape = cardShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                tool.accent.copy(alpha = if (selected) 0.34f else 0.16f),
                                tool.accent.copy(alpha = if (selected) 0.14f else 0.06f)
                            )
                        )
                    )
                    .border(
                        width = 0.8.dp,
                        color = tool.accent.copy(alpha = if (selected) 0.65f else 0.22f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = if (isArabic) tool.titleAr else tool.titleEn,
                    tint = if (selected) tool.accent else tool.accent.copy(alpha = 0.82f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (isArabic) tool.titleAr else tool.titleEn,
                    fontSize = 13.5.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (selected) ScannerGlassPalette.TextPrimary else ScannerGlassPalette.TextPrimary.copy(alpha = 0.86f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isArabic) tool.subtitleAr else tool.subtitleEn,
                    fontSize = 10.5.sp,
                    color = if (selected) tool.accent.copy(alpha = 0.92f) else ScannerGlassPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GlassPdfDropzone(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .iosLiquidGlass(shape = shape, glowAccent = true, fillAlpha = 0.62f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                color = accent.copy(alpha = 0.42f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                style = Stroke(
                    width = 1.4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)
                )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.30f),
                                accent.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(1.dp, accent.copy(alpha = 0.50f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ScannerGlassPalette.TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = ScannerGlassPalette.TextSecondary
            )
        }
    }
}

@Composable
private fun SelectedPdfPreviewCard(
    meta: PdfUriMetadata?,
    accent: Color,
    isArabic: Boolean,
    onChangeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .iosLiquidGlass(shape = RoundedCornerShape(22.dp), glowAccent = true, fillAlpha = 0.74f)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PdfThumbnailFrame(bitmap = meta?.previewBitmap, accent = accent, widthDp = 54, heightDp = 70)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = meta?.displayName ?: (if (isArabic) "ملف PDF محدد" else "Selected PDF"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ScannerGlassPalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (meta != null && meta.pageCount > 0) {
                    MetaGlassTag(
                        text = if (isArabic) "${meta.pageCount} صفحة" else "${meta.pageCount} pages",
                        accent = accent
                    )
                }
                if (meta != null && meta.sizeBytes > 0L) {
                    MetaGlassTag(
                        text = formatScannerBytes(meta.sizeBytes, isArabic),
                        accent = ScannerGlassPalette.TextSecondary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .iosLiquidGlass(shape = CircleShape, glowAccent = false, fillAlpha = 0.80f)
                .clickable { onChangeClick() }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isArabic) "تغيير" else "Change",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
private fun PdfThumbnailFrame(
    bitmap: Bitmap?,
    accent: Color,
    widthDp: Int,
    heightDp: Int
) {
    val img = remember(bitmap) { bitmap?.asImageBitmap() }
    Box(
        modifier = Modifier
            .size(width = widthDp.dp, height = heightDp.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ScannerGlassPalette.ThumbnailFrame)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun MetaGlassTag(text: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f))
            .border(0.7.dp, accent.copy(alpha = 0.34f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
    }
}

@Composable
private fun MergeWorkspaceSection(
    isArabic: Boolean,
    mergeUris: List<Uri>,
    metadataCache: Map<String, PdfUriMetadata>,
    outputTitle: String,
    textFieldColors: androidx.compose.material3.TextFieldColors,
    onPickFiles: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onOutputTitleChange: (String) -> Unit
) {
    val accent = ScannerGlassPalette.CyanGlow
    if (mergeUris.isEmpty()) {
        GlassPdfDropzone(
            title = if (isArabic) "اختر ملفين PDF أو أكثر للدمج" else "Select 2+ PDFs to Merge",
            subtitle = if (isArabic) "اضغط لاختيار الملفات وترتيبها قبل الدمج" else "Tap to pick local PDFs & arrange their order",
            icon = Icons.Default.MergeType,
            accent = accent,
            onClick = onPickFiles
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .iosLiquidGlass(shape = RoundedCornerShape(22.dp), glowAccent = mergeUris.size >= 2, fillAlpha = 0.72f)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "ترتيب الدمج (${mergeUris.size})" else "Merge Order (${mergeUris.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScannerGlassPalette.TextPrimary
                )
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f))
                        .border(0.8.dp, accent.copy(alpha = 0.45f), CircleShape)
                        .clickable { onPickFiles() }
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                    Text(
                        text = if (isArabic) "إضافة ملفات" else "Add PDFs",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }

            mergeUris.forEachIndexed { index, uri ->
                val meta = metadataCache[uri.toString()]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ScannerGlassPalette.FrostedCardDark)
                        .border(0.8.dp, ScannerGlassPalette.SubtleStroke, RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent
                        )
                    }

                    PdfThumbnailFrame(bitmap = meta?.previewBitmap, accent = accent, widthDp = 38, heightDp = 48)

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = meta?.displayName ?: "PDF #${index + 1}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ScannerGlassPalette.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val infoParts = buildList {
                            if (meta != null && meta.pageCount > 0) {
                                add(if (isArabic) "${meta.pageCount} ص" else "${meta.pageCount}p")
                            }
                            if (meta != null && meta.sizeBytes > 0L) {
                                add(formatScannerBytes(meta.sizeBytes, isArabic))
                            }
                        }.joinToString(" • ")
                        if (infoParts.isNotEmpty()) {
                            Text(
                                text = infoParts,
                                fontSize = 11.sp,
                                color = ScannerGlassPalette.TextSecondary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onMoveUp(index) },
                            enabled = index > 0,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = if (isArabic) "تحريك للأعلى" else "Move Up",
                                tint = if (index > 0) ScannerGlassPalette.TextPrimary else ScannerGlassPalette.TextSecondary.copy(alpha = 0.30f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { onMoveDown(index) },
                            enabled = index < mergeUris.lastIndex,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isArabic) "تحريك للأسفل" else "Move Down",
                                tint = if (index < mergeUris.lastIndex) ScannerGlassPalette.TextPrimary else ScannerGlassPalette.TextSecondary.copy(alpha = 0.30f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { onRemove(index) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isArabic) "إزالة" else "Remove",
                                tint = ScannerGlassPalette.RoseGlow,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (mergeUris.size == 1) {
                Text(
                    text = if (isArabic) "أضف ملف PDF واحداً إضافياً على الأقل لتفعيل الدمج" else "Select at least 1 more PDF file to merge",
                    fontSize = 11.5.sp,
                    color = ScannerGlassPalette.AmberGlow,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    OutlinedTextField(
        value = outputTitle,
        onValueChange = onOutputTitleChange,
        label = { Text(if (isArabic) "اسم الملف المدموج (اختياري)" else "Output PDF Name (optional)") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = textFieldColors,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SplitWorkspaceSection(
    isArabic: Boolean,
    splitUri: Uri?,
    meta: PdfUriMetadata?,
    outputTitle: String,
    textFieldColors: androidx.compose.material3.TextFieldColors,
    onPickFile: () -> Unit,
    onOutputTitleChange: (String) -> Unit
) {
    val accent = ScannerGlassPalette.VioletGlow
    if (splitUri == null) {
        GlassPdfDropzone(
            title = if (isArabic) "اختر ملف PDF لتقسيم صفحاته" else "Select PDF File to Split",
            subtitle = if (isArabic) "تحويل كل صفحة إلى ملف PDF مستقل بدقة كاملة" else "Extract every page into a crisp standalone PDF",
            icon = Icons.AutoMirrored.Filled.CallSplit,
            accent = accent,
            onClick = onPickFile
        )
    } else {
        SelectedPdfPreviewCard(
            meta = meta,
            accent = accent,
            isArabic = isArabic,
            onChangeClick = onPickFile
        )

        if (meta != null && meta.pageCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosLiquidGlass(shape = RoundedCornerShape(18.dp), glowAccent = false, fillAlpha = 0.65f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Text(
                    text = if (isArabic) {
                        "سيتم استخراج ${meta.pageCount} ملف PDF مستقل لكل صفحة"
                    } else {
                        "${meta.pageCount} individual PDF files will be extracted"
                    },
                    fontSize = 12.5.sp,
                    color = ScannerGlassPalette.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    OutlinedTextField(
        value = outputTitle,
        onValueChange = onOutputTitleChange,
        label = { Text(if (isArabic) "بادئة أسماء الصفحات (اختياري)" else "Split Prefix Name (optional)") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = textFieldColors,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CompressWorkspaceSection(
    isArabic: Boolean,
    compressUri: Uri?,
    meta: PdfUriMetadata?,
    selectedQuality: CompressionQuality,
    outputTitle: String,
    textFieldColors: androidx.compose.material3.TextFieldColors,
    onPickFile: () -> Unit,
    onSelectQuality: (CompressionQuality) -> Unit,
    onOutputTitleChange: (String) -> Unit
) {
    val accent = ScannerGlassPalette.EmeraldGlow
    val origBytes = meta?.sizeBytes ?: 0L
    val estimatedBytes = if (origBytes > 0L) PdfEngine.estimateCompressedSizeBytes(origBytes, selectedQuality) else 0L

    if (compressUri == null) {
        GlassPdfDropzone(
            title = if (isArabic) "اختر ملف PDF لضغط حجمه" else "Select PDF to Compress",
            subtitle = if (isArabic) "تقليل حجم الملف مع معاينة فورية للحجم المتوقع" else "Reduce file size with instant output estimation",
            icon = Icons.Default.Compress,
            accent = accent,
            onClick = onPickFile
        )
    } else {
        SelectedPdfPreviewCard(
            meta = meta,
            accent = accent,
            isArabic = isArabic,
            onChangeClick = onPickFile
        )

        if (origBytes > 0L && estimatedBytes > 0L) {
            val savedRatio = ((origBytes - estimatedBytes).toFloat() / origBytes.toFloat()).coerceIn(0.05f, 0.90f)
            val savedPercent = (savedRatio * 100).toInt()
            val animatedBar by animateFloatAsState(
                targetValue = (estimatedBytes.toFloat() / origBytes.toFloat()).coerceIn(0.10f, 0.95f),
                animationSpec = tween(300),
                label = "compress_ratio"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosLiquidGlass(shape = RoundedCornerShape(20.dp), glowAccent = true, fillAlpha = 0.76f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isArabic) "الأصلي" else "Original",
                            fontSize = 11.sp,
                            color = ScannerGlassPalette.TextSecondary
                        )
                        Text(
                            text = formatScannerBytes(origBytes, isArabic),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ScannerGlassPalette.TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.18f))
                            .border(0.8.dp, accent.copy(alpha = 0.50f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
                        Text(
                            text = "-$savedPercent%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isArabic) "المتوقع" else "Estimated",
                            fontSize = 11.sp,
                            color = accent
                        )
                        Text(
                            text = "~${formatScannerBytes(estimatedBytes, isArabic)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(ScannerGlassPalette.SubtleChipBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedBar)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(ScannerGlassPalette.CyanGlow, accent)
                                )
                            )
                    )
                }
            }
        }
    }

    // Quality Preset Glass Selector Cards
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (isArabic) "مستوى الضغط والجودة" else "Compression Level",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = ScannerGlassPalette.TextPrimary
        )

        CompressionQuality.entries.forEach { preset ->
            val isSel = selectedQuality == preset
            val presetHint = when (preset) {
                CompressionQuality.LOW -> if (isArabic) "أصغر حجم مثالي للبريد والواتساب" else "Smallest size for fast sharing"
                CompressionQuality.MEDIUM -> if (isArabic) "توازن ممتاز بين الحجم والوضوح" else "Best balance of clarity & size"
                CompressionQuality.HIGH -> if (isArabic) "أعلى وضوح للنصوص والطباعة" else "Crisp clarity for print & archive"
            }
            val presetEst = if (origBytes > 0L) formatScannerBytes(PdfEngine.estimateCompressedSizeBytes(origBytes, preset), isArabic) else null

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .iosLiquidGlass(
                        shape = RoundedCornerShape(18.dp),
                        glowAccent = isSel,
                        fillAlpha = if (isSel) 0.86f else 0.56f
                    )
                    .clickable { onSelectQuality(preset) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isArabic) preset.labelAr else preset.labelEn,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isSel) accent else ScannerGlassPalette.TextPrimary
                    )
                    Text(
                        text = presetHint,
                        fontSize = 11.5.sp,
                        color = ScannerGlassPalette.TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (presetEst != null) {
                        Text(
                            text = "~$presetEst",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) accent else ScannerGlassPalette.TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, if (isSel) accent else ScannerGlassPalette.SubtleStroke, CircleShape)
                            .background(if (isSel) accent else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSel) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(ScannerGlassPalette.contrastOn(accent))
                            )
                        }
                    }
                }
            }
        }
    }

    OutlinedTextField(
        value = outputTitle,
        onValueChange = onOutputTitleChange,
        label = { Text(if (isArabic) "اسم الملف المضغوط (اختياري)" else "Compressed File Name (optional)") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = textFieldColors,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ProtectWorkspaceSection(
    isArabic: Boolean,
    protectUri: Uri?,
    meta: PdfUriMetadata?,
    password: String,
    showPassword: Boolean,
    outputTitle: String,
    textFieldColors: androidx.compose.material3.TextFieldColors,
    onPickFile: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onOutputTitleChange: (String) -> Unit
) {
    val accent = ScannerGlassPalette.AmberGlow
    if (protectUri == null) {
        GlassPdfDropzone(
            title = if (isArabic) "اختر ملف PDF لحمايته بكلمة مرور" else "Select PDF to Password-Protect",
            subtitle = if (isArabic) "تشفير محلي آمن لحماية مستنداتك الحساسة" else "Local encryption to keep sensitive documents private",
            icon = Icons.Default.Lock,
            accent = accent,
            onClick = onPickFile
        )
    } else {
        SelectedPdfPreviewCard(
            meta = meta,
            accent = accent,
            isArabic = isArabic,
            onChangeClick = onPickFile
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .iosLiquidGlass(shape = RoundedCornerShape(22.dp), glowAccent = password.length >= 6, fillAlpha = 0.72f)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(
                text = if (isArabic) "إعداد كلمة مرور التشفير" else "Encryption Password",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = ScannerGlassPalette.TextPrimary
            )
        }

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(if (isArabic) "كلمة مرور فتح الملف" else "Document Password") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isArabic) "إظهار أو إخفاء" else "Toggle visibility",
                        tint = ScannerGlassPalette.TextSecondary
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        if (password.isNotEmpty()) {
            val strength = when {
                password.length >= 8 && (password.any { it.isDigit() } || password.any { !it.isLetterOrDigit() }) -> 3
                password.length >= 5 -> 2
                else -> 1
            }
            val strengthColor = when (strength) {
                3 -> ScannerGlassPalette.EmeraldGlow
                2 -> accent
                else -> ScannerGlassPalette.RoseGlow
            }
            val strengthLabel = when (strength) {
                3 -> if (isArabic) "قوية" else "Strong"
                2 -> if (isArabic) "متوسطة" else "Medium"
                else -> if (isArabic) "ضعيفة" else "Weak"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    repeat(3) { step ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (step < strength) strengthColor else ScannerGlassPalette.SubtleChipBg
                                )
                        )
                    }
                }
                Text(
                    text = strengthLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = strengthColor
                )
            }
        }
    }

    OutlinedTextField(
        value = outputTitle,
        onValueChange = onOutputTitleChange,
        label = { Text(if (isArabic) "اسم الملف المحمي (اختياري)" else "Protected File Name (optional)") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = textFieldColors,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun readPdfUriMetadata(context: Context, uri: Uri): PdfUriMetadata {
    val name = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"

    val size = PdfEngine.getFileSizeBytes(context, uri)
    val pages = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            PdfRenderer(pfd).use { it.pageCount }
        } ?: 0
    }.getOrDefault(0)
    val preview = PdfEngine.renderPdfFirstPagePreview(context, uri)

    return PdfUriMetadata(
        displayName = name,
        sizeBytes = size,
        pageCount = pages,
        previewBitmap = preview
    )
}
