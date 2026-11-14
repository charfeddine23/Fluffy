package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppThemePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerSheet(
    visible: Boolean,
    currentAvatarChoice: String,
    googlePhotoUrl: String?,
    firstName: String,
    lastName: String,
    displayName: String?,
    email: String?,
    useGoogleAccountName: Boolean,
    isGoogleLinked: Boolean,
    statusGlowColor: Color,
    isDark: Boolean,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onUpdateProfileName: (firstName: String, lastName: String) -> Unit,
    onToggleUseGoogleAccountName: (Boolean) -> Unit,
    onPickCustomPhoto: (android.net.Uri) -> Unit,
    onSelectPresetAvatar: (String) -> Unit,
    onUseGooglePhoto: () -> Unit,
    onUseInitials: () -> Unit
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activePreset = LocalAppThemePreset.current
    val textColor = activePreset.onBackground
    val metaColor = activePreset.onSurfaceVariant

    var selectedCategory by remember { mutableStateOf(AvatarCategory.ALL) }
    var draftFirst by remember(firstName, visible) { mutableStateOf(firstName) }
    var draftLast by remember(lastName, visible) { mutableStateOf(lastName) }

    LaunchedEffect(firstName, lastName) {
        draftFirst = firstName
        draftLast = lastName
    }

    val filteredAvatars = remember(selectedCategory) {
        if (selectedCategory == AvatarCategory.ALL) {
            ProfileAvatarCatalog.allAvatars
        } else {
            ProfileAvatarCatalog.allAvatars.filter { it.category == selectedCategory }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickCustomPhoto(uri)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = activePreset.surface.copy(alpha = 0.97f),
        contentColor = textColor,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = statusGlowColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isArabic) "تخصيص الملف الشخصي" else "Customize Profile",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(metaColor.copy(alpha = 0.12f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isArabic) "إغلاق" else "Close",
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Circular Preview
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(statusGlowColor.copy(alpha = 0.25f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                ProfileAvatar(
                    avatarChoice = currentAvatarChoice,
                    photoUrl = googlePhotoUrl,
                    firstName = draftFirst,
                    lastName = draftLast,
                    displayName = displayName,
                    email = email,
                    isDark = isDark,
                    size = 76.dp,
                    borderWidth = 2.4.dp,
                    fontSize = 24.sp,
                    customRingBrush = Brush.linearGradient(
                        listOf(statusGlowColor, activePreset.primary, activePreset.secondary)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Gallery Picker + Quick Source Options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickSourcePill(
                    icon = Icons.Default.AddPhotoAlternate,
                    label = if (isArabic) "من المعرض" else "From Gallery",
                    accent = activePreset.primary,
                    isSelected = currentAvatarChoice.startsWith("custom:"),
                    testTag = "pick_gallery_avatar_button",
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                if (!googlePhotoUrl.isNullOrBlank()) {
                    QuickSourcePill(
                        icon = Icons.Default.Person,
                        label = if (isArabic) "صورة Google" else "Google Photo",
                        accent = Color(0xFF10B981),
                        isSelected = currentAvatarChoice == "google" || currentAvatarChoice.isBlank(),
                        testTag = "use_google_avatar_button",
                        onClick = {
                            onUseGooglePhoto()
                            onDismiss()
                        }
                    )
                }

                QuickSourcePill(
                    icon = Icons.Default.Person,
                    label = if (isArabic) "الأحرف الأولى" else "Initials",
                    accent = activePreset.secondary,
                    isSelected = currentAvatarChoice == "initials",
                    testTag = "use_initials_avatar_button",
                    onClick = {
                        onUseInitials()
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // NAME & SURNAME + USE GOOGLE ACCOUNT NAME CARD (Inside Profile Picture Sheet)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(activePreset.surfaceVariant.copy(alpha = if (isDark) 0.55f else 0.72f))
                    .border(
                        width = 1.dp,
                        color = activePreset.primary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Badge,
                            contentDescription = null,
                            tint = statusGlowColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = if (isArabic) "التسمية حسب حساب Google" else "Use Google Account Name",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = when {
                                    isGoogleLinked && !displayName.isNullOrBlank() -> displayName
                                    isGoogleLinked -> if (isArabic) "مزامنة الاسم تلقائياً من Google" else "Auto-sync from Google"
                                    else -> if (isArabic) "متاح عند ربط حساب Google" else "Available when linked to Google"
                                },
                                fontSize = 11.sp,
                                color = metaColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Switch(
                        checked = useGoogleAccountName,
                        onCheckedChange = { enabled ->
                            onToggleUseGoogleAccountName(enabled)
                        },
                        enabled = isGoogleLinked,
                        modifier = Modifier.testTag("use_google_name_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981)
                        )
                    )
                }

                AnimatedVisibility(visible = !useGoogleAccountName || !isGoogleLinked) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = draftFirst,
                                onValueChange = { draftFirst = it },
                                label = { Text(if (isArabic) "الاسم" else "First Name", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = activePreset.primary,
                                    unfocusedBorderColor = metaColor.copy(alpha = 0.35f),
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_first_name_input")
                            )

                            OutlinedTextField(
                                value = draftLast,
                                onValueChange = { draftLast = it },
                                label = { Text(if (isArabic) "اللقب" else "Last Name", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = activePreset.primary,
                                    unfocusedBorderColor = metaColor.copy(alpha = 0.35f),
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_last_name_input")
                            )
                        }

                        Button(
                            onClick = { onUpdateProfileName(draftFirst, draftLast) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activePreset.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("save_profile_name_button")
                        ) {
                            Text(
                                text = if (isArabic) "حفظ الاسم واللقب" else "Save Name & Surname",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 50 Ready-Made Avatars Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "أفاتارات جاهزة" else "Ready-Made Avatars",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(activePreset.primary.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "50",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = activePreset.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AvatarCategory.entries.forEach { cat ->
                    val active = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (active) activePreset.primary.copy(alpha = 0.22f)
                                else metaColor.copy(alpha = 0.10f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (active) activePreset.primary.copy(alpha = 0.65f)
                                else metaColor.copy(alpha = 0.20f),
                                shape = CircleShape
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isArabic) cat.nameAr else cat.nameEn,
                            fontSize = 11.5.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) activePreset.primary else metaColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 50-Avatar Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .testTag("avatar_picker_grid"),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredAvatars, key = { it.id }) { avatar ->
                    val isSelected = currentAvatarChoice == "preset:${avatar.id}"
                    val itemScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.09f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "avatar_grid_scale"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .scale(itemScale)
                            .testTag("preset_avatar_${avatar.id}")
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true)
                            ) {
                                onSelectPresetAvatar(avatar.id)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) statusGlowColor else Color.White.copy(alpha = 0.22f),
                                        shape = CircleShape
                                    )
                                    .padding(2.5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                PresetAvatarArtwork(
                                    avatar = avatar,
                                    size = 48.dp
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(statusGlowColor)
                                        .border(1.5.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isArabic) avatar.nameAr else avatar.nameEn,
                            fontSize = 9.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) statusGlowColor else metaColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSourcePill(
    icon: ImageVector,
    label: String,
    accent: Color,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .testTag(testTag)
            .clip(CircleShape)
            .background(
                if (isSelected) accent.copy(alpha = 0.22f) else accent.copy(alpha = 0.12f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accent else accent.copy(alpha = 0.35f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}
