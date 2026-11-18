package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlassTint
import com.example.data.model.NotesViewMode
import com.example.ui.glass.liquidGlass
import com.example.ui.theme.LocalAppThemePreset
import com.example.ui.theme.tr
import com.example.update.UpdateUiState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FluffyTopBar(
    isDark: Boolean,
    isArabic: Boolean,
    isGridView: Boolean,
    viewMode: NotesViewMode = if (isGridView) NotesViewMode.GRID else NotesViewMode.LIST,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onToggleLanguage: (() -> Unit)? = null,
    onToggleViewMode: () -> Unit,
    onSelectViewMode: (NotesViewMode) -> Unit = { _ -> onToggleViewMode() },
    onOpenStats: () -> Unit,
    updateState: UpdateUiState = UpdateUiState.Idle,
    onOpenUpdate: () -> Unit = {},
    onOpenSettings: () -> Unit,
    profilePhotoUrl: String? = null,
    profileAvatarChoice: String = "",
    profileFirstName: String = "",
    profileLastName: String = "",
    profileDisplayName: String? = null,
    profileEmail: String? = null,
    isCloudSignedIn: Boolean = false,
    isGuestMode: Boolean = false,
    onOpenCloudAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val activePreset = LocalAppThemePreset.current
    val textColor = activePreset.onBackground
    val hintColor = activePreset.onSurfaceVariant
    var showViewModeMenu by remember { mutableStateOf(false) }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
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
                        contentDescription = tr("الإعدادات", "Paramètres", "Settings", "Iɣewwaṛen"),
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Fluffy",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = textColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onOpenSettings
                        )
                    )
                    Text(
                        text = "${activePreset.name} Notes",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = hintColor,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("topbar_theme_subtitle")
                    )
                }
            }

            // Quick Glass Control Actions (Theme, Language, Grid, Stats)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Insights button — seamlessly transforms into a luxury update arrow when an update is available
                val hasAvailableUpdate = when (updateState) {
                    is UpdateUiState.Available,
                    is UpdateUiState.Downloading,
                    is UpdateUiState.ReadyToInstall,
                    is UpdateUiState.NeedsInstallPermission -> true
                    is UpdateUiState.Error -> updateState.info != null
                    else -> false
                }
                AnimatedContent(
                    targetState = hasAvailableUpdate,
                    transitionSpec = {
                        (scaleIn(
                            initialScale = 0.78f,
                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f)
                        ) + fadeIn(tween(220))) togetherWith
                            (scaleOut(targetScale = 0.82f, animationSpec = tween(160)) + fadeOut(tween(150)))
                    },
                    label = "insights_or_update_slot"
                ) { updateReady ->
                    if (updateReady) {
                        LuxuryUpdateArrowAction(
                            updateState = updateState,
                            isDark = isDark,
                            isArabic = isArabic,
                            onClick = onOpenUpdate
                        )
                    } else {
                        GlassIconAction(
                            icon = Icons.Default.Insights,
                            contentDescription = tr("الإحصائيات", "Statistiques", "Statistics", "Tidaddanin"),
                            isDark = isDark,
                            testTag = "stats_button",
                            onClick = onOpenStats
                        )
                    }
                }

                // View Mode Picker (Grid, List, Gallery View, Magazine View)
                Box {
                    val activeModeIcon = when (viewMode) {
                        NotesViewMode.GRID -> Icons.Default.GridView
                        NotesViewMode.LIST -> Icons.Default.ViewAgenda
                        NotesViewMode.GALLERY -> Icons.Default.AutoAwesomeMosaic
                        NotesViewMode.MAGAZINE -> Icons.Default.MenuBook
                    }
                    GlassIconAction(
                        icon = activeModeIcon,
                        contentDescription = tr("تبديل طريقة العرض", "Changer l'affichage", "View Mode", "Beddel timeẓri"),
                        isDark = isDark,
                        testTag = "view_mode_toggle",
                        onClick = { showViewModeMenu = true }
                    )

                    DropdownMenu(
                        expanded = showViewModeMenu,
                        onDismissRequest = { showViewModeMenu = false },
                        containerColor = activePreset.surface.copy(alpha = 0.98f)
                    ) {
                        NotesViewMode.entries.forEach { mode ->
                            val selected = mode == viewMode
                            val optionIcon = when (mode) {
                                NotesViewMode.GRID -> Icons.Default.GridView
                                NotesViewMode.LIST -> Icons.Default.ViewAgenda
                                NotesViewMode.GALLERY -> Icons.Default.AutoAwesomeMosaic
                                NotesViewMode.MAGAZINE -> Icons.Default.MenuBook
                            }
                            val optionLabel = when (mode) {
                                NotesViewMode.GRID -> tr("شبكة • Grid View", "Vue grille", "Grid View", "Timeẓri n tferkit")
                                NotesViewMode.LIST -> tr("قائمة • List View", "Vue liste", "List View", "Timeẓri n tebdart")
                                NotesViewMode.GALLERY -> tr("معرض • Gallery View", "Gallery View", "Gallery View", "Gallery View")
                                NotesViewMode.MAGAZINE -> tr("مجلة • Magazine View", "Magazine View", "Magazine View", "Magazine View")
                            }
                            DropdownMenuItem(
                                modifier = Modifier.testTag("view_mode_option_${mode.id}"),
                                text = {
                                    Text(
                                        text = optionLabel,
                                        color = if (selected) activePreset.primary else textColor,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = optionIcon,
                                        contentDescription = null,
                                        tint = if (selected) activePreset.primary else hintColor
                                    )
                                },
                                trailingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = activePreset.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null,
                                onClick = {
                                    showViewModeMenu = false
                                    onSelectViewMode(mode)
                                }
                            )
                        }
                    }
                }


                // Circular Profile Photo (Display-only badge for the user's profile avatar)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("topbar_profile_button")
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ProfileAvatar(
                        avatarChoice = profileAvatarChoice,
                        photoUrl = profilePhotoUrl,
                        firstName = profileFirstName,
                        lastName = profileLastName,
                        displayName = profileDisplayName,
                        email = profileEmail,
                        isDark = isDark,
                        size = 40.dp,
                        borderWidth = 1.8.dp,
                        fontSize = 14.sp
                    )
                }
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
                    contentDescription = tr("بحث", "Rechercher", "Search", "Nadi"),
                    tint = hintColor,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = tr(
                                "ابحث في الملاحظات والمهام والأقسام...",
                                "Rechercher notes, tâches, tags...",
                                "Search notes, checklists, tags...",
                                "Nadi di tira, timahalin, tags..."
                            ),
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
                        cursorBrush = SolidColor(activePreset.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )
                }

                if (searchQuery.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("topbar_cloud_auth_button")
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    isCloudSignedIn -> Color(0xFF10B981).copy(alpha = 0.16f)
                                    isGuestMode -> Color(0xFF38BDF8).copy(alpha = 0.14f)
                                    else -> Color(0xFF38BDF8).copy(alpha = 0.14f)
                                }
                            )
                            .clickable(onClick = onOpenCloudAuth)
                            .padding(horizontal = if (!isCloudSignedIn && isGuestMode) 10.dp else 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isCloudSignedIn -> Icons.Default.CloudDone
                                isGuestMode -> Icons.Default.PersonOutline
                                else -> Icons.Default.CloudQueue
                            },
                            contentDescription = if (isArabic) "الحساب السحابي أو وضع الضيف" else "Cloud Account or Guest Mode",
                            tint = if (isCloudSignedIn) Color(0xFF10B981) else Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        if (!isCloudSignedIn && isGuestMode) {
                            Text(
                                text = tr("ضيف", "Invité", "Guest", "Inebgi"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
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
    val iconColor = LocalAppThemePreset.current.onBackground
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

@Composable
private fun LuxuryUpdateArrowAction(
    updateState: UpdateUiState,
    isDark: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val activePreset = LocalAppThemePreset.current
    val effectiveDark = activePreset.isDark || isDark
    val primary = activePreset.primary
    val secondary = activePreset.secondary
    val tertiary = activePreset.tertiary
    val isDownloading = updateState is UpdateUiState.Downloading
    val downloadProgress = (updateState as? UpdateUiState.Downloading)?.progress ?: 0f

    val infiniteTransition = rememberInfiniteTransition(label = "luxury_update_arrow")
    val arrowFloat by infiniteTransition.animateFloat(
        initialValue = -2.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isDownloading) 550 else 1150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_float"
    )
    val shimmerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_shimmer"
    )
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )

    val pillShape = RoundedCornerShape(19.dp)
    val statusLabel = when (updateState) {
        is UpdateUiState.Downloading -> "${(downloadProgress * 100).toInt().coerceIn(0, 100)}%"
        is UpdateUiState.ReadyToInstall,
        is UpdateUiState.NeedsInstallPermission -> tr("تثبيت", "Installer", "Install", "Sebded")
        else -> tr("تحديث", "MàJ", "Update", "Aleqqem")
    }
    val contentDesc = if (isArabic) "تحديث جديد متاح" else "New update available"

    val angleRad = Math.toRadians(shimmerAngle.toDouble())
    val dirX = cos(angleRad).toFloat()
    val dirY = sin(angleRad).toFloat()

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("topbar_update_button")
            .clip(pillShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = primary),
                onClickLabel = contentDesc,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 10.dp,
                    shape = pillShape,
                    ambientColor = primary.copy(alpha = 0.45f),
                    spotColor = secondary.copy(alpha = 0.45f)
                )
                .height(38.dp)
                .clip(pillShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primary.copy(alpha = if (effectiveDark) 0.28f else 0.20f),
                            secondary.copy(alpha = if (effectiveDark) 0.24f else 0.16f),
                            tertiary.copy(alpha = if (effectiveDark) 0.20f else 0.14f)
                        )
                    )
                )
                .border(
                    width = 1.4.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.92f),
                            Color.White.copy(alpha = haloPulse),
                            secondary.copy(alpha = 0.92f),
                            tertiary.copy(alpha = 0.85f)
                        ),
                        start = Offset(40f + 40f * dirX, 19f + 19f * dirY),
                        end = Offset(40f - 40f * dirX, 19f - 19f * dirY)
                    ),
                    shape = pillShape
                )
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier.size(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = size.minDimension / 2f

                        // Soft radial jewel halo
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primary.copy(alpha = 0.34f * haloPulse),
                                    secondary.copy(alpha = 0.16f * haloPulse),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = r
                            ),
                            radius = r,
                            center = Offset(cx, cy)
                        )

                        if (isDownloading) {
                            drawArc(
                                color = activePreset.onBackground.copy(alpha = 0.18f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                                size = Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
                                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(listOf(primary, secondary, tertiary, primary)),
                                startAngle = -90f,
                                sweepAngle = 360f * downloadProgress.coerceIn(0.05f, 1f),
                                useCenter = false,
                                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                                size = Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        } else {
                            // Sleek bottom cradle arc under the arrow
                            val trayPath = Path().apply {
                                moveTo(cx - r * 0.54f, cy + r * 0.38f)
                                quadraticTo(cx, cy + r * 0.72f, cx + r * 0.54f, cy + r * 0.38f)
                            }
                            drawPath(
                                path = trayPath,
                                brush = Brush.horizontalGradient(listOf(primary, secondary, tertiary)),
                                style = Stroke(
                                    width = 1.9.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // Floating luxury downward arrow
                        val yOff = arrowFloat.dp.toPx() * 0.68f
                        val shaftTop = cy - r * 0.52f + yOff
                        val tipY = cy + r * 0.24f + yOff
                        val wingDx = r * 0.36f
                        val wingY = tipY - r * 0.34f

                        val arrowGradient = Brush.verticalGradient(
                            colors = listOf(Color.White, primary, secondary),
                            startY = shaftTop,
                            endY = tipY
                        )

                        drawLine(
                            brush = arrowGradient,
                            start = Offset(cx, shaftTop),
                            end = Offset(cx, tipY),
                            strokeWidth = 2.1.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        val headPath = Path().apply {
                            moveTo(cx - wingDx, wingY)
                            lineTo(cx, tipY)
                            lineTo(cx + wingDx, wingY)
                        }
                        drawPath(
                            path = headPath,
                            brush = arrowGradient,
                            style = Stroke(
                                width = 2.1.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Tiny glowing top-end beacon dot
                        drawCircle(
                            color = tertiary.copy(alpha = haloPulse),
                            radius = 2.1.dp.toPx(),
                            center = Offset(size.width - 2.6.dp.toPx(), 2.6.dp.toPx())
                        )
                    }
                }

                Text(
                    text = statusLabel,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = activePreset.onBackground,
                    maxLines = 1
                )
            }
        }
    }
}
