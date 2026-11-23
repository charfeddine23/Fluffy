package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlassTint
import com.example.data.model.NotePagesCodec
import com.example.data.model.RichFormatAction
import com.example.ui.glass.liquidGlass
import com.example.ui.theme.EditorFontFamily

/**
 * Minimal Dark Liquid Glass Top App Bar:
 * - Start: Back navigation (+ compact Focus exit pill when Focus Mode is active)
 * - End: Pin toggle, Save/Done checkmark (or Edit button in View Mode), and a single Overflow menu (⋮)
 *   housing Add Page, Lock/Vault, Export/Share, Focus Mode, Favorite, Archive, and Trash.
 */
@Composable
fun EditorTopAppBar(
    isEditMode: Boolean,
    isPinned: Boolean,
    isFavorite: Boolean,
    isArchived: Boolean,
    canArchiveOrDelete: Boolean,
    isFocusMode: Boolean,
    isDark: Boolean,
    isArabic: Boolean,
    selectedTint: GlassTint,
    accentColor: Color,
    textColor: Color,
    metaColor: Color,
    pillFill: Color,
    onBack: () -> Unit,
    onTogglePin: () -> Unit,
    onSaveOrEdit: () -> Unit,
    onAddPage: () -> Unit,
    onMoveToVault: () -> Unit,
    onExportNote: () -> Unit,
    onCopyPage: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Start group: Back button + optional Focus Mode exit chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .testTag("editor_back_button")
                    .liquidGlass(
                        isDark = isDark,
                        tint = GlassTint.FROST,
                        shape = CircleShape,
                        borderWidth = 1.dp,
                        customFill = pillFill.copy(alpha = if (isDark) 0.55f else 0.72f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true)
                    ) { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isArabic) "رجوع" else "Back",
                    tint = textColor,
                    modifier = Modifier.size(21.dp)
                )
            }

            AnimatedVisibility(visible = isFocusMode) {
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = if (isDark) 0.22f else 0.14f))
                        .border(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.45f),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true)
                        ) { onToggleFocusMode() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("editor_exit_focus_pill"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isArabic) "خروج التركيز" else "Focus • Exit",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }

        // End group: Pin, Save/Done (or Edit), and single Overflow Menu (⋮)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pin Toggle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .testTag("editor_pin_toggle")
                    .liquidGlass(
                        isDark = isDark,
                        tint = if (isPinned) GlassTint.AMBER else GlassTint.FROST,
                        shape = RoundedCornerShape(14.dp),
                        borderWidth = 1.dp,
                        customFill = if (isPinned) Color(0xFFFBBF24).copy(alpha = 0.24f) else pillFill
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true)
                    ) { onTogglePin() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = if (isArabic) "تثبيت" else "Pin",
                    tint = if (isPinned) Color(0xFFFBBF24) else textColor,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Save/Done Checkmark (Edit mode) or Edit Pencil (View-Only mode)
            val actionTag = if (isEditMode) "editor_save_button" else "editor_edit_button"
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .testTag(actionTag)
                    .liquidGlass(
                        isDark = isDark,
                        tint = selectedTint,
                        shape = RoundedCornerShape(14.dp),
                        borderWidth = 1.2.dp,
                        customFill = accentColor.copy(alpha = if (isDark) 0.28f else 0.18f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true)
                    ) { onSaveOrEdit() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = if (isEditMode) {
                        if (isArabic) "حفظ" else "Save"
                    } else {
                        if (isArabic) "تعديل" else "Edit"
                    },
                    tint = if (isEditMode && isDark) Color(0xFFE0F2FE) else textColor,
                    modifier = Modifier.size(21.dp)
                )
            }

            // Single Overflow Menu (⋮)
            Box {
                val overflowTag = if (isEditMode) "editor_more_button" else "editor_view_more_button"
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .testTag(overflowTag)
                        .liquidGlass(
                            isDark = isDark,
                            tint = GlassTint.FROST,
                            shape = RoundedCornerShape(14.dp),
                            borderWidth = 1.dp,
                            customFill = pillFill
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true)
                        ) { showOverflowMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = if (isArabic) "المزيد من الخيارات" else "More options",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                    containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (isEditMode) {
                        DropdownMenuItem(
                            modifier = Modifier.testTag("editor_menu_add_page"),
                            text = {
                                Text(
                                    text = if (isArabic) "إضافة صفحة" else "Add Page",
                                    color = textColor,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                                    contentDescription = null,
                                    tint = accentColor
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onAddPage()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            modifier = Modifier.testTag("editor_copy_page_button"),
                            text = {
                                Text(
                                    text = if (isArabic) "نسخ محتوى الصفحة" else "Copy Current Page",
                                    color = textColor,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981)
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onCopyPage()
                            }
                        )
                    }

                    DropdownMenuItem(
                        modifier = Modifier.testTag(
                            if (isEditMode) "editor_move_to_vault_edit_item" else "editor_move_to_vault_view_item"
                        ),
                        text = {
                            Text(
                                text = if (isArabic) "قفل في الخزنة (Vault)" else "Lock / Move to Vault",
                                color = textColor,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFC084FC)
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onMoveToVault()
                        }
                    )

                    DropdownMenuItem(
                        modifier = Modifier.testTag("editor_menu_export"),
                        text = {
                            Text(
                                text = if (isArabic) "تصدير ومشاركة" else "Export / Share",
                                color = textColor,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.IosShare,
                                contentDescription = null,
                                tint = accentColor
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onExportNote()
                        }
                    )

                    DropdownMenuItem(
                        modifier = Modifier.testTag("editor_menu_focus_mode"),
                        text = {
                            Text(
                                text = if (isFocusMode) {
                                    if (isArabic) "إنهاء وضع التركيز" else "Exit Focus Mode"
                                } else {
                                    if (isArabic) "وضع التركيز" else "Focus Mode"
                                },
                                color = if (isFocusMode) accentColor else textColor,
                                fontWeight = if (isFocusMode) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = null,
                                tint = accentColor
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onToggleFocusMode()
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        color = metaColor.copy(alpha = 0.16f)
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isFavorite) {
                                    if (isArabic) "إزالة من المفضلة" else "Remove Favorite"
                                } else {
                                    if (isArabic) "إضافة للمفضلة" else "Add to Favorites"
                                },
                                color = textColor,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) Color(0xFFF43F5E) else metaColor
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onToggleFavorite()
                        }
                    )

                    if (canArchiveOrDelete) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isArchived) {
                                        if (isArabic) "إلغاء الأرشفة" else "Unarchive Note"
                                    } else {
                                        if (isArabic) "أرشفة الملاحظة" else "Archive Note"
                                    },
                                    color = textColor,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = accentColor
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onToggleArchive()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isArabic) "نقل إلى سلة المهملات" else "Move to Trash",
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444)
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Streamlined Note Title & Compact Single-Row Multi-Page Strip.
 * Collapses smoothly when Focus Mode is active.
 */
