package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.theme.LocalAppThemePreset
import java.io.File

@Composable
fun ProfileAvatar(
    photoUrl: String?,
    firstName: String,
    lastName: String,
    displayName: String?,
    email: String?,
    isDark: Boolean,
    avatarChoice: String = "",
    size: Dp = 40.dp,
    borderWidth: Dp = 1.6.dp,
    fontSize: TextUnit = 15.sp,
    customRingBrush: Brush? = null,
    modifier: Modifier = Modifier
) {
    val activePreset = LocalAppThemePreset.current
    val initials = rememberInitials(firstName, lastName, displayName, email)

    val ringBrush = customRingBrush ?: Brush.linearGradient(
        colors = listOf(
            activePreset.primary,
            activePreset.secondary,
            activePreset.tertiary
        )
    )

    val innerBgBrush = Brush.linearGradient(
        colors = listOf(
            activePreset.primary.copy(alpha = if (isDark) 0.34f else 0.22f),
            activePreset.secondary.copy(alpha = if (isDark) 0.32f else 0.20f)
        )
    )

    val presetAvatar = remember(avatarChoice) {
        if (avatarChoice.startsWith("preset:")) {
            ProfileAvatarCatalog.findById(avatarChoice.removePrefix("preset:"))
        } else {
            null
        }
    }

    val customImageFile = remember(avatarChoice) {
        if (avatarChoice.startsWith("custom:")) {
            val file = File(avatarChoice.removePrefix("custom:"))
            if (file.exists() && file.length() > 0L) file else null
        } else {
            null
        }
    }

    val innerSize = (size - 4.dp).coerceAtLeast(16.dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(width = borderWidth, brush = ringBrush, shape = CircleShape)
            .padding(2.dp)
            .clip(CircleShape)
            .background(innerBgBrush),
        contentAlignment = Alignment.Center
    ) {
        when {
            customImageFile != null -> {
                SubcomposeAsyncImage(
                    model = customImageFile,
                    contentDescription = "Profile Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    loading = {
                        MonogramFallback(
                            initials = initials,
                            fontSize = fontSize,
                            tintColor = activePreset.onBackground,
                            size = size
                        )
                    },
                    error = {
                        MonogramFallback(
                            initials = initials,
                            fontSize = fontSize,
                            tintColor = activePreset.onBackground,
                            size = size
                        )
                    }
                )
            }

            presetAvatar != null -> {
                PresetAvatarArtwork(
                    avatar = presetAvatar,
                    size = innerSize,
                    modifier = Modifier.fillMaxSize()
                )
            }

            avatarChoice == "initials" -> {
                MonogramFallback(
                    initials = initials,
                    fontSize = fontSize,
                    tintColor = activePreset.onBackground,
                    size = size
                )
            }

            !photoUrl.isNullOrBlank() -> {
                SubcomposeAsyncImage(
                    model = photoUrl,
                    contentDescription = "Profile Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    loading = {
                        MonogramFallback(
                            initials = initials,
                            fontSize = fontSize,
                            tintColor = activePreset.onBackground,
                            size = size
                        )
                    },
                    error = {
                        MonogramFallback(
                            initials = initials,
                            fontSize = fontSize,
                            tintColor = activePreset.onBackground,
                            size = size
                        )
                    }
                )
            }

            else -> {
                MonogramFallback(
                    initials = initials,
                    fontSize = fontSize,
                    tintColor = activePreset.onBackground,
                    size = size
                )
            }
        }
    }
}

@Composable
private fun MonogramFallback(
    initials: String,
    fontSize: TextUnit,
    tintColor: Color,
    size: Dp
) {
    if (initials.isNotBlank()) {
        Text(
            text = initials,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = tintColor
        )
    } else {
        Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = "Profile",
            tint = tintColor,
            modifier = Modifier.size(size * 0.54f)
        )
    }
}

private fun rememberInitials(
    firstName: String,
    lastName: String,
    displayName: String?,
    email: String?
): String {
    val f = firstName.trim().firstOrNull()
    val l = lastName.trim().firstOrNull()
    if (f != null || l != null) {
        return buildString {
            if (f != null) append(f.uppercaseChar())
            if (l != null) append(l.uppercaseChar())
        }
    }

    val rawName = displayName?.trim().orEmpty()
    if (rawName.isNotBlank()) {
        val parts = rawName.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val p1 = parts.getOrNull(0)?.firstOrNull()
        val p2 = parts.getOrNull(1)?.firstOrNull()
        return buildString {
            if (p1 != null) append(p1.uppercaseChar())
            if (p2 != null) append(p2.uppercaseChar())
        }
    }

    val mailChar = email?.trim()?.firstOrNull()
    return mailChar?.uppercaseChar()?.toString().orEmpty()
}
