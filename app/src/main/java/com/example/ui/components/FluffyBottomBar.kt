package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FabMenuStyle
import com.example.ui.theme.LocalAppThemePreset
import com.example.ui.theme.tr
import kotlin.math.roundToInt

@Composable
fun FluffyBottomBar(
    isDark: Boolean,
    isArabic: Boolean,
    onNewNote: () -> Unit,
    onNewChecklist: () -> Unit,
    onNewVoiceNote: () -> Unit,
    onOpenScanner: () -> Unit = {},
    fabMenuStyle: FabMenuStyle = FabMenuStyle.VERTICAL_STACK,
    modifier: Modifier = Modifier
) {
    val activePreset = LocalAppThemePreset.current
    val accentColor = activePreset.primary
    val secondaryColor = activePreset.secondary
    val tertiaryColor = activePreset.tertiary
    var isExpanded by remember { mutableStateOf(false) }

    if (isExpanded) {
        BackHandler { isExpanded = false }
    }

    val voiceLabel = tr("صوت", "Voix", "Voice", "Aɣra")
    val noteLabel = tr("ملاحظة", "Note", "Note", "Tira")
    val checklistLabel = tr("مهام", "Tâches", "Tasks", "Timahalin")
    val scanLabel = tr("مسح", "Scanner", "Scan", "Sken")

    val voiceDesc = tr("تسجيل صوتي", "Note vocale", "Voice Note", "Asekles n taɣect")
    val noteDesc = tr("ملاحظة جديدة", "Nouvelle note", "New Note", "Tira tamaynut")
    val checklistDesc = tr("قائمة مهام", "Liste", "New Checklist", "Tabdart n tmahalin")
    val scanDesc = tr("ماسح المستندات", "Scanner", "Doc Scanner", "Amsken n yifuyla")
    val cancelLabel = tr("إلغاء", "Annuler", "Cancel", "Sefsex")
    val addNoteLabel = tr("إضافة ملاحظة", "Ajouter", "Add Note", "Rnu tira")

    val actions = remember(
        accentColor,
        voiceLabel,
        noteLabel,
        checklistLabel,
        scanLabel,
        voiceDesc,
        noteDesc,
        checklistDesc,
        scanDesc
    ) {
        listOf(
            FabActionItem(
                icon = Icons.Default.Mic,
                label = voiceLabel,
                contentDescription = voiceDesc,
                tintColor = Color(0xFFF43F5E),
                fromSideDp = 48f,
                aboveNavDp = 148f,
                curveLift = -14f,
                testTag = "action_voice_note_btn",
                onClick = {
                    isExpanded = false
                    onNewVoiceNote()
                }
            ),
            FabActionItem(
                icon = Icons.Default.EditNote,
                label = noteLabel,
                contentDescription = noteDesc,
                tintColor = accentColor,
                fromSideDp = 114f,
                aboveNavDp = 170f,
                curveLift = -18f,
                testTag = "action_new_note_btn",
                onClick = {
                    isExpanded = false
                    onNewNote()
                }
            ),
            FabActionItem(
                icon = Icons.Default.Checklist,
                label = checklistLabel,
                contentDescription = checklistDesc,
                tintColor = Color(0xFF10B981),
                fromSideDp = 170f,
                aboveNavDp = 114f,
                curveLift = 18f,
                testTag = "action_checklist_btn",
                onClick = {
                    isExpanded = false
                    onNewChecklist()
                }
            ),
            FabActionItem(
                icon = Icons.Default.DocumentScanner,
                label = scanLabel,
                contentDescription = scanDesc,
                tintColor = Color(0xFF38BDF8),
                fromSideDp = 148f,
                aboveNavDp = 50f,
                curveLift = 14f,
                testTag = "action_doc_scanner_btn",
                onClick = {
                    isExpanded = false
                    onOpenScanner()
                }
            )
        )
    }

    when (fabMenuStyle) {
        FabMenuStyle.ORIGINAL -> OriginalCapsuleMenu(
            isExpanded = isExpanded,
            isDark = isDark,
            isArabic = isArabic,
            accentColor = accentColor,
            cancelLabel = cancelLabel,
            addNoteLabel = addNoteLabel,
            actions = actions,
            onExpand = { isExpanded = true },
            onCollapse = { isExpanded = false },
            modifier = modifier
        )

        FabMenuStyle.VERTICAL_STACK -> VerticalGlassStackMenu(
            isExpanded = isExpanded,
            isDark = isDark,
            isArabic = isArabic,
            accentColor = accentColor,
            cancelLabel = cancelLabel,
            addNoteLabel = addNoteLabel,
            actions = actions,
            onExpand = { isExpanded = true },
            onCollapse = { isExpanded = false },
            modifier = modifier
        )

        FabMenuStyle.ORBITAL_ARC -> OrbitalRadialDialMenu(
            isExpanded = isExpanded,
            isDark = isDark,
            isArabic = isArabic,
            accentColor = accentColor,
            secondaryColor = secondaryColor,
            cancelLabel = cancelLabel,
            addNoteLabel = addNoteLabel,
            actions = actions,
            onExpand = { isExpanded = true },
            onCollapse = { isExpanded = false },
            modifier = modifier
        )

        FabMenuStyle.DIAMOND_MOSAIC -> DiamondMosaicBentoMenu(
            isExpanded = isExpanded,
            isDark = isDark,
            isArabic = isArabic,
            accentColor = accentColor,
            cancelLabel = cancelLabel,
            addNoteLabel = addNoteLabel,
            actions = actions,
            onExpand = { isExpanded = true },
            onCollapse = { isExpanded = false },
            modifier = modifier
        )

        FabMenuStyle.CONSTELLATION -> ConstellationCornerMenu(
            isExpanded = isExpanded,
            isDark = isDark,
            isArabic = isArabic,
            accentColor = accentColor,
            secondaryColor = secondaryColor,
            tertiaryColor = tertiaryColor,
            cancelLabel = cancelLabel,
            addNoteLabel = addNoteLabel,
            actions = actions,
            onExpand = { isExpanded = true },
            onCollapse = { isExpanded = false },
            modifier = modifier
        )
    }
}

