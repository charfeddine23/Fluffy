package com.charfeddine.fluffy.scanner.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.charfeddine.fluffy.scanner.data.model.SavedSignatureEntity
import com.charfeddine.fluffy.scanner.data.model.WatermarkConfig
import com.charfeddine.fluffy.scanner.ui.theme.ScannerGlassPalette
import com.charfeddine.fluffy.scanner.ui.theme.scannerGlassSurface

@Composable
fun SignatureStudioDialog(
    visible: Boolean,
    isArabic: Boolean,
    savedSignatures: List<SavedSignatureEntity>,
    loadSignatureBitmap: (SavedSignatureEntity) -> Bitmap?,
    onSelectSavedSignature: (SavedSignatureEntity) -> Unit,
    onDeleteSignature: (SavedSignatureEntity) -> Unit,
    onCreateSignature: (name: String, bitmap: Bitmap, colorArgb: Long) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val inkColors = listOf(
        0xFF0F172AL to (if (isArabic) "أسود" else "Black"),
        0xFF1D4ED8L to (if (isArabic) "أزرق" else "Blue"),
        0xFFDC2626L to (if (isArabic) "أحمر" else "Red")
    )

    var selectedColorArgb by remember { mutableLongStateOf(0xFF0F172AL) }
    var penWidthDp by remember { mutableFloatStateOf(4.5f) }
    var sigName by remember { mutableStateOf("") }
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize(600, 260)) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .scannerGlassSurface(RoundedCornerShape(24.dp), glowBorder = true, fillAlpha = 0.94f)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isArabic) "التوقيع الإلكتروني المشفّر" else "E-Signature Studio",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ScannerGlassPalette.TextPrimary
            )

            // Saved Encrypted Signatures Strip
            if (savedSignatures.isNotEmpty()) {
                Text(
                    text = if (isArabic) "التواقيع المحفوظة (اضغط للإضافة):" else "Saved Signatures (tap to stamp):",
                    fontSize = 12.sp,
                    color = ScannerGlassPalette.CyanGlow
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(savedSignatures, key = { it.id }) { sig ->
                        val bmp = remember(sig.id) { loadSignatureBitmap(sig) }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                                .clickable {
                                    onSelectSavedSignature(sig)
                                    onDismiss()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = sig.name,
                                    modifier = Modifier.size(width = 76.dp, height = 34.dp)
                                )
                            } else {
                                Text(sig.name, color = Color.Black, fontSize = 12.sp)
                            }
                            IconButton(
                                onClick = { onDeleteSignature(sig) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = ScannerGlassPalette.RoseGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Smooth Draw Signature Touch Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.5.dp, ScannerGlassPalette.CyanGlow.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { start -> currentStroke = listOf(start) },
                            onDrag = { change, _ ->
                                change.consume()
                                currentStroke = currentStroke + change.position
                            },
                            onDragEnd = {
                                if (currentStroke.size >= 2) strokes.add(currentStroke)
                                currentStroke = emptyList()
                            },
                            onDragCancel = { currentStroke = emptyList() }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val inkColor = Color(selectedColorArgb.toInt())
                    val all = strokes + listOf(currentStroke)
                    all.forEach { pts ->
                        if (pts.size >= 2) {
                            val p = Path().apply {
                                moveTo(pts.first().x, pts.first().y)
                                for (i in 1 until pts.size) {
                                    lineTo(pts[i].x, pts[i].y)
                                }
                            }
                            drawPath(
                                path = p,
                                color = inkColor,
                                style = Stroke(
                                    width = penWidthDp.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
                if (strokes.isEmpty() && currentStroke.isEmpty()) {
                    Text(
                        text = if (isArabic) "ارسم توقيعك هنا..." else "Draw your signature here...",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Color & Pen Thickness controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    inkColors.forEach { (argb, label) ->
                        val active = selectedColorArgb == argb
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(argb.toInt()))
                                .border(
                                    width = if (active) 2.5.dp else 1.dp,
                                    color = if (active) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.SubtleStroke,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorArgb = argb }
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.width(150.dp)
                ) {
                    Slider(
                        value = penWidthDp,
                        onValueChange = { penWidthDp = it },
                        valueRange = 2f..10f,
                        colors = SliderDefaults.colors(
                            thumbColor = ScannerGlassPalette.CyanGlow,
                            activeTrackColor = ScannerGlassPalette.CyanGlow
                        )
                    )
                }
            }

            OutlinedTextField(
                value = sigName,
                onValueChange = { sigName = it },
                label = { Text(if (isArabic) "اسم التوقيع" else "Signature Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ScannerGlassPalette.CyanGlow,
                    unfocusedBorderColor = ScannerGlassPalette.SubtleStroke,
                    focusedTextColor = ScannerGlassPalette.TextPrimary,
                    unfocusedTextColor = ScannerGlassPalette.TextPrimary,
                    focusedLabelColor = ScannerGlassPalette.CyanGlow,
                    unfocusedLabelColor = ScannerGlassPalette.TextSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { strokes.clear() }) {
                    Text(if (isArabic) "مسح" else "Clear", color = ScannerGlassPalette.RoseGlow)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isArabic) "إغلاق" else "Close", color = ScannerGlassPalette.TextSecondary)
                    }
                    Button(
                        enabled = strokes.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                        onClick = {
                            val bmp = Bitmap.createBitmap(
                                canvasSize.width.coerceAtLeast(300),
                                canvasSize.height.coerceAtLeast(150),
                                Bitmap.Config.ARGB_8888
                            )
                            val c = AndroidCanvas(bmp)
                            val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                style = AndroidPaint.Style.STROKE
                                strokeCap = AndroidPaint.Cap.ROUND
                                strokeJoin = AndroidPaint.Join.ROUND
                                color = selectedColorArgb.toInt()
                                strokeWidth = penWidthDp * 2.5f
                            }
                            strokes.forEach { pts ->
                                val path = AndroidPath()
                                pts.forEachIndexed { i, pt ->
                                    if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                                }
                                c.drawPath(path, p)
                            }
                            onCreateSignature(sigName.ifBlank { "My Signature" }, bmp, selectedColorArgb)
                            strokes.clear()
                            onDismiss()
                        }
                    ) {
                        Text(
                            if (isArabic) "حفظ وإدراج" else "Save & Stamp",
                            color = ScannerGlassPalette.OnAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WatermarkConfigDialog(
    visible: Boolean,
    initial: WatermarkConfig?,
    isArabic: Boolean,
    onSave: (WatermarkConfig?) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var text by remember(initial) { mutableStateOf(initial?.text ?: "") }
    var opacity by remember(initial) { mutableFloatStateOf(initial?.opacity ?: 0.25f) }
    var angle by remember(initial) { mutableFloatStateOf(initial?.angleDeg ?: -35f) }
    var colorArgb by remember(initial) { mutableLongStateOf(initial?.colorArgb ?: 0xFF64748BL) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .scannerGlassSurface(RoundedCornerShape(24.dp), glowBorder = true, fillAlpha = 0.94f)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isArabic) "علامة مائية مخصصة (Watermark)" else "Custom Watermark Overlay",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ScannerGlassPalette.TextPrimary
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(if (isArabic) "نص العلامة المائية (مثال: سري)" else "Watermark text (e.g. CONFIDENTIAL)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ScannerGlassPalette.CyanGlow,
                    unfocusedBorderColor = ScannerGlassPalette.SubtleStroke,
                    focusedTextColor = ScannerGlassPalette.TextPrimary,
                    unfocusedTextColor = ScannerGlassPalette.TextPrimary,
                    focusedLabelColor = ScannerGlassPalette.CyanGlow,
                    unfocusedLabelColor = ScannerGlassPalette.TextSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = if (isArabic) "الشفافية: ${(opacity * 100).toInt()}%" else "Opacity: ${(opacity * 100).toInt()}%",
                color = ScannerGlassPalette.TextSecondary,
                fontSize = 12.sp
            )
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                valueRange = 0.08f..0.75f,
                colors = SliderDefaults.colors(thumbColor = ScannerGlassPalette.CyanGlow, activeTrackColor = ScannerGlassPalette.CyanGlow)
            )

            Text(
                text = if (isArabic) "الزاوية: ${angle.toInt()}°" else "Angle: ${angle.toInt()}°",
                color = ScannerGlassPalette.TextSecondary,
                fontSize = 12.sp
            )
            Slider(
                value = angle,
                onValueChange = { angle = it },
                valueRange = -60f..60f,
                colors = SliderDefaults.colors(thumbColor = ScannerGlassPalette.VioletGlow, activeTrackColor = ScannerGlassPalette.VioletGlow)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(0xFF64748BL, 0xFF0EA5E9L, 0xFFEF4444L, 0xFF0F172AL).forEach { argb ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(argb.toInt()))
                            .border(
                                if (colorArgb == argb) 2.dp else 1.dp,
                                if (colorArgb == argb) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.SubtleStroke,
                                CircleShape
                            )
                            .clickable { colorArgb = argb }
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = {
                    onSave(null)
                    onDismiss()
                }) {
                    Text(if (isArabic) "إزالة العلامة" else "Remove", color = ScannerGlassPalette.RoseGlow)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isArabic) "إلغاء" else "Cancel", color = ScannerGlassPalette.TextSecondary)
                    }
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                        onClick = {
                            onSave(
                                if (text.isBlank()) null
                                else WatermarkConfig(text.trim(), opacity, angle, colorArgb)
                            )
                            onDismiss()
                        }
                    ) {
                        Text(if (isArabic) "تطبيق" else "Apply", color = ScannerGlassPalette.OnAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerBiometricPinDialog(
    visible: Boolean,
    isArabic: Boolean,
    hasPinConfigured: Boolean,
    onTriggerBiometric: () -> Unit,
    onSetNewPin: (String) -> Unit,
    onVerifyPin: (String) -> Boolean,
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .scannerGlassSurface(RoundedCornerShape(24.dp), glowBorder = true, fillAlpha = 0.95f)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scannerGlassSurface(CircleShape, glowBorder = true),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = ScannerGlassPalette.CyanGlow, modifier = Modifier.size(28.dp))
            }
            Text(
                text = if (!hasPinConfigured) {
                    if (isArabic) "تعيين رمز سري للمستندات المحمية" else "Create Scanner Vault PIN"
                } else {
                    if (isArabic) "مستند محمي بالبصمة و PIN" else "Biometric & PIN Vault"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ScannerGlassPalette.TextPrimary
            )

            if (hasPinConfigured) {
                OutlinedButton(
                    onClick = onTriggerBiometric,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = ScannerGlassPalette.CyanGlow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isArabic) "فتح بالبصمة / الوجه" else "Unlock with Biometrics",
                        color = ScannerGlassPalette.CyanGlow
                    )
                }
            }

            OutlinedTextField(
                value = pinInput,
                onValueChange = {
                    if (it.length <= 8 && it.all(Char::isDigit)) {
                        pinInput = it
                        errorMsg = null
                    }
                },
                label = {
                    Text(
                        if (!hasPinConfigured) {
                            if (isArabic) "أدخل رمز PIN (4 أرقام على الأقل)" else "Enter PIN (min 4 digits)"
                        } else {
                            if (isArabic) "أو أدخل رمز PIN" else "Or enter PIN fallback"
                        }
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ScannerGlassPalette.CyanGlow,
                    unfocusedBorderColor = ScannerGlassPalette.SubtleStroke,
                    focusedTextColor = ScannerGlassPalette.TextPrimary,
                    unfocusedTextColor = ScannerGlassPalette.TextPrimary,
                    focusedLabelColor = ScannerGlassPalette.CyanGlow,
                    unfocusedLabelColor = ScannerGlassPalette.TextSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMsg != null) {
                Text(errorMsg!!, color = ScannerGlassPalette.RoseGlow, fontSize = 12.sp)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = ScannerGlassPalette.TextSecondary)
                }
                Button(
                    enabled = pinInput.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                    onClick = {
                        if (!hasPinConfigured) {
                            onSetNewPin(pinInput)
                            onUnlocked()
                        } else if (onVerifyPin(pinInput)) {
                            onUnlocked()
                        } else {
                            errorMsg = if (isArabic) "رمز PIN غير صحيح" else "Incorrect PIN"
                        }
                    }
                ) {
                    Text(
                        if (!hasPinConfigured) {
                            if (isArabic) "حفظ وفتح" else "Save & Unlock"
                        } else {
                            if (isArabic) "تأكيد" else "Unlock"
                        },
                        color = ScannerGlassPalette.OnAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
