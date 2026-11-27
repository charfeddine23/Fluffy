package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.example.data.model.NoteEntity
import com.example.ui.screens.NoteEditorScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class NoteEditorDeletionRegressionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editingAndDeletingWordsAndLinesInWritingModeDoesNotCrash() {
        var latestSavedContent = ""
        val note = NoteEntity(
            id = 1L,
            title = "ملاحظة تجريبية",
            content = "السطر الأول **مهم**\n- [ ] مهمة ثانية\nكلمة أخيرة",
            category = "Personal"
        )

        composeRule.setContent {
            NoteEditorScreen(
                note = note,
                isCreatingNew = false,
                initialEditMode = true,
                isDark = true,
                isArabic = true,
                onBack = {},
                onSave = { _, content, _, _, _, _, _, _, _, _, _, _, _, _ ->
                    latestSavedContent = content
                },
                onDelete = {},
                onAutoSave = { _, content, _, _, _, _, _, _, _, _, _, _, _, _ ->
                    latestSavedContent = content
                }
            )
        }

        composeRule.waitForIdle()
        val editorNode = composeRule.onNodeWithTag("editor_content_input")
        editorNode.assertIsDisplayed()

        // 1. Delete entire first line leaving a leading '\n'
        editorNode.performTextReplacement("\n- [ ] مهمة ثانية\nكلمة أخيرة")
        composeRule.waitForIdle()

        // 2. Delete words leaving plain line
        editorNode.performTextReplacement("\nكلمة")
        composeRule.waitForIdle()

        // 3. Delete all remaining text
        editorNode.performTextReplacement("")
        composeRule.waitForIdle()

        // 4. Re-type Arabic text after full deletion
        editorNode.performTextReplacement("نص جديد بعد الحذف")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("editor_save_button").assertIsDisplayed()
        composeRule.onNodeWithTag("editor_save_button").performClick()
        composeRule.waitForIdle()
        assertEquals("نص جديد بعد الحذف", latestSavedContent)
    }
}
