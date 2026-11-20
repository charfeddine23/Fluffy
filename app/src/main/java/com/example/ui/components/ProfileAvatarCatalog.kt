package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Anchor
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.ChangeHistory
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FilterVintage
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material.icons.rounded.Lens
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Nature
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import kotlin.math.cos
import kotlin.math.sin

enum class AvatarCategory(val id: String, val nameAr: String, val nameEn: String) {
    ALL("all", "الكل (50)", "All (50)"),
    COSMIC("cosmic", "كوزمي", "Cosmic"),
    CRYSTAL("crystal", "كريستال", "Crystal"),
    PERSONAS("personas", "شخصيات", "Personas"),
    NATURE("nature", "طبيعة", "Nature"),
    CYBER("cyber", "سايبر", "Cyber")
}

enum class AvatarPattern {
    ORBITAL_RINGS,
    PRISM_FACETS,
    AURORA_WAVES,
    RADIAL_BURST,
    CYBER_HEX
}

data class PresetAvatar(
    val id: String,
    val number: Int,
    val category: AvatarCategory,
    val nameAr: String,
    val nameEn: String,
    val colorStart: Color,
    val colorMid: Color,
    val colorEnd: Color,
    val iconTint: Color,
    val pattern: AvatarPattern,
    val icon: ImageVector
)

object ProfileAvatarCatalog {

