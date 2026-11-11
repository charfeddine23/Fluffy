package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class GlassTint(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val lightFill: Color,
    val darkFill: Color,
    val accentColor: Color,
    val chromaticLeading: Color,
    val chromaticTrailing: Color
) {
    FROST(
        id = "frost",
        nameEn = "Crystal Frost",
        nameAr = "بلور نقي",
        lightFill = Color(0xD9FFFFFF),
        darkFill = Color(0x2E1E293B),
        accentColor = Color(0xFF38BDF8),
        chromaticLeading = Color(0xFF38BDF8),
        chromaticTrailing = Color(0xFFC084FC)
    ),
    ROSE(
        id = "rose",
        nameEn = "Rose Quartz",
        nameAr = "كوارتز وردي",
        lightFill = Color(0xE0FFF1F2),
        darkFill = Color(0x334C0519),
        accentColor = Color(0xFFF43F5E),
        chromaticLeading = Color(0xFFFB7185),
        chromaticTrailing = Color(0xFFE879F9)
    ),
    CYAN(
        id = "cyan",
        nameEn = "Arctic Cyan",
        nameAr = "سماوي قطبي",
        lightFill = Color(0xE0ECFEFF),
        darkFill = Color(0x33083344),
        accentColor = Color(0xFF06B6D4),
        chromaticLeading = Color(0xFF22D3EE),
        chromaticTrailing = Color(0xFF60A5FA)
    ),
    AMETHYST(
        id = "amethyst",
        nameEn = "Amethyst Glow",
        nameAr = "جمشت بنفسجي",
        lightFill = Color(0xE0F3E8FF),
        darkFill = Color(0x333B0764),
        accentColor = Color(0xFFA855F7),
        chromaticLeading = Color(0xFFC084FC),
        chromaticTrailing = Color(0xFFF472B6)
    ),
    AMBER(
        id = "amber",
        nameEn = "Sunset Amber",
        nameAr = "كهرمان الغروب",
        lightFill = Color(0xE0FFFBEB),
        darkFill = Color(0x33451A03),
        accentColor = Color(0xFFF59E0B),
        chromaticLeading = Color(0xFFFBBF24),
        chromaticTrailing = Color(0xFFFB7185)
    ),
    EMERALD(
        id = "emerald",
        nameEn = "Jade Emerald",
        nameAr = "زمرد ناصع",
        lightFill = Color(0xE0ECFDF5),
        darkFill = Color(0x33022C22),
        accentColor = Color(0xFF10B981),
        chromaticLeading = Color(0xFF34D399),
        chromaticTrailing = Color(0xFF38BDF8)
    );

    companion object {
        fun fromId(id: String?): GlassTint {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: FROST
        }
    }
}
