package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.GlassTint
import com.example.ui.glass.liquidGlass

@Composable
fun FluffyBottomBar(
    isDark: Boolean,
    onNewNote: () -> Unit,
    onNewChecklist: () -> Unit,
    onNewVoiceNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Liquid Glass Dock
        Row(
            modifier = Modifier
                .liquidGlass(
                    isDark = isDark,
                    tint = GlassTint.FROST,
                    shape = RoundedCornerShape(32.dp),
                    borderWidth = 1.6.dp
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Voice Note Pill
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .testTag("quick_voice_note_button")
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color(0xFFEC4899)),
                        onClick = onNewVoiceNote
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Note",
                    tint = Color(0xFFEC4899),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Primary Floating Action Button (New Note) with radiant chromatic dispersion
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .testTag("new_note_fab")
                    .liquidGlass(
                        isDark = isDark,
                        tint = GlassTint.CYAN,
                        shape = CircleShape,
                        borderWidth = 2.dp,
                        customFill = if (isDark) Color(0xFF0284C7).copy(alpha = 0.5f) else Color(0xFF38BDF8).copy(alpha = 0.35f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color.White),
                        onClick = onNewNote
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Note",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            // Quick Checklist Pill
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .testTag("quick_checklist_button")
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color(0xFF38BDF8)),
                        onClick = onNewChecklist
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Checklist,
                    contentDescription = "New Checklist",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