@Composable
fun EditorNoteHeader(
    title: String,
    isEditMode: Boolean,
    isFocusMode: Boolean,
    pages: List<String>,
    currentPage: Int,
    canAppendAnotherPage: Boolean,
    totalNoteBytes: Long,
    isPremium: Boolean,
    maxAllowedPages: Int,
    pageChipListState: LazyListState,
    copyFeedbackMessage: String?,
    justCopiedPage: Boolean,
    limitWarningMessage: String?,
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    textColor: Color,
    metaColor: Color,
    pillFill: Color,
    onTitleChange: (String) -> Unit,
    onOpenPagePicker: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    onRemoveCurrentPage: () -> Unit,
    onAddPage: () -> Unit,
    onRequestPremiumUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        AnimatedVisibility(
            visible = !isFocusMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Main Note Title
                if (isEditMode) {
                    BasicTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 10.dp)
                            .testTag("editor_title_input"),
                        textStyle = TextStyle(
                            fontFamily = EditorFontFamily,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 32.sp,
                            letterSpacing = (-0.3).sp,
                            color = textColor
                        ),
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (title.isEmpty()) {
                                    Text(
                                        text = if (isArabic) "عنوان الملاحظة..." else "Note title...",
                                        style = TextStyle(
                                            fontFamily = EditorFontFamily,
                                            fontSize = 25.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 32.sp,
                                            letterSpacing = (-0.3).sp,
                                            color = metaColor.copy(alpha = 0.50f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                } else if (title.isNotEmpty()) {
                    SelectionContainer {
                        Text(
                            text = title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 10.dp)
                                .testTag("editor_title_readonly"),
                            style = TextStyle(
                                fontFamily = EditorFontFamily,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 32.sp,
                                letterSpacing = (-0.3).sp,
                                color = textColor
                            )
                        )
                    }
                }

                // Compact Single-Row Multi-Page Bar
                val activePageDisplayIdx = currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                val sizeRatio = totalNoteBytes.toFloat() / NotePagesCodec.MAX_NOTE_BYTES.toFloat()
                val sizeIndicatorColor = when {
                    sizeRatio >= 0.96f -> Color(0xFFEF4444)
                    sizeRatio >= 0.80f -> Color(0xFFF59E0B)
                    else -> metaColor
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isDark) Color(0xFF0F172A).copy(alpha = 0.52f)
                            else Color.White.copy(alpha = 0.65f)
                        )
                        .border(
                            width = 0.9.dp,
                            color = accentColor.copy(alpha = if (isDark) 0.24f else 0.16f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Page X/Y interactive capsule
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 32.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = if (isDark) 0.22f else 0.13f))
                            .border(
                                width = 0.8.dp,
                                color = accentColor.copy(alpha = 0.42f),
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true)
                            ) { onOpenPagePicker() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("editor_page_counter_pill"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = if (isArabic) "صفحات المسودة" else "Draft Pages",
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isArabic) {
                                "صفحة ${activePageDisplayIdx + 1}/${pages.size}"
                            } else {
                                "Page ${activePageDisplayIdx + 1}/${pages.size}"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1
                        )
                    }

                    // Page Chips OR subtle "Swipe for Page 2" hint on the same compact row
                    if (pages.size > 1) {
                        LazyRow(
                            state = pageChipListState,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(pages) { idx, _ ->
                                val isSelectedPage = idx == currentPage
                                Box(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 28.dp, minHeight = 28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelectedPage) accentColor
                                            else pillFill.copy(alpha = 0.72f)
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true)
                                        ) { onJumpToPage(idx) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("editor_page_chip_$idx"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelectedPage) Color.White else metaColor
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = if (isEditMode) {
                                if (isArabic) "اسحب لصفحة جديدة" else "Swipe for Page 2"
                            } else "",
                            fontSize = 11.5.sp,
                            color = metaColor.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Compact size badge only when note grows meaningful or near cap
                    if (pages.size > 1 || totalNoteBytes >= 4096L) {
                        Text(
                            text = NotePagesCodec.formatSizeKb(totalNoteBytes, isArabic),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = sizeIndicatorColor,
                            modifier = Modifier.testTag("editor_note_size_label")
                        )
                    }

                    if (isEditMode) {
                        if (pages.size > 1 && currentPage < pages.size) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.14f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true)
                                    ) { onRemoveCurrentPage() }
                                    .testTag("editor_remove_page_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = if (isArabic) "حذف الصفحة الحالية" else "Delete current page",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canAppendAnotherPage) accentColor.copy(alpha = if (isDark) 0.26f else 0.16f)
                                    else metaColor.copy(alpha = 0.14f)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) { onAddPage() }
                                .testTag("editor_add_page_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = if (isArabic) "إضافة صفحة جديدة" else "Add New Page",
                                tint = if (canAppendAnotherPage) accentColor else metaColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }

        // Transient copy / OCR / limit banners
        AnimatedVisibility(visible = copyFeedbackMessage != null) {
            val isSuccess = justCopiedPage
            val toastAccent = if (isSuccess) Color(0xFF10B981) else accentColor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(toastAccent.copy(alpha = if (isDark) 0.20f else 0.12f))
                    .border(1.dp, toastAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = copyFeedbackMessage.orEmpty(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFE0F2FE) else Color(0xFF0F172A)
                )
            }
        }

        AnimatedVisibility(visible = limitWarningMessage != null) {
            val isFreePageCapWarning = !isPremium && pages.size >= maxAllowedPages
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEF4444).copy(alpha = if (isDark) 0.22f else 0.12f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .clickable(enabled = isFreePageCapWarning) { onRequestPremiumUpgrade() }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = limitWarningMessage.orEmpty(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                    modifier = Modifier.weight(1f)
                )
                if (isFreePageCapWarning) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isArabic) "ترقية" else "Upgrade",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1F1200)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sleek optional per-page title input with minimalist frosted underline styling.
 */
@Composable
fun EditorPageTitleField(
    pageIndex: Int,
    currentPageTitle: String,
    isEditMode: Boolean,
    isFocusMode: Boolean,
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    textColor: Color,
    metaColor: Color,
    onPageTitleChange: (String) -> Unit
) {
    if (isEditMode) {
        if (!isFocusMode || currentPageTitle.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isArabic) "ص ${pageIndex + 1}" else "P${pageIndex + 1}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                    }

                    BasicTextField(
                        value = currentPageTitle,
                        onValueChange = onPageTitleChange,
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("editor_page_title_input_$pageIndex"),
                        textStyle = TextStyle(
                            fontFamily = EditorFontFamily,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        ),
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { innerTitleField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (currentPageTitle.isEmpty()) {
                                    Text(
                                        text = if (isArabic) {
                                            "عنوان الصفحة ${pageIndex + 1} (اختياري)..."
                                        } else {
                                            "Page ${pageIndex + 1} title (optional)..."
                                        },
                                        fontFamily = EditorFontFamily,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = metaColor.copy(alpha = 0.52f)
                                    )
                                }
                                innerTitleField()
                            }
                        }
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor.copy(alpha = if (currentPageTitle.isNotBlank()) 0.45f else 0.18f),
                                    metaColor.copy(alpha = 0.08f)
                                )
                            )
                        )
                )
            }
        }
    } else if (currentPageTitle.isNotBlank() || pageIndex > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isDark) 0.22f else 0.14f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isArabic) "صفحة ${pageIndex + 1}" else "Page ${pageIndex + 1}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            if (currentPageTitle.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = currentPageTitle,
                        fontFamily = EditorFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        modifier = Modifier.testTag("editor_page_title_readonly_$pageIndex")
                    )
                }
            }
        }
    }
}

