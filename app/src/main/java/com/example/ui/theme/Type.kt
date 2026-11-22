package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Shared editor & reader font family.
 * [FontFamily.SansSerif] maps to system Sans-Serif (which includes native Italic & BoldItalic
 * font files for Latin) and pairs with [FontSynthesis.All] so Arabic and fallback scripts
 * synthesize a clean oblique slant when [androidx.compose.ui.text.font.FontStyle.Italic] is active.
 */
val EditorFontFamily: FontFamily = FontFamily.SansSerif

// Set of Material typography styles
val Typography =
  Typography(
    bodyLarge =
      TextStyle(
        fontFamily = EditorFontFamily,
        fontSynthesis = FontSynthesis.All,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = EditorFontFamily,
        fontSynthesis = FontSynthesis.All,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = EditorFontFamily,
        fontSynthesis = FontSynthesis.All,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = EditorFontFamily,
        fontSynthesis = FontSynthesis.All,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
      ),
  )
