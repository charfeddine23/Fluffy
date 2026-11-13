package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlassTint
import com.example.data.model.VaultItemEntity
import com.example.data.storage.LocalDeviceStorageManager
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultScreen(
    vaultItems: List<VaultItemEntity>,
    isDark: Boolean,
    isArabic: Boolean,
    onSaveLiteralText: (title: String, literalContent: String) -> Unit,
    onSaveFileUri: (Uri) -> Unit,
    onExportItemToUri: (VaultItemEntity, Uri) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    var showTextComposer by remember { mutableStateOf(false) }
    var draftTitle by remember { mutableStateOf("") }
    var draftContent by remember { mutableStateOf("") }
    var pendingExportItem by remember { mutableStateOf<VaultItemEntity?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onSaveFileUri(it) }
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { destUri ->
        val item = pendingExportItem
        pendingExportItem = null
        if (destUri != null && item != null) {
            onExportItemToUri(item, destUri)
        }
    }

    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    val infiniteTransition = rememberInfiniteTransition(label = "vault_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vault_halo_scale"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vault_float"
    )

    FrostedBackdrop(isDark = isDark) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .testTag("vault_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Liquid Glass Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("vault_back_button")
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .liquidGlass(
                                isDark = isDark,
                                tint = GlassTint.FROST,
                                shape = CircleShape,
                                borderWidth = 1.2.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back",
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .liquidGlass(
                            isDark = isDark,
                            tint = GlassTint.AMETHYST,
                            shape = CircleShape,
                            borderWidth = 1.2.dp
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Vault",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            letterSpacing = 0.6.sp
                        )
                    )
                }

                // Immutable lock badge
                Box(
                    modifier = Modifier
                        .size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = if (isArabic) "حفظ حرفي محمي" else "Immutable Storage",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Action Buttons: Save Literal Text / Save Phone File Byte-for-Byte
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassCard(
                    isDark = isDark,
                    tint = GlassTint.AMETHYST,
                    shape = RoundedCornerShape(18.dp),
                    borderWidth = 1.2.dp,
                    onClick = { showTextComposer = !showTextComposer },
                    testTag = "vault_add_text_button",
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NoteAdd,
                            contentDescription = null,
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "حفظ نص حرفي" else "Save Text",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }

                LiquidGlassCard(
                    isDark = isDark,
                    tint = GlassTint.CYAN,
                    shape = RoundedCornerShape(18.dp),
                    borderWidth = 1.2.dp,
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    testTag = "vault_add_file_button",
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "حفظ ملف من الهاتف" else "Save File",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }

            // Literal Immutable Text Composer
            AnimatedVisibility(visible = showTextComposer) {
                LiquidGlassCard(
                    isDark = isDark,
                    tint = GlassTint.AMETHYST,
                    shape = RoundedCornerShape(22.dp),
                    borderWidth = 1.4.dp,
                    customFill = if (isDark) Color(0xFF0F172A).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.88f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 540.dp)
                        .padding(top = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isArabic)
                                "سيُحفظ النص حرفياً كما كتبته في ذاكرة الهاتف والـ Vault ولا يتغير بعدها لأي سبب"
                            else
                                "Saved verbatim in phone storage & Vault — strictly immutable once sealed",
                            fontSize = 11.5.sp,
                            color = metaColor
                        )

                        OutlinedTextField(
                            value = draftTitle,
                            onValueChange = { draftTitle = it },
                            placeholder = {
                                Text(
                                    text = if (isArabic) "العنوان (اختياري)" else "Title (optional)",
                                    color = metaColor.copy(alpha = 0.7f)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = Color(0xFFC084FC),
                                unfocusedBorderColor = metaColor.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_text_title_input")
                        )

                        OutlinedTextField(
                            value = draftContent,
                            onValueChange = { draftContent = it },
                            placeholder = {
                                Text(
                                    text = if (isArabic) "اكتب أي شيء لحفظه حرفياً..." else "Write anything to preserve literally...",
                                    color = metaColor.copy(alpha = 0.7f)
                                )
                            },
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = metaColor.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_text_content_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    if (draftTitle.isNotEmpty() || draftContent.isNotEmpty()) {
                                        onSaveLiteralText(draftTitle, draftContent)
                                        draftTitle = ""
                                        draftContent = ""
                                        showTextComposer = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF8B5CF6),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("vault_seal_save_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "حفظ وتثبيت حرفياً" else "Save Verbatim",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (vaultItems.isEmpty()) {
                // Empty Vault Emblem with Soon / قريباً badge + Immutable Storage Assurance
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    LiquidGlassCard(
                        isDark = isDark,
                        tint = GlassTint.AMETHYST,
                        shape = RoundedCornerShape(30.dp),
                        borderWidth = 1.6.dp,
                        customFill = if (isDark) {
                            Color(0xFF0F172A).copy(alpha = 0.78f)
                        } else {
                            Color(0xFFFFFFFF).copy(alpha = 0.82f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 380.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 26.dp, vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(104.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = pulseScale
                                            scaleY = pulseScale
                                        }
                                ) {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val outerRadius = size.minDimension / 2f

                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0x55C084FC),
                                                Color(0x3338BDF8),
                                                Color.Transparent
                                            ),
                                            center = center,
                                            radius = outerRadius
                                        ),
                                        radius = outerRadius,
                                        center = center
                                    )

                                    drawCircle(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                Color(0xFF38BDF8).copy(alpha = 0.75f),
                                                Color(0xFFC084FC).copy(alpha = 0.85f),
                                                Color(0xFFF472B6).copy(alpha = 0.75f),
                                                Color(0xFF38BDF8).copy(alpha = 0.75f)
                                            )
                                        ),
                                        radius = outerRadius * 0.80f,
                                        center = center,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(66.dp)
                                        .graphicsLayer {
                                            translationY = floatOffset.dp.toPx()
                                        }
                                        .liquidGlass(
                                            isDark = isDark,
                                            tint = GlassTint.AMETHYST,
                                            shape = CircleShape,
                                            borderWidth = 1.4.dp
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Vault",
                                        tint = if (isDark) Color(0xFFE9D5FF) else Color(0xFF7C3AED),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Vault",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .testTag("vault_status_badge")
                                    .liquidGlass(
                                        isDark = isDark,
                                        tint = GlassTint.CYAN,
                                        shape = CircleShape,
                                        borderWidth = 1.2.dp,
                                        customFill = if (isDark) Color(0x2E8B5CF6) else Color(0x228B5CF6)
                                    )
                                    .padding(horizontal = 20.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "قريباً المزيد • Soon" else "Soon • Immutable Vault",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFE9D5FF) else Color(0xFF6D28D9)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (isArabic)
                                    "أي نص أو ملف تحفظه هنا يُخزَّن حرفياً في ذاكرة تخزين الهاتف ويبقى كما هو تماماً دون أي تغيير."
                                else
                                    "Anything saved here is stored byte-for-byte in phone storage and never modified for any reason.",
                                fontSize = 12.sp,
                                color = metaColor,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 540.dp)
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(vaultItems, key = { it.id }) { item ->
                        VaultEntryCard(
                            item = item,
                            isDark = isDark,
                            isArabic = isArabic,
                            textColor = textColor,
                            metaColor = metaColor,
                            onExportClick = {
                                pendingExportItem = item
                                val suggested = item.fileName.ifBlank {
                                    (item.title.ifBlank { "vault_note_${item.createdAt}" }) + ".txt"
                                }
                                exportFileLauncher.launch(suggested)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultEntryCard(
    item: VaultItemEntity,
    isDark: Boolean,
    isArabic: Boolean,
    textColor: Color,
    metaColor: Color,
    onExportClick: () -> Unit
) {
    val isIntegrityValid = remember(item.id, item.sha256, item.localFilePath) {
        LocalDeviceStorageManager.verifyVaultItemIntegrity(item)
    }
    val dateFormatted = remember(item.createdAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.createdAt))
    }

    LiquidGlassCard(
        isDark = isDark,
        tint = if (item.fileName.isNotEmpty()) GlassTint.CYAN else GlassTint.AMETHYST,
        shape = RoundedCornerShape(20.dp),
        borderWidth = 1.2.dp,
        customFill = if (isDark) Color(0xFF0F172A).copy(alpha = 0.80f) else Color.White.copy(alpha = 0.84f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (item.fileName.isNotEmpty()) Icons.Default.AttachFile else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (item.fileName.isNotEmpty()) Color(0xFF38BDF8) else Color(0xFFC084FC),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.title.ifEmpty { item.fileName.ifEmpty { "Vault Note" } },
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.14f))
                        .clickable(onClick = onExportClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = if (isArabic) "تصدير إلى الهاتف" else "Export to phone",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (item.literalContent.isNotEmpty()) {
                Text(
                    text = item.literalContent,
                    fontSize = 13.5.sp,
                    color = textColor.copy(alpha = 0.92f),
                    lineHeight = 20.sp
                )
            }

            if (item.fileName.isNotEmpty()) {
                val sizeKb = (item.byteSize / 1024f)
                val sizeLabel = if (sizeKb >= 1024f) {
                    String.format(Locale.US, "%.2f MB", sizeKb / 1024f)
                } else {
                    String.format(Locale.US, "%.1f KB", sizeKb.coerceAtLeast(0.1f))
                }
                Text(
                    text = "${item.fileName} • $sizeLabel",
                    fontSize = 12.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormatted,
                    fontSize = 10.5.sp,
                    color = metaColor
                )

                Text(
                    text = if (isIntegrityValid) {
                        if (isArabic) "محفوظ حرفياً ولا يتغير ✓" else "Verbatim & Immutable ✓"
                    } else {
                        if (isArabic) "تم التحقق محلياً" else "Local Copy"
                    },
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}
