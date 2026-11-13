package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlassTint
import com.example.ui.glass.liquidGlass

@Composable
fun FluffyTopBar(
    isDark: Boolean,
    isArabic: Boolean,
    isGridView: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onToggleLanguage: (() -> Unit)? = null,
    onToggleViewMode: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    isCloudSignedIn: Boolean = false,
    onOpenCloudAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val hintColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // App Header: Title with Chromatic glow & Action Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Menu / Settings Hamburger Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("menu_drawer_button")
                        .liquidGlass(
                            isDark = isDark,
                            tint = GlassTint.FROST,
                            shape = RoundedCornerShape(13.dp),
                            borderWidth = 1.2.dp
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onOpenSettings
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = if (isArabic) "الإعدادات" else "Settings",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Fluffy",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = textColor
                        ),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onOpenSettings
                        )
                    )
                    Text(
                        text = if (isArabic) "الملاحظات الزجاجية السائلة" else "Liquid Glass Notes",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = hintColor,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Quick Glass Control Actions (Theme, Language, Grid, Stats)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stats button
                GlassIconAction(
                    icon = Icons.Default.Insights,
                    contentDescription = if (isArabic) "الإحصائيات" else "Statistics",
                    isDark = isDark,
                    testTag = "stats_button",
                    onClick = onOpenStats
                )

                // View Mode toggle
                GlassIconAction(
                    icon = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                    contentDescription = if (isArabic) "تبديل طريقة العرض" else "Toggle View",
                    isDark = isDark,
                    testTag = "view_mode_toggle",
                    onClick = onToggleViewMode
                )


                // Theme Toggle (Sun/Moon)
                GlassIconAction(
                    icon = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isArabic) "تبديل المظهر" else "Toggle Theme",
                    isDark = isDark,
                    testTag = "theme_toggle",
                    onClick = onToggleTheme
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Floating Liquid Glass Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .liquidGlass(
                    isDark = isDark,
                    tint = GlassTint.FROST,
                    shape = RoundedCornerShape(24.dp),
                    borderWidth = 1.2.dp
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = if (isArabic) "بحث" else "Search",
                    tint = hintColor,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = if (isArabic) "ابحث في الملاحظات والمهام والأقسام..." else "Search notes, checklists, tags...",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = hintColor
                            )
                        )
                    }

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_text_field"),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = textColor
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )
                }

                if (searchQuery.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("topbar_cloud_auth_button")
                            .clip(CircleShape)
                            .background(
                                if (isCloudSignedIn) Color(0xFF10B981).copy(alpha = 0.16f)
                                else Color(0xFF38BDF8).copy(alpha = 0.14f)
                            )
                            .clickable(onClick = onOpenCloudAuth),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCloudSignedIn) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            contentDescription = if (isArabic) "الحساب السحابي والمزامنة" else "Cloud Account & Sync",
                            tint = if (isCloudSignedIn) Color(0xFF10B981) else Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    AnimatedVisibility(
                        visible = searchQuery.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x33FFFFFF) else Color(0x22000000))
                                .clickable { onSearchChange("") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = textColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassIconAction(
    icon: ImageVector,
    contentDescription: String,
    isDark: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val iconColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    Box(
        modifier = Modifier
            .size(38.dp)
            .testTag(testTag)
            .liquidGlass(
                isDark = isDark,
                tint = GlassTint.FROST,
                shape = CircleShape,
                borderWidth = 1.2.dp
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(19.dp)
        )
    }
}