    val allAvatars: List<PresetAvatar> = listOf(
        // 1..10 Cosmic & Celestial
        PresetAvatar(
            id = "avatar_01", number = 1, category = AvatarCategory.COSMIC,
            nameAr = "أورورا السماوي", nameEn = "Celestial Aurora",
            colorStart = Color(0xFF0F172A), colorMid = Color(0xFF2563EB), colorEnd = Color(0xFF38BDF8),
            iconTint = Color(0xFFE0F2FE), pattern = AvatarPattern.ORBITAL_RINGS, icon = Icons.Rounded.AutoAwesome
        ),
        PresetAvatar(
            id = "avatar_02", number = 2, category = AvatarCategory.COSMIC,
            nameAr = "سديم المجرة", nameEn = "Nebula Core",
            colorStart = Color(0xFF3B0764), colorMid = Color(0xFF9333EA), colorEnd = Color(0xFFF472B6),
            iconTint = Color(0xFFFDF2F8), pattern = AvatarPattern.AURORA_WAVES, icon = Icons.Rounded.NightsStay
        ),
        PresetAvatar(
            id = "avatar_03", number = 3, category = AvatarCategory.COSMIC,
            nameAr = "سوبرنوفا", nameEn = "Supernova",
            colorStart = Color(0xFF450A0A), colorMid = Color(0xFFEA580C), colorEnd = Color(0xFFFACC15),
            iconTint = Color(0xFFFEF9C3), pattern = AvatarPattern.RADIAL_BURST, icon = Icons.Rounded.Flare
        ),
        PresetAvatar(
            id = "avatar_04", number = 4, category = AvatarCategory.COSMIC,
            nameAr = "كسوف القمر", nameEn = "Lunar Eclipse",
            colorStart = Color(0xFF090D16), colorMid = Color(0xFF334155), colorEnd = Color(0xFF60A5FA),
            iconTint = Color(0xFFF8FAFC), pattern = AvatarPattern.ORBITAL_RINGS, icon = Icons.Rounded.Brightness4
        ),
        PresetAvatar(
            id = "avatar_05", number = 5, category = AvatarCategory.COSMIC,
            nameAr = "مستكشف الفضاء", nameEn = "Star Voyager",
            colorStart = Color(0xFF1E1B4B), colorMid = Color(0xFF4F46E5), colorEnd = Color(0xFF22D3EE),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.PRISM_FACETS, icon = Icons.Rounded.RocketLaunch
        ),
        PresetAvatar(
            id = "avatar_06", number = 6, category = AvatarCategory.COSMIC,
            nameAr = "نجم الشمال", nameEn = "Polaris Star",
            colorStart = Color(0xFF082F49), colorMid = Color(0xFF0284C7), colorEnd = Color(0xFF7DD3FC),
            iconTint = Color(0xFFF0F9FF), pattern = AvatarPattern.RADIAL_BURST, icon = Icons.Rounded.Star
        ),
        PresetAvatar(
            id = "avatar_07", number = 7, category = AvatarCategory.COSMIC,
            nameAr = "مدار زحل", nameEn = "Saturn Orbit",
            colorStart = Color(0xFF2E1065), colorMid = Color(0xFF7C3AED), colorEnd = Color(0xFFC084FC),
            iconTint = Color(0xFFF5F3FF), pattern = AvatarPattern.ORBITAL_RINGS, icon = Icons.Rounded.Public
        ),
        PresetAvatar(
            id = "avatar_08", number = 8, category = AvatarCategory.COSMIC,
            nameAr = "شفق الليل", nameEn = "Midnight Dusk",
            colorStart = Color(0xFF020617), colorMid = Color(0xFF1D4ED8), colorEnd = Color(0xFFA855F7),
            iconTint = Color(0xFFE0E7FF), pattern = AvatarPattern.AURORA_WAVES, icon = Icons.Rounded.DarkMode
        ),
        PresetAvatar(
            id = "avatar_09", number = 9, category = AvatarCategory.COSMIC,
            nameAr = "نبض الكون", nameEn = "Cosmic Pulsar",
            colorStart = Color(0xFF500724), colorMid = Color(0xFFDB2777), colorEnd = Color(0xFF38BDF8),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.RADIAL_BURST, icon = Icons.Rounded.BlurOn
        ),
        PresetAvatar(
            id = "avatar_10", number = 10, category = AvatarCategory.COSMIC,
            nameAr = "شمس الكوزموس", nameEn = "Solar Crown",
            colorStart = Color(0xFF7C2D12), colorMid = Color(0xFFF59E0B), colorEnd = Color(0xFFFDE047),
            iconTint = Color(0xFFFFFBEB), pattern = AvatarPattern.ORBITAL_RINGS, icon = Icons.Rounded.WbSunny
        ),

        // 11..20 Liquid Crystal & Prism
        PresetAvatar(
            id = "avatar_11", number = 11, category = AvatarCategory.CRYSTAL,
            nameAr = "ياقوت أزرق", nameEn = "Royal Sapphire",
            colorStart = Color(0xFF0A192F), colorMid = Color(0xFF1D4ED8), colorEnd = Color(0xFF67E8F9),
            iconTint = Color(0xFFECFEFF), pattern = AvatarPattern.PRISM_FACETS, icon = Icons.Rounded.Diamond
        ),
        PresetAvatar(
            id = "avatar_12", number = 12, category = AvatarCategory.CRYSTAL,
            nameAr = "زمرد نقي", nameEn = "Pure Emerald",
            colorStart = Color(0xFF022C22), colorMid = Color(0xFF059669), colorEnd = Color(0xFF6EE7B7),
            iconTint = Color(0xFFECFDF5), pattern = AvatarPattern.PRISM_FACETS, icon = Icons.Rounded.Hexagon
        ),
        PresetAvatar(
            id = "avatar_13", number = 13, category = AvatarCategory.CRYSTAL,
            nameAr = "روبي متوهج", nameEn = "Crimson Ruby",
            colorStart = Color(0xFF4C0519), colorMid = Color(0xFFE11D48), colorEnd = Color(0xFFFB7185),
            iconTint = Color(0xFFFFF1F2), pattern = AvatarPattern.RADIAL_BURST, icon = Icons.Rounded.Favorite
        ),
        PresetAvatar(
            id = "avatar_14", number = 14, category = AvatarCategory.CRYSTAL,
            nameAr = "جمشت ملكي", nameEn = "Royal Amethyst",
            colorStart = Color(0xFF2E1065), colorMid = Color(0xFF8B5CF6), colorEnd = Color(0xFFE879F9),
            iconTint = Color(0xFFFAF5FF), pattern = AvatarPattern.PRISM_FACETS, icon = Icons.Rounded.WorkspacePremium
        ),
        PresetAvatar(
            id = "avatar_15", number = 15, category = AvatarCategory.CRYSTAL,
            nameAr = "زئبق سائل", nameEn = "Liquid Mercury",
            colorStart = Color(0xFF0F172A), colorMid = Color(0xFF64748B), colorEnd = Color(0xFFE2E8F0),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.AURORA_WAVES, icon = Icons.Rounded.Lens
        ),
        PresetAvatar(
            id = "avatar_16", number = 16, category = AvatarCategory.CRYSTAL,
            nameAr = "كوارتز وردي", nameEn = "Rose Quartz",
            colorStart = Color(0xFF831843), colorMid = Color(0xFFEC4899), colorEnd = Color(0xFFFBCFE8),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.PRISM_FACETS, icon = Icons.Rounded.Spa
        ),
        PresetAvatar(
            id = "avatar_17", number = 17, category = AvatarCategory.CRYSTAL,
            nameAr = "منشور ضوئي", nameEn = "Prism Light",
            colorStart = Color(0xFF312E81), colorMid = Color(0xFF06B6D4), colorEnd = Color(0xFFF472B6),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.PRISM_FACETS, icon = Icons.Rounded.ChangeHistory
        ),
        PresetAvatar(
            id = "avatar_18", number = 18, category = AvatarCategory.CRYSTAL,
            nameAr = "عنبر ذهبي", nameEn = "Amber Gold",
            colorStart = Color(0xFF451A03), colorMid = Color(0xFFD97706), colorEnd = Color(0xFFFDE68A),
            iconTint = Color(0xFFFFFBEB), pattern = AvatarPattern.RADIAL_BURST, icon = Icons.Rounded.Grade
        ),
        PresetAvatar(
            id = "avatar_19", number = 19, category = AvatarCategory.CRYSTAL,
            nameAr = "أوبال اللؤلؤ", nameEn = "Pearl Opal",
            colorStart = Color(0xFF164E63), colorMid = Color(0xFF2DD4BF), colorEnd = Color(0xFFC4B5FD),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.ORBITAL_RINGS, icon = Icons.Rounded.Interests
        ),
        PresetAvatar(
            id = "avatar_20", number = 20, category = AvatarCategory.CRYSTAL,
            nameAr = "كريستال الصقيع", nameEn = "Arctic Crystal",
            colorStart = Color(0xFF0C4A6E), colorMid = Color(0xFF38BDF8), colorEnd = Color(0xFFE0F2FE),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.PRISM_FACETS, icon = Icons.Rounded.AcUnit
        ),

        // 21..30 Minimal Personas & Emblems
        PresetAvatar(
            id = "avatar_21", number = 21, category = AvatarCategory.PERSONAS,
            nameAr = "الوجه المبتسم", nameEn = "Radiant Smile",
            colorStart = Color(0xFF1E3A8A), colorMid = Color(0xFF3B82F6), colorEnd = Color(0xFF93C5FD),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.ORBITAL_RINGS, icon = Icons.Rounded.Mood
        ),
        PresetAvatar(
            id = "avatar_22", number = 22, category = AvatarCategory.PERSONAS,
            nameAr = "ملامح أنيقة", nameEn = "Minimal Persona",
            colorStart = Color(0xFF311042), colorMid = Color(0xFFA855F7), colorEnd = Color(0xFFF0ABFC),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.AURORA_WAVES, icon = Icons.Rounded.Face
        ),
        PresetAvatar(
            id = "avatar_23", number = 23, category = AvatarCategory.PERSONAS,
            nameAr = "الحارس الرقمي", nameEn = "Guardian Shield",
            colorStart = Color(0xFF064E3B), colorMid = Color(0xFF10B981), colorEnd = Color(0xFF34D399),
            iconTint = Color(0xFFECFDF5), pattern = AvatarPattern.CYBER_HEX, icon = Icons.Rounded.Shield
        ),
        PresetAvatar(
            id = "avatar_24", number = 24, category = AvatarCategory.PERSONAS,
            nameAr = "المستكشف", nameEn = "Navigator",
            colorStart = Color(0xFF1E293B), colorMid = Color(0xFF0EA5E9), colorEnd = Color(0xFF38BDF8),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.ORBITAL_RINGS, icon = Icons.Rounded.Explore
        ),
        PresetAvatar(
            id = "avatar_25", number = 25, category = AvatarCategory.PERSONAS,
            nameAr = "المفكر المبدع", nameEn = "Creative Mind",
            colorStart = Color(0xFF4A044E), colorMid = Color(0xFFC026D3), colorEnd = Color(0xFF818CF8),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.RADIAL_BURST, icon = Icons.Rounded.Psychology
        ),
        PresetAvatar(
            id = "avatar_26", number = 26, category = AvatarCategory.PERSONAS,
            nameAr = "الفنان الزجاجي", nameEn = "Glass Artist",
            colorStart = Color(0xFF701A75), colorMid = Color(0xFFF43F5E), colorEnd = Color(0xFFFB923C),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.AURORA_WAVES, icon = Icons.Rounded.Palette
        ),
        PresetAvatar(
            id = "avatar_27", number = 27, category = AvatarCategory.PERSONAS,
            nameAr = "عازف الألحان", nameEn = "Sound Maestro",
            colorStart = Color(0xFF111827), colorMid = Color(0xFF6366F1), colorEnd = Color(0xFFA78BFA),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.ORBITAL_RINGS, icon = Icons.Rounded.Headphones
        ),
        PresetAvatar(
            id = "avatar_28", number = 28, category = AvatarCategory.PERSONAS,
            nameAr = "الموثق الملكي", nameEn = "Verified Crest",
            colorStart = Color(0xFF0F172A), colorMid = Color(0xFF0284C7), colorEnd = Color(0xFF10B981),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.PRISM_FACETS, icon = Icons.Rounded.Verified
        ),
        PresetAvatar(
            id = "avatar_29", number = 29, category = AvatarCategory.PERSONAS,
            nameAr = "رفيق ذكي", nameEn = "Loyal Spirit",
            colorStart = Color(0xFF431407), colorMid = Color(0xFFEA580C), colorEnd = Color(0xFFFDBA74),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.AURORA_WAVES, icon = Icons.Rounded.Pets
        ),
        PresetAvatar(
            id = "avatar_30", number = 30, category = AvatarCategory.PERSONAS,
            nameAr = "البصمة الفريدة", nameEn = "Unique Identity",
            colorStart = Color(0xFF020617), colorMid = Color(0xFF7C3AED), colorEnd = Color(0xFF2DD4BF),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.CYBER_HEX, icon = Icons.Rounded.Fingerprint
        ),

        // 31..40 Nature & Elements
        PresetAvatar(
            id = "avatar_31", number = 31, category = AvatarCategory.NATURE,
            nameAr = "لهب العنقاء", nameEn = "Phoenix Flame",
            colorStart = Color(0xFF7F1D1D), colorMid = Color(0xFFEF4444), colorEnd = Color(0xFFF59E0B),
            iconTint = Color(0xFFFEF2F2), pattern = AvatarPattern.RADIAL_BURST, icon = Icons.Rounded.LocalFireDepartment
        ),
        PresetAvatar(
            id = "avatar_32", number = 32, category = AvatarCategory.NATURE,
            nameAr = "أمواج المحيط", nameEn = "Deep Ocean",
            colorStart = Color(0xFF082F49), colorMid = Color(0xFF0369A1), colorEnd = Color(0xFF22D3EE),
            iconTint = Color(0xFFECFEFF), pattern = AvatarPattern.AURORA_WAVES, icon = Icons.Rounded.Waves
        ),
        PresetAvatar(
            id = "avatar_33", number = 33, category = AvatarCategory.NATURE,
            nameAr = "قطرة الندى", nameEn = "Morning Dew",
            colorStart = Color(0xFF064E3B), colorMid = Color(0xFF0D9488), colorEnd = Color(0xFF5EEAD4),
            iconTint = Color(0xFFF0FDFA), pattern = AvatarPattern.ORBITAL_RINGS, icon = Icons.Rounded.WaterDrop
        ),
        PresetAvatar(
            id = "avatar_34", number = 34, category = AvatarCategory.NATURE,
            nameAr = "واحة الغابات", nameEn = "Emerald Forest",
            colorStart = Color(0xFF052E16), colorMid = Color(0xFF15803D), colorEnd = Color(0xFF4ADE80),
            iconTint = Color(0xFFF0FDF4), pattern = AvatarPattern.PRISM_FACETS, icon = Icons.Rounded.Park
        ),
        PresetAvatar(
            id = "avatar_35", number = 35, category = AvatarCategory.NATURE,
            nameAr = "زهرة الكرز", nameEn = "Sakura Bloom",
            colorStart = Color(0xFF831843), colorMid = Color(0xFFF472B6), colorEnd = Color(0xFFFCE7F3),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.RADIAL_BURST, icon = Icons.Rounded.FilterVintage
        ),
        PresetAvatar(
            id = "avatar_36", number = 36, category = AvatarCategory.NATURE,
            nameAr = "قمم الجبال", nameEn = "Alpine Peak",
            colorStart = Color(0xFF1E293B), colorMid = Color(0xFF475569), colorEnd = Color(0xFF94A3B8),
            iconTint = Color(0xFFF8FAFC), pattern = AvatarPattern.PRISM_FACETS, icon = Icons.Rounded.Terrain
        ),
        PresetAvatar(
            id = "avatar_37", number = 37, category = AvatarCategory.NATURE,
            nameAr = "غصن السلام", nameEn = "Verdant Leaf",
            colorStart = Color(0xFF14532D), colorMid = Color(0xFF16A34A), colorEnd = Color(0xFFA3E635),
            iconTint = Color(0xFFF7FEE7), pattern = AvatarPattern.AURORA_WAVES, icon = Icons.Rounded.Nature
        ),
        PresetAvatar(
            id = "avatar_38", number = 38, category = AvatarCategory.NATURE,
            nameAr = "سحاب الفجر", nameEn = "Dawn Cloud",
            colorStart = Color(0xFF312E81), colorMid = Color(0xFF60A5FA), colorEnd = Color(0xFFFDA4AF),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.AURORA_WAVES, icon = Icons.Rounded.Cloud
        ),
        PresetAvatar(
            id = "avatar_39", number = 39, category = AvatarCategory.NATURE,
            nameAr = "نور الصباح", nameEn = "Morning Glow",
            colorStart = Color(0xFF9A3412), colorMid = Color(0xFFFB923C), colorEnd = Color(0xFFFEF08A),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.RADIAL_BURST, icon = Icons.Rounded.LightMode
        ),
        PresetAvatar(
            id = "avatar_40", number = 40, category = AvatarCategory.NATURE,
            nameAr = "مرساة البحر", nameEn = "Sea Anchor",
            colorStart = Color(0xFF0F172A), colorMid = Color(0xFF0F766E), colorEnd = Color(0xFF38BDF8),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.ORBITAL_RINGS, icon = Icons.Rounded.Anchor
        ),

        // 41..50 Neon & Cyber Geometry
        PresetAvatar(
            id = "avatar_41", number = 41, category = AvatarCategory.CYBER,
            nameAr = "برق النيون", nameEn = "Cyber Bolt",
            colorStart = Color(0xFF090A1A), colorMid = Color(0xFF6D28D9), colorEnd = Color(0xFF00E5FF),
            iconTint = Color(0xFFE0F7FA), pattern = AvatarPattern.CYBER_HEX, icon = Icons.Rounded.Bolt
        ),
        PresetAvatar(
            id = "avatar_42", number = 42, category = AvatarCategory.CYBER,
            nameAr = "قلب الماتريكس", nameEn = "Matrix Terminal",
            colorStart = Color(0xFF020617), colorMid = Color(0xFF047857), colorEnd = Color(0xFF22C55E),
            iconTint = Color(0xFFDCFCE7), pattern = AvatarPattern.CYBER_HEX, icon = Icons.Rounded.Terminal
        ),
        PresetAvatar(
            id = "avatar_43", number = 43, category = AvatarCategory.CYBER,
            nameAr = "شبكة الكم", nameEn = "Quantum Hub",
            colorStart = Color(0xFF172554), colorMid = Color(0xFF4F46E5), colorEnd = Color(0xFFEC4899),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.CYBER_HEX, icon = Icons.Rounded.Hub
        ),
        PresetAvatar(
            id = "avatar_44", number = 44, category = AvatarCategory.CYBER,
            nameAr = "شريحة المستقبل", nameEn = "Neural Chip",
            colorStart = Color(0xFF0F172A), colorMid = Color(0xFF0284C7), colorEnd = Color(0xFFA855F7),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.CYBER_HEX, icon = Icons.Rounded.Memory
        ),
        PresetAvatar(
            id = "avatar_45", number = 45, category = AvatarCategory.CYBER,
            nameAr = "المفتاح السري", nameEn = "Vault Key",
            colorStart = Color(0xFF1F2937), colorMid = Color(0xFFD97706), colorEnd = Color(0xFFFBBF24),
            iconTint = Color(0xFFFFFBEB), pattern = AvatarPattern.ORBITAL_RINGS, icon = Icons.Rounded.VpnKey
        ),
        PresetAvatar(
            id = "avatar_46", number = 46, category = AvatarCategory.CYBER,
            nameAr = "أفق لانهائي", nameEn = "Infinity Loop",
            colorStart = Color(0xFF1E1B4B), colorMid = Color(0xFF7C3AED), colorEnd = Color(0xFF38BDF8),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.ORBITAL_RINGS, icon = Icons.Rounded.AllInclusive
        ),
        PresetAvatar(
            id = "avatar_47", number = 47, category = AvatarCategory.CYBER,
            nameAr = "بصيرة النيون", nameEn = "Neon Vision",
            colorStart = Color(0xFF4C0519), colorMid = Color(0xFFBE185D), colorEnd = Color(0xFF22D3EE),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.PRISM_FACETS, icon = Icons.Rounded.Visibility
        ),
        PresetAvatar(
            id = "avatar_48", number = 48, category = AvatarCategory.CYBER,
            nameAr = "ألعاب المستقبل", nameEn = "Arcade Pulse",
            colorStart = Color(0xFF2E1065), colorMid = Color(0xFF9333EA), colorEnd = Color(0xFFF43F5E),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.CYBER_HEX, icon = Icons.Rounded.SportsEsports
        ),
        PresetAvatar(
            id = "avatar_49", number = 49, category = AvatarCategory.CYBER,
            nameAr = "شيفرة الإبداع", nameEn = "Synth Code",
            colorStart = Color(0xFF090D16), colorMid = Color(0xFF2563EB), colorEnd = Color(0xFF2DD4BF),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.CYBER_HEX, icon = Icons.Rounded.Code
        ),
        PresetAvatar(
            id = "avatar_50", number = 50, category = AvatarCategory.CYBER,
            nameAr = "وميض فلوفي", nameEn = "Fluffy Flash",
            colorStart = Color(0xFF3B0764), colorMid = Color(0xFFEC4899), colorEnd = Color(0xFF38BDF8),
            iconTint = Color(0xFFFFFFFF), pattern = AvatarPattern.RADIAL_BURST, icon = Icons.Rounded.ElectricBolt
        )
    )

