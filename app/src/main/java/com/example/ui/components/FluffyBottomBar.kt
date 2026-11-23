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
import kotlin.math.roundToInt

private data class ConstellationAction(
    val icon: ImageVector,
    val label: String,
    val contentDescription: String,
    val tintColor: Color,
    val fromSideDp: Float,
    val aboveNavDp: Float,
    val curveLift: Float,
    val testTag: String,
    val onClick: () -> Unit
)

/**
 * Edge-flush Quarter-Circle Sector anchored strictly to the bottom-right (LTR)
 * or bottom-left (Arabic/RTL) corner with no outer border stroke.
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
            dampingRatio = 0.76f,
            stiffness = 340f
        ),
        label = "quarter_constellation_expand"
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
        targetValue = if (isClosePressed) 0.88f else 1f,
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

    // Voice & Doc Scanner sit close around the central X hub; Note & Checklist crown the outer arc
    val constellationActions = remember(
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
            ConstellationAction(
                icon = Icons.Default.Mic,
                label = voiceLabel,
                contentDescription = voiceDesc,
                tintColor = Color(0xFFF43F5E),
                fromSideDp = 46f,
                aboveNavDp = 138f,
                curveLift = -14f,
                testTag = "action_voice_note_btn",
                onClick = {
                    isExpanded = false
                    onNewVoiceNote()
                }
            ),
            ConstellationAction(
                icon = Icons.Default.EditNote,
                label = noteLabel,
                contentDescription = noteDesc,
                tintColor = accentColor,
                fromSideDp = 104f,
                aboveNavDp = 154f,
                curveLift = -18f,
                testTag = "action_new_note_btn",
                onClick = {
                    isExpanded = false
                    onNewNote()
                }
            ),
            ConstellationAction(
                icon = Icons.Default.Checklist,
                label = checklistLabel,
                contentDescription = checklistDesc,
                tintColor = Color(0xFF10B981),
                fromSideDp = 154f,
                aboveNavDp = 104f,
                curveLift = 18f,
                testTag = "action_checklist_btn",
                onClick = {
                    isExpanded = false
                    onNewChecklist()
                }
            ),
            ConstellationAction(
                icon = Icons.Default.DocumentScanner,
                label = scanLabel,
                contentDescription = scanDesc,
                tintColor = Color(0xFF38BDF8),
                fromSideDp = 138f,
                aboveNavDp = 46f,
                curveLift = 14f,
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
        val baseMaxRadius = minOf(maxWidth * 0.68f, maxHeight * 0.52f, 224.dp)
        val layoutScale = (baseMaxRadius.value / 224f).coerceIn(0.82f, 1.08f)
        val totalSectorSize = (baseMaxRadius + navBarBottomDp * 0.72f).coerceAtLeast(204.dp)

        // Central X hub positioned cleanly in the visual heart of the quarter-circle
        val hubFromSideDp = 70f * layoutScale
        val hubAboveNavDp = 70f * layoutScale

        // Transparent full-screen outside tap listener -> zero background dimming
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
                        onClick = { isExpanded = false }
                    )
            )
        }

        // LTR guarantees physical bottom-left (Arabic) vs bottom-right (English/French) corner attachment
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
                        val hubSizeDp: Dp = 52.dp
                        val orbSizeDp: Dp = 46.dp
                        val hubCenterSidePx = with(density) { hubFromSideDp.dp.toPx() }
                        val hubCenterBottomPx = with(density) { hubAboveNavDp.dp.toPx() } + navBottomPx

                        // Frameless 5% Translucent Crystal Sector + Curved Luminous Energy Filaments (NO Outer Rim)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val maxRadius = minOf(w, h)
                            val radius = (maxRadius * expandProgress).coerceAtLeast(1f)
                            val cornerX = if (isArabic) 0f else w
                            val cornerY = h

                            // Borderless 5% feathered radial quarter-circle tint (NO outer stroke/border)
                            val baseCrystal = if (isDark) Color.White else accentColor
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0.0f to baseCrystal.copy(alpha = 0.05f * expandProgress),
                                    0.72f to baseCrystal.copy(alpha = 0.05f * expandProgress),
                                    0.96f to baseCrystal.copy(alpha = 0.025f * expandProgress),
                                    1.0f to Color.Transparent,
                                    center = Offset(cornerX, cornerY),
                                    radius = radius
                                ),
                                radius = radius,
                                center = Offset(cornerX, cornerY)
                            )

                            val hubCenter = Offset(
                                x = if (isArabic) hubCenterSidePx else w - hubCenterSidePx,
                                y = h - hubCenterBottomPx
                            )

                            // Curved Bezier light-filaments & constellation nodes radiating from the middle X core
                            constellationActions.forEach { action ->
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

                                // Soft translucent colored energy filament connecting X core -> tool droplet
                                drawPath(
                                    path = filamentPath,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.05f * expandProgress),
                                            action.tintColor.copy(alpha = 0.34f * expandProgress),
                                            action.tintColor.copy(alpha = 0.14f * expandProgress)
                                        ),
                                        start = hubCenter,
                                        end = endPoint
                                    ),
                                    style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Tiny glowing orbital starlight dot halfway along the curved filament
                                val midX = 0.25f * hubCenter.x + 0.5f * controlPt.x + 0.25f * endPoint.x
                                val midY = 0.25f * hubCenter.y + 0.5f * controlPt.y + 0.25f * endPoint.y
                                drawCircle(
                                    color = action.tintColor.copy(alpha = 0.65f * expandProgress),
                                    radius = (2.1.dp.toPx() * pulseBreath),
                                    center = Offset(midX, midY)
                                )

                                // Subtle 5% aura glow behind each orb
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            action.tintColor.copy(alpha = 0.05f * expandProgress),
                                            Color.Transparent
                                        ),
                                        center = endPoint,
                                        radius = 30.dp.toPx()
                                    ),
                                    radius = 30.dp.toPx(),
                                    center = endPoint
                                )
                            }

                            // Slowly rotating twin holographic arcs orbiting the central Cancel (X) core
                            val coreRingRadius = 31.dp.toPx() * expandProgress
                            val coreRectTopLeft = Offset(
                                hubCenter.x - coreRingRadius,
                                hubCenter.y - coreRingRadius
                            )
                            val coreRectSize = Size(coreRingRadius * 2f, coreRingRadius * 2f)
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.72f * expandProgress),
                                        secondaryColor.copy(alpha = 0.25f * expandProgress),
                                        Color.Transparent
                                    ),
                                    center = hubCenter
                                ),
                                startAngle = orbitSpinDeg,
                                sweepAngle = 115f,
                                useCenter = false,
                                topLeft = coreRectTopLeft,
                                size = coreRectSize,
                                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        tertiaryColor.copy(alpha = 0.65f * expandProgress),
                                        Color.White.copy(alpha = 0.30f * expandProgress),
                                        Color.Transparent
                                    ),
                                    center = hubCenter
                                ),
                                startAngle = orbitSpinDeg + 180f,
                                sweepAngle = 115f,
                                useCenter = false,
                                topLeft = coreRectTopLeft,
                                size = coreRectSize,
                                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Interactive Jewel Droplets & Middle Cancel Core
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val boxWpx = with(density) { maxWidth.toPx() }
                            val boxHpx = with(density) { maxHeight.toPx() }
                            val orbSizePx = with(density) { orbSizeDp.toPx() }
                            val slotWidthDp = 64.dp
                            val slotHalfWpx = with(density) { (slotWidthDp / 2f).toPx() }
                            val hubSizePx = with(density) { hubSizeDp.toPx() }
                            val navPx = with(density) { navBarBottomDp.toPx() }

                            val hubPxX = if (isArabic) hubCenterSidePx else boxWpx - hubCenterSidePx
                            val hubPxY = boxHpx - hubCenterBottomPx

                            // 4 Constellation Action Orbs blooming radially out from the middle X hub
                            constellationActions.forEachIndexed { idx, action ->
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

                            // Central Cancel (X) Orb nestled in the middle of the quarter-circle
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
                                        alpha = expandProgress
                                    }
                                    .testTag("action_collapse_dock_btn")
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark) Color.White.copy(alpha = 0.05f)
                                        else Color(0xFF0F172A).copy(alpha = 0.05f)
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
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                        }
                    }
                }

                // Collapsed Floating Plus Orb
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
private fun ConstellationDropletOrb(
    action: ConstellationAction,
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
    val labelColor = if (isDark) Color.White else Color(0xFF0F172A)
    val labelShadowColor = if (isDark) Color.Black.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.90f)

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
        // Floating 5% translucent crystal droplet with partial specular crescent highlight (no full heavy border)
        Box(
            modifier = Modifier
                .size(orbSize)
                .minimumInteractiveComponentSize()
                .testTag(action.testTag)
                .clip(CircleShape)
                .background(action.tintColor.copy(alpha = 0.05f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = action.tintColor),
                    onClick = action.onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.5.dp.toPx()
                val inset = strokeW / 2f
                val arcSize = Size(size.width - strokeW, size.height - strokeW)
                // Upper-left luminous specular crescent
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.White.copy(alpha = if (isDark) 0.58f else 0.82f),
                            action.tintColor.copy(alpha = 0.92f),
                            action.tintColor.copy(alpha = 0.15f)
                        )
                    ),
                    startAngle = 200f,
                    sweepAngle = 155f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                // Delicate lower complementary accent arc
                drawArc(
                    color = action.tintColor.copy(alpha = 0.45f),
                    startAngle = 35f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                )
            }
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
