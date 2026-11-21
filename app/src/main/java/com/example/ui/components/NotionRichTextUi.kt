package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GlassTint
import com.example.data.model.MarkdownTableData
import com.example.data.model.RichBlock
import com.example.data.model.RichTextCodec
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass


/**
 * Interactive Notion-style Table Builder & Cell Editor Dialog.
 * Supports creating any rows × cols table OR editing an existing [MarkdownTableData] in-place.
 */
@Composable
fun TableEditorDialog(
    initialTable: MarkdownTableData?,
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    textColor: Color,
    metaColor: Color,
    onDismiss: () -> Unit,
    onConfirmTable: (List<List<String>>) -> Unit,
    onDeleteTable: (() -> Unit)? = null
) {
    val gridRows = remember(initialTable) {
        mutableStateListOf<MutableList<String>>().apply {
            if (initialTable != null && initialTable.rows.isNotEmpty()) {
                val cols = initialTable.colCount.coerceAtLeast(1)
                initialTable.rows.forEach { row ->
                    add(MutableList(cols) { c -> row.getOrElse(c) { "" } })
                }
            } else {
                add(
                    mutableListOf(
                        if (isArabic) "العنوان 1" else "Column 1",
                        if (isArabic) "العنوان 2" else "Column 2",
                        if (isArabic) "العنوان 3" else "Column 3"
                    )
                )
                add(mutableListOf("", "", ""))
                add(mutableListOf("", "", ""))
            }
        }
    }

    val colCount = gridRows.firstOrNull()?.size ?: 2
    val rowCount = gridRows.size

    fun resizeGrid(targetRows: Int, targetCols: Int) {
        val safeRows = targetRows.coerceIn(2, 12)
        val safeCols = targetCols.coerceIn(1, 7)
        while (gridRows.size < safeRows) {
            gridRows.add(MutableList(safeCols) { "" })
        }
        while (gridRows.size > safeRows) {
            gridRows.removeAt(gridRows.lastIndex)
        }
        for (r in gridRows.indices) {
            val row = gridRows[r].toMutableList()
            while (row.size < safeCols) {
                row.add(if (r == 0) (if (isArabic) "عمود ${row.size + 1}" else "Col ${row.size + 1}") else "")
            }
            while (row.size > safeCols) {
                row.removeAt(row.lastIndex)
            }
            gridRows[r] = row
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LiquidGlassCard(
            isDark = isDark,
            tint = GlassTint.FROST,
            shape = RoundedCornerShape(26.dp),
            borderWidth = 1.4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .widthIn(max = 520.dp)
                .testTag("table_editor_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (initialTable != null) {
                                if (isArabic) "تعديل الجدول ($rowCount × $colCount)" else "Edit Table ($rowCount × $colCount)"
                            } else {
                                if (isArabic) "إنشاء جدول ($rowCount × $colCount)" else "Insert Table ($rowCount × $colCount)"
                            },
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isArabic) "إغلاق" else "Close",
                            tint = metaColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Quick Size Controls (+Row, -Row, +Col, -Col)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DimensionStepperPill(
                        label = if (isArabic) "الصفوف: $rowCount" else "Rows: $rowCount",
                        onMinus = { resizeGrid(rowCount - 1, colCount) },
                        onPlus = { resizeGrid(rowCount + 1, colCount) },
                        canMinus = rowCount > 2,
                        canPlus = rowCount < 12,
                        accentColor = accentColor,
                        textColor = textColor,
                        isDark = isDark
                    )
                    DimensionStepperPill(
                        label = if (isArabic) "الأعمدة: $colCount" else "Cols: $colCount",
                        onMinus = { resizeGrid(rowCount, colCount - 1) },
                        onPlus = { resizeGrid(rowCount, colCount + 1) },
                        canMinus = colCount > 1,
                        canPlus = colCount < 7,
                        accentColor = accentColor,
                        textColor = textColor,
                        isDark = isDark
                    )
                }

                // Interactive Scrollable Grid Table
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, metaColor.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column {
                        gridRows.forEachIndexed { rIdx, rowCells ->
                            val isHeader = rIdx == 0
                            Row(
                                modifier = Modifier.background(
                                    when {
                                        isHeader -> accentColor.copy(alpha = if (isDark) 0.24f else 0.14f)
                                        rIdx % 2 == 1 -> Color.Transparent
                                        else -> if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.025f)
                                    }
                                )
                            ) {
                                rowCells.forEachIndexed { cIdx, cellValue ->
                                    Box(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .defaultMinSize(minHeight = 44.dp)
                                            .border(0.6.dp, metaColor.copy(alpha = 0.22f))
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        BasicTextField(
                                            value = cellValue,
                                            onValueChange = { updated ->
                                                val updatedRow = gridRows[rIdx].toMutableList()
                                                updatedRow[cIdx] = updated.replace('\n', ' ')
                                                gridRows[rIdx] = updatedRow
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("table_cell_${rIdx}_$cIdx"),
                                            textStyle = TextStyle(
                                                fontSize = if (isHeader) 13.sp else 12.5.sp,
                                                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isHeader) accentColor else textColor
                                            ),
                                            cursorBrush = SolidColor(accentColor),
                                            decorationBox = { inner ->
                                                if (cellValue.isEmpty()) {
                                                    Text(
                                                        text = if (isHeader) {
                                                            if (isArabic) "رأس ${cIdx + 1}" else "Header ${cIdx + 1}"
                                                        } else {
                                                            if (isArabic) "بيانات..." else "Value..."
                                                        },
                                                        fontSize = 12.sp,
                                                        color = metaColor.copy(alpha = 0.45f)
                                                    )
                                                }
                                                inner()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (initialTable != null && onDeleteTable != null) {
                        OutlinedButton(
                            onClick = onDeleteTable,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = if (isArabic) "حذف الجدول" else "Delete Table",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "حذف" else "Delete",
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text(
                            text = if (isArabic) "إلغاء" else "Cancel",
                            color = metaColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = { onConfirmTable(gridRows.map { it.toList() }) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 44.dp)
                            .testTag("table_confirm_button")
                    ) {
                        Text(
                            text = if (initialTable != null) {
                                if (isArabic) "تحديث الجدول" else "Update Table"
                            } else {
                                if (isArabic) "إدراج الجدول" else "Insert Table"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DimensionStepperPill(
    label: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    canMinus: Boolean,
    canPlus: Boolean,
    accentColor: Color,
    textColor: Color,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (canMinus) accentColor.copy(alpha = 0.16f) else Color.Transparent)
                .clickable(enabled = canMinus, onClick = onMinus),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "-",
                tint = if (canMinus) accentColor else textColor.copy(alpha = 0.3f),
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (canPlus) accentColor.copy(alpha = 0.16f) else Color.Transparent)
                .clickable(enabled = canPlus, onClick = onPlus),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "+",
                tint = if (canPlus) accentColor else textColor.copy(alpha = 0.3f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * Interactive Rendered Grid Cards for all Pipe Tables on the active page in Writing Mode,
 * allowing 1-tap visual editing of cells/rows/columns without manually typing `|` syntax.
 */
@Composable
fun InlineTablesPreviewSection(
    tables: List<MarkdownTableData>,
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    textColor: Color,
    metaColor: Color,
    onEditTable: (MarkdownTableData) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tables.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tables.forEachIndexed { idx, table ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f))
                    .border(1.dp, accentColor.copy(alpha = 0.32f), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isArabic) {
                                "جدول ${idx + 1} (${table.rowCount}×${table.colCount})"
                            } else {
                                "Table ${idx + 1} (${table.rowCount}×${table.colCount})"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.16f))
                            .clickable { onEditTable(table) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isArabic) "تعديل الجدول" else "Edit Table",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }

                StyledMarkdownTable(
                    table = table,
                    isDark = isDark,
                    accentColor = accentColor,
                    textColor = textColor,
                    metaColor = metaColor
                )
            }
        }
    }
}

/**
 * Full Rich Content Renderer for Read / View-Only Mode & Vault Draft Browser.
 * Renders Bold, Italic, Underline, Strikethrough, Inline Code, Highlight,
 * H1/H2/H3 Headings, Quotes, Notion Callouts, Bullet/Numbered/Todo Lists,
 * Code Blocks, Dividers, and Styled Tables.
 */
@Composable
fun RichContentView(
    rawText: String,
    fontSizeSp: Float,
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    textColor: Color,
    bodyColor: Color,
    metaColor: Color,
    onToggleChecklistLine: ((Int) -> Unit)? = null,
    onEditTable: ((MarkdownTableData) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val blocks = remember(rawText, accentColor, isDark) {
        RichTextCodec.parseRichBlocks(rawText, accentColor, isDark)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is RichBlock.Heading -> {
                    val size = when (block.level) {
                        1 -> (fontSizeSp + 8f).coerceIn(18f, 36f)
                        2 -> (fontSizeSp + 5f).coerceIn(16f, 32f)
                        else -> (fontSizeSp + 2.5f).coerceIn(15f, 28f)
                    }
                    Text(
                        text = block.text,
                        style = TextStyle(
                            fontSize = size.sp,
                            fontWeight = if (block.level == 1) FontWeight.Black else FontWeight.ExtraBold,
                            color = if (block.level == 3) accentColor else textColor,
                            lineHeight = (size * 1.32f).sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp)
                    )
                }

                is RichBlock.Paragraph -> {
                    if (block.text.isEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                    } else {
                        Text(
                            text = block.text,
                            style = TextStyle(
                                fontSize = fontSizeSp.sp,
                                color = bodyColor,
                                lineHeight = (fontSizeSp * 1.52f).sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                is RichBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontSize = (fontSizeSp + 2f).sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                        Text(
                            text = block.text,
                            style = TextStyle(
                                fontSize = fontSizeSp.sp,
                                color = bodyColor,
                                lineHeight = (fontSizeSp * 1.48f).sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is RichBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = block.numberLabel,
                            fontSize = fontSizeSp.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Text(
                            text = block.text,
                            style = TextStyle(
                                fontSize = fontSizeSp.sp,
                                color = bodyColor,
                                lineHeight = (fontSizeSp * 1.48f).sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is RichBlock.ChecklistBlockItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = onToggleChecklistLine != null) {
                                onToggleChecklistLine?.invoke(block.lineStartOffset)
                            }
                            .padding(vertical = 3.dp, horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (block.checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (block.checked) accentColor else metaColor,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(19.dp)
                        )
                        Text(
                            text = block.text,
                            style = TextStyle(
                                fontSize = fontSizeSp.sp,
                                color = if (block.checked) metaColor else bodyColor,
                                textDecoration = if (block.checked) TextDecoration.LineThrough else TextDecoration.None,
                                lineHeight = (fontSizeSp * 1.48f).sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is RichBlock.Quote -> {
                    if (block.isCallout) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accentColor.copy(alpha = if (isDark) 0.18f else 0.11f))
                                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = "💡", fontSize = (fontSizeSp + 1f).sp)
                            Text(
                                text = block.text,
                                style = TextStyle(
                                    fontSize = fontSizeSp.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor,
                                    lineHeight = (fontSizeSp * 1.48f).sp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f))
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(22.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Text(
                                text = block.text,
                                style = TextStyle(
                                    fontSize = fontSizeSp.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = metaColor,
                                    lineHeight = (fontSizeSp * 1.46f).sp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                is RichBlock.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF0B1220) else Color(0xFFF1F5F9))
                            .border(1.dp, metaColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = block.code,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = (fontSizeSp - 2f).coerceIn(11f, 20f).sp,
                                color = if (isDark) Color(0xFFBAE6FD) else Color(0xFF0F172A),
                                lineHeight = ((fontSizeSp - 1f) * 1.45f).sp
                            )
                        )
                    }
                }

                RichBlock.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = metaColor.copy(alpha = 0.28f)
                    )
                }

                is RichBlock.Table -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onEditTable != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = if (isArabic) "تعديل الجدول" else "Edit Table",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { onEditTable(block.data) }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        StyledMarkdownTable(
                            table = block.data,
                            isDark = isDark,
                            accentColor = accentColor,
                            textColor = textColor,
                            metaColor = metaColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StyledMarkdownTable(
    table: MarkdownTableData,
    isDark: Boolean,
    accentColor: Color,
    textColor: Color,
    metaColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, metaColor.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            table.rows.forEachIndexed { rIdx, row ->
                val isHeader = rIdx == 0
                Row(
                    modifier = Modifier.background(
                        when {
                            isHeader -> accentColor.copy(alpha = if (isDark) 0.24f else 0.14f)
                            rIdx % 2 == 1 -> Color.Transparent
                            else -> if (isDark) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.025f)
                        }
                    )
                ) {
                    row.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .widthIn(min = 88.dp, max = 210.dp)
                                .border(0.6.dp, metaColor.copy(alpha = 0.20f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = RichTextCodec.renderInlineRichAnnotatedString(
                                    rawLine = cell,
                                    accentColor = accentColor,
                                    isDark = isDark
                                ),
                                fontSize = if (isHeader) 12.5.sp else 12.5.sp,
                                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                color = if (isHeader) accentColor else textColor,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
