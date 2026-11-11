package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlassTint
import com.example.ui.glass.LiquidGlassPill

data class CategoryOption(
    val id: String,
    val labelAr: String,
    val labelEn: String,
    val icon: ImageVector,
    val tint: GlassTint
)

val defaultCategories = listOf(
    CategoryOption("ALL", "الكل", "All", Icons.Default.WorkspacePremium, GlassTint.FROST),
    CategoryOption("PINNED", "المثبتة", "Pinned", Icons.Default.PushPin, GlassTint.AMBER),
    CategoryOption("FAVORITES", "المفضلة", "Favorites", Icons.Default.Favorite, GlassTint.ROSE),
    CategoryOption("TODO", "المهام", "Checklists", Icons.Default.CheckCircle, GlassTint.CYAN),
    CategoryOption("أفكار", "أفكار", "Ideas", Icons.Default.Lightbulb, GlassTint.AMETHYST),
    CategoryOption("عمل", "عمل", "Work", Icons.Default.Work, GlassTint.CYAN),
    CategoryOption("شخصي", "شخصي", "Personal", Icons.Default.Person, GlassTint.EMERALD)
)

@Composable
fun CategoryChips(
    isDark: Boolean,
    isArabic: Boolean,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        defaultCategories.forEach { option ->
            val isSelected = selectedCategory.equals(option.id, ignoreCase = true)
            val label = if (isArabic) option.labelAr else option.labelEn
            val textColor = if (isSelected) {
                if (isDark) Color.White else Color(0xFF0F172A)
            } else {
                if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
            }

            LiquidGlassPill(
                isDark = isDark,
                isSelected = isSelected,
                tint = option.tint,
                onClick = { onSelectCategory(option.id) },
                testTag = "category_chip_${option.id}",
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = label,
                        tint = if (isSelected) option.tint.accentColor else textColor,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}
