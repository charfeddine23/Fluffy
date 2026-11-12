package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.GlassTint
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass

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

    // Dynamic width: 62.dp circle -> 280.dp centered pill dock
    val targetWidth = if (isExpanded) 280.dp else 62.dp
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 380f),
        label = "container_width"
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
                    .background(Color.Black.copy(alpha = if (isDark) 0.35f else 0.18f))
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
                val containerFill = if (isDark) {
                    Color(0xFF0F172A).copy(alpha = 0.85f)
                } else {
                    Color(0xFFFFFFFF).copy(alpha = 0.85f)
                }

                // The SAME container that expands from corner to center
                LiquidGlassCard(
                    isDark = isDark,
                    tint = if (isExpanded) GlassTint.FROST else GlassTint.CYAN,
                    shape = RoundedCornerShape(31.dp),
                    borderWidth = 1.8.dp,
                    customFill = if (isExpanded) containerFill else {
                        if (isDark) Color(0xFF0284C7).copy(alpha = 0.55f) else Color(0xFF0EA5E9).copy(alpha = 0.40f)
                    },
                    modifier = Modifier
                        .width(animatedWidth)
                        .height(62.dp)
                        .testTag("morphing_action_dock")
                ) {
                    if (!isExpanded) {
                        // Collapsed state: ONLY the "+" icon in the center of the circle
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("floating_new_note_button")
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = Color.White),
                                    onClick = { isExpanded = true }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = if (isArabic) "إضافة ملاحظة" else "Add Note",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        // Expanded state: 3 icons horizontally in the center + close button
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
                                tintColor = Color(0xFF06B6D4),
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
                                tintColor = Color(0xFF10B981),
                                contentDescription = if (isArabic) "قائمة مهام" else "New Checklist",
                                testTag = "action_checklist_btn",
                                onClick = {
                                    isExpanded = false
                                    onNewChecklist()
                                }
                            )

                            // Icon 4: Close / Collapse button
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("action_collapse_dock_btn")
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x22FFFFFF) else Color(0x15000000))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true),
                                        onClick = { isExpanded = false }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isArabic) "إغلاق" else "Close",
                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
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
    Box(
        modifier = Modifier
            .size(46.dp)
            .testTag(testTag)
            .clip(CircleShape)
            .background(tintColor.copy(alpha = 0.15f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = tintColor),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tintColor,
            modifier = Modifier.size(26.dp)
        )
    }
}
