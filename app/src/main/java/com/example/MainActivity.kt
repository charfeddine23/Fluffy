package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.FluffyHomeScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.theme.FluffyTheme
import com.example.viewmodel.FluffyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FluffyViewModel = viewModel()
            val isDark by viewModel.isDarkMode.collectAsState()
            val isArabic by viewModel.isArabic.collectAsState()
            val editingNote by viewModel.editingNote.collectAsState()

            val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                FluffyTheme(darkTheme = isDark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        AnimatedContent(
                            targetState = editingNote,
                            transitionSpec = {
                                if (targetState != null) {
                                    (slideInHorizontally { width -> if (isArabic) -width else width } + fadeIn())
                                        .togetherWith(slideOutHorizontally { width -> if (isArabic) width else -width } + fadeOut())
                                } else {
                                    (slideInHorizontally { width -> if (isArabic) width else -width } + fadeIn())
                                        .togetherWith(slideOutHorizontally { width -> if (isArabic) -width else width } + fadeOut())
                                }
                            },
                            label = "screen_transition"
                        ) { activeNote ->
                            if (activeNote != null) {
                                NoteEditorScreen(
                                    note = activeNote,
                                    isDark = isDark,
                                    isArabic = isArabic,
                                    onBack = { viewModel.closeEditor() },
                                    onSave = { title, content, category, colorPreset, isPinned, isFav, checklist, audioDuration ->
                                        viewModel.saveNote(
                                            title = title,
                                            content = content,
                                            category = category,
                                            colorPreset = colorPreset,
                                            isPinned = isPinned,
                                            isFavorite = isFav,
                                            checklist = checklist,
                                            audioDurationSec = audioDuration
                                        )
                                    },
                                    onDelete = { viewModel.deleteCurrentNote() }
                                )
                            } else {
                                FluffyHomeScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
