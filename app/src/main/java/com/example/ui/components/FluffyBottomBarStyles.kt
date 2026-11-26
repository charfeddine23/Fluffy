package com.example.ui.components

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlassTint
import com.example.ui.glass.liquidGlass
import com.example.ui.theme.LocalAppThemePreset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal data class FabActionItem(
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
internal fun quarterCircleCornerShape(isLeftCorner: Boolean): Shape = GenericShape { size, _ ->
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

/**
 * Style 0 (Original): Centered morphing Liquid Glass horizontal capsule dock.
 */
@Composable
internal fun OriginalCapsuleMenu(
    isExpanded: Boolean,
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    cancelLabel: String,
    addNoteLabel: String,
    actions: List<FabActionItem>,
    dragController: FabDragSelectionController,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
        label = "original_dock_expand"
    )
    val plusInteraction = remember { MutableInteractionSource() }
    val isPlusPressed by plusInteraction.collectIsPressedAsState()
    val plusScale by animateFloatAsState(
        targetValue = if (!isExpanded && isPlusPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "original_plus_scale"
    )

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isDark) 0.34f else 0.18f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCollapse
                    )
            )
        }

        // Morphing capsule anchored centered when expanded, bottom-corner when collapsed
        val cornerBias = if (isArabic) -1f else 1f
        val currentBias = cornerBias * (1f - expandProgress)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            val preset = LocalAppThemePreset.current
            val expandedSurface = if (isDark) preset.surface.copy(alpha = 1f) else Color.White
            val capsuleWidth = 62.dp + (264.dp * expandProgress)
            val capsuleColor = if (isExpanded) {
                expandedSurface
            } else {
                accentColor.copy(alpha = if (isDark) 0.44f else 0.88f)
            }
            val hoveredAction = if (dragController.isDragSessionActive) {
                actions.firstOrNull { it.testTag == dragController.hoveredActionTag }
            } else {
                null
            }

            AnimatedVisibility(
                visible = hoveredAction != null,
                enter = fadeIn(tween(110)),
                exit = fadeOut(tween(100)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            ) {
                hoveredAction?.let { selected ->
                    val pillFill = selected.tintColor
                        .copy(alpha = if (isDark) 0.22f else 0.12f)
                        .compositeOver(expandedSurface)
                    Row(
                        modifier = Modifier
                            .background(pillFill, RoundedCornerShape(20.dp))
                            .border(1.4.dp, selected.tintColor, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = selected.icon,
                            contentDescription = null,
                            tint = selected.tintColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = selected.contentDescription,
                            maxLines = 1,
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(
                        androidx.compose.ui.BiasAlignment(
                            horizontalBias = currentBias,
                            verticalBias = 1f
                        )
                    )
                    .width(capsuleWidth)
                    .height(64.dp)
                    .fabPressAndDragAnchor(
                        controller = dragController,
                        isExpanded = isExpanded,
                        expandProgress = expandProgress,
                        actions = actions,
                        hitSlopDp = 18.dp,
                        topExtraSlopDp = 34.dp,
                        onExpand = onExpand,
                        onCollapse = onCollapse
                    )
                    .graphicsLayer {
                        if (!isExpanded) {
                            scaleX = plusScale
                            scaleY = plusScale
                        }
                    }
                    .testTag("morphing_action_dock")
                    .then(
                        if (isExpanded) Modifier.background(expandedSurface, RoundedCornerShape(32.dp)) else Modifier
                    )
                    .liquidGlass(
                        isDark = isDark,
                        tint = if (isExpanded) GlassTint.FROST else GlassTint.CYAN,
                        shape = RoundedCornerShape(32.dp),
                        borderWidth = 1.4.dp,
                        customFill = capsuleColor
                    )
                    .clip(RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!isExpanded && expandProgress < 0.05f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .minimumInteractiveComponentSize()
                            .testTag("floating_new_note_button")
                            .clickable(
                                enabled = !isExpanded,
                                interactionSource = plusInteraction,
                                indication = ripple(bounded = true, color = Color.White),
                                onClick = onExpand
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = addNoteLabel,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                } else {
                    val activeHoveredTag = dragController.hoveredActionTag
                    val isAnyHovered = dragController.isDragSessionActive && activeHoveredTag != null
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        actions.forEach { action ->
                            HorizontalCapsuleOrbButton(
                                icon = action.icon,
                                contentDescription = action.contentDescription,
                                tintColor = action.tintColor,
                                isDark = isDark,
                                testTag = action.testTag,
                                isDragHovered = activeHoveredTag == action.testTag,
                                isDimmed = isAnyHovered && activeHoveredTag != action.testTag,
                                modifier = Modifier.fabDragTarget(dragController, action.testTag),
                                onClick = action.onClick
                            )
                        }
                        HorizontalCapsuleOrbButton(
                            icon = Icons.Default.Close,
                            contentDescription = cancelLabel,
                            tintColor = if (isDark) Color.White else Color(0xFF0F172A),
                            isDark = isDark,
                            testTag = "action_collapse_dock_btn",
                            isDragHovered = false,
                            isDimmed = false,
                            modifier = Modifier.fabCancelTarget(dragController, active = isExpanded),
                            onClick = onCollapse
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalCapsuleOrbButton(
    icon: ImageVector,
    contentDescription: String,
    tintColor: Color,
    isDark: Boolean,
    testTag: String,
    isDragHovered: Boolean = false,
    isDimmed: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            isDragHovered -> 1.22f
            pressed -> 0.86f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 420f),
        label = "capsule_orb_scale"
    )
    val preset = LocalAppThemePreset.current
    val baseOpaqueSurface = if (isDark) preset.surface.copy(alpha = 1f) else Color.White
    val orbFill = tintColor
        .copy(alpha = when {
            isDragHovered -> if (isDark) 0.36f else 0.24f
            isDark -> 0.22f
            else -> 0.14f
        })
        .compositeOver(baseOpaqueSurface)
    val orbBorder = if (isDragHovered) {
        tintColor
    } else {
        tintColor.copy(alpha = 0.55f).compositeOver(baseOpaqueSurface)
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag(testTag)
            .clip(CircleShape)
            .background(orbFill)
            .border(if (isDragHovered) 2.2.dp else 1.dp, orbBorder, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = tintColor),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tintColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Style 2 (New): Vertical Floating Glass Stack — sleek translucent pill cards rising
 * directly above the corner orb with crisp labels and zero background haze.
 */
@Composable
internal fun VerticalGlassStackMenu(
    isExpanded: Boolean,
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    cancelLabel: String,
    addNoteLabel: String,
    actions: List<FabActionItem>,
    dragController: FabDragSelectionController,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
        label = "vertical_stack_expand"
    )
    val preset = LocalAppThemePreset.current
    val cornerAlign = if (isArabic) Alignment.BottomStart else Alignment.BottomEnd
    val horizontalAlign = if (isArabic) Alignment.Start else Alignment.End

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(120))
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

        Column(
            modifier = Modifier
                .align(cornerAlign)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .then(if (isExpanded) Modifier.testTag("morphing_action_dock") else Modifier),
            horizontalAlignment = horizontalAlign,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isExpanded || expandProgress > 0.01f) {
                val activeHoveredTag = dragController.hoveredActionTag
                val isAnyHovered = dragController.isDragSessionActive && activeHoveredTag != null
                val ordered = actions.reversed()
                ordered.forEachIndexed { idx, action ->
                    val reverseIdx = (ordered.size - 1) - idx
                    val stagger = ((expandProgress - reverseIdx * 0.07f) / (1f - reverseIdx * 0.07f)).coerceIn(0f, 1f)
                    val isDragHovered = activeHoveredTag == action.testTag
                    val isDimmed = isAnyHovered && !isDragHovered
                    val itemInteraction = remember { MutableInteractionSource() }
                    val pressed by itemInteraction.collectIsPressedAsState()
                    val pressScale by animateFloatAsState(
                        targetValue = when {
                            isDragHovered -> 1.09f
                            pressed -> 0.94f
                            else -> 1f
                        },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 420f),
                        label = "stack_pill_press"
                    )
                    val baseCardFill = if (isDark) {
                        preset.surface.copy(alpha = 1f)
                    } else {
                        Color.White
                    }
                    val cardFill = if (isDragHovered) {
                        action.tintColor
                            .copy(alpha = if (isDark) 0.22f else 0.14f)
                            .compositeOver(baseCardFill)
                    } else {
                        baseCardFill
                    }
                    val iconBadgeFill = action.tintColor
                        .copy(alpha = if (isDragHovered) (if (isDark) 0.34f else 0.24f) else (if (isDark) 0.24f else 0.16f))
                        .compositeOver(cardFill)
                    val cardBorder = if (isDragHovered) {
                        action.tintColor
                    } else {
                        action.tintColor
                            .copy(alpha = 0.58f)
                            .compositeOver(cardFill)
                    }

                    Row(
                        modifier = Modifier
                            .fabDragTarget(dragController, action.testTag)
                            .graphicsLayer {
                                val s = pressScale * (0.72f + 0.28f * stagger)
                                scaleX = s
                                scaleY = s
                                translationY = (1f - stagger) * 32.dp.toPx()
                                alpha = (stagger * 1.4f).coerceIn(0f, 1f)
                            }
                            .minimumInteractiveComponentSize()
                            .testTag(action.testTag)
                            .background(cardFill, RoundedCornerShape(24.dp))
                            .liquidGlass(
                                isDark = isDark,
                                tint = GlassTint.FROST,
                                shape = RoundedCornerShape(24.dp),
                                borderWidth = 1.2.dp,
                                customFill = cardFill
                            )
                            .border(
                                width = if (isDragHovered) 2.2.dp else 1.1.dp,
                                color = cardBorder,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(
                                interactionSource = itemInteraction,
                                indication = ripple(bounded = true, color = action.tintColor),
                                onClick = action.onClick
                            )
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = action.contentDescription,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(iconBadgeFill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.contentDescription,
                                tint = action.tintColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            CornerMorphFabAnchor(
                isExpanded = isExpanded,
                expandProgress = expandProgress,
                isDark = isDark,
                accentColor = accentColor,
                addNoteLabel = addNoteLabel,
                cancelLabel = cancelLabel,
                actions = actions,
                dragController = dragController,
                onExpand = onExpand,
                onCollapse = onCollapse
            )
        }
    }
}

/**
 * Style 3 (New): Frosted Radial Halo Dial — a sleek translucent corner quarter-disc
 * instrument dial with radial divider lines, glowing sweep rings, and 4 arc-aligned orbs.
 */
@Composable
internal fun OrbitalRadialDialMenu(
    isExpanded: Boolean,
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    secondaryColor: Color,
    cancelLabel: String,
    addNoteLabel: String,
    actions: List<FabActionItem>,
    dragController: FabDragSelectionController,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 330f),
        label = "orbital_dial_expand"
    )
    val navBarBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxDialRadius = minOf(maxWidth * 0.74f, maxHeight * 0.54f, 248.dp)
        val layoutScale = (maxDialRadius.value / 248f).coerceIn(0.84f, 1.08f)
        val sectorSize = (maxDialRadius + navBarBottomDp * 0.75f).coerceAtLeast(224.dp)

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(130)),
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
                if (isExpanded || expandProgress > 0.01f) {
                    val infiniteTransition = rememberInfiniteTransition(label = "orbital_dial_anim")
                    val sweepPhase by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 14000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "dial_sweep"
                    )
                    val cornerAlign = if (isArabic) Alignment.BottomStart else Alignment.BottomEnd
                    val sectorShape = remember(isArabic) { quarterCircleCornerShape(isLeftCorner = isArabic) }

                    Box(
                        modifier = Modifier
                            .align(cornerAlign)
                            .size(sectorSize)
                            .clip(sectorShape)
                            .testTag("morphing_action_dock")
                    ) {
                        val navPx = with(density) { navBarBottomDp.toPx() }
                        val hubOffsetSidePx = with(density) { (58f * layoutScale).dp.toPx() }
                        val hubOffsetBottomPx = with(density) { (58f * layoutScale).dp.toPx() } + navPx
                        val dialRadiusPx = with(density) { (172f * layoutScale).dp.toPx() }

                        // Subtle translucent frosted glass quarter-disc + radial divider rays + precision ring
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val pivot = Offset(if (isArabic) 0f else w, h - navPx * 0.35f)
                            val maxR = minOf(w, h) * expandProgress

                            // Ultra-light frosted wedge fill
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = if (isDark) 0.22f else 0.18f),
                                        (if (isDark) Color(0xFF0B1120) else Color.White).copy(alpha = if (isDark) 0.56f else 0.72f),
                                        (if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0)).copy(alpha = if (isDark) 0.32f else 0.45f)
                                    ),
                                    center = pivot,
                                    radius = maxR.coerceAtLeast(1f)
                                ),
                                radius = maxR,
                                center = pivot
                            )

                            // Concentric orbital gauge rings
                            listOf(0.50f, 0.86f).forEach { factor ->
                                val ringR = maxR * factor
                                drawCircle(
                                    color = accentColor.copy(alpha = 0.28f * expandProgress),
                                    radius = ringR,
                                    center = pivot,
                                    style = Stroke(width = 1.1.dp.toPx())
                                )
                            }

                            // 3 delicate radial sector divider lines separating the 4 action wedges
                            listOf(22.5, 45.0, 67.5).forEach { deg ->
                                val rad = deg * PI / 180.0
                                val dirX = if (isArabic) cos(rad).toFloat() else -cos(rad).toFloat()
                                val dirY = -sin(rad).toFloat()
                                val innerR = maxR * 0.32f
                                val outerR = maxR * 0.96f
                                drawLine(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.26f * expandProgress),
                                            Color.Transparent
                                        ),
                                        start = Offset(pivot.x + dirX * innerR, pivot.y + dirY * innerR),
                                        end = Offset(pivot.x + dirX * outerR, pivot.y + dirY * outerR)
                                    ),
                                    start = Offset(pivot.x + dirX * innerR, pivot.y + dirY * innerR),
                                    end = Offset(pivot.x + dirX * outerR, pivot.y + dirY * outerR),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // Rotating accent sweep highlight near the hub
                            val hubCenter = Offset(
                                x = if (isArabic) hubOffsetSidePx else w - hubOffsetSidePx,
                                y = h - hubOffsetBottomPx
                            )
                            val ringR = 30.dp.toPx() * expandProgress
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.9f * expandProgress),
                                        secondaryColor.copy(alpha = 0.4f * expandProgress),
                                        Color.Transparent
                                    ),
                                    center = hubCenter
                                ),
                                startAngle = sweepPhase,
                                sweepAngle = 150f,
                                useCenter = false,
                                topLeft = Offset(hubCenter.x - ringR, hubCenter.y - ringR),
                                size = Size(ringR * 2f, ringR * 2f),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val boxW = with(density) { maxWidth.toPx() }
                            val boxH = with(density) { maxHeight.toPx() }
                            val hubCenterX = if (isArabic) hubOffsetSidePx else boxW - hubOffsetSidePx
                            val hubCenterY = boxH - hubOffsetBottomPx
                            val slotW = 68.dp
                            val slotHalfWpx = with(density) { (slotW / 2f).toPx() }
                            val orbSize = 50.dp
                            val orbHalfPx = with(density) { (orbSize / 2f).toPx() }

                            val activeHoveredTag = dragController.hoveredActionTag
                            val isAnyHovered = dragController.isDragSessionActive && activeHoveredTag != null
                            val arcAnglesDeg = listOf(12.0, 36.0, 62.0, 84.0)
                            actions.forEachIndexed { index, action ->
                                val stagger = ((expandProgress - index * 0.05f) / (1f - index * 0.05f)).coerceIn(0f, 1f)
                                val rad = arcAnglesDeg[index] * PI / 180.0
                                val targetX = if (isArabic) {
                                    (dialRadiusPx * cos(rad)).toFloat() + with(density) { 26.dp.toPx() }
                                } else {
                                    boxW - ((dialRadiusPx * cos(rad)).toFloat() + with(density) { 26.dp.toPx() })
                                }
                                val targetY = (boxH - navPx) - (dialRadiusPx * sin(rad)).toFloat() - with(density) { 24.dp.toPx() }

                                val curX = hubCenterX + (targetX - hubCenterX) * stagger
                                val curY = hubCenterY + (targetY - hubCenterY) * stagger

                                DialSegmentButton(
                                    action = action,
                                    isDark = isDark,
                                    orbSize = orbSize,
                                    slotWidth = slotW,
                                    progress = stagger,
                                    isDragHovered = activeHoveredTag == action.testTag,
                                    isDimmed = isAnyHovered && activeHoveredTag != action.testTag,
                                    dragController = dragController,
                                    modifier = Modifier.offset {
                                        IntOffset(
                                            x = (curX - slotHalfWpx).roundToInt(),
                                            y = (curY - orbHalfPx).roundToInt()
                                        )
                                    }
                                )
                            }

                            // Corner X Cancel Button
                            val closeInteraction = remember { MutableInteractionSource() }
                            val closePressed by closeInteraction.collectIsPressedAsState()
                            val closeScale by animateFloatAsState(
                                targetValue = if (closePressed) 0.88f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "orbital_close_scale"
                            )
                            val closeSize = 54.dp
                            val closeHalfPx = with(density) { (closeSize / 2f).toPx() }

                            val preset = LocalAppThemePreset.current
                            val closeSurface = if (isDark) preset.surface.copy(alpha = 1f) else Color.White
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = (hubCenterX - closeHalfPx).roundToInt(),
                                            y = (hubCenterY - closeHalfPx).roundToInt()
                                        )
                                    }
                                    .size(closeSize)
                                    .minimumInteractiveComponentSize()
                                    .fabCancelTarget(dragController, active = isExpanded)
                                    .graphicsLayer {
                                        scaleX = closeScale * (0.65f + 0.35f * expandProgress)
                                        scaleY = closeScale * (0.65f + 0.35f * expandProgress)
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
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Collapsed Plus Button
                if (!isExpanded || expandProgress < 0.35f || dragController.isDragSessionActive) {
                    val collapsedAlpha = (1f - expandProgress * 2.8f).coerceIn(0f, 1f)
                    val cornerAlign = if (isArabic) Alignment.BottomStart else Alignment.BottomEnd
                    Box(
                        modifier = Modifier
                            .align(cornerAlign)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 18.dp, vertical = 20.dp)
                            .then(
                                if (!isExpanded) Modifier.testTag("morphing_action_dock") else Modifier
                            )
                            .fabPressAndDragAnchor(
                                controller = dragController,
                                isExpanded = isExpanded,
                                expandProgress = expandProgress,
                                actions = actions,
                                onExpand = onExpand,
                                onCollapse = onCollapse
                            )
                    ) {
                        FloatingPlusButtonCore(
                            isExpanded = isExpanded,
                            collapsedAlpha = collapsedAlpha,
                            expandProgress = expandProgress,
                            isDark = isDark,
                            accentColor = accentColor,
                            addNoteLabel = addNoteLabel,
                            onExpand = onExpand
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialSegmentButton(
    action: FabActionItem,
    isDark: Boolean,
    orbSize: androidx.compose.ui.unit.Dp,
    slotWidth: androidx.compose.ui.unit.Dp,
    progress: Float,
    isDragHovered: Boolean,
    isDimmed: Boolean,
    dragController: FabDragSelectionController,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = when {
            isDragHovered -> 1.22f
            pressed -> 0.88f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 420f),
        label = "dial_orb_press"
    )
    val preset = LocalAppThemePreset.current
    val baseOrbSurface = if (isDark) preset.surface.copy(alpha = 1f) else Color.White
    val orbSurface = if (isDragHovered) {
        action.tintColor
            .copy(alpha = if (isDark) 0.28f else 0.16f)
            .compositeOver(baseOrbSurface)
    } else {
        baseOrbSurface
    }
    val orbBorder = if (isDragHovered) {
        action.tintColor
    } else {
        action.tintColor.copy(alpha = 0.80f).compositeOver(baseOrbSurface)
    }
    val labelColor = if (isDark) Color.White else Color(0xFF0F172A)
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)

    Column(
        modifier = modifier
            .width(slotWidth)
            .graphicsLayer {
                val s = pressScale * (0.5f + 0.5f * progress)
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
                .fabDragTarget(dragController, action.testTag)
                .testTag(action.testTag)
                .clip(CircleShape)
                .background(orbSurface)
                .border(if (isDragHovered) 2.4.dp else 1.4.dp, orbBorder, CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(bounded = true, color = action.tintColor),
                    onClick = action.onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.contentDescription,
                tint = action.tintColor,
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = action.label,
            maxLines = 1,
            style = TextStyle(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                shadow = Shadow(color = shadowColor, offset = Offset(0f, 1f), blurRadius = 4f)
            )
        )
    }
}

/**
 * Style 4 (New): Crystal Bento Mosaic — a floating 2×2 kinetic prism tile grid
 * hovering cleanly above the bottom corner X capsule.
 */
@Composable
internal fun DiamondMosaicBentoMenu(
    isExpanded: Boolean,
    isDark: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    cancelLabel: String,
    addNoteLabel: String,
    actions: List<FabActionItem>,
    dragController: FabDragSelectionController,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 350f),
        label = "mosaic_bento_expand"
    )
    val preset = LocalAppThemePreset.current
    val cornerAlign = if (isArabic) Alignment.BottomStart else Alignment.BottomEnd
    val horizontalAlign = if (isArabic) Alignment.Start else Alignment.End

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(120))
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

        Column(
            modifier = Modifier
                .align(cornerAlign)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .then(if (isExpanded) Modifier.testTag("morphing_action_dock") else Modifier),
            horizontalAlignment = horizontalAlign,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isExpanded || expandProgress > 0.01f) {
                val activeHoveredTag = dragController.hoveredActionTag
                val isAnyHovered = dragController.isDragSessionActive && activeHoveredTag != null
                val topRow = listOfNotNull(actions.getOrNull(0), actions.getOrNull(1))
                val bottomRow = listOfNotNull(actions.getOrNull(2), actions.getOrNull(3))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = horizontalAlign
                ) {
                    listOf(topRow, bottomRow).forEachIndexed { rowIdx, rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowItems.forEachIndexed { colIdx, action ->
                                val idx = rowIdx * 2 + colIdx
                                val stagger = ((expandProgress - (3 - idx) * 0.05f) / (1f - (3 - idx) * 0.05f)).coerceIn(0f, 1f)
                                MosaicTileButton(
                                    action = action,
                                    isDark = isDark,
                                    surfaceColor = preset.surface,
                                    progress = stagger,
                                    isDragHovered = activeHoveredTag == action.testTag,
                                    isDimmed = isAnyHovered && activeHoveredTag != action.testTag,
                                    dragController = dragController
                                )
                            }
                        }
                    }
                }
            }

            CornerMorphFabAnchor(
                isExpanded = isExpanded,
                expandProgress = expandProgress,
                isDark = isDark,
                accentColor = accentColor,
                addNoteLabel = addNoteLabel,
                cancelLabel = cancelLabel,
                actions = actions,
                dragController = dragController,
                onExpand = onExpand,
                onCollapse = onCollapse
            )
        }
    }
}

