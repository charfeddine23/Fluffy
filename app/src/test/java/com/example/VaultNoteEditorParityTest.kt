package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.model.NoteEntity
import com.example.data.model.VaultItemEntity
import com.example.ui.screens.VaultScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class VaultNoteEditorParityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleVaultItem(): Pair<VaultItemEntity, NoteEntity> {
        val item = VaultItemEntity(
            id = "vault_note_1",
            title = "Secret Plan",
            literalContent = "**Step 1** complete",
            fileName = "",
            mimeType = "text/plain; charset=utf-8",
            localFilePath = "",
            byteSize = 32L,
            sha256 = "abc",
            createdAt = 1_700_000_000_000L,
            isImmutable = true
        )
        val meta = NoteEntity(
            id = 0L,
            title = "Secret Plan",
            content = "**Step 1** complete",
            category = "Personal",
            syncId = item.id
        )
        return item to meta
    }

    @Test
    fun tappingVaultNoteOpensIdenticalReadModeAndTransitionsToEditMode() {
        val (item, meta) = sampleVaultItem()
        composeRule.setContent {
            VaultScreen(
                vaultItems = listOf(item),
                isDark = true,
                isArabic = false,
                vaultNoteMetaMap = mapOf(item.id to meta),
                onExportItemToUri = { _, _ -> },
                onBack = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vault_card_${item.id}").performClick()
        composeRule.waitForIdle()

        // Must render regular NoteEditorScreen Read-Only UI
        composeRule.onNodeWithTag("editor_content_readonly").assertIsDisplayed()
        composeRule.onNodeWithTag("editor_edit_button").assertIsDisplayed()

        // Clicking Edit inside NoteEditorScreen switches seamlessly into regular Edit mode
        composeRule.onNodeWithTag("editor_edit_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("editor_content_input").assertIsDisplayed()
        composeRule.onNodeWithTag("editor_save_button").assertIsDisplayed()
    }

    @Test
    fun clickingSaveInVaultEditModeUpdatesNoteAndReturnsToVaultList() {
        val (item, meta) = sampleVaultItem()
        var savedTitle = ""
        composeRule.setContent {
            VaultScreen(
                vaultItems = listOf(item),
                isDark = true,
                isArabic = false,
                vaultNoteMetaMap = mapOf(item.id to meta),
                onUpdateVaultNoteFull = { _, _, title, _, _, _, _, _, _, _, _, _, _, _, _, _, showFeedback ->
                    if (showFeedback) {
                        savedTitle = title
                    }
                },
                onExportItemToUri = { _, _ -> },
                onBack = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vault_edit_item_${item.id}").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("editor_content_input").assertIsDisplayed()
        composeRule.onNodeWithTag("editor_save_button").assertIsDisplayed()
        composeRule.onNodeWithTag("editor_save_button").performClick()
        composeRule.waitForIdle()

        assertTrue(savedTitle == "Secret Plan")
        composeRule.onNodeWithTag("vault_screen").assertIsDisplayed()
    }
}
