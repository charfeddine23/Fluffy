package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlassTint
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass

@Composable
fun VaultScreen(
    isDark: Boolean,
    isArabic: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    val infiniteTransition = rememberInfiniteTransition(label = "vault_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vault_halo_scale"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vault_float"
    )

    FrostedBackdrop(isDark = isDark) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("vault_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Liquid Glass Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("vault_back_button")
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .liquidGlass(
                                isDark = isDark,
                                tint = GlassTint.FROST,
                                shape = CircleShape,
                                borderWidth = 1.2.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back",
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .liquidGlass(
                            isDark = isDark,
                            tint = GlassTint.AMETHYST,
                            shape = CircleShape,
                            borderWidth = 1.2.dp
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Vault",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            letterSpacing = 0.6.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.size(48.dp))
            }

            // Centered Floating Liquid Glass Card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassCard(
                    isDark = isDark,
                    tint = GlassTint.AMETHYST,
                    shape = RoundedCornerShape(32.dp),
                    borderWidth = 1.8.dp,
                    customFill = if (isDark) {
                        Color(0xFF0F172A).copy(alpha = 0.78f)
                    } else {
                        Color(0xFFFFFFFF).copy(alpha = 0.82f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 380.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 38.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Animated Chromatic Vault Emblem
                        Box(
                            modifier = Modifier.size(116.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                    }
                            ) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val outerRadius = size.minDimension / 2f

                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0x55C084FC),
                                            Color(0x3338BDF8),
                                            Color.Transparent
                                        ),
                                        center = center,
                                        radius = outerRadius
                                    ),
                                    radius = outerRadius,
                                    center = center
                                )

                                drawCircle(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            Color(0xFF38BDF8).copy(alpha = 0.75f),
                                            Color(0xFFC084FC).copy(alpha = 0.85f),
                                            Color(0xFFF472B6).copy(alpha = 0.75f),
                                            Color(0xFF38BDF8).copy(alpha = 0.75f)
                                        )
                                    ),
                                    radius = outerRadius * 0.80f,
                                    center = center,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .graphicsLayer {
                                        translationY = floatOffset.dp.toPx()
                                    }
                                    .liquidGlass(
                                        isDark = isDark,
                                        tint = GlassTint.AMETHYST,
                                        shape = CircleShape,
                                        borderWidth = 1.5.dp
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Vault",
                                    tint = if (isDark) Color(0xFFE9D5FF) else Color(0xFF7C3AED),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Text(
                            text = "Vault",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .testTag("vault_status_badge")
                                .liquidGlass(
                                    isDark = isDark,
                                    tint = GlassTint.CYAN,
                                    shape = CircleShape,
                                    borderWidth = 1.4.dp,
                                    customFill = if (isDark) Color(0x2E8B5CF6) else Color(0x228B5CF6)
                                )
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = if (isArabic) "قريباً" else "Soon",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFE9D5FF) else Color(0xFF6D28D9),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (isArabic) "اسحب للخلف أو اضغط رجوع للعودة" else "Tap back to return to Fluffy",
                            fontSize = 12.sp,
                            color = metaColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
