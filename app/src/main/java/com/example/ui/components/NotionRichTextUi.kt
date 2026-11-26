package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.EditorPageSegment
import com.example.data.model.GlassTint
import com.example.data.model.InsertFeatureType
import com.example.data.model.MarkdownTableData
import com.example.data.model.RichBlock
import com.example.data.model.RichFormatAction
import com.example.data.model.RichTextCodec
import java.io.File
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.theme.LocalAppFontOption

/**
 * Sleek Liquid Glass formatting bar for Writing Mode containing:
 * - Add (+ إضافة) button launching the 20-tool Insert Menu
 * - Heading size controls (H1, H2, H3)
 * - Bold, Italic, Strikethrough, Underline, Highlight
 */
@Composable
fun WritingFormattingBar(
    activeStyles: Set<RichFormatAction>,
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    textColor: Color,
    metaColor: Color,
    pillFill: Color,
    onFormatAction: (RichFormatAction) -> Unit,
    onOpenAddMenu: () -> Unit,
    modifier: Modifier = Modifier,
    outerVerticalPadding: Dp = 4.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = outerVerticalPadding)
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) Color(0xFF0F172A).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.84f))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = if (isDark) 0.28f else 0.20f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Prominent Add Button (always visible)
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 40.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.94f),
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

        // Scrollable formatting controls: H1, H2, H3, Bold, Italic, Strikethrough, Underline, Highlight
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Heading Size Group (H1 / H2 / H3)
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pillFill.copy(alpha = if (isDark) 0.55f else 0.72f))
                    .border(
                        width = 0.8.dp,
                        color = metaColor.copy(alpha = 0.22f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 3.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FormatHeadingPill(
                    label = "H1",
                    fontSizeSp = 13.5f,
                    isActive = RichFormatAction.H1 in activeStyles,
                    accentColor = accentColor,
                    textColor = textColor,
                    onClick = { onFormatAction(RichFormatAction.H1) },
                    testTag = "editor_fmt_h1"
                )
                FormatHeadingPill(
                    label = "H2",
                    fontSizeSp = 12.5f,
                    isActive = RichFormatAction.H2 in activeStyles,
                    accentColor = accentColor,
                    textColor = textColor,
                    onClick = { onFormatAction(RichFormatAction.H2) },
                    testTag = "editor_fmt_h2"
                )
                FormatHeadingPill(
                    label = "H3",
                    fontSizeSp = 11.5f,
                    isActive = RichFormatAction.H3 in activeStyles,
                    accentColor = accentColor,
                    textColor = textColor,
                    onClick = { onFormatAction(RichFormatAction.H3) },
                    testTag = "editor_fmt_h3"
                )
            }

            FormatIconButton(
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

            FormatIconButton(
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

            FormatIconButton(
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

            FormatIconButton(
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

            // Highlight toggle pill (amber tint when active)
            val highlightActive = RichFormatAction.HIGHLIGHT in activeStyles
            val amberColor = Color(0xFFF59E0B)
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                    .clip(CircleShape)
                    .background(
                        if (highlightActive) amberColor.copy(alpha = if (isDark) 0.34f else 0.28f)
                        else pillFill.copy(alpha = if (isDark) 0.55f else 0.72f)
                    )
                    .border(
                        width = if (highlightActive) 1.2.dp else 0.8.dp,
                        color = if (highlightActive) amberColor else metaColor.copy(alpha = 0.22f),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true)
                    ) { onFormatAction(RichFormatAction.HIGHLIGHT) }
                    .padding(horizontal = 9.dp, vertical = 7.dp)
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
        }
    }
}

@Composable
private fun FormatHeadingPill(
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
private fun FormatIconButton(
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
                else pillFill.copy(alpha = if (isDark) 0.55f else 0.72f)
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

private data class InsertMenuDescriptor(
    val type: InsertFeatureType,
    val categoryIdx: Int,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val icon: ImageVector,
    val tint: Color
)

private val insertFeatureDescriptors: List<InsertMenuDescriptor> = listOf(
    // 0: Interactive & Media (5 items)
    InsertMenuDescriptor(
        type = InsertFeatureType.TABLE,
        categoryIdx = 0,
        titleAr = "جدول تفاعلي",
        titleEn = "Interactive Table",
        subtitleAr = "إنشاء جدول بصفوف وأعمدة مخصصة",
        subtitleEn = "Custom rows & columns grid",
        icon = Icons.Default.TableChart,
        tint = Color(0xFF38BDF8)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.TODO_CHECKBOX,
        categoryIdx = 0,
        titleAr = "مهمة داخل النص",
        titleEn = "Inline Todo Box",
        subtitleAr = "مربع اختيار تفاعلي - [ ]",
        subtitleEn = "Checkable task inside page",
        icon = Icons.Default.CheckBox,
        tint = Color(0xFF10B981)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.CHECKLIST_CARD,
        categoryIdx = 0,
        titleAr = "بطاقة مهام سفلية",
        titleEn = "Checklist Item",
        subtitleAr = "إضافة بند في قائمة المهام",
        subtitleEn = "Add item to note checklist",
        icon = Icons.Default.Checklist,
        tint = Color(0xFF34D399)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.VOICE_MEMO,
        categoryIdx = 0,
        titleAr = "تسجيل صوتي",
        titleEn = "Voice Memo",
        subtitleAr = "تسجيل مقطع صوتي وإرفاقه",
        subtitleEn = "Record & attach voice note",
        icon = Icons.Default.Mic,
        tint = Color(0xFFF43F5E)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.NEW_PAGE,
        categoryIdx = 0,
        titleAr = "صفحة جديدة",
        titleEn = "New Draft Page",
        subtitleAr = "إضافة صفحة جديدة للمسودة",
        subtitleEn = "Append a new page now",
        icon = Icons.AutoMirrored.Filled.NoteAdd,
        tint = Color(0xFF818CF8)
    ),

    // 1: Structure & Blocks (7 items)
    InsertMenuDescriptor(
        type = InsertFeatureType.BULLET_LIST,
        categoryIdx = 1,
        titleAr = "قائمة نقطية",
        titleEn = "Bullet List",
        subtitleAr = "تعداد نقطي مرتب للأسطر",
        subtitleEn = "Clean bullet points",
        icon = Icons.AutoMirrored.Filled.FormatListBulleted,
        tint = Color(0xFF60A5FA)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.NUMBERED_LIST,
        categoryIdx = 1,
        titleAr = "قائمة مرقمة",
        titleEn = "Numbered List",
        subtitleAr = "ترقيم تلقائي للخطوات 1، 2",
        subtitleEn = "Auto-incrementing 1. 2. 3.",
        icon = Icons.Default.FormatListNumbered,
        tint = Color(0xFF6366F1)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.QUOTE,
        categoryIdx = 1,
        titleAr = "اقتباس مميز",
        titleEn = "Quote Block",
        subtitleAr = "إبراز مقولة أو عبارة جانبية",
        subtitleEn = "Styled vertical quote bar",
        icon = Icons.Default.FormatQuote,
        tint = Color(0xFFA855F7)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.CALLOUT,
        categoryIdx = 1,
        titleAr = "صندوق تنبيه",
        titleEn = "Callout Note",
        subtitleAr = "بطاقة ملاحظة بأيقونة 💡",
        subtitleEn = "Highlighted tip card 💡",
        icon = Icons.Default.Lightbulb,
        tint = Color(0xFFF59E0B)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.CODE_BLOCK,
        categoryIdx = 1,
        titleAr = "صندوق كود",
        titleEn = "Code Block",
        subtitleAr = "كتلة برمجية متعددة الأسطر",
        subtitleEn = "Monospace fenced code",
        icon = Icons.Default.DataObject,
        tint = Color(0xFF06B6D4)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.INLINE_CODE,
        categoryIdx = 1,
        titleAr = "كود سطري",
        titleEn = "Inline Code",
        subtitleAr = "تنسيق كلمة أو أمر تقني",
        subtitleEn = "Wrap selected code tag",
        icon = Icons.Default.Code,
        tint = Color(0xFF0EA5E9)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.DIVIDER,
        categoryIdx = 1,
        titleAr = "خط فاصل",
        titleEn = "Divider Line",
        subtitleAr = "فاصل أفقي أنيق بين الفقرات",
        subtitleEn = "Horizontal section divider",
        icon = Icons.Default.HorizontalRule,
        tint = Color(0xFF94A3B8)
    ),

    // 2: Date, Time & Visual Badges (6 items)
    InsertMenuDescriptor(
        type = InsertFeatureType.DATE_STAMP,
        categoryIdx = 2,
        titleAr = "تاريخ اليوم",
        titleEn = "Today's Date",
        subtitleAr = "إدراج التاريخ الحالي تلقائياً",
        subtitleEn = "Insert current date stamp",
        icon = Icons.Default.CalendarToday,
        tint = Color(0xFF22C55E)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.TIME_STAMP,
        categoryIdx = 2,
        titleAr = "الوقت الحالي",
        titleEn = "Current Time",
        subtitleAr = "إدراج الساعة والدقيقة الآن",
        subtitleEn = "Insert current time stamp",
        icon = Icons.Default.Schedule,
        tint = Color(0xFF14B8A6)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.DAILY_HEADER,
        categoryIdx = 2,
        titleAr = "ترويسة يومية",
        titleEn = "Daily Entry Header",
        subtitleAr = "عنوان مع التاريخ والوقت جاهز",
        subtitleEn = "Heading with date & time",
        icon = Icons.Default.EventNote,
        tint = Color(0xFF06B6D4)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.STAR_RATING,
        categoryIdx = 2,
        titleAr = "تقييم بالنجوم",
        titleEn = "Star Rating",
        subtitleAr = "شريط نجوم جاهز ★★★★☆",
        subtitleEn = "Visual 5-star rating row",
        icon = Icons.Default.Star,
        tint = Color(0xFFEAB308)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.PROGRESS_BAR,
        categoryIdx = 2,
        titleAr = "شريط التقدم",
        titleEn = "Progress Bar",
        subtitleAr = "مؤشر نسبة الإنجاز البصري",
        subtitleEn = "Visual progress meter 60%",
        icon = Icons.Default.TrendingUp,
        tint = Color(0xFF10B981)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.STATUS_BADGE,
        categoryIdx = 2,
        titleAr = "شارة الحالة",
        titleEn = "Status Badge",
        subtitleAr = "وسم حالة جاهز قيد التنفيذ",
        subtitleEn = "Status tag pill marker",
        icon = Icons.Default.Label,
        tint = Color(0xFFEC4899)
    ),

    // 3: Smart Templates (2 items -> total 20!)
    InsertMenuDescriptor(
        type = InsertFeatureType.MEETING_TEMPLATE,
        categoryIdx = 3,
        titleAr = "قالب محضر اجتماع",
        titleEn = "Meeting Template",
        subtitleAr = "هيكل موضوع، حضور، ونقاط عمل",
        subtitleEn = "Topic, attendees & action list",
        icon = Icons.Default.Groups,
        tint = Color(0xFF6366F1)
    ),
    InsertMenuDescriptor(
        type = InsertFeatureType.PROS_CONS_TABLE,
        categoryIdx = 3,
        titleAr = "جدول إيجابيات وسلبيات",
        titleEn = "Pros & Cons Table",
        subtitleAr = "جدول مقارنة جاهز بعمودين",
        subtitleEn = "Ready 2-column comparison",
        icon = Icons.Default.CompareArrows,
        tint = Color(0xFFF97316)
    )
)

/**
 * Interactive Modal Bottom Sheet presenting 20 categorized insertion tools for Writing Mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsertFeaturesBottomSheet(
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    textColor: Color,
    metaColor: Color,
    onSelectFeature: (InsertFeatureType) -> Unit,
    onDismiss: () -> Unit,
    onQuickImageScan: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery, isArabic) {
        val q = searchQuery.trim()
        if (q.isEmpty()) insertFeatureDescriptors
        else insertFeatureDescriptors.filter {
            it.titleAr.contains(q, ignoreCase = true) ||
                it.titleEn.contains(q, ignoreCase = true) ||
                it.subtitleAr.contains(q, ignoreCase = true) ||
                it.subtitleEn.contains(q, ignoreCase = true)
        }
    }

    val categoryTitles = remember(isArabic) {
        if (isArabic) {
            listOf(
                "العناصر التفاعلية والوسائط",
                "القوائم والهيكلة",
                "الوقت والشارات والمؤشرات",
                "القوالب الجاهزة"
            )
        } else {
            listOf(
                "Interactive & Media",
                "Lists & Blocks",
                "Time, Badges & Metrics",
                "Ready Templates"
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
        contentColor = textColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "إدراج في الصفحة" else "Insert Into Page",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                    Text(
                        text = if (isArabic) "اختر من بين 20 ميزة وأداة كتابة ذكية" else "Choose from 20 rich writing tools",
                        fontSize = 12.sp,
                        color = metaColor
                    )
                }
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                        .clickable { onDismiss() },
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

            // Quick Filter Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (isArabic) "ابحث عن ميزة (جدول، تاريخ، قالب...)" else "Search tools (table, date, template...)",
                        fontSize = 13.sp,
                        color = metaColor.copy(alpha = 0.7f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = metaColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = metaColor.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Featured Quick Insert Strip (Image/Scanner, Checklist, Voice Note, Divider)
            if (searchQuery.isBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onQuickImageScan != null) {
                        QuickInsertPillCard(
                            label = if (isArabic) "صورة / مسح" else "Image / Scan",
                            icon = Icons.Default.DocumentScanner,
                            tint = Color(0xFF38BDF8),
                            isDark = isDark,
                            textColor = textColor,
                            onClick = { onQuickImageScan() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_insert_image_scan")
                        )
                    } else {
                        QuickInsertPillCard(
                            label = if (isArabic) "جدول" else "Table",
                            icon = Icons.Default.TableChart,
                            tint = Color(0xFF38BDF8),
                            isDark = isDark,
                            textColor = textColor,
                            onClick = { onSelectFeature(InsertFeatureType.TABLE) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    QuickInsertPillCard(
                        label = if (isArabic) "مهام" else "Checklist",
                        icon = Icons.Default.Checklist,
                        tint = Color(0xFF10B981),
                        isDark = isDark,
                        textColor = textColor,
                        onClick = { onSelectFeature(InsertFeatureType.CHECKLIST_CARD) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_insert_checklist")
                    )
                    QuickInsertPillCard(
                        label = if (isArabic) "صوتية" else "Voice",
                        icon = Icons.Default.Mic,
                        tint = Color(0xFFF43F5E),
                        isDark = isDark,
                        textColor = textColor,
                        onClick = { onSelectFeature(InsertFeatureType.VOICE_MEMO) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_insert_voice")
                    )
                    QuickInsertPillCard(
                        label = if (isArabic) "فاصل" else "Divider",
                        icon = Icons.Default.HorizontalRule,
                        tint = Color(0xFF94A3B8),
                        isDark = isDark,
                        textColor = textColor,
                        onClick = { onSelectFeature(InsertFeatureType.DIVIDER) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_insert_divider")
                    )
                }
            }

            // 20 Categorized Insert Tools in 2-Column Rows
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                val grouped = filtered.groupBy { it.categoryIdx }
                grouped.toSortedMap().forEach { (catIdx, itemsInCat) ->
                    item(key = "cat_header_$catIdx") {
                        Text(
                            text = categoryTitles.getOrElse(catIdx) { "" },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }
                    val pairs = itemsInCat.chunked(2)
                    items(
                        count = pairs.size,
                        key = { rowIdx -> "cat_${catIdx}_row_$rowIdx" }
                    ) { rowIdx ->
                        val pair = pairs[rowIdx]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { item ->
                                InsertFeatureCard(
                                    item = item,
                                    isDark = isDark,
                                    isArabic = isArabic,
                                    textColor = textColor,
                                    metaColor = metaColor,
                                    onClick = { onSelectFeature(item.type) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickInsertPillCard(
    label: String,
    icon: ImageVector,
    tint: Color,
    isDark: Boolean,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = if (isDark) 0.14f else 0.10f))
            .border(
                width = 1.dp,
                color = tint.copy(alpha = if (isDark) 0.34f else 0.24f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InsertFeatureCard(
    item: InsertMenuDescriptor,
    isDark: Boolean,
    isArabic: Boolean,
    textColor: Color,
    metaColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.White)
            .border(
                width = 1.dp,
                color = item.tint.copy(alpha = if (isDark) 0.28f else 0.22f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .testTag("insert_feature_${item.type.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(item.tint.copy(alpha = if (isDark) 0.20f else 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.tint,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isArabic) item.titleAr else item.titleEn,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isArabic) item.subtitleAr else item.subtitleEn,
                fontSize = 10.5.sp,
                color = metaColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp
            )
        }
    }
}

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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard(
                isDark = isDark,
                tint = GlassTint.FROST,
                shape = RoundedCornerShape(26.dp),
                borderWidth = 1.4.dp,
                modifier = Modifier
                    .fillMaxWidth()
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
                            modifier = Modifier.weight(1f),
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

                    // Interactive Scrollable Grid Table — shrinks cleanly when IME opens so buttons never overlap
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(min = 120.dp, max = 300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, metaColor.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Column {
                            gridRows.forEachIndexed { rIdx, rowCells ->
                                val isHeader = rIdx == 0
                                Row(
                                    modifier = Modifier
                                        .height(IntrinsicSize.Max)
                                        .background(
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
                                                .width(124.dp)
                                                .fillMaxHeight()
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
                                                    fontFamily = LocalAppFontOption.current.fontFamily,
                                                    fontSynthesis = FontSynthesis.All,
                                                    fontSize = if (isHeader) 13.sp else 12.5.sp,
                                                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isHeader) accentColor else textColor
                                                ),
                                                cursorBrush = SolidColor(accentColor),
                                                decorationBox = { inner ->
                                                    Box(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        contentAlignment = Alignment.CenterStart
                                                    ) {
                                                        if (cellValue.isEmpty()) {
                                                            Text(
                                                                text = if (isHeader) {
                                                                    if (isArabic) "رأس ${cIdx + 1}" else "Header ${cIdx + 1}"
                                                                } else {
                                                                    if (isArabic) "بيانات..." else "Value..."
                                                                },
                                                                fontSize = 12.sp,
                                                                color = metaColor.copy(alpha = 0.45f),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        inner()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Action Buttons — compact padding & non-wrapping labels so 3 buttons never collide
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (initialTable != null && onDeleteTable != null) {
                            OutlinedButton(
                                onClick = onDeleteTable,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
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
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text(
                                text = if (isArabic) "إلغاء" else "Cancel",
                                color = metaColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = { onConfirmTable(gridRows.map { it.toList() }) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
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
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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

data class SegmentedEditResult(
    val updatedPageText: String,
    val targetSegmentIndex: Int,
    val updatedSelection: TextRange,
    val updatedPendingStyles: Set<RichFormatAction>
)

fun resolveActiveTextSegment(
    segments: List<EditorPageSegment>,
    preferredSegmentIndex: Int
): Pair<Int, EditorPageSegment.TextSegment> {
    val preferred = segments.getOrNull(preferredSegmentIndex) as? EditorPageSegment.TextSegment
    if (preferred != null) return preferredSegmentIndex to preferred
    val lastTextIdx = segments.indexOfLast { it is EditorPageSegment.TextSegment }
    if (lastTextIdx >= 0) {
        return lastTextIdx to (segments[lastTextIdx] as EditorPageSegment.TextSegment)
    }
    return 0 to EditorPageSegment.TextSegment(0, 0, "")
}

fun applyActionToSegmentedPage(
    pageText: String,
    preferredSegmentIndex: Int,
    segmentSelections: Map<Int, TextRange>,
    segmentCompositions: Map<Int, TextRange?>,
    pendingStyles: Set<RichFormatAction>,
    action: RichFormatAction
): SegmentedEditResult {
    val segments = RichTextCodec.splitPageIntoEditorSegments(pageText)
    val (segIdx, seg) = resolveActiveTextSegment(segments, preferredSegmentIndex)
    val localSel = (segmentSelections[segIdx] ?: TextRange(seg.text.length)).let {
        TextRange(it.start.coerceIn(0, seg.text.length), it.end.coerceIn(0, seg.text.length))
    }
    val localComp = segmentCompositions[segIdx]?.let { comp ->
        if (comp.start in 0..seg.text.length && comp.end in 0..seg.text.length) comp else null
    }
    val curVal = TextFieldValue(seg.text, localSel, localComp)
    val outcome = RichTextCodec.toggleFormatForEditor(curVal, action, pendingStyles)
    val hasTableBefore = segIdx > 0 && segments.getOrNull(segIdx - 1) !is EditorPageSegment.TextSegment
    val hasTableAfter = segIdx < segments.lastIndex && segments.getOrNull(segIdx + 1) !is EditorPageSegment.TextSegment
    val updatedPageText = RichTextCodec.replaceSegmentInPage(
        rawText = pageText,
        segStart = seg.startOffset,
        segEnd = seg.endOffset,
        newSegmentText = outcome.fieldValue.text,
        hasTableBefore = hasTableBefore,
        hasTableAfter = hasTableAfter
    )
    return SegmentedEditResult(
        updatedPageText = updatedPageText,
        targetSegmentIndex = segIdx,
        updatedSelection = outcome.fieldValue.selection,
        updatedPendingStyles = outcome.pendingStyles
    )
}

fun insertSnippetInSegmentedPage(
    pageText: String,
    preferredSegmentIndex: Int,
    segmentSelections: Map<Int, TextRange>,
    snippet: String,
    ensureNewLinePrefix: Boolean
): SegmentedEditResult {
    val segments = RichTextCodec.splitPageIntoEditorSegments(pageText)
    val (segIdx, seg) = resolveActiveTextSegment(segments, preferredSegmentIndex)
    val localSel = (segmentSelections[segIdx] ?: TextRange(seg.text.length)).let {
        TextRange(it.start.coerceIn(0, seg.text.length), it.end.coerceIn(0, seg.text.length))
    }
    val nextSegVal = RichTextCodec.insertSnippetAtSelection(
        value = TextFieldValue(seg.text, localSel),
        snippet = snippet,
        ensureNewLinePrefix = ensureNewLinePrefix
    )
    val hasTableBefore = segIdx > 0 && segments.getOrNull(segIdx - 1) !is EditorPageSegment.TextSegment
    val hasTableAfter = segIdx < segments.lastIndex && segments.getOrNull(segIdx + 1) !is EditorPageSegment.TextSegment
    val updatedPageText = RichTextCodec.replaceSegmentInPage(
        rawText = pageText,
        segStart = seg.startOffset,
        segEnd = seg.endOffset,
        newSegmentText = nextSegVal.text,
        hasTableBefore = hasTableBefore,
        hasTableAfter = hasTableAfter
    )
    return SegmentedEditResult(
        updatedPageText = updatedPageText,
        targetSegmentIndex = segIdx,
        updatedSelection = nextSegVal.selection,
        updatedPendingStyles = emptySet()
    )
}

fun insertTableInSegmentedPage(
    pageText: String,
    preferredSegmentIndex: Int,
    segmentSelections: Map<Int, TextRange>,
    tableMarkdown: String
): String {
    val segments = RichTextCodec.splitPageIntoEditorSegments(pageText)
    val (segIdx, seg) = resolveActiveTextSegment(segments, preferredSegmentIndex)
    val localSel = (segmentSelections[segIdx] ?: TextRange(seg.text.length)).let {
        TextRange(it.start.coerceIn(0, seg.text.length), it.end.coerceIn(0, seg.text.length))
    }
    val nextSegVal = RichTextCodec.insertTableAtSelection(seg.text, localSel, tableMarkdown)
    val hasTableBefore = segIdx > 0 && segments.getOrNull(segIdx - 1) !is EditorPageSegment.TextSegment
    val hasTableAfter = segIdx < segments.lastIndex && segments.getOrNull(segIdx + 1) !is EditorPageSegment.TextSegment
    return RichTextCodec.replaceSegmentInPage(
        rawText = pageText,
        segStart = seg.startOffset,
        segEnd = seg.endOffset,
        newSegmentText = nextSegVal.text,
        hasTableBefore = hasTableBefore,
        hasTableAfter = hasTableAfter
    )
}

/**
 * Segment-aware rich editor that splits raw Markdown Pipe Tables out of BasicTextField
 * so tables render purely as sleek interactive visual cards with ZERO raw `|` or `---` symbols.
 */
@Composable
fun SegmentedRichPageEditor(
    pageText: String,
    activeSegmentIndex: Int,
    segmentSelections: Map<Int, TextRange>,
    segmentCompositions: Map<Int, TextRange?>,
    pendingInlineStyles: Set<RichFormatAction>,
    richVisualTransformation: VisualTransformation,
    fontSizeSp: Float,
    minHeight: Dp,
    placeholderText: String,
    fieldTestTag: String,
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    textColor: Color,
    bodyColor: Color,
    metaColor: Color,
    onSegmentTextChanged: (
        newPageText: String,
        segmentIndex: Int,
        newSegSelection: TextRange,
        newSegComposition: TextRange?,
        nextPendingStyles: Set<RichFormatAction>
    ) -> Unit,
    onSegmentFocused: (Int) -> Unit,
    onEditTable: (MarkdownTableData) -> Unit,
    onDeleteTable: (MarkdownTableData) -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    onDeleteImage: ((EditorPageSegment.ImageSegment) -> Unit)? = null
) {
    val segments = remember(pageText) { RichTextCodec.splitPageIntoEditorSegments(pageText) }
    val hasTables = segments.any { it !is EditorPageSegment.TextSegment }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        segments.forEachIndexed { segIdx, seg ->
            when (seg) {
                is EditorPageSegment.TextSegment -> {
                    val hasTableBefore = segIdx > 0 && segments[segIdx - 1] !is EditorPageSegment.TextSegment
                    val hasTableAfter = segIdx < segments.lastIndex && segments[segIdx + 1] !is EditorPageSegment.TextSegment
                    val localSel = (segmentSelections[segIdx] ?: TextRange(seg.text.length)).let {
                        TextRange(it.start.coerceIn(0, seg.text.length), it.end.coerceIn(0, seg.text.length))
                    }
                    val localComp = segmentCompositions[segIdx]?.let { comp ->
                        if (comp.start in 0..seg.text.length && comp.end in 0..seg.text.length) comp else null
                    }
                    val fieldValue = TextFieldValue(
                        text = seg.text,
                        selection = localSel,
                        composition = localComp
                    )
                    val segMinHeight = when {
                        !hasTables -> minHeight
                        segIdx == segments.lastIndex -> 96.dp
                        else -> 32.dp
                    }
                    val segPlaceholder = when {
                        !hasTables -> placeholderText
                        hasTableBefore -> if (isArabic) "أكمل الكتابة هنا..." else "Continue writing here..."
                        else -> placeholderText
                    }

                    val activeFontFamily = LocalAppFontOption.current.fontFamily
                    var fieldLayoutResult by remember(segIdx) { mutableStateOf<TextLayoutResult?>(null) }
                    val fieldOverlay = rememberObliqueOverlayState(
                        layoutResult = fieldLayoutResult,
                        defaultTextColor = bodyColor
                    )

                    BasicTextField(
                        value = fieldValue,
                        onValueChange = { updatedVal ->
                            val activePending = if (segIdx == activeSegmentIndex) pendingInlineStyles else emptySet()
                            val outcome = RichTextCodec.applyPendingStylesOnEdit(
                                prevText = seg.text,
                                nextVal = updatedVal,
                                pendingStyles = activePending
                            )
                            val updatedPage = RichTextCodec.replaceSegmentInPage(
                                rawText = pageText,
                                segStart = seg.startOffset,
                                segEnd = seg.endOffset,
                                newSegmentText = outcome.fieldValue.text,
                                hasTableBefore = hasTableBefore,
                                hasTableAfter = hasTableAfter
                            )
                            onSegmentTextChanged(
                                updatedPage,
                                segIdx,
                                outcome.fieldValue.selection,
                                outcome.fieldValue.composition,
                                outcome.pendingStyles
                            )
                        },
                        visualTransformation = richVisualTransformation,
                        onTextLayout = { fieldLayoutResult = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = segMinHeight)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) onSegmentFocused(segIdx)
                            }
                            .testTag(if (segIdx == 0) fieldTestTag else "${fieldTestTag}_seg_$segIdx"),
                        textStyle = TextStyle(
                            fontFamily = activeFontFamily,
                            fontSynthesis = FontSynthesis.All,
                            fontSize = fontSizeSp.sp,
                            color = bodyColor,
                            lineHeight = (fontSizeSp * 1.55f).sp,
                            textAlign = textAlign
                        ),
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawObliqueOverlay(fieldOverlay)
                            ) {
                                if (seg.text.isEmpty() && (!hasTables || segIdx == segments.lastIndex || segIdx == 0)) {
                                    Text(
                                        text = segPlaceholder,
                                        modifier = Modifier.fillMaxWidth(),
                                        style = TextStyle(
                                            fontFamily = activeFontFamily,
                                            fontSynthesis = FontSynthesis.All,
                                            fontSize = fontSizeSp.sp,
                                            color = metaColor.copy(alpha = 0.56f),
                                            lineHeight = (fontSizeSp * 1.55f).sp,
                                            textAlign = textAlign
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                is EditorPageSegment.TableSegment -> {
                    val table = seg.table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f))
                            .border(1.dp, accentColor.copy(alpha = 0.34f), RoundedCornerShape(16.dp))
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
                                        "جدول ${seg.index + 1} (${table.rowCount}×${table.colCount})"
                                    } else {
                                        "Table ${seg.index + 1} (${table.rowCount}×${table.colCount})"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
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
                                        text = if (isArabic) "تعديل" else "Edit",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.14f))
                                        .clickable { onDeleteTable(table) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = if (isArabic) "حذف الجدول" else "Delete Table",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
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

                is EditorPageSegment.ImageSegment -> {
                    InlineMarkdownImageCard(
                        alt = seg.alt,
                        path = seg.path,
                        isDark = isDark,
                        isArabic = isArabic,
                        accentColor = accentColor,
                        metaColor = metaColor,
                        onDelete = {
                            if (onDeleteImage != null) {
                                onDeleteImage(seg)
                            } else {
                                val updated = RichTextCodec.deleteImageAtRange(
                                    pageText,
                                    seg.startOffset,
                                    seg.endOffset
                                )
                                onSegmentTextChanged(updated, 0, TextRange.Zero, null, emptySet())
                            }
                        }
                    )
                }
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
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    val activeFontOption = LocalAppFontOption.current
    val activeFontFamily = activeFontOption.fontFamily
    val blocks = remember(rawText, accentColor, isDark, activeFontOption) {
        RichTextCodec.parseRichBlocks(rawText, accentColor, isDark, activeFontOption)
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
                    ObliqueAwareText(
                        text = block.text,
                        style = TextStyle(
                            fontFamily = activeFontFamily,
                            fontSynthesis = FontSynthesis.All,
                            fontSize = size.sp,
                            fontWeight = if (block.level == 1) FontWeight.Black else FontWeight.ExtraBold,
                            color = if (block.level == 3) accentColor else textColor,
                            lineHeight = (size * 1.32f).sp,
                            textAlign = textAlign
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
                        ObliqueAwareText(
                            text = block.text,
                            style = TextStyle(
                                fontFamily = activeFontFamily,
                                fontSynthesis = FontSynthesis.All,
                                fontSize = fontSizeSp.sp,
                                color = bodyColor,
                                lineHeight = (fontSizeSp * 1.55f).sp,
                                textAlign = textAlign
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
                        ObliqueAwareText(
                            text = block.text,
                            style = TextStyle(
                                fontFamily = activeFontFamily,
                                fontSynthesis = FontSynthesis.All,
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
                        ObliqueAwareText(
                            text = block.text,
                            style = TextStyle(
                                fontFamily = activeFontFamily,
                                fontSynthesis = FontSynthesis.All,
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
                        ObliqueAwareText(
                            text = block.text,
                            style = TextStyle(
                                fontFamily = activeFontFamily,
                                fontSynthesis = FontSynthesis.All,
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
                            ObliqueAwareText(
                                text = block.text,
                                style = TextStyle(
                                    fontFamily = activeFontFamily,
                                    fontSynthesis = FontSynthesis.All,
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
                            ObliqueAwareText(
                                text = block.text,
                                style = TextStyle(
                                    fontFamily = activeFontFamily,
                                    fontSynthesis = FontSynthesis.All,
                                    fontSize = fontSizeSp.sp,
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

                is RichBlock.Image -> {
                    InlineMarkdownImageCard(
                        alt = block.alt,
                        path = block.path,
                        isDark = isDark,
                        isArabic = isArabic,
                        accentColor = accentColor,
                        metaColor = metaColor,
                        onDelete = null
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineMarkdownImageCard(
    alt: String,
    path: String,
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    metaColor: Color,
    onDelete: (() -> Unit)? = null
) {
    val modelData = remember(path) {
        val trimmed = path.trim()
        if (trimmed.startsWith("/") || trimmed.startsWith("file:")) {
            val filePath = trimmed.removePrefix("file://").removePrefix("file:")
            File(filePath)
        } else {
            trimmed
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f))
            .border(1.dp, accentColor.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            SubcomposeAsyncImage(
                model = modelData,
                contentDescription = alt.ifBlank { if (isArabic) "صورة" else "Image" },
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 320.dp)
                    .clip(RoundedCornerShape(12.dp)),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.2.dp,
                            color = accentColor
                        )
                    }
                },
                error = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = metaColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isArabic) "تعذر تحميل الصورة" else "Image unavailable",
                            fontSize = 12.5.sp,
                            color = metaColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )

            if (onDelete != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A).copy(alpha = 0.78f))
                        .border(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.65f), CircleShape)
                        .clickable { onDelete() }
                        .testTag("editor_delete_image_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = if (isArabic) "حذف الصورة" else "Delete Image",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        val cleanAlt = alt.trim()
        if (cleanAlt.isNotEmpty() && cleanAlt != "Image") {
            Text(
                text = cleanAlt,
                fontSize = 11.5.sp,
                color = metaColor,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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
    val colWidths = remember(table) {
        val cols = table.colCount.coerceAtLeast(1)
        List(cols) { cIdx ->
            val maxChars = table.rows.maxOfOrNull { it.getOrElse(cIdx) { "" }.length } ?: 0
            (96 + (maxChars - 6).coerceAtLeast(0) * 6).coerceIn(96, 200).dp
        }
    }
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
                    modifier = Modifier
                        .height(IntrinsicSize.Max)
                        .background(
                            when {
                                isHeader -> accentColor.copy(alpha = if (isDark) 0.24f else 0.14f)
                                rIdx % 2 == 1 -> Color.Transparent
                                else -> if (isDark) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.025f)
                            }
                        )
                ) {
                    row.forEachIndexed { cIdx, cell ->
                        val colW = colWidths.getOrElse(cIdx) { 104.dp }
                        Box(
                            modifier = Modifier
                                .width(colW)
                                .fillMaxHeight()
                                .border(0.6.dp, metaColor.copy(alpha = 0.20f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val activeFontOption = LocalAppFontOption.current
                            ObliqueAwareText(
                                text = RichTextCodec.renderInlineRichAnnotatedString(
                                    rawLine = cell,
                                    accentColor = accentColor,
                                    isDark = isDark,
                                    fontOption = activeFontOption
                                ),
                                style = TextStyle(
                                    fontFamily = activeFontOption.fontFamily,
                                    fontSynthesis = FontSynthesis.All,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isHeader) accentColor else textColor
                                ),
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

private data class ObliqueOverlayState(
    val layoutResult: TextLayoutResult,
    val overlayParagraph: MultiParagraph,
    val obliqueRanges: List<AnnotatedString.Range<String>>
)

private fun buildObliqueOverlayAnnotatedString(
    primary: AnnotatedString,
    defaultTextColor: Color
): AnnotatedString? {
    val annotations = primary.getStringAnnotations(RichTextCodec.OBLIQUE_SPAN_TAG, 0, primary.length)
    if (annotations.isEmpty()) return null
    val builder = AnnotatedString.Builder(primary.text)
    builder.addStyle(
        SpanStyle(color = Color.Transparent, background = Color.Unspecified, textDecoration = null),
        0,
        primary.length
    )
    for (range in primary.spanStyles) {
        builder.addStyle(
            range.item.copy(
                color = Color.Transparent,
                background = Color.Unspecified,
                textDecoration = null
            ),
            range.start,
            range.end
        )
    }
    for (ann in annotations) {
        val visibleColor = RichTextCodec.decodeObliqueColor(ann.item, defaultTextColor)
        val combinedDeco = primary.spanStyles
            .filter { it.start < ann.end && it.end > ann.start && it.item.textDecoration != null }
            .mapNotNull { it.item.textDecoration }
            .takeIf { it.isNotEmpty() }
            ?.let { TextDecoration.combine(it) }
        builder.addStyle(
            SpanStyle(
                color = visibleColor,
                fontStyle = FontStyle.Normal,
                textDecoration = combinedDeco
            ),
            ann.start,
            ann.end
        )
    }
    return builder.toAnnotatedString()
}

@Composable
private fun rememberObliqueOverlayState(
    layoutResult: TextLayoutResult?,
    defaultTextColor: Color
): ObliqueOverlayState? {
    val primaryAnnotated = layoutResult?.layoutInput?.text ?: return null
    val overlayAnnotated = remember(primaryAnnotated, defaultTextColor) {
        buildObliqueOverlayAnnotatedString(primaryAnnotated, defaultTextColor)
    } ?: return null
    val obliqueRanges = remember(primaryAnnotated) {
        primaryAnnotated.getStringAnnotations(RichTextCodec.OBLIQUE_SPAN_TAG, 0, primaryAnnotated.length)
    }
    if (obliqueRanges.isEmpty()) return null

    return remember(overlayAnnotated, layoutResult, obliqueRanges) {
        val input = layoutResult.layoutInput
        val resolvedStyle = resolveDefaults(
            style = input.style.copy(color = Color.Transparent, fontStyle = FontStyle.Normal),
            direction = input.layoutDirection
        )
        val maxLines = if (input.softWrap) input.maxLines else 1
        val paragraph = MultiParagraph(
            annotatedString = overlayAnnotated,
            style = resolvedStyle,
            constraints = Constraints(maxWidth = layoutResult.size.width),
            density = input.density,
            fontFamilyResolver = input.fontFamilyResolver,
            placeholders = input.placeholders,
            maxLines = maxLines,
            ellipsis = input.overflow == TextOverflow.Ellipsis
        )
        ObliqueOverlayState(
            layoutResult = layoutResult,
            overlayParagraph = paragraph,
            obliqueRanges = obliqueRanges
        )
    }
}

private fun Modifier.drawObliqueOverlay(state: ObliqueOverlayState?): Modifier {
    if (state == null) return this
    return drawWithContent {
        drawContent()
        val lr = state.layoutResult
        val op = state.overlayParagraph
        val ranges = state.obliqueRanges
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val lastLine = lr.lineCount - 1
            for (line in 0 until lr.lineCount) {
                val lStart = lr.getLineStart(line)
                val lEnd = lr.getLineEnd(line, visibleEnd = false)
                val hasObliqueOnLine = ranges.any { it.start < lEnd && it.end > lStart }
                if (!hasObliqueOnLine) continue
                val top = if (line == 0) lr.getLineTop(line) - 8f else lr.getLineTop(line)
                val bottom = if (line == lastLine) lr.getLineBottom(line) + 8f else lr.getLineBottom(line)
                val baseline = lr.getLineBaseline(line)
                canvas.save()
                canvas.clipRect(-64f, top, size.width + 64f, bottom)
                native.translate(0f, baseline)
                native.skew(-0.25f, 0f)
                native.translate(0f, -baseline)
                op.paint(canvas)
                canvas.restore()
            }
        }
    }
}

@Composable
private fun ObliqueAwareText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    var layoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val overlayState = rememberObliqueOverlayState(
        layoutResult = layoutResult,
        defaultTextColor = style.color
    )
    Text(
        text = text,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { layoutResult = it },
        modifier = modifier.drawObliqueOverlay(overlayState)
    )
}
