package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val labelAr: String,
    val labelFr: String,
    val labelEn: String,
    val labelTzm: String,
    val shortBadge: String,
    val isRtl: Boolean
) {
    ARABIC(
        code = "ar",
        nativeName = "العربية",
        labelAr = "العربية",
        labelFr = "Arabe",
        labelEn = "Arabic",
        labelTzm = "Taɛrabt",
        shortBadge = "ع",
        isRtl = true
    ),
    FRENCH(
        code = "fr",
        nativeName = "Français",
        labelAr = "الفرنسية",
        labelFr = "Français",
        labelEn = "French",
        labelTzm = "Tafṛansist",
        shortBadge = "FR",
        isRtl = false
    ),
    ENGLISH(
        code = "en",
        nativeName = "English",
        labelAr = "الإنجليزية",
        labelFr = "Anglais",
        labelEn = "English",
        labelTzm = "Taglizit",
        shortBadge = "EN",
        isRtl = false
    ),
    TAMAZIGHT(
        code = "tzm",
        nativeName = "ⵜⴰⵎⴰⵣⵉⵖⵜ (Dzair)",
        labelAr = "الأمازيغية الجزائرية",
        labelFr = "Tamazight (Algérie)",
        labelEn = "Algerian Amazigh",
        labelTzm = "Tamaziɣt n Dzayer",
        shortBadge = "ⵣ",
        isRtl = false
    );

    fun localizedSubtitle(current: AppLanguage): String = when (current) {
        ARABIC -> labelAr
        FRENCH -> labelFr
        ENGLISH -> labelEn
        TAMAZIGHT -> labelTzm
    }

    companion object {
        fun fromCode(code: String?, fallbackArabic: Boolean = true): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: if (fallbackArabic) ARABIC else ENGLISH
        }
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ARABIC }

fun translateText(
    language: AppLanguage,
    ar: String,
    fr: String,
    en: String,
    tzm: String
): String = when (language) {
    AppLanguage.ARABIC -> ar
    AppLanguage.FRENCH -> fr
    AppLanguage.ENGLISH -> en
    AppLanguage.TAMAZIGHT -> tzm
}

@Composable
fun tr(
    ar: String,
    fr: String,
    en: String,
    tzm: String
): String = translateText(LocalAppLanguage.current, ar, fr, en, tzm)
