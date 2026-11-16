package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.glass.darkLiquidGlass

@Composable
fun FluffyBottomBar(
    isDark: Boolean,
    isArabic: Boolean,
    onNewNote: () -> Unit,
    onNewChecklist: () -> Unit,
    onNewVoiceNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    if (isExpanded) {
        BackHandler { isExpanded = false }
    }

    // Dynamic horizontal bias: corner in collapsed, center in expanded
    val cornerBias = if (isArabic) -0.84f else 0.84f
    val targetBias = if (isExpanded) 0.0f else cornerBias

    val animatedBias by animateFloatAsState(
        targetValue = targetBias,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 380f),
        label = "container_horizontal_bias"
    )

    // Dynamic width: 64.dp circle -> 286.dp centered Dark Liquid Glass dock
    val targetWidth = if (isExpanded) 286.dp else 64.dp
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 380f),
        label = "container_width"
    )

    val plusInteractionSource = remember { MutableInteractionSource() }
    val isPlusPressed by plusInteractionSource.collectIsPressedAsState()
    val plusScale by animateFloatAsState(
        targetValue = if (!isExpanded && isPlusPressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "floating_plus_scale"
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Subtle scrim backdrop when expanded to catch outside clicks
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(160))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isDark) 0.36f else 0.20f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isExpanded = false }
                    )
            )
        }

        // LTR provider ensures -bias is consistently physical left and +bias is physical right
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                contentAlignment = BiasAlignment(animatedBias, 1.0f)
            ) {
                // Dark Liquid Glass Floating Plus Button -> Morphs smoothly into Dark Liquid Glass Action Capsule
                Box(
                    modifier = Modifier
                        .width(animatedWidth)
                        .height(64.dp)
                        .graphicsLayer {
                            scaleX = plusScale
                            scaleY = plusScale
                        }
                        .testTag("morphing_action_dock")
                        .darkLiquidGlass(
                            shape = RoundedCornerShape(32.dp),
                            elevation = 16.dp,
                            rimWidth = 1.4.dp,
                            baseAlpha = if (isExpanded) 0.62f else 0.48f,
                            isPressed = !isExpanded && isPlusPressed
                        )
                ) {
                    if (!isExpanded) {
                        // Collapsed state: Dark Liquid Glass Floating Plus Button
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("floating_new_note_button")
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = plusInteractionSource,
                                    indication = ripple(bounded = true, color = Color.White),
                                    onClick = { isExpanded = true }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = if (isArabic) "إضافة ملاحظة" else "Add Note",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    } else {
                        // Expanded state: 3 Dark Liquid Glass circular action lenses + Close orb
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon 1: Voice Note (Microphone)
                            HorizontalActionButton(
                                icon = Icons.Default.Mic,
                                tintColor = Color(0xFFF43F5E),
                                contentDescription = if (isArabic) "تسجيل صوتي" else "Voice Note",
                                testTag = "action_voice_note_btn",
                                onClick = {
                                    isExpanded = false
                                    onNewVoiceNote()
                                }
                            )

                            // Icon 2: New Text Note (Center, Prominent)
                            HorizontalActionButton(
                                icon = Icons.Default.EditNote,
                                tintColor = Color(0xFF38BDF8),
                                contentDescription = if (isArabic) "ملاحظة جديدة" else "New Note",
                                testTag = "action_new_note_btn",
                                onClick = {
                                    isExpanded = false
                                    onNewNote()
                                }
                            )

                            // Icon 3: Checklist
                            HorizontalActionButton(
                                icon = Icons.Default.Checklist,
                                tintColor = Color(0xFF34D399),
                                contentDescription = if (isArabic) "قائمة مهام" else "New Checklist",
                                testTag = "action_checklist_btn",
                                onClick = {
                                    isExpanded = false
                                    onNewChecklist()
                                }
                            )

                            // Icon 4: Close / Collapse orb
                            val closeInteraction = remember { MutableInteractionSource() }
                            val isClosePressed by closeInteraction.collectIsPressedAsState()
                            val closeScale by animateFloatAsState(
                                targetValue = if (isClosePressed) 0.90f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "dock_close_scale"
                            )
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .graphicsLayer {
                                        scaleX = closeScale
                                        scaleY = closeScale
                                    }
                                    .testTag("action_collapse_dock_btn")
                                    .darkLiquidGlass(
                                        shape = CircleShape,
                                        elevation = 6.dp,
                                        rimWidth = 1.1.dp,
                                        baseAlpha = 0.44f,
                                        isPressed = isClosePressed
                                    )
                                    .clickable(
                                        interactionSource = closeInteraction,
                                        indication = ripple(bounded = true, color = Color.White),
                                        onClick = { isExpanded = false }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isArabic) "إغلاق" else "Close",
                                    tint = Color.White.copy(alpha = 0.90f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalActionButton(
    icon: ImageVector,
    tintColor: Color,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dock_item_scale"
    )
    Box(
        modifier = Modifier
            .size(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag(testTag)
            .darkLiquidGlass(
                shape = CircleShape,
                tintColor = tintColor,
                elevation = 8.dp,
                rimWidth = 1.15.dp,
                baseAlpha = 0.46f,
                isPressed = isPressed
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