@Composable
private fun MosaicTileButton(
    action: FabActionItem,
    isDark: Boolean,
    surfaceColor: Color,
    progress: Float,
    isDragHovered: Boolean,
    isDimmed: Boolean,
    dragController: FabDragSelectionController
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = when {
            isDragHovered -> 1.11f
            pressed -> 0.92f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 420f),
        label = "mosaic_tile_press"
    )
    val baseTileFill = if (isDark) surfaceColor.copy(alpha = 1f) else Color.White
    val tileFill = if (isDragHovered) {
        action.tintColor
            .copy(alpha = if (isDark) 0.24f else 0.14f)
            .compositeOver(baseTileFill)
    } else {
        baseTileFill
    }
    val iconBoxFill = action.tintColor
        .copy(alpha = if (isDragHovered) (if (isDark) 0.36f else 0.24f) else (if (isDark) 0.24f else 0.14f))
        .compositeOver(tileFill)
    val borderStart = if (isDragHovered) action.tintColor else action.tintColor.copy(alpha = 0.78f).compositeOver(tileFill)
    val borderEnd = if (isDragHovered) action.tintColor else action.tintColor.copy(alpha = 0.28f).compositeOver(tileFill)

    Column(
        modifier = Modifier
            .width(88.dp)
            .height(74.dp)
            .minimumInteractiveComponentSize()
            .fabDragTarget(dragController, action.testTag)
            .graphicsLayer {
                val s = pressScale * (0.58f + 0.42f * progress)
                scaleX = s
                scaleY = s
                translationY = (1f - progress) * 24.dp.toPx()
                alpha = (progress * 1.4f).coerceIn(0f, 1f)
            }
            .testTag(action.testTag)
            .background(tileFill, RoundedCornerShape(22.dp))
            .liquidGlass(
                isDark = isDark,
                tint = GlassTint.FROST,
                shape = RoundedCornerShape(22.dp),
                borderWidth = 1.2.dp,
                customFill = tileFill
            )
            .border(
                width = if (isDragHovered) 2.2.dp else 1.2.dp,
                brush = Brush.linearGradient(listOf(borderStart, borderEnd)),
                shape = RoundedCornerShape(22.dp)
            )
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = action.tintColor),
                onClick = action.onClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBoxFill),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.contentDescription,
                tint = action.tintColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = action.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
        )
    }
}

