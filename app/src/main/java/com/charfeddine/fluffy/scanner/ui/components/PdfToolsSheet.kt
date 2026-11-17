package com.charfeddine.fluffy.scanner.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charfeddine.fluffy.scanner.data.model.CompressionQuality
import com.charfeddine.fluffy.scanner.ui.theme.ScannerGlassPalette
import com.charfeddine.fluffy.scanner.ui.theme.formatScannerBytes
import com.charfeddine.fluffy.scanner.ui.theme.scannerGlassSurface
import com.charfeddine.fluffy.scanner.utils.PdfEngine

@OptIn(ExperimentalMaterial3Api::class)
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
    if (!visible) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeTool by remember { mutableIntStateOf(0) } // 0=Merge, 1=Split, 2=Compress, 3=Protect

    var mergeUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var splitUri by remember { mutableStateOf<Uri?>(null) }
    var compressUri by remember { mutableStateOf<Uri?>(null) }
    var compressOrigBytes by remember { mutableLongStateOf(0L) }
    var selectedQuality by remember { mutableStateOf(CompressionQuality.MEDIUM) }
    var protectUri by remember { mutableStateOf<Uri?>(null) }
    var protectPassword by remember { mutableStateOf("") }
    var outputTitle by remember { mutableStateOf("") }

    val multiPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) mergeUris = uris
    }

    val singlePdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            when (activeTool) {
                1 -> splitUri = uri
                2 -> {
                    compressUri = uri
                    compressOrigBytes = PdfEngine.getFileSizeBytes(context, uri)
                }
                3 -> protectUri = uri
            }
        }
    }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scannerGlassSurface(CircleShape, glowBorder = true),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = ScannerGlassPalette.CyanGlow,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Column {
                    Text(
                        text = if (isArabic) "أدوات PDF الاحترافية" else "PDF Utility Tools",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScannerGlassPalette.TextPrimary
                    )
                    Text(
                        text = if (isArabic) "دمج، تقسيم، ضغط، وتشفير محلي 100%" else "Merge, Split, Compress & Encrypt 100% Offline",
                        fontSize = 12.sp,
                        color = ScannerGlassPalette.TextSecondary
                    )
                }
            }

            // 4 Tool Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    0 to (if (isArabic) "دمج" else "Merge"),
                    1 to (if (isArabic) "تقسيم" else "Split"),
                    2 to (if (isArabic) "ضغط" else "Compress"),
                    3 to (if (isArabic) "تشفير" else "Protect")
                )
                tabs.forEach { (idx, title) ->
                    val selected = activeTool == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("pdf_tool_tab_$idx")
                            .scannerGlassSurface(
                                shape = RoundedCornerShape(14.dp),
                                borderWidth = if (selected) 1.4.dp else 0.9.dp,
                                glowBorder = selected,
                                fillAlpha = if (selected) 0.90f else 0.55f
                            )
                            .clickable { activeTool = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ScannerGlassPalette.CyanGlow,
                unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                focusedTextColor = ScannerGlassPalette.TextPrimary,
                unfocusedTextColor = ScannerGlassPalette.TextPrimary,
                focusedLabelColor = ScannerGlassPalette.CyanGlow,
                unfocusedLabelColor = ScannerGlassPalette.TextSecondary
            )

            when (activeTool) {
                0 -> {
                    // Merge PDFs
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .scannerGlassSurface(RoundedCornerShape(16.dp), glowBorder = mergeUris.size >= 2)
                            .clickable { multiPdfPicker.launch(arrayOf("application/pdf")) }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = ScannerGlassPalette.CyanGlow)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (mergeUris.isEmpty()) {
                                        if (isArabic) "اختر ملفين PDF أو أكثر للدمج" else "Select 2+ PDFs to Merge"
                                    } else {
                                        if (isArabic) "تم اختيار ${mergeUris.size} ملفات PDF" else "${mergeUris.size} PDFs selected"
                                    },
                                    color = ScannerGlassPalette.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isArabic) "اضغط لاختيار الملفات من الهاتف" else "Tap to pick local PDF files",
                                    color = ScannerGlassPalette.TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = outputTitle,
                        onValueChange = { outputTitle = it },
                        label = { Text(if (isArabic) "اسم الملف المدموج (اختياري)" else "Output PDF Name (optional)") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onMergePdfs(mergeUris, outputTitle)
                            onDismiss()
                        },
                        enabled = mergeUris.size >= 2,
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pdf_tool_merge_action")
                    ) {
                        Icon(Icons.Default.MergeType, contentDescription = null, tint = Color(0xFF04101B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "دمج وحفظ في Downloads/FluffyScans" else "Merge to Downloads/FluffyScans",
                            color = Color(0xFF04101B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                1 -> {
                    // Split PDF
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .scannerGlassSurface(RoundedCornerShape(16.dp), glowBorder = splitUri != null)
                            .clickable { singlePdfPicker.launch(arrayOf("application/pdf")) }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, tint = ScannerGlassPalette.VioletGlow)
                            Text(
                                text = if (splitUri == null) {
                                    if (isArabic) "اختر ملف PDF لتقسيم صفحاته" else "Select PDF File to Split"
                                } else {
                                    if (isArabic) "الملف جاهز للتقسيم ✓" else "PDF selected ✓"
                                },
                                color = ScannerGlassPalette.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = outputTitle,
                        onValueChange = { outputTitle = it },
                        label = { Text(if (isArabic) "بادئة أسماء الصفحات" else "Split Base Name") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            splitUri?.let {
                                onSplitPdf(it, outputTitle)
                                onDismiss()
                            }
                        },
                        enabled = splitUri != null,
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.VioletGlow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pdf_tool_split_action")
                    ) {
                        Text(
                            text = if (isArabic) "تقسيم كل صفحة إلى ملف مستقل" else "Split Into Separate PDFs",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                2 -> {
                    // Compress PDF with live estimated size preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .scannerGlassSurface(RoundedCornerShape(16.dp), glowBorder = compressUri != null)
                            .clickable { singlePdfPicker.launch(arrayOf("application/pdf")) }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Compress, contentDescription = null, tint = ScannerGlassPalette.EmeraldGlow)
                            Column {
                                Text(
                                    text = if (compressUri == null) {
                                        if (isArabic) "اختر ملف PDF لضغط حجمه" else "Select PDF to Compress"
                                    } else {
                                        val origStr = formatScannerBytes(compressOrigBytes, isArabic)
                                        if (isArabic) "الحجم الأصلي: $origStr" else "Original size: $origStr"
                                    },
                                    color = ScannerGlassPalette.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                if (compressUri != null && compressOrigBytes > 0L) {
                                    val est = PdfEngine.estimateCompressedSizeBytes(compressOrigBytes, selectedQuality)
                                    Text(
                                        text = if (isArabic) {
                                            "الحجم المتوقع بعد الضغط: ~${formatScannerBytes(est, true)}"
                                        } else {
                                            "Estimated output: ~${formatScannerBytes(est, false)}"
                                        },
                                        color = ScannerGlassPalette.EmeraldGlow,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Quality presets
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompressionQuality.entries.forEach { preset ->
                            val isSel = selectedQuality == preset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .scannerGlassSurface(
                                        RoundedCornerShape(14.dp),
                                        glowBorder = isSel,
                                        fillAlpha = if (isSel) 0.90f else 0.50f
                                    )
                                    .clickable { selectedQuality = preset }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isArabic) preset.labelAr else preset.labelEn,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) ScannerGlassPalette.EmeraldGlow else ScannerGlassPalette.TextSecondary,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            compressUri?.let {
                                onCompressPdf(it, selectedQuality, outputTitle)
                                onDismiss()
                            }
                        },
                        enabled = compressUri != null,
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.EmeraldGlow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pdf_tool_compress_action")
                    ) {
                        Text(
                            text = if (isArabic) "ضغط وتصدير PDF" else "Compress & Export PDF",
                            color = Color(0xFF042117),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                else -> {
                    // Password Protect PDF
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .scannerGlassSurface(RoundedCornerShape(16.dp), glowBorder = protectUri != null)
                            .clickable { singlePdfPicker.launch(arrayOf("application/pdf")) }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ScannerGlassPalette.AmberGlow)
                            Text(
                                text = if (protectUri == null) {
                                    if (isArabic) "اختر ملف PDF لحمايته بكلمة مرور" else "Select PDF to Password-Protect"
                                } else {
                                    if (isArabic) "الملف جاهز للتشفير ✓" else "PDF selected ✓"
                                },
                                color = ScannerGlassPalette.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = protectPassword,
                        onValueChange = { protectPassword = it },
                        label = { Text(if (isArabic) "كلمة مرور فتح الملف" else "Document Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            protectUri?.let {
                                onProtectPdf(it, protectPassword, outputTitle)
                                onDismiss()
                            }
                        },
                        enabled = protectUri != null && protectPassword.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.AmberGlow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pdf_tool_protect_action")
                    ) {
                        Text(
                            text = if (isArabic) "تشفير PDF وحفظه" else "Encrypt & Save Protected PDF",
                            color = Color(0xFF1F1200),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
