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
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.example.data.model.GlassTint
import com.example.ui.glass.liquidGlass
import com.example.ui.theme.LocalAppThemePreset

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

    // Dynamic width: 64.dp circle -> 318.dp centered Liquid Glass dock (holds Voice, Note, Checklist, Doc Scanner, Close)
    val targetWidth = if (isExpanded) 318.dp else 64.dp
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
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = BiasAlignment(animatedBias, 1.0f)
            ) {
                // Restored Liquid Glass Floating Plus Button -> Morphs smoothly into Liquid Glass Action Capsule
                Box(
                    modifier = Modifier
                        .width(animatedWidth)
                        .height(64.dp)
                        .graphicsLayer {
                            scaleX = plusScale
                            scaleY = plusScale
                        }
                        .testTag("morphing_action_dock")
                        .liquidGlass(
                            isDark = isDark,
                            tint = if (isExpanded) GlassTint.FROST else GlassTint.CYAN,
                            shape = RoundedCornerShape(32.dp),
                            borderWidth = 1.5.dp,
                            customFill = if (isExpanded) {
                                if (isDark) activePreset.surface.copy(alpha = 0.84f) else Color.White.copy(alpha = 0.90f)
                            } else {
                                accentColor.copy(alpha = if (isDark) 0.44f else 0.88f)
                            }
                        )
                ) {
                    if (!isExpanded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .minimumInteractiveComponentSize()
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
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon 1: Voice Note
                            HorizontalActionButton(
                                icon = Icons.Default.Mic,
                                isDark = isDark,
                                glassTint = GlassTint.ROSE,
                                tintColor = Color(0xFFF43F5E),
                                contentDescription = if (isArabic) "تسجيل صوتي" else "Voice Note",
                                testTag = "action_voice_note_btn",
                                onClick = {
                                    isExpanded = false
                                    onNewVoiceNote()
                                }
                            )

                            // Icon 2: New Text Note
                            HorizontalActionButton(
                                icon = Icons.Default.EditNote,
                                isDark = isDark,
                                glassTint = GlassTint.CYAN,
                                tintColor = accentColor,
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
                                isDark = isDark,
                                glassTint = GlassTint.EMERALD,
                                tintColor = Color(0xFF10B981),
                                contentDescription = if (isArabic) "قائمة مهام" else "New Checklist",
                                testTag = "action_checklist_btn",
                                onClick = {
                                    isExpanded = false
                                    onNewChecklist()
                                }
                            )

                            // Icon 4: Doc Scanner (Opens full offline Document Scanner Ecosystem)
                            HorizontalActionButton(
                                icon = Icons.Default.DocumentScanner,
                                isDark = isDark,
                                glassTint = GlassTint.AMETHYST,
                                tintColor = Color(0xFF38BDF8),
                                contentDescription = if (isArabic) "تصوير الملفات" else "Doc Scanner",
                                testTag = "action_doc_scanner_btn",
                                onClick = {
                                    isExpanded = false
                                    onOpenScanner()
                                }
                            )

                            // Icon 5: Close / Collapse orb
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
                                    .minimumInteractiveComponentSize()
                                    .graphicsLayer {
                                        scaleX = closeScale
                                        scaleY = closeScale
                                    }
                                    .testTag("action_collapse_dock_btn")
                                    .liquidGlass(
                                        isDark = isDark,
                                        tint = GlassTint.FROST,
                                        shape = CircleShape,
                                        borderWidth = 1.2.dp,
                                        customFill = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFF0F172A).copy(alpha = 0.08f)
                                    )
                                    .clickable(
                                        interactionSource = closeInteraction,
                                        indication = ripple(bounded = true),
                                        onClick = { isExpanded = false }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isArabic) "إغلاق" else "Close",
                                    tint = if (isDark) Color.White.copy(alpha = 0.90f) else Color(0xFF0F172A),
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
    isDark: Boolean,
    glassTint: GlassTint,
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
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag(testTag)
            .liquidGlass(
                isDark = isDark,
                tint = glassTint,
                shape = CircleShape,
                borderWidth = 1.25.dp,
                customFill = tintColor.copy(alpha = if (isDark) 0.26f else 0.16f)
            )
            .clickable(
                interactionSource = interactionSource,
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
