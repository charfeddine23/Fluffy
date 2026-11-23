package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.data.model.GlassTint
import com.example.ui.glass.liquidGlass
import com.example.ui.theme.LocalAppThemePreset
import com.example.ui.theme.tr
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class RadialQuickAction(
    val icon: ImageVector,
    val label: String,
    val contentDescription: String,
    val tintColor: Color,
    val angleDeg: Float,
    val testTag: String,
    val onClick: () -> Unit
)

/**
 * Edge-flush Quarter-Circle Shape anchored strictly to the physical bottom-right (LTR)
 * or bottom-left (Arabic/RTL) screen corner.
 */
private fun quarterCircleCornerShape(isLeftCorner: Boolean): Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = minOf(w, h)
    if (isLeftCorner) {
        moveTo(0f, h)
        lineTo(0f, h - r)
        arcTo(
            rect = Rect(left = -r, top = h - r, right = r, bottom = h + r),
            startAngleDegrees = 270f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        close()
    } else {
        moveTo(w, h)
        lineTo(w - r, h)
        arcTo(
            rect = Rect(left = w - r, top = h - r, right = w + r, bottom = h + r),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        close()
    }
}

@Composable
fun FluffyBottomBar(
    isDark: Boolean,
    isArabic: Boolean,
    onNewNote: () -> Unit,
    onNewChecklist: () -> Unit,
    onNewVoiceNote: () -> Unit,
    onOpenScanner: () -> Unit = {},
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

    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = 340f
        ),
        label = "quarter_circle_expand_progress"
    )

    val plusInteractionSource = remember { MutableInteractionSource() }
    val isPlusPressed by plusInteractionSource.collectIsPressedAsState()
    val plusScale by animateFloatAsState(
        targetValue = if (!isExpanded && isPlusPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "floating_plus_scale"
    )

    val closeInteraction = remember { MutableInteractionSource() }
    val isClosePressed by closeInteraction.collectIsPressedAsState()
    val closeScale by animateFloatAsState(
        targetValue = if (isClosePressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "quarter_close_scale"
    )

    val navBarBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current

    val voiceLabel = tr("صوت", "Voix", "Voice", "Aɣra")
    val noteLabel = tr("ملاحظة", "Note", "Note", "Tira")
    val checklistLabel = tr("مهام", "Tâches", "Tasks", "Timahalin")
    val scanLabel = tr("مسح", "Scanner", "Scan", "Sken")

    val voiceDesc = tr("تسجيل صوتي", "Note vocale", "Voice Note", "Asekles n taɣect")
    val noteDesc = tr("ملاحظة جديدة", "Nouvelle note", "New Note", "Tira tamaynut")
    val checklistDesc = tr("قائمة مهام", "Liste", "New Checklist", "Tabdart n tmahalin")
    val scanDesc = tr("ماسح المستندات", "Scanner", "Doc Scanner", "Amsken n yifuyla")

    val radialActions = remember(
        isArabic,
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
            RadialQuickAction(
                icon = Icons.Default.Mic,
                label = voiceLabel,
                contentDescription = voiceDesc,
                tintColor = Color(0xFFF43F5E),
                angleDeg = 76f,
                testTag = "action_voice_note_btn",
                onClick = {
                    isExpanded = false
                    onNewVoiceNote()
                }
            ),
            RadialQuickAction(
                icon = Icons.Default.EditNote,
                label = noteLabel,
                contentDescription = noteDesc,
                tintColor = accentColor,
                angleDeg = 56.5f,
                testTag = "action_new_note_btn",
                onClick = {
                    isExpanded = false
                    onNewNote()
                }
            ),
            RadialQuickAction(
                icon = Icons.Default.Checklist,
                label = checklistLabel,
                contentDescription = checklistDesc,
                tintColor = Color(0xFF10B981),
                angleDeg = 37f,
                testTag = "action_checklist_btn",
                onClick = {
                    isExpanded = false
                    onNewChecklist()
                }
            ),
            RadialQuickAction(
                icon = Icons.Default.DocumentScanner,
                label = scanLabel,
                contentDescription = scanDesc,
                tintColor = Color(0xFF38BDF8),
                angleDeg = 17.5f,
                testTag = "action_doc_scanner_btn",
                onClick = {
                    isExpanded = false
                    onOpenScanner()
                }
            )
        )
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        // Compute responsive quarter-circle radius flush with the screen edges
        val baseMaxRadius = minOf(maxWidth * 0.72f, maxHeight * 0.56f, 246.dp)
        val totalSectorSize = (baseMaxRadius + navBarBottomDp * 0.65f).coerceAtLeast(210.dp)
        val orbitRadius = (baseMaxRadius * 0.74f).coerceIn(144.dp, 182.dp)
        val hubInsetFromSide = 50.dp
        val hubInsetAboveNav = 50.dp

        // Ultra-clear transparent outside dismiss layer so background remains 100% unobstructed
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(140))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isExpanded = false }
                    )
            )
        }

        // LTR layout ensures left/right physical corners match Arabic (left) vs English/French (right)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Expanded Edge-Flush 5% Translucent Quarter-Circle Menu
                if (expandProgress > 0.01f) {
                    val sectorShape = remember(isArabic) { quarterCircleCornerShape(isLeftCorner = isArabic) }
                    val animatedRadiusDp = totalSectorSize * expandProgress.coerceIn(0.05f, 1.04f)
                    val cornerAlignment = if (isArabic) Alignment.BottomStart else Alignment.BottomEnd

                    Box(
                        modifier = Modifier
                            .align(cornerAlignment)
                            .size(animatedRadiusDp)
                            .clip(sectorShape)
                            .testTag("morphing_action_dock")
                    ) {
                        val navBottomPx = with(density) { navBarBottomDp.toPx() }
                        val orbitRadiusPx = with(density) { orbitRadius.toPx() }
                        val hubSizeDp: Dp = 54.dp
                        val orbSizeDp: Dp = 46.dp
                        val hubCenterXFromEdgePx = with(density) { hubInsetFromSide.toPx() }
                        val hubCenterYFromBottomPx = with(density) { hubInsetAboveNav.toPx() } + navBottomPx

                        // 5% Transparency Optical Glass Canvas Artwork + Concentric Guides
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val radius = minOf(w, h)
                            val cornerX = if (isArabic) 0f else w
                            val cornerY = h

                            val startAngle = if (isArabic) 270f else 180f
                            val arcTopLeft = if (isArabic) {
                                Offset(-radius, h - radius)
                            } else {
                                Offset(w - radius, h - radius)
                            }
                            val arcSize = Size(radius * 2f, radius * 2f)

                            // Strictly 5% translucent crystal fill so underlying content remains crystal clear
                            val glassFillColor = if (isDark) {
                                Color.White.copy(alpha = 0.05f * expandProgress)
                            } else {
                                accentColor.copy(alpha = 0.05f * expandProgress)
                            }
                            drawArc(
                                color = glassFillColor,
                                startAngle = startAngle,
                                sweepAngle = 90f,
                                useCenter = true,
                                topLeft = arcTopLeft,
                                size = arcSize
                            )

                            // Ultra-subtle 5% prismatic rim bloom on outer arc
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.05f * expandProgress),
                                        secondaryColor.copy(alpha = 0.05f * expandProgress),
                                        tertiaryColor.copy(alpha = 0.05f * expandProgress),
                                        accentColor.copy(alpha = 0.05f * expandProgress)
                                    ),
                                    center = Offset(cornerX, cornerY)
                                ),
                                startAngle = startAngle,
                                sweepAngle = 90f,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Sharp refractive outer quarter-circle rim
                            drawArc(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.78f * expandProgress),
                                        Color.White.copy(alpha = (if (isDark) 0.55f else 0.72f) * expandProgress),
                                        secondaryColor.copy(alpha = 0.74f * expandProgress),
                                        tertiaryColor.copy(alpha = 0.68f * expandProgress)
                                    ),
                                    start = Offset(if (isArabic) 0f else w - radius, h - radius),
                                    end = Offset(if (isArabic) radius else w, h)
                                ),
                                startAngle = startAngle,
                                sweepAngle = 90f,
                                useCenter = false,
                                topLeft = Offset(arcTopLeft.x + 1.dp.toPx(), arcTopLeft.y + 1.dp.toPx()),
                                size = Size(arcSize.width - 2.dp.toPx(), arcSize.height - 2.dp.toPx()),
                                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Delicate inner dashed orbit track running cleanly along the 4 radial action orbs
                            val visualCenterY = h - navBottomPx
                            val visualOrbitR = orbitRadiusPx * expandProgress
                            val orbitRectTopLeft = Offset(
                                cornerX - visualOrbitR,
                                visualCenterY - visualOrbitR
                            )
                            val orbitRectSize = Size(visualOrbitR * 2f, visualOrbitR * 2f)
                            drawArc(
                                color = (if (isDark) Color.White else accentColor).copy(alpha = 0.22f * expandProgress),
                                startAngle = startAngle,
                                sweepAngle = 90f,
                                useCenter = false,
                                topLeft = orbitRectTopLeft,
                                size = orbitRectSize,
                                style = Stroke(
                                    width = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(
                                        intervals = floatArrayOf(5.dp.toPx(), 6.dp.toPx()),
                                        phase = 0f
                                    )
                                )
                            )

                            // Subtle radial spoke guides connecting the Cancel orb hub to each action orb
                            val hubCenter = Offset(
                                x = if (isArabic) hubCenterXFromEdgePx else w - hubCenterXFromEdgePx,
                                y = h - hubCenterYFromBottomPx
                            )
                            radialActions.forEach { action ->
                                val rad = action.angleDeg * (PI / 180.0)
                                val targetX = if (isArabic) {
                                    (visualOrbitR * cos(rad)).toFloat()
                                } else {
                                    w - (visualOrbitR * cos(rad)).toFloat()
                                }
                                val targetY = visualCenterY - (visualOrbitR * sin(rad)).toFloat()
                                val dir = Offset(targetX - hubCenter.x, targetY - hubCenter.y)
                                val dist = dir.getDistance().coerceAtLeast(1f)
                                val ux = dir.x / dist
                                val uy = dir.y / dist
                                val startSpoke = Offset(
                                    hubCenter.x + ux * 30.dp.toPx(),
                                    hubCenter.y + uy * 30.dp.toPx()
                                )
                                val endSpoke = Offset(
                                    targetX - ux * 26.dp.toPx(),
                                    targetY - uy * 26.dp.toPx()
                                )
                                if ((endSpoke - startSpoke).getDistance() > 8f) {
                                    drawLine(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.08f * expandProgress),
                                                action.tintColor.copy(alpha = 0.34f * expandProgress)
                                            ),
                                            start = startSpoke,
                                            end = endSpoke
                                        ),
                                        start = startSpoke,
                                        end = endSpoke,
                                        strokeWidth = 1.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }

                            // Crisp edge hairlines showing flush attachment to bottom & side screen borders
                            val edgeLineColor = (if (isDark) Color.White else accentColor).copy(alpha = 0.16f * expandProgress)
                            drawPath(
                                path = Path().apply {
                                    if (isArabic) {
                                        moveTo(1.dp.toPx(), h - radius)
                                        lineTo(1.dp.toPx(), h - 1.dp.toPx())
                                        lineTo(radius, h - 1.dp.toPx())
                                    } else {
                                        moveTo(w - radius, h - 1.dp.toPx())
                                        lineTo(w - 1.dp.toPx(), h - 1.dp.toPx())
                                        lineTo(w - 1.dp.toPx(), h - radius)
                                    }
                                },
                                color = edgeLineColor,
                                style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Positioned Radial Action Orbs + Cancel Orb inside the Quarter-Circle
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val boxWpx = with(density) { maxWidth.toPx() }
                            val boxHpx = with(density) { maxHeight.toPx() }
                            val orbSizePx = with(density) { orbSizeDp.toPx() }
                            val orbSlotWidthDp = 68.dp
                            val orbSlotHalfWpx = with(density) { (orbSlotWidthDp / 2f).toPx() }
                            val hubSizePx = with(density) { hubSizeDp.toPx() }
                            val navPx = with(density) { navBarBottomDp.toPx() }

                            // 4 Radial Action Buttons spaced cleanly along the 90-degree crystal arc
                            radialActions.forEachIndexed { idx, action ->
                                val stagger = ((expandProgress - idx * 0.06f) / (1f - idx * 0.06f)).coerceIn(0f, 1f)
                                val rad = action.angleDeg * (PI / 180.0)
                                val curR = orbitRadiusPx * expandProgress * (0.45f + 0.55f * stagger)
                                val centerX = if (isArabic) {
                                    (curR * cos(rad)).toFloat()
                                } else {
                                    boxWpx - (curR * cos(rad)).toFloat()
                                }
                                val centerY = (boxHpx - navPx) - (curR * sin(rad)).toFloat()

                                QuarterRadialOrb(
                                    action = action,
                                    isDark = isDark,
                                    orbSize = orbSizeDp,
                                    slotWidth = orbSlotWidthDp,
                                    progress = stagger,
                                    modifier = Modifier.offset {
                                        IntOffset(
                                            x = (centerX - orbSlotHalfWpx).roundToInt(),
                                            y = (centerY - orbSizePx / 2f).roundToInt()
                                        )
                                    }
                                )
                            }

                            // Cancel (X) Button nestled inside the inner corner of the quarter-circle
                            val closeCenterX = if (isArabic) {
                                hubCenterXFromEdgePx
                            } else {
                                boxWpx - hubCenterXFromEdgePx
                            }
                            val closeCenterY = boxHpx - hubCenterYFromBottomPx

                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = (closeCenterX - hubSizePx / 2f).roundToInt(),
                                            y = (closeCenterY - hubSizePx / 2f).roundToInt()
                                        )
                                    }
                                    .size(hubSizeDp)
                                    .minimumInteractiveComponentSize()
                                    .graphicsLayer {
                                        scaleX = closeScale * (0.72f + 0.28f * expandProgress)
                                        scaleY = closeScale * (0.72f + 0.28f * expandProgress)
                                        rotationZ = (1f - expandProgress) * -45f
                                        alpha = expandProgress
                                    }
                                    .testTag("action_collapse_dock_btn")
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark) Color.White.copy(alpha = 0.05f)
                                        else Color(0xFF0F172A).copy(alpha = 0.05f)
                                    )
                                    .border(
                                        width = 1.4.dp,
                                        brush = Brush.linearGradient(
                                            listOf(
                                                Color.White.copy(alpha = if (isDark) 0.55f else 0.72f),
                                                accentColor.copy(alpha = 0.75f)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        interactionSource = closeInteraction,
                                        indication = ripple(bounded = true, color = accentColor),
                                        onClick = { isExpanded = false }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = tr("إلغاء", "Annuler", "Cancel", "Sefsex"),
                                    tint = if (isDark) Color.White else Color(0xFF0F172A),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Collapsed Floating Plus Orb (smoothly shrinks/rotates out as quarter-circle opens)
                if (!isExpanded || expandProgress < 0.35f) {
                    val collapsedAlpha = (1f - expandProgress * 2.8f).coerceIn(0f, 1f)
                    val cornerAlign = if (isArabic) Alignment.BottomStart else Alignment.BottomEnd
                    Box(
                        modifier = Modifier
                            .align(cornerAlign)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 18.dp, vertical = 20.dp)
                            .size(62.dp)
                            .minimumInteractiveComponentSize()
                            .graphicsLayer {
                                val s = plusScale * (0.65f + 0.35f * collapsedAlpha)
                                scaleX = s
                                scaleY = s
                                rotationZ = expandProgress * 45f
                                alpha = collapsedAlpha
                            }
                            .then(
                                if (!isExpanded) Modifier.testTag("morphing_action_dock") else Modifier
                            )
                            .liquidGlass(
                                isDark = isDark,
                                tint = GlassTint.CYAN,
                                shape = CircleShape,
                                borderWidth = 1.5.dp,
                                customFill = accentColor.copy(alpha = if (isDark) 0.44f else 0.88f)
                            )
                            .testTag("floating_new_note_button")
                            .clip(CircleShape)
                            .clickable(
                                enabled = !isExpanded,
                                interactionSource = plusInteractionSource,
                                indication = ripple(bounded = true, color = Color.White),
                                onClick = { isExpanded = true }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = tr("إضافة ملاحظة", "Ajouter", "Add Note", "Rnu tira"),
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuarterRadialOrb(
    action: RadialQuickAction,
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
        label = "quarter_orb_press"
    )
    val labelColor = if (isDark) Color.White else Color(0xFF0F172A)
    val labelShadowColor = if (isDark) Color.Black.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.85f)

    Column(
        modifier = modifier
            .width(slotWidth)
            .graphicsLayer {
                val s = pressScale * (0.45f + 0.55f * progress)
                scaleX = s
                scaleY = s
                alpha = progress
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 5% translucent crystal orb with vivid prismatic rim
        Box(
            modifier = Modifier
                .size(orbSize)
                .minimumInteractiveComponentSize()
                .testTag(action.testTag)
                .clip(CircleShape)
                .background(action.tintColor.copy(alpha = 0.05f))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            action.tintColor.copy(alpha = 0.90f),
                            Color.White.copy(alpha = if (isDark) 0.50f else 0.75f),
                            action.tintColor.copy(alpha = 0.68f)
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = action.tintColor),
                    onClick = action.onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.contentDescription,
                tint = action.tintColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = action.label,
            maxLines = 1,
            style = TextStyle(
                fontSize = 10.sp,
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
