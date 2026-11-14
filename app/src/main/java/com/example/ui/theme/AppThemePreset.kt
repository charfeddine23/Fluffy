package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeGroup {
    LIQUID_GLASS,
    CLASSIC
}

@Immutable
data class AppThemePreset(
    val id: String,
    val name: String,
    val group: AppThemeGroup,
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val orb1: Color,
    val orb2: Color,
    val orb3: Color,
    val pairedThemeId: String? = null
) {
    val isGlassStyle: Boolean
        get() = group == AppThemeGroup.LIQUID_GLASS

    val swatchColors: List<Color>
        get() = listOf(background, surface, primary, secondary, tertiary)

    companion object {
        const val DEFAULT_DARK_ID = "obsidian_liquid_glass"
        const val DEFAULT_LIGHT_ID = "frosted_arctic_glass"
    }
}

val LocalAppThemePreset = staticCompositionLocalOf { AppThemeRegistry.defaultDark }

object AppThemeRegistry {

    private fun darkTheme(
        id: String,
        name: String,
        group: AppThemeGroup,
        bg: Long,
        surface: Long,
        variant: Long,
        primary: Long,
        secondary: Long,
        tertiary: Long,
        onBg: Long = 0xFFF8FAFC,
        onSurface: Long = 0xFFF1F5F9,
        onVariant: Long = 0xFF94A3B8,
        pairedId: String? = null
    ): AppThemePreset {
        val p = Color(primary)
        val s = Color(secondary)
        val t = Color(tertiary)
        return AppThemePreset(
            id = id,
            name = name,
            group = group,
            isDark = true,
            background = Color(bg),
            surface = Color(surface),
            surfaceVariant = Color(variant),
            primary = p,
            secondary = s,
            tertiary = t,
            onBackground = Color(onBg),
            onSurface = Color(onSurface),
            onSurfaceVariant = Color(onVariant),
            orb1 = p.copy(alpha = if (group == AppThemeGroup.LIQUID_GLASS) 0.34f else 0.20f),
            orb2 = s.copy(alpha = if (group == AppThemeGroup.LIQUID_GLASS) 0.32f else 0.18f),
            orb3 = t.copy(alpha = if (group == AppThemeGroup.LIQUID_GLASS) 0.28f else 0.15f),
            pairedThemeId = pairedId
        )
    }

    private fun lightTheme(
        id: String,
        name: String,
        group: AppThemeGroup,
        bg: Long,
        surface: Long,
        variant: Long,
        primary: Long,
        secondary: Long,
        tertiary: Long,
        onBg: Long = 0xFF0F172A,
        onSurface: Long = 0xFF1E293B,
        onVariant: Long = 0xFF64748B,
        pairedId: String? = null
    ): AppThemePreset {
        val p = Color(primary)
        val s = Color(secondary)
        val t = Color(tertiary)
        return AppThemePreset(
            id = id,
            name = name,
            group = group,
            isDark = false,
            background = Color(bg),
            surface = Color(surface),
            surfaceVariant = Color(variant),
            primary = p,
            secondary = s,
            tertiary = t,
            onBackground = Color(onBg),
            onSurface = Color(onSurface),
            onSurfaceVariant = Color(onVariant),
            orb1 = p.copy(alpha = if (group == AppThemeGroup.LIQUID_GLASS) 0.32f else 0.16f),
            orb2 = s.copy(alpha = if (group == AppThemeGroup.LIQUID_GLASS) 0.28f else 0.14f),
            orb3 = t.copy(alpha = if (group == AppThemeGroup.LIQUID_GLASS) 0.24f else 0.12f),
            pairedThemeId = pairedId
        )
    }