/**
 * Unified Single-Row Dark Liquid Glass Formatting & Insert Bar positioned directly above
 * the soft keyboard / bottom insets with zero duplicate icons:
 * - Anchored primary `+ Add` pill opening Quick Insert + 20 tools
 * - Horizontally scrollable core toggles: H1, H2, H3, B, I, U, Strikethrough, Highlight,
 *   Bullet List, Checklist, Alignment cycle, and single Style/Color palette trigger.
 */
@Composable
fun UnifiedWritingBottomBar(
    activeStyles: Set<RichFormatAction>,
    currentAlignment: TextAlign,
    hasCustomStyle: Boolean,
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    textColor: Color,
    metaColor: Color,
    pillFill: Color,
    onFormatAction: (RichFormatAction) -> Unit,
    onOpenAddMenu: () -> Unit,
    onCycleAlignment: () -> Unit,
    onOpenStyleSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isDark) Color(0xFF0F172A).copy(alpha = 0.86f)
                else Color.White.copy(alpha = 0.90f)
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = if (isDark) 0.30f else 0.22f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Primary + Add Pill Button
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 40.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.96f),
                            accentColor.copy(alpha = 0.78f)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true)
                ) { onOpenAddMenu() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("editor_insert_add_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isArabic) "إضافة" else "Add",
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = if (isArabic) "إضافة" else "Add",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(22.dp)
                .background(metaColor.copy(alpha = 0.20f))
        )

        // Single horizontally scrollable row for formatting, list/alignment, and Style/Color
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Heading Size Segmented Pill (H1 / H2 / H3)
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pillFill.copy(alpha = if (isDark) 0.56f else 0.74f))
                    .border(
                        width = 0.8.dp,
                        color = metaColor.copy(alpha = 0.22f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 3.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ToolbarHeadingPill(
                    label = "H1",
                    fontSizeSp = 13f,
                    isActive = RichFormatAction.H1 in activeStyles,
                    accentColor = accentColor,
                    textColor = textColor,
                    onClick = { onFormatAction(RichFormatAction.H1) },
                    testTag = "editor_fmt_h1"
                )
                ToolbarHeadingPill(
                    label = "H2",
                    fontSizeSp = 12.2f,
                    isActive = RichFormatAction.H2 in activeStyles,
                    accentColor = accentColor,
                    textColor = textColor,
                    onClick = { onFormatAction(RichFormatAction.H2) },
                    testTag = "editor_fmt_h2"
                )
                ToolbarHeadingPill(
                    label = "H3",
                    fontSizeSp = 11.5f,
                    isActive = RichFormatAction.H3 in activeStyles,
                    accentColor = accentColor,
                    textColor = textColor,
                    onClick = { onFormatAction(RichFormatAction.H3) },
                    testTag = "editor_fmt_h3"
                )
            }

            ToolbarIconToggle(
                icon = Icons.Default.FormatBold,
                contentDescription = if (isArabic) "عريض" else "Bold",
                isActive = RichFormatAction.BOLD in activeStyles,
                accentColor = accentColor,
                textColor = textColor,
                pillFill = pillFill,
                isDark = isDark,
                onClick = { onFormatAction(RichFormatAction.BOLD) },
                testTag = "editor_fmt_bold"
            )

            ToolbarIconToggle(
                icon = Icons.Default.FormatItalic,
                contentDescription = if (isArabic) "مائل" else "Italic",
                isActive = RichFormatAction.ITALIC in activeStyles,
                accentColor = accentColor,
                textColor = textColor,
                pillFill = pillFill,
                isDark = isDark,
                onClick = { onFormatAction(RichFormatAction.ITALIC) },
                testTag = "editor_fmt_italic"
            )

            ToolbarIconToggle(
                icon = Icons.Default.FormatUnderlined,
                contentDescription = if (isArabic) "تسطير النص" else "Underline",
                isActive = RichFormatAction.UNDERLINE in activeStyles,
                accentColor = accentColor,
                textColor = textColor,
                pillFill = pillFill,
                isDark = isDark,
                onClick = { onFormatAction(RichFormatAction.UNDERLINE) },
                testTag = "editor_fmt_underline"
            )

            ToolbarIconToggle(
                icon = Icons.Default.FormatStrikethrough,
                contentDescription = if (isArabic) "تشطيب النص" else "Strikethrough",
                isActive = RichFormatAction.STRIKETHROUGH in activeStyles,
                accentColor = accentColor,
                textColor = textColor,
                pillFill = pillFill,
                isDark = isDark,
                onClick = { onFormatAction(RichFormatAction.STRIKETHROUGH) },
                testTag = "editor_fmt_strikethrough"
            )

            // Highlight toggle
            val highlightActive = RichFormatAction.HIGHLIGHT in activeStyles
            val amberColor = Color(0xFFF59E0B)
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                    .clip(CircleShape)
                    .background(
                        if (highlightActive) amberColor.copy(alpha = if (isDark) 0.34f else 0.24f)
                        else pillFill.copy(alpha = if (isDark) 0.56f else 0.74f)
                    )
                    .border(
                        width = if (highlightActive) 1.2.dp else 0.8.dp,
                        color = if (highlightActive) amberColor else metaColor.copy(alpha = 0.20f),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true)
                    ) { onFormatAction(RichFormatAction.HIGHLIGHT) }
                    .padding(8.dp)
                    .testTag("editor_fmt_highlight"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BorderColor,
                    contentDescription = if (isArabic) "تمييز النص" else "Highlight",
                    tint = if (highlightActive) amberColor else textColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Bullet List & Inline Checkbox toggles
            ToolbarIconToggle(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = if (isArabic) "قائمة نقطية" else "Bullet List",
                isActive = RichFormatAction.BULLET_LIST in activeStyles,
                accentColor = accentColor,
                textColor = textColor,
                pillFill = pillFill,
                isDark = isDark,
                onClick = { onFormatAction(RichFormatAction.BULLET_LIST) },
                testTag = "editor_fmt_bullet_list"
            )

            ToolbarIconToggle(
                icon = Icons.Default.CheckBox,
                contentDescription = if (isArabic) "مربع مهمة" else "Todo Checkbox",
                isActive = RichFormatAction.CHECKBOX in activeStyles,
                accentColor = accentColor,
                textColor = textColor,
                pillFill = pillFill,
                isDark = isDark,
                onClick = { onFormatAction(RichFormatAction.CHECKBOX) },
                testTag = "editor_fmt_checkbox"
            )

            // Alignment Toggle (Start -> Center -> End)
            val alignIcon = when (currentAlignment) {
                TextAlign.Center -> Icons.Default.FormatAlignCenter
                TextAlign.End, TextAlign.Right -> Icons.AutoMirrored.Filled.FormatAlignRight
                else -> Icons.AutoMirrored.Filled.FormatAlignLeft
            }
            ToolbarIconToggle(
                icon = alignIcon,
                contentDescription = if (isArabic) "محاذاة النص" else "Text Alignment",
                isActive = currentAlignment != TextAlign.Start,
                accentColor = accentColor,
                textColor = textColor,
                pillFill = pillFill,
                isDark = isDark,
                onClick = onCycleAlignment,
                testTag = "editor_fmt_align"
            )

            // Single Style / Color / Background Button (no duplicate icons!)
            ToolbarIconToggle(
                icon = Icons.Default.Palette,
                contentDescription = if (isArabic) "نمط ولون المستند" else "Style & Colors",
                isActive = hasCustomStyle,
                accentColor = accentColor,
                textColor = textColor,
                pillFill = pillFill,
                isDark = isDark,
                onClick = onOpenStyleSheet,
                testTag = "editor_palette_button"
            )
        }
    }
}

@Composable
private fun ToolbarHeadingPill(
    label: String,
    fontSizeSp: Float,
    isActive: Boolean,
    accentColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
            .clip(CircleShape)
            .background(if (isActive) accentColor else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(horizontal = 9.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = fontSizeSp.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isActive) Color.White else textColor
        )
    }
}

@Composable
private fun ToolbarIconToggle(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    accentColor: Color,
    textColor: Color,
    pillFill: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
            .clip(CircleShape)
            .background(
                if (isActive) accentColor.copy(alpha = if (isDark) 0.30f else 0.20f)
                else pillFill.copy(alpha = if (isDark) 0.56f else 0.74f)
            )
            .border(
                width = if (isActive) 1.2.dp else 0.8.dp,
                color = if (isActive) accentColor else textColor.copy(alpha = 0.14f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) accentColor else textColor,
            modifier = Modifier.size(19.dp)
        )
    }
}