@Composable
private fun ConstellationCornerMenu(
    isExpanded: Boolean,
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    cancelLabel: String,
    addNoteLabel: String,
    actions: List<FabActionItem>,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = 340f
        ),
        label = "quarter_constellation_expand"
    )

    val closeInteraction = remember { MutableInteractionSource() }
    val isClosePressed by closeInteraction.collectIsPressedAsState()
    val closeScale by animateFloatAsState(
        targetValue = if (isClosePressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "quarter_close_scale"
    )

    val navBarBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val baseMaxRadius = minOf(maxWidth * 0.72f, maxHeight * 0.56f, 242.dp)
        val layoutScale = (baseMaxRadius.value / 242f).coerceIn(0.84f, 1.08f)
        val totalSectorSize = (baseMaxRadius + navBarBottomDp * 0.72f).coerceAtLeast(220.dp)

        val hubFromSideDp = 72f * layoutScale
        val hubAboveNavDp = 72f * layoutScale

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(130))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCollapse
                    )
            )
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (expandProgress > 0.01f) {
                    val infiniteTransition = rememberInfiniteTransition(label = "prism_constellation_anim")
                    val orbitSpinDeg by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 12000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "orbit_spin"
                    )
                    val pulseBreath by infiniteTransition.animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2400),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "constellation_breath"
                    )

                    val sectorShape = remember(isArabic) { quarterCircleCornerShape(isLeftCorner = isArabic) }
                    val cornerAlignment = if (isArabic) Alignment.BottomStart else Alignment.BottomEnd

                    Box(
                        modifier = Modifier
                            .align(cornerAlignment)
                            .size(totalSectorSize)
                            .clip(sectorShape)
                            .testTag("morphing_action_dock")
                    ) {
                        val navBottomPx = with(density) { navBarBottomDp.toPx() }
                        val hubSizeDp: Dp = 58.dp
                        val orbSizeDp: Dp = 56.dp
                        val hubCenterSidePx = with(density) { hubFromSideDp.dp.toPx() }
                        val hubCenterBottomPx = with(density) { hubAboveNavDp.dp.toPx() } + navBottomPx

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val hubCenter = Offset(
                                x = if (isArabic) hubCenterSidePx else w - hubCenterSidePx,
                                y = h - hubCenterBottomPx
                            )

                            actions.forEach { action ->
                                val targetX = if (isArabic) {
                                    with(density) { (action.fromSideDp * layoutScale).dp.toPx() }
                                } else {
                                    w - with(density) { (action.fromSideDp * layoutScale).dp.toPx() }
                                }
                                val targetY = h - (navBottomPx + with(density) { (action.aboveNavDp * layoutScale).dp.toPx() })

                                val endPoint = Offset(
                                    x = hubCenter.x + (targetX - hubCenter.x) * expandProgress,
                                    y = hubCenter.y + (targetY - hubCenter.y) * expandProgress
                                )

                                val dx = endPoint.x - hubCenter.x
                                val dy = endPoint.y - hubCenter.y
                                val len = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
                                val nx = -dy / len
                                val ny = dx / len
                                val liftPx = with(density) { action.curveLift.dp.toPx() } * (if (isArabic) -1f else 1f) * expandProgress
                                val controlPt = Offset(
                                    x = (hubCenter.x + endPoint.x) * 0.5f + nx * liftPx,
                                    y = (hubCenter.y + endPoint.y) * 0.5f + ny * liftPx
                                )

                                val filamentPath = Path().apply {
                                    moveTo(hubCenter.x, hubCenter.y)
                                    quadraticTo(controlPt.x, controlPt.y, endPoint.x, endPoint.y)
                                }

                                drawPath(
                                    path = filamentPath,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            action.tintColor.copy(alpha = 0.42f * expandProgress),
                                            action.tintColor.copy(alpha = 0.22f * expandProgress)
                                        ),
                                        start = hubCenter,
                                        end = endPoint
                                    ),
                                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                                )

                                val midX = 0.25f * hubCenter.x + 0.5f * controlPt.x + 0.25f * endPoint.x
                                val midY = 0.25f * hubCenter.y + 0.5f * controlPt.y + 0.25f * endPoint.y
                                drawCircle(
                                    color = action.tintColor.copy(alpha = 0.78f * expandProgress),
                                    radius = (2.4.dp.toPx() * pulseBreath),
                                    center = Offset(midX, midY)
                                )
                            }

                            val coreRingRadius = 34.dp.toPx() * expandProgress
                            val coreRectTopLeft = Offset(
                                hubCenter.x - coreRingRadius,
                                hubCenter.y - coreRingRadius
                            )
                            val coreRectSize = Size(coreRingRadius * 2f, coreRingRadius * 2f)
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.85f * expandProgress),
                                        secondaryColor.copy(alpha = 0.35f * expandProgress),
                                        Color.Transparent
                                    ),
                                    center = hubCenter
                                ),
                                startAngle = orbitSpinDeg,
                                sweepAngle = 120f,
                                useCenter = false,
                                topLeft = coreRectTopLeft,
                                size = coreRectSize,
                                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        tertiaryColor.copy(alpha = 0.80f * expandProgress),
                                        Color.White.copy(alpha = 0.42f * expandProgress),
                                        Color.Transparent
                                    ),
                                    center = hubCenter
                                ),
                                startAngle = orbitSpinDeg + 180f,
                                sweepAngle = 120f,
                                useCenter = false,
                                topLeft = coreRectTopLeft,
                                size = coreRectSize,
                                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val boxWpx = with(density) { maxWidth.toPx() }
                            val boxHpx = with(density) { maxHeight.toPx() }
                            val orbSizePx = with(density) { orbSizeDp.toPx() }
                            val slotWidthDp = 72.dp
                            val slotHalfWpx = with(density) { (slotWidthDp / 2f).toPx() }
                            val hubSizePx = with(density) { hubSizeDp.toPx() }
                            val navPx = with(density) { navBarBottomDp.toPx() }

                            val hubPxX = if (isArabic) hubCenterSidePx else boxWpx - hubCenterSidePx
                            val hubPxY = boxHpx - hubCenterBottomPx

                            actions.forEachIndexed { idx, action ->
                                val stagger = ((expandProgress - idx * 0.055f) / (1f - idx * 0.055f)).coerceIn(0f, 1f)
                                val targetCenterX = if (isArabic) {
                                    with(density) { (action.fromSideDp * layoutScale).dp.toPx() }
                                } else {
                                    boxWpx - with(density) { (action.fromSideDp * layoutScale).dp.toPx() }
                                }
                                val targetCenterY = boxHpx - (navPx + with(density) { (action.aboveNavDp * layoutScale).dp.toPx() })

                                val curX = hubPxX + (targetCenterX - hubPxX) * stagger
                                val curY = hubPxY + (targetCenterY - hubPxY) * stagger

                                ConstellationDropletOrb(
                                    action = action,
                                    isDark = isDark,
                                    orbSize = orbSizeDp,
                                    slotWidth = slotWidthDp,
                                    progress = stagger,
                                    modifier = Modifier.offset {
                                        IntOffset(
                                            x = (curX - slotHalfWpx).roundToInt(),
                                            y = (curY - orbSizePx / 2f).roundToInt()
                                        )
                                    }
                                )
                            }

                            val activePreset = LocalAppThemePreset.current
                            val closeSurface = if (isDark) activePreset.surface.copy(alpha = 1f) else Color.White
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = (hubPxX - hubSizePx / 2f).roundToInt(),
                                            y = (hubPxY - hubSizePx / 2f).roundToInt()
                                        )
                                    }
                                    .size(hubSizeDp)
                                    .minimumInteractiveComponentSize()
                                    .graphicsLayer {
                                        scaleX = closeScale * (0.70f + 0.30f * expandProgress)
                                        scaleY = closeScale * (0.70f + 0.30f * expandProgress)
                                        rotationZ = (1f - expandProgress) * -45f
                                        alpha = (expandProgress * 1.4f).coerceIn(0f, 1f)
                                    }
                                    .testTag("action_collapse_dock_btn")
                                    .clip(CircleShape)
                                    .background(closeSurface)
                                    .border(
                                        1.3.dp,
                                        accentColor.copy(alpha = 0.72f).compositeOver(closeSurface),
                                        CircleShape
                                    )
                                    .clickable(
                                        interactionSource = closeInteraction,
                                        indication = ripple(bounded = true, color = accentColor),
                                        onClick = onCollapse
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = cancelLabel,
                                    tint = if (isDark) Color.White else Color(0xFF0F172A),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }

                if (!isExpanded || expandProgress < 0.35f) {
                    val collapsedAlpha = (1f - expandProgress * 2.8f).coerceIn(0f, 1f)
                    val cornerAlign = if (isArabic) Alignment.BottomStart else Alignment.BottomEnd
                    FloatingPlusButtonCore(
                        isExpanded = isExpanded,
                        collapsedAlpha = collapsedAlpha,
                        expandProgress = expandProgress,
                        isDark = isDark,
                        accentColor = accentColor,
                        addNoteLabel = addNoteLabel,
                        onExpand = onExpand,
                        modifier = Modifier
                            .align(cornerAlign)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 18.dp, vertical = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConstellationDropletOrb(
    action: FabActionItem,
    isDark: Boolean,
    orbSize: Dp,
    slotWidth: Dp,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "constellation_orb_press"
    )
    val preset = LocalAppThemePreset.current
    val baseOpaqueSurface = if (isDark) preset.surface.copy(alpha = 1f) else Color.White
    val orbFill = action.tintColor.copy(alpha = if (isDark) 0.14f else 0.08f).compositeOver(baseOpaqueSurface)
    val labelColor = if (isDark) Color.White else Color(0xFF0F172A)
    val labelShadowColor = if (isDark) Color.Black.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.90f)

    Column(
        modifier = modifier
            .width(slotWidth)
            .graphicsLayer {
                val s = pressScale * (0.45f + 0.55f * progress)
                scaleX = s
                scaleY = s
                alpha = (progress * 1.4f).coerceIn(0f, 1f)
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(orbSize)
                .minimumInteractiveComponentSize()
                .testTag(action.testTag)
                .clip(CircleShape)
                .background(orbFill)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = action.tintColor),
                    onClick = action.onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val inset = strokeW / 2f
                val arcSize = Size(size.width - strokeW, size.height - strokeW)
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.White.copy(alpha = if (isDark) 0.68f else 0.88f),
                            action.tintColor.copy(alpha = 0.96f),
                            action.tintColor.copy(alpha = 0.22f)
                        )
                    ),
                    startAngle = 200f,
                    sweepAngle = 155f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                drawArc(
                    color = action.tintColor.copy(alpha = 0.55f),
                    startAngle = 35f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Icon(
                imageVector = action.icon,
                contentDescription = action.contentDescription,
                tint = action.tintColor,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = action.label,
            maxLines = 1,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                shadow = Shadow(
                    color = labelShadowColor,
                    offset = Offset(0f, 1f),
                    blurRadius = 4f
                )
            )
        )
    }
}
