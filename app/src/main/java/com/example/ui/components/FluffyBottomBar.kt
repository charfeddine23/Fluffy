package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "fab_rotation"
    )

    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Scrim backdrop when expanded
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isDark) 0.40f else 0.22f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isExpanded = false }
                    )
            )
        }

        // 2. Expanded Creation Window in the Center Bottom of the screen
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f)
            ) + fadeIn(tween(220)) + scaleIn(
                initialScale = 0.88f,
                transformOrigin = TransformOrigin(if (isArabic) 0.15f else 0.85f, 1f),
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(160)) + scaleOut(
                targetScale = 0.88f,
                animationSpec = tween(180)
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            val expandedFill = if (isDark) {
                Color(0xFF0F172A).copy(alpha = 0.85f)
            } else {
                Color(0xFFFFFFFF).copy(alpha = 0.85f)
            }

            LiquidGlassCard(
                isDark = isDark,
                tint = GlassTint.FROST,
                shape = RoundedCornerShape(32.dp),
                borderWidth = 1.8.dp,
                customFill = expandedFill,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_entry_expanded_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                ) {
                    // Header with title and close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .liquidGlass(
                                        isDark = isDark,
                                        tint = GlassTint.CYAN,
                                        shape = CircleShape,
                                        borderWidth = 1.dp
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF06B6D4),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Text(
                                text = if (isArabic) "إنشاء جديد" else "Create New",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }

                        // Close pill
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x22FFFFFF) else Color(0x12000000))
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
                                tint = metaColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 1: New Note
                    CreationOptionRow(
                        isDark = isDark,
                        icon = Icons.Default.EditNote,
                        iconTint = Color(0xFF06B6D4),
                        cardTint = GlassTint.CYAN,
                        title = if (isArabic) "ملاحظة جديدة" else "New Note",
                        subtitle = if (isArabic) "تدوين أفكار ونصوص حرة" else "Capture quick thoughts or rich memos",
                        textColor = textColor,
                        metaColor = metaColor,
                        testTag = "create_note_option",
                        onClick = {
                            isExpanded = false
                            onNewNote()
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 2: New Checklist
                    CreationOptionRow(
                        isDark = isDark,
                        icon = Icons.Default.Checklist,
                        iconTint = Color(0xFF10B981),
                        cardTint = GlassTint.EMERALD,
                        title = if (isArabic) "قائمة مهام" else "Interactive Checklist",
                        subtitle = if (isArabic) "مهام قابلة للتعليم والمتابعة" else "Tasks, shopping, or daily goals",
                        textColor = textColor,
                        metaColor = metaColor,
                        testTag = "create_checklist_option",
                        onClick = {
                            isExpanded = false
                            onNewChecklist()
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 3: Voice Note
                    CreationOptionRow(
                        isDark = isDark,
                        icon = Icons.Default.Mic,
                        iconTint = Color(0xFFF43F5E),
                        cardTint = GlassTint.ROSE,
                        title = if (isArabic) "تسجيل صوتي" else "Voice Note",
                        subtitle = if (isArabic) "مذكرة صوتية سريعة ومحفوظة" else "Record an audio memo with waveform",
                        textColor = textColor,
                        metaColor = metaColor,
                        testTag = "create_voice_option",
                        onClick = {
                            isExpanded = false
                            onNewVoiceNote()
                        }
                    )
                }
            }
        }

        // 3. Floating Action Button (Only the "+" icon)
        // Positioned at Bottom-Left in Arabic, Bottom-Right in English
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = if (isArabic) Alignment.BottomStart else Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .testTag("floating_new_note_button")
                        .liquidGlass(
                            isDark = isDark,
                            tint = GlassTint.CYAN,
                            shape = CircleShape,
                            borderWidth = 2.dp,
                            customFill = if (isDark) Color(0xFF0284C7).copy(alpha = 0.55f) else Color(0xFF0EA5E9).copy(alpha = 0.40f)
                        )
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = { isExpanded = !isExpanded }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (isArabic) "إضافة ملاحظة" else "Add Note",
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .rotate(rotation)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreationOptionRow(
    isDark: Boolean,
    icon: ImageVector,
    iconTint: Color,
    cardTint: GlassTint,
    title: String,
    subtitle: String,
    textColor: Color,
    metaColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val fill = if (isDark) {
        Color(0xFF1E293B).copy(alpha = 0.70f)
    } else {
        Color(0xFFF8FAFC).copy(alpha = 0.75f)
    }

    LiquidGlassCard(
        isDark = isDark,
        tint = cardTint,
        shape = RoundedCornerShape(20.dp),
        borderWidth = 1.2.dp,
        customFill = fill,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = iconTint),
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .liquidGlass(
                        isDark = isDark,
                        tint = cardTint,
                        shape = RoundedCornerShape(14.dp),
                        borderWidth = 1.2.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = metaColor,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