    val allThemes: List<AppThemePreset> = listOf(
        // Classic & Editor Themes
        darkTheme(
            id = "monokai",
            name = "Monokai",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF272822,
            surface = 0xFF3E3D32,
            variant = 0xFF49483E,
            primary = 0xFFA6E22E,
            secondary = 0xFFF92672,
            tertiary = 0xFF66D9EF,
            onBg = 0xFFF8F8F2,
            onSurface = 0xFFF8F8F2,
            onVariant = 0xFFAEAE9A
        ),
        darkTheme(
            id = "monokai_pro",
            name = "Monokai Pro",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF2D2A2E,
            surface = 0xFF403E41,
            variant = 0xFF5B595C,
            primary = 0xFFFFD866,
            secondary = 0xFFFF6188,
            tertiary = 0xFF78DCE8,
            onBg = 0xFFFCFCFA,
            onSurface = 0xFFFCFCFA,
            onVariant = 0xFF939293
        ),
        darkTheme(
            id = "solarized_dark",
            name = "Solarized Dark",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF002B36,
            surface = 0xFF073642,
            variant = 0xFF0E4554,
            primary = 0xFF268BD2,
            secondary = 0xFF2AA198,
            tertiary = 0xFFB58900,
            onBg = 0xFF93A1A1,
            onSurface = 0xFFEEE8D5,
            onVariant = 0xFF657B83,
            pairedId = "solarized_light"
        ),
        lightTheme(
            id = "solarized_light",
            name = "Solarized Light",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFFFDF6E3,
            surface = 0xFFEEE8D5,
            variant = 0xFFE4DEC8,
            primary = 0xFF268BD2,
            secondary = 0xFF2AA198,
            tertiary = 0xFFD33682,
            onBg = 0xFF073642,
            onSurface = 0xFF002B36,
            onVariant = 0xFF586E75,
            pairedId = "solarized_dark"
        ),
        darkTheme(
            id = "one_dark_pro",
            name = "One Dark Pro",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF282C34,
            surface = 0xFF21252B,
            variant = 0xFF323842,
            primary = 0xFF61AFEF,
            secondary = 0xFFC678DD,
            tertiary = 0xFFE06C75,
            onBg = 0xFFABB2BF,
            onSurface = 0xFFD7DAE0,
            onVariant = 0xFF7F848E,
            pairedId = "one_light"
        ),
        lightTheme(
            id = "one_light",
            name = "One Light",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFFFAFAFA,
            surface = 0xFFEAEAEB,
            variant = 0xFFDFDFE2,
            primary = 0xFF4078F2,
            secondary = 0xFFA626A4,
            tertiary = 0xFF50A14F,
            onBg = 0xFF383A42,
            onSurface = 0xFF23252A,
            onVariant = 0xFF696C77,
            pairedId = "one_dark_pro"
        ),
        darkTheme(
            id = "dracula",
            name = "Dracula",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF282A36,
            surface = 0xFF44475A,
            variant = 0xFF373A4B,
            primary = 0xFFBD93F9,
            secondary = 0xFFFF79C6,
            tertiary = 0xFF8BE9FD,
            onBg = 0xFFF8F8F2,
            onSurface = 0xFFF8F8F2,
            onVariant = 0xFF9EA6C7
        ),
        darkTheme(
            id = "tomorrow_night_eighties",
            name = "Tomorrow Night Eighties",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF2D2D2D,
            surface = 0xFF393939,
            variant = 0xFF515151,
            primary = 0xFF6699CC,
            secondary = 0xFFCC99CC,
            tertiary = 0xFFF2777A,
            onBg = 0xFFCCCCCC,
            onSurface = 0xFFEEEEEE,
            onVariant = 0xFF999999
        ),
        darkTheme(
            id = "nord",
            name = "Nord",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF2E3440,
            surface = 0xFF3B4252,
            variant = 0xFF434C5E,
            primary = 0xFF88C0D0,
            secondary = 0xFF81A1C1,
            tertiary = 0xFFB48EAD,
            onBg = 0xFFECEFF4,
            onSurface = 0xFFE5E9F0,
            onVariant = 0xFFD8DEE9
        ),
        darkTheme(
            id = "night_owl",
            name = "Night Owl",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF011627,
            surface = 0xFF0B2942,
            variant = 0xFF1D3B53,
            primary = 0xFF82AAFF,
            secondary = 0xFFC792EA,
            tertiary = 0xFF7FDBCA,
            onBg = 0xFFD6DEEB,
            onSurface = 0xFFE6EDF7,
            onVariant = 0xFF7E97AC
        ),
        darkTheme(
            id = "bluloco_dark",
            name = "Bluloco Dark",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF282C34,
            surface = 0xFF1E222A,
            variant = 0xFF333844,
            primary = 0xFF3691FF,
            secondary = 0xFFFF6480,
            tertiary = 0xFF3FC56B,
            onBg = 0xFFABB2BF,
            onSurface = 0xFFE6EBF5,
            onVariant = 0xFF7A8291
        ),
        darkTheme(
            id = "ayu_dark",
            name = "Ayu Dark",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF0A0E14,
            surface = 0xFF131721,
            variant = 0xFF1C2230,
            primary = 0xFFFFB454,
            secondary = 0xFF59C2FF,
            tertiary = 0xFFF07178,
            onBg = 0xFFB3B1AD,
            onSurface = 0xFFE6E1CF,
            onVariant = 0xFF6C7680,
            pairedId = "ayu_light"
        ),
        darkTheme(
            id = "ayu_mirage",
            name = "Ayu Mirage",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF1F2430,
            surface = 0xFF272D3B,
            variant = 0xFF32394A,
            primary = 0xFFFFCC66,
            secondary = 0xFF73D0FF,
            tertiary = 0xFFF28779,
            onBg = 0xFFCBCCC6,
            onSurface = 0xFFECECEC,
            onVariant = 0xFF707A8C,
            pairedId = "ayu_light"
        ),
        lightTheme(
            id = "ayu_light",
            name = "Ayu Light",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFFFAFAFA,
            surface = 0xFFF0F0F0,
            variant = 0xFFE4E6E8,
            primary = 0xFFFF9940,
            secondary = 0xFF399EE6,
            tertiary = 0xFFF07171,
            onBg = 0xFF575F66,
            onSurface = 0xFF2D3339,
            onVariant = 0xFF8A9199,
            pairedId = "ayu_dark"
        ),
        darkTheme(
            id = "winter_is_coming",
            name = "Winter is Coming",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF011627,
            surface = 0xFF0B253A,
            variant = 0xFF14344D,
            primary = 0xFF00BFF9,
            secondary = 0xFF7FDBCA,
            tertiary = 0xFFC792EA,
            onBg = 0xFFD6DEEB,
            onSurface = 0xFFEFF6FF,
            onVariant = 0xFF7890A8
        ),
        darkTheme(
            id = "zenburn",
            name = "Zenburn",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF3F3F3F,
            surface = 0xFF4F4F4F,
            variant = 0xFF5F5F5F,
            primary = 0xFF8CD0D3,
            secondary = 0xFFCC9393,
            tertiary = 0xFF7F9F7F,
            onBg = 0xFFDCDCCC,
            onSurface = 0xFFF0DFAF,
            onVariant = 0xFFAFAFAF
        ),
        darkTheme(
            id = "synthwave_84",
            name = "SynthWave '84",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF262335,
            surface = 0xFF241B2F,
            variant = 0xFF34294F,
            primary = 0xFFFF7EDB,
            secondary = 0xFF36F9F6,
            tertiary = 0xFFFEDE5D,
            onBg = 0xFFFFFFFF,
            onSurface = 0xFFF7F4FF,
            onVariant = 0xFFB8A6D9
        ),
        darkTheme(
            id = "shades_of_purple",
            name = "Shades of Purple",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF2D2B55,
            surface = 0xFF1E1E3F,
            variant = 0xFF3B3872,
            primary = 0xFFFAD000,
            secondary = 0xFFFF9D00,
            tertiary = 0xFFA599E9,
            onBg = 0xFFFFFFFF,
            onSurface = 0xFFEDE8FF,
            onVariant = 0xFFA599E9
        ),
        darkTheme(
            id = "laserwave",
            name = "LaserWave",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF27212E,
            surface = 0xFF32293D,
            variant = 0xFF3E344D,
            primary = 0xFFEB64B9,
            secondary = 0xFF40B4C4,
            tertiary = 0xFFB381C5,
            onBg = 0xFFFFFFFF,
            onSurface = 0xFFF4ECFA,
            onVariant = 0xFFA89BB8
        ),
        darkTheme(
            id = "abyss",
            name = "Abyss",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF000C18,
            surface = 0xFF06192E,
            variant = 0xFF0D2847,
            primary = 0xFF6688CC,
            secondary = 0xFF22AA44,
            tertiary = 0xFF9966B8,
            onBg = 0xFF8AA8C7,
            onSurface = 0xFFD9E6F5,
            onVariant = 0xFF597799
        ),
        darkTheme(
            id = "cobalt2",
            name = "Cobalt2",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF193549,
            surface = 0xFF122738,
            variant = 0xFF1F4662,
            primary = 0xFFFFC600,
            secondary = 0xFF0088FF,
            tertiary = 0xFFFF628C,
            onBg = 0xFFFFFFFF,
            onSurface = 0xFFEBF5FF,
            onVariant = 0xFF8BA8C4
        ),
        darkTheme(
            id = "red",
            name = "Red",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF390000,
            surface = 0xFF4E0808,
            variant = 0xFF641111,
            primary = 0xFFFF6666,
            secondary = 0xFFFFB454,
            tertiary = 0xFFFF3333,
            onBg = 0xFFFFE5E5,
            onSurface = 0xFFFFFFFF,
            onVariant = 0xFFD99A9A
        ),
        darkTheme(
            id = "poimandres",
            name = "Poimandres",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF1B1E28,
            surface = 0xFF252B37,
            variant = 0xFF303747,
            primary = 0xFFADD7FF,
            secondary = 0xFF5DE4C7,
            tertiary = 0xFFFCC5E9,
            onBg = 0xFFE4F0FB,
            onSurface = 0xFFF2F7FD,
            onVariant = 0xFF8695B7
        ),
        darkTheme(
            id = "hopscotch",
            name = "Hopscotch",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF322931,
            surface = 0xFF433B42,
            variant = 0xFF5C545B,
            primary = 0xFFFD8B19,
            secondary = 0xFF149B93,
            tertiary = 0xFFC85E7C,
            onBg = 0xFFD5D3D5,
            onSurface = 0xFFF4F2F4,
            onVariant = 0xFF989498
        ),
        darkTheme(
            id = "gruvbox_dark",
            name = "Gruvbox Dark",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF282828,
            surface = 0xFF3C3836,
            variant = 0xFF504945,
            primary = 0xFFFABD2F,
            secondary = 0xFFFE8019,
            tertiary = 0xFF8EC07C,
            onBg = 0xFFEBDBB2,
            onSurface = 0xFFFBF1C7,
            onVariant = 0xFFA89984,
            pairedId = "gruvbox_light"
        ),
        lightTheme(
            id = "gruvbox_light",
            name = "Gruvbox Light",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFFFBF1C7,
            surface = 0xFFEBDBB2,
            variant = 0xFFD5C4A1,
            primary = 0xFFB57614,
            secondary = 0xFFAF3A03,
            tertiary = 0xFF427B58,
            onBg = 0xFF3C3836,
            onSurface = 0xFF282828,
            onVariant = 0xFF7C6F64,
            pairedId = "gruvbox_dark"
        ),
        darkTheme(
            id = "oceanic_next",
            name = "Oceanic Next",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF1B2B34,
            surface = 0xFF343D46,
            variant = 0xFF4F5B66,
            primary = 0xFF6699CC,
            secondary = 0xFF5FB3B3,
            tertiary = 0xFFC594C5,
            onBg = 0xFFD8DEE9,
            onSurface = 0xFFEFF3F8,
            onVariant = 0xFFA7ADBA
        ),
        darkTheme(
            id = "base16_eighties",
            name = "Base16 Eighties",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF2D2D2D,
            surface = 0xFF393939,
            variant = 0xFF515151,
            primary = 0xFFF2777A,
            secondary = 0xFF6699CC,
            tertiary = 0xFF99CC99,
            onBg = 0xFFD3D0C8,
            onSurface = 0xFFF2F0EC,
            onVariant = 0xFFA09F93
        ),
        darkTheme(
            id = "tomorrow_night_blue",
            name = "Tomorrow Night Blue",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF002451,
            surface = 0xFF00346E,
            variant = 0xFF00458F,
            primary = 0xFFBBDAFF,
            secondary = 0xFFEBBBFF,
            tertiary = 0xFF99FFFF,
            onBg = 0xFFFFFFFF,
            onSurface = 0xFFF0F7FF,
            onVariant = 0xFF8FB5E6
        ),
        darkTheme(
            id = "material_oceanic",
            name = "Material Theme (Oceanic)",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF263238,
            surface = 0xFF2E3C43,
            variant = 0xFF37474F,
            primary = 0xFF80CBC4,
            secondary = 0xFF82AAFF,
            tertiary = 0xFFC792EA,
            onBg = 0xFFB0BEC5,
            onSurface = 0xFFECEFF1,
            onVariant = 0xFF78909C
        ),
        darkTheme(
            id = "material_palenight",
            name = "Material Theme (Palenight)",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF292D3E,
            surface = 0xFF34324A,
            variant = 0xFF414863,
            primary = 0xFFC792EA,
            secondary = 0xFF82AAFF,
            tertiary = 0xFFF78C6C,
            onBg = 0xFFA6ACCD,
            onSurface = 0xFFEEFFFF,
            onVariant = 0xFF676E95
        ),
        darkTheme(
            id = "material_darker",
            name = "Material Theme (Darker)",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF212121,
            surface = 0xFF2C2C2C,
            variant = 0xFF383838,
            primary = 0xFF82AAFF,
            secondary = 0xFFC792EA,
            tertiary = 0xFFFFCB6B,
            onBg = 0xFFEEFFFF,
            onSurface = 0xFFFFFFFF,
            onVariant = 0xFF8A8A8A
        ),
        darkTheme(
            id = "catppuccin_mocha",
            name = "Catppuccin (Mocha)",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF1E1E2E,
            surface = 0xFF313244,
            variant = 0xFF45475A,
            primary = 0xFFCBA6F7,
            secondary = 0xFF89B4FA,
            tertiary = 0xFFF38BA8,
            onBg = 0xFFCDD6F4,
            onSurface = 0xFFCDD6F4,
            onVariant = 0xFFA6ADC8
        ),
        darkTheme(
            id = "kanagawa",
            name = "Kanagawa",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF1F1F28,
            surface = 0xFF2A2A37,
            variant = 0xFF363646,
            primary = 0xFF7E9CD8,
            secondary = 0xFF957FB8,
            tertiary = 0xFFE6C384,
            onBg = 0xFFDCD7BA,
            onSurface = 0xFFEAE5C9,
            onVariant = 0xFF938AA9
        ),
        darkTheme(
            id = "rose_pine",
            name = "Rose Pine",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF191724,
            surface = 0xFF26233A,
            variant = 0xFF342F4D,
            primary = 0xFFEBBCBA,
            secondary = 0xFFC4A7E7,
            tertiary = 0xFF9CCFD8,
            onBg = 0xFFE0DEF4,
            onSurface = 0xFFE0DEF4,
            onVariant = 0xFF908CAA
        ),
        darkTheme(
            id = "github_dark",
            name = "GitHub Dark",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF0D1117,
            surface = 0xFF161B22,
            variant = 0xFF21262D,
            primary = 0xFF58A6FF,
            secondary = 0xFFBC8CFF,
            tertiary = 0xFF3FB950,
            onBg = 0xFFC9D1D9,
            onSurface = 0xFFF0F6FC,
            onVariant = 0xFF8B949E,
            pairedId = "github_light"
        ),
        lightTheme(
            id = "github_light",
            name = "GitHub Light",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFFFFFFFF,
            surface = 0xFFF6F8FA,
            variant = 0xFFEAECEF,
            primary = 0xFF0969DA,
            secondary = 0xFF8250DF,
            tertiary = 0xFF1A7F37,
            onBg = 0xFF1F2328,
            onSurface = 0xFF1F2328,
            onVariant = 0xFF656D76,
            pairedId = "github_dark"
        ),
        darkTheme(
            id = "panda_theme",
            name = "Panda Theme",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF292A2B,
            surface = 0xFF373B41,
            variant = 0xFF454A52,
            primary = 0xFFFF75B5,
            secondary = 0xFF19F9D8,
            tertiary = 0xFFFFB86C,
            onBg = 0xFFE6E6E6,
            onSurface = 0xFFF5F5F5,
            onVariant = 0xFF8F96A0
        ),
        darkTheme(
            id = "atom_one_dark",
            name = "Atom One Dark",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF282C34,
            surface = 0xFF21252B,
            variant = 0xFF343A46,
            primary = 0xFF61AFEF,
            secondary = 0xFFC678DD,
            tertiary = 0xFF98C379,
            onBg = 0xFFABB2BF,
            onSurface = 0xFFE5E9F0,
            onVariant = 0xFF7D8593
        ),
        darkTheme(
            id = "tokyo_night",
            name = "Tokyo Night",
            group = AppThemeGroup.CLASSIC,
            bg = 0xFF1A1B26,
            surface = 0xFF24283B,
            variant = 0xFF2F3549,
            primary = 0xFF7AA2F7,
            secondary = 0xFFBB9AF7,
            tertiary = 0xFF7DCFFF,
            onBg = 0xFFA9B1D6,
            onSurface = 0xFFC0CAF5,
            onVariant = 0xFF787C99
        ),

        // Liquid Glass Themes
        darkTheme(
            id = "obsidian_liquid_glass",
            name = "Obsidian Liquid Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF070A12,
            surface = 0xFF0F172A,
            variant = 0xFF1E293B,
            primary = 0xFF38BDF8,
            secondary = 0xFF8B5CF6,
            tertiary = 0xFFEC4899,
            pairedId = "frosted_arctic_glass"
        ),
        lightTheme(
            id = "frosted_arctic_glass",
            name = "Frosted Arctic Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFFF4F8FF,
            surface = 0xFFFFFFFF,
            variant = 0xFFE8F1FD,
            primary = 0xFF0284C7,
            secondary = 0xFF38BDF8,
            tertiary = 0xFF8B5CF6,
            pairedId = "obsidian_liquid_glass"
        ),
        darkTheme(
            id = "cyber_neon_liquid",
            name = "Cyber Neon Liquid",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF090514,
            surface = 0xFF150B2E,
            variant = 0xFF231347,
            primary = 0xFF00F5D4,
            secondary = 0xFFFF2A85,
            tertiary = 0xFFFEE440
        ),
        darkTheme(
            id = "smoked_quartz_glass",
            name = "Smoked Quartz Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF141214,
            surface = 0xFF242024,
            variant = 0xFF342E34,
            primary = 0xFFE2C290,
            secondary = 0xFFB8A9C9,
            tertiary = 0xFFD4A373
        ),
        darkTheme(
            id = "midnight_velvet_glass",
            name = "Midnight Velvet Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF0B0719,
            surface = 0xFF181132,
            variant = 0xFF261C4A,
            primary = 0xFFC084FC,
            secondary = 0xFFF472B6,
            tertiary = 0xFF60A5FA
        ),
        darkTheme(
            id = "aurora_borealis_glass",
            name = "Aurora Borealis Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF04131A,
            surface = 0xFF0A242E,
            variant = 0xFF123642,
            primary = 0xFF34D399,
            secondary = 0xFF22D3EE,
            tertiary = 0xFFA78BFA
        ),
        lightTheme(
            id = "crystal_prism_liquid",
            name = "Crystal Prism Liquid",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFFF5F7FF,
            surface = 0xFFFFFFFF,
            variant = 0xFFECEFFD,
            primary = 0xFF7C3AED,
            secondary = 0xFF0EA5E9,
            tertiary = 0xFFEC4899
        ),
        darkTheme(
            id = "deep_ocean_glass",
            name = "Deep Ocean Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF041423,
            surface = 0xFF0A233B,
            variant = 0xFF123354,
            primary = 0xFF38BDF8,
            secondary = 0xFF2DD4BF,
            tertiary = 0xFF6366F1
        ),
        darkTheme(
            id = "amber_mercury_glass",
            name = "Amber Mercury Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF150E06,
            surface = 0xFF261A0C,
            variant = 0xFF382714,
            primary = 0xFFFBBF24,
            secondary = 0xFFFB923C,
            tertiary = 0xFFFDE68A
        ),
        lightTheme(
            id = "titanium_mist_glass",
            name = "Titanium Mist Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFFF1F5F9,
            surface = 0xFFFFFFFF,
            variant = 0xFFE2E8F0,
            primary = 0xFF475569,
            secondary = 0xFF0EA5E9,
            tertiary = 0xFF64748B
        ),
        darkTheme(
            id = "holographic_liquid_glass",
            name = "Holographic Liquid Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF0F1020,
            surface = 0xFF1B1E38,
            variant = 0xFF292E52,
            primary = 0xFFF472B6,
            secondary = 0xFF38BDF8,
            tertiary = 0xFFA78BFA
        ),
        darkTheme(
            id = "carbon_blur_glass",
            name = "Carbon Blur Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF0B0C0E,
            surface = 0xFF181A1F,
            variant = 0xFF252830,
            primary = 0xFF94A3B8,
            secondary = 0xFF38BDF8,
            tertiary = 0xFFE2E8F0
        ),
        darkTheme(
            id = "emerald_translucent_glass",
            name = "Emerald Translucent Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF041610,
            surface = 0xFF09291E,
            variant = 0xFF103E2E,
            primary = 0xFF10B981,
            secondary = 0xFF34D399,
            tertiary = 0xFF2DD4BF
        ),
        darkTheme(
            id = "ultraviolet_frost_glass",
            name = "Ultraviolet Frost Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF120624,
            surface = 0xFF210D3F,
            variant = 0xFF311659,
            primary = 0xFFA855F7,
            secondary = 0xFFE879F9,
            tertiary = 0xFF6366F1
        ),
        darkTheme(
            id = "sapphire_liquid_glass",
            name = "Sapphire Liquid Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF050E26,
            surface = 0xFF0D1E4A,
            variant = 0xFF162E69,
            primary = 0xFF3B82F6,
            secondary = 0xFF60A5FA,
            tertiary = 0xFF38BDF8
        ),
        darkTheme(
            id = "pearlescent_dark_glass",
            name = "Pearlescent Dark Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF111319,
            surface = 0xFF1E222D,
            variant = 0xFF2B3040,
            primary = 0xFFE2E8F0,
            secondary = 0xFFF0ABFC,
            tertiary = 0xFF7DD3FC
        ),
        darkTheme(
            id = "ruby_refractive_glass",
            name = "Ruby Refractive Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF1B050B,
            surface = 0xFF310B16,
            variant = 0xFF471222,
            primary = 0xFFFB7185,
            secondary = 0xFFF43F5E,
            tertiary = 0xFFFDA4AF
        ),
        darkTheme(
            id = "chromatic_aberration_glass",
            name = "Chromatic Aberration Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF080810,
            surface = 0xFF141424,
            variant = 0xFF212138,
            primary = 0xFFFF3366,
            secondary = 0xFF00F0FF,
            tertiary = 0xFFFFE600
        ),
        darkTheme(
            id = "liquid_mercury_dark",
            name = "Liquid Mercury Dark",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF0F1115,
            surface = 0xFF1D2129,
            variant = 0xFF2B313D,
            primary = 0xFFCBD5E1,
            secondary = 0xFF94A3B8,
            tertiary = 0xFF38BDF8
        ),
        darkTheme(
            id = "bioluminescent_glass",
            name = "Bioluminescent Glass",
            group = AppThemeGroup.LIQUID_GLASS,
            bg = 0xFF021319,
            surface = 0xFF06242E,
            variant = 0xFF0C3644,
            primary = 0xFF22D3EE,
            secondary = 0xFF4ADE80,
            tertiary = 0xFF818CF8
        )
    )

    val defaultDark: AppThemePreset = allThemes.first { it.id == AppThemePreset.DEFAULT_DARK_ID }
    val defaultLight: AppThemePreset = allThemes.first { it.id == AppThemePreset.DEFAULT_LIGHT_ID }

    private val byId: Map<String, AppThemePreset> = allThemes.associateBy { it.id }

    fun findById(id: String?): AppThemePreset? = if (id == null) null else byId[id]

    fun resolve(themeId: String?, isDarkFallback: Boolean): AppThemePreset {
        return byId[themeId] ?: if (isDarkFallback) defaultDark else defaultLight
    }
}