@Composable
private fun CornerMorphFabAnchor(
    isExpanded: Boolean,
    expandProgress: Float,
    isDark: Boolean,
    accentColor: Color,
    addNoteLabel: String,
    cancelLabel: String,
    actions: List<FabActionItem>,
    dragController: FabDragSelectionController,
    onExpand: () -> Unit,
    onCollapse: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.89f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "corner_anchor_scale"
    )
    val preset = LocalAppThemePreset.current
    val expandedFill = if (isDark) preset.surface.copy(alpha = 1f) else Color.White
    val fillColor = if (isExpanded) {
        expandedFill
    } else {
        accentColor.copy(alpha = if (isDark) 0.44f else 0.88f)
    }

    Box(
        modifier = Modifier
            .size(62.dp)
            .minimumInteractiveComponentSize()
            .fabCancelTarget(dragController, active = isExpanded)
            .fabPressAndDragAnchor(
                controller = dragController,
                isExpanded = isExpanded,
                expandProgress = expandProgress,
                actions = actions,
                onExpand = onExpand,
                onCollapse = onCollapse
            )
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                rotationZ = expandProgress * 45f
            }
            .then(
                if (isExpanded) Modifier.background(expandedFill, CircleShape) else Modifier
            )
            .liquidGlass(
                isDark = isDark,
                tint = if (isExpanded) GlassTint.FROST else GlassTint.CYAN,
                shape = CircleShape,
                borderWidth = 1.5.dp,
                customFill = fillColor
            )
            .testTag(if (isExpanded) "action_collapse_dock_btn" else "floating_new_note_button")
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = Color.White),
                onClick = { if (isExpanded) onCollapse() else onExpand() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = if (isExpanded) cancelLabel else addNoteLabel,
            tint = if (isExpanded) (if (isDark) Color.White else Color(0xFF0F172A)) else Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
internal fun FloatingPlusButtonCore(
    isExpanded: Boolean,
    collapsedAlpha: Float,
    expandProgress: Float,
    isDark: Boolean,
    accentColor: Color,
    addNoteLabel: String,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    Box(
        modifier = modifier
            .size(62.dp)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                val s = plusScale * (0.65f + 0.35f * collapsedAlpha)
                scaleX = s
                scaleY = s
                rotationZ = expandProgress * 45f
                alpha = collapsedAlpha
            }
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
                onClick = onExpand
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = addNoteLabel,
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}
