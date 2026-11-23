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
import androidx.compose.foundation.layout.fillMaxWidth
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
            val capsuleWidth = 62.dp + (264.dp * expandProgress)
            val capsuleColor = if (isExpanded) {
                if (isDark) Color(0xFF0F172A).copy(alpha = 0.76f) else Color.White.copy(alpha = 0.86f)
            } else {
                accentColor.copy(alpha = if (isDark) 0.44f else 0.88f)
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
                    .graphicsLayer {
                        if (!isExpanded) {
                            scaleX = plusScale
                            scaleY = plusScale
                        }
                    }
                    .testTag("morphing_action_dock")
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
                if (!isExpanded && expandProgress < 0.25f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .minimumInteractiveComponentSize()
                            .testTag("floating_new_note_button")
                            .clickable(
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
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp)
                            .graphicsLayer {
                                alpha = ((expandProgress - 0.15f) / 0.85f).coerceIn(0f, 1f)
                            },
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
                                onClick = action.onClick
                            )
                        }
                        HorizontalCapsuleOrbButton(
                            icon = Icons.Default.Close,
                            contentDescription = cancelLabel,
                            tintColor = if (isDark) Color.White else Color(0xFF0F172A),
                            isDark = isDark,
                            testTag = "action_collapse_dock_btn",
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
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "capsule_orb_scale"
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag(testTag)
            .clip(CircleShape)
            .background(tintColor.copy(alpha = if (isDark) 0.18f else 0.14f))
            .border(1.dp, tintColor.copy(alpha = 0.42f), CircleShape)
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
            if (expandProgress > 0.02f) {
                val ordered = actions.reversed()
                ordered.forEachIndexed { idx, action ->
                    val reverseIdx = (ordered.size - 1) - idx
                    val stagger = ((expandProgress - reverseIdx * 0.07f) / (1f - reverseIdx * 0.07f)).coerceIn(0f, 1f)
                    val itemInteraction = remember { MutableInteractionSource() }
                    val pressed by itemInteraction.collectIsPressedAsState()
                    val pressScale by animateFloatAsState(
                        targetValue = if (pressed) 0.94f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "stack_pill_press"
                    )
                    val cardFill = if (isDark) {
                        preset.surface.copy(alpha = 0.84f)
                    } else {
                        Color.White.copy(alpha = 0.90f)
                    }

                    Row(
                        modifier = Modifier
                            .graphicsLayer {
                                val s = pressScale * (0.72f + 0.28f * stagger)
                                scaleX = s
                                scaleY = s
                                translationY = (1f - stagger) * 32.dp.toPx()
                                alpha = stagger
                            }
                            .minimumInteractiveComponentSize()
                            .testTag(action.testTag)
                            .liquidGlass(
                                isDark = isDark,
                                tint = GlassTint.FROST,
                                shape = RoundedCornerShape(24.dp),
                                borderWidth = 1.2.dp,
                                customFill = cardFill
                            )
                            .border(
                                width = 1.1.dp,
                                color = action.tintColor.copy(alpha = 0.48f),
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
                                .background(action.tintColor.copy(alpha = if (isDark) 0.22f else 0.16f)),
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
                if (expandProgress > 0.01f) {
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
                                    .graphicsLayer {
                                        scaleX = closeScale * (0.65f + 0.35f * expandProgress)
                                        scaleY = closeScale * (0.65f + 0.35f * expandProgress)
                                        rotationZ = (1f - expandProgress) * -45f
                                        alpha = expandProgress
                                    }
                                    .testTag("action_collapse_dock_btn")
                                    .clip(CircleShape)
                                    .background(
                                        (if (isDark) Color(0xFF0F172A) else Color.White).copy(alpha = 0.85f)
                                    )
                                    .border(1.3.dp, accentColor.copy(alpha = 0.65f), CircleShape)
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
private fun DialSegmentButton(
    action: FabActionItem,
    isDark: Boolean,
    orbSize: androidx.compose.ui.unit.Dp,
    slotWidth: androidx.compose.ui.unit.Dp,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dial_orb_press"
    )
    val labelColor = if (isDark) Color.White else Color(0xFF0F172A)
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)

    Column(
        modifier = modifier
            .width(slotWidth)
            .graphicsLayer {
                val s = pressScale * (0.5f + 0.5f * progress)
                scaleX = s
                scaleY = s
                alpha = progress
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(orbSize)
                .minimumInteractiveComponentSize()
                .testTag(action.testTag)
                .clip(CircleShape)
                .background(
                    (if (isDark) Color(0xFF0F172A) else Color.White).copy(alpha = if (isDark) 0.78f else 0.90f)
                )
                .border(1.4.dp, action.tintColor.copy(alpha = 0.72f), CircleShape)
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
            if (expandProgress > 0.02f) {
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
                                    progress = stagger
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
    progress: Float
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "mosaic_tile_press"
    )
    val tileFill = if (isDark) surfaceColor.copy(alpha = 0.86f) else Color.White.copy(alpha = 0.92f)

    Column(
        modifier = Modifier
            .width(88.dp)
            .height(74.dp)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                val s = pressScale * (0.58f + 0.42f * progress)
                scaleX = s
                scaleY = s
                translationY = (1f - progress) * 24.dp.toPx()
                alpha = progress
            }
            .testTag(action.testTag)
            .liquidGlass(
                isDark = isDark,
                tint = GlassTint.FROST,
                shape = RoundedCornerShape(22.dp),
                borderWidth = 1.2.dp,
                customFill = tileFill
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        action.tintColor.copy(alpha = 0.72f),
                        action.tintColor.copy(alpha = 0.22f)
                    )
                ),
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
                .background(action.tintColor.copy(alpha = if (isDark) 0.22f else 0.14f)),
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
    val fillColor = if (isExpanded) {
        (if (isDark) Color(0xFF0F172A) else Color.White).copy(alpha = 0.88f)
    } else {
        accentColor.copy(alpha = if (isDark) 0.44f else 0.88f)
    }

    Box(
        modifier = Modifier
            .size(62.dp)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                rotationZ = expandProgress * 45f
            }
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