    fun findById(id: String): PresetAvatar? = allAvatars.firstOrNull { it.id == id }
}

@Composable
fun PresetAvatarArtwork(
    avatar: PresetAvatar,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)
            val radius = w.coerceAtMost(h) / 2f

            // 1. Rich multi-stop diagonal gradient fill
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(avatar.colorStart, avatar.colorMid, avatar.colorEnd),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                radius = radius,
                center = center
            )

            // 2. Decorative liquid-glass geometric pattern
            when (avatar.pattern) {
                AvatarPattern.ORBITAL_RINGS -> {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.16f),
                        radius = radius * 0.78f,
                        center = center,
                        style = Stroke(width = (w * 0.028f).coerceAtLeast(1.2f))
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.24f),
                        radius = radius * 0.56f,
                        center = Offset(w * 0.58f, h * 0.44f),
                        style = Stroke(width = (w * 0.02f).coerceAtLeast(1f))
                    )
                }

                AvatarPattern.PRISM_FACETS -> {
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.08f)
                        lineTo(w * 0.90f, h * 0.50f)
                        lineTo(w * 0.5f, h * 0.92f)
                        lineTo(w * 0.10f, h * 0.50f)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.13f)
                    )
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.24f),
                        style = Stroke(width = (w * 0.02f).coerceAtLeast(1f))
                    )
                }

                AvatarPattern.AURORA_WAVES -> {
                    val wave1 = Path().apply {
                        moveTo(0f, h * 0.62f)
                        cubicTo(w * 0.28f, h * 0.42f, w * 0.68f, h * 0.80f, w, h * 0.52f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = wave1,
                        color = Color.White.copy(alpha = 0.14f)
                    )
                    val wave2 = Path().apply {
                        moveTo(0f, h * 0.34f)
                        cubicTo(w * 0.35f, h * 0.20f, w * 0.72f, h * 0.48f, w, h * 0.28f)
                    }
                    drawPath(
                        path = wave2,
                        color = Color.White.copy(alpha = 0.22f),
                        style = Stroke(width = (w * 0.03f).coerceAtLeast(1.2f), cap = StrokeCap.Round)
                    )
                }

                AvatarPattern.RADIAL_BURST -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                            center = center,
                            radius = radius * 0.85f
                        ),
                        radius = radius * 0.85f,
                        center = center
                    )
                    for (i in 0 until 8) {
                        val angle = (i * 45.0) * (Math.PI / 180.0)
                        val startR = radius * 0.45f
                        val endR = radius * 0.92f
                        drawLine(
                            color = Color.White.copy(alpha = 0.18f),
                            start = Offset(
                                center.x + (startR * cos(angle)).toFloat(),
                                center.y + (startR * sin(angle)).toFloat()
                            ),
                            end = Offset(
                                center.x + (endR * cos(angle)).toFloat(),
                                center.y + (endR * sin(angle)).toFloat()
                            ),
                            strokeWidth = (w * 0.02f).coerceAtLeast(1f),
                            cap = StrokeCap.Round
                        )
                    }
                }

                AvatarPattern.CYBER_HEX -> {
                    val hexPath = Path()
                    val hexRadius = radius * 0.76f
                    for (i in 0 until 6) {
                        val angle = ((i * 60.0) - 30.0) * (Math.PI / 180.0)
                        val px = center.x + (hexRadius * cos(angle)).toFloat()
                        val py = center.y + (hexRadius * sin(angle)).toFloat()
                        if (i == 0) hexPath.moveTo(px, py) else hexPath.lineTo(px, py)
                    }
                    hexPath.close()
                    drawPath(
                        path = hexPath,
                        color = Color.White.copy(alpha = 0.16f),
                        style = Stroke(width = (w * 0.024f).coerceAtLeast(1.1f))
                    )
                }
            }

            // 3. Soft top-left liquid glass specular highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.32f),
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.28f, h * 0.24f),
                    radius = radius * 0.72f
                ),
                radius = radius,
                center = center
            )
        }

        Icon(
            imageVector = avatar.icon,
            contentDescription = avatar.nameEn,
            tint = avatar.iconTint,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}
