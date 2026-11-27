package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.components.TableEditorDialog
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.test.onNodeWithText
import com.example.ui.components.ProgressBarPickerDialog
import com.example.ui.components.StarRatingPickerDialog
import com.example.ui.components.StatusBadgePickerDialog
import org.junit.Assert.assertEquals
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class TableEditorDialogCenteringTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun insertTableDialogIsCenteredOnScreenDuringEditing() {
        composeRule.setContent {
            TableEditorDialog(
                initialTable = null,
                isDark = true,
                isArabic = true,
                accentColor = Color(0xFF0EA5E9),
                textColor = Color.White,
                metaColor = Color(0xFF94A3B8),
                onDismiss = {},
                onConfirmTable = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("table_editor_dialog").assertIsDisplayed()

        // Tap a cell to enter editing state
        composeRule.onNodeWithTag("table_cell_1_0").performClick()
        composeRule.waitForIdle()

        val bounds = composeRule.onNodeWithTag("table_editor_dialog").getUnclippedBoundsInRoot()
        val cardCenterX = (bounds.left.value + bounds.right.value) / 2f
        val cardCenterY = (bounds.top.value + bounds.bottom.value) / 2f

        // Screen is 411dp x 891dp -> expected center is ~205.5dp x ~445.5dp
        assertTrue(
            "Expected horizontal center near 205.5dp, got $cardCenterX",
            abs(cardCenterX - 205.5f) < 24f
        )
        assertTrue(
            "Expected vertical center near 445.5dp, got $cardCenterY",
            abs(cardCenterY - 445.5f) < 40f
        )
    }

    @Test
    fun starRatingPickerAllowsChoosingStarCountBeforeInsert() {
        var confirmedStars = -1
        composeRule.setContent {
            StarRatingPickerDialog(
                isDark = true,
                isArabic = true,
                accentColor = Color(0xFF0EA5E9),
                textColor = Color.White,
                metaColor = Color(0xFF94A3B8),
                onDismiss = {},
                onConfirmStars = { confirmedStars = it }
            )
        }

        composeRule.onNodeWithTag("star_rating_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("star_rating_option_5").performClick()
        composeRule.onNodeWithText("★★★★★ (5/5)").assertIsDisplayed()
        composeRule.onNodeWithTag("star_rating_confirm_btn").performClick()
        composeRule.waitForIdle()

        assertEquals(5, confirmedStars)
    }

    @Test
    fun progressBarPickerAllowsChoosingPercentBeforeInsert() {
        var confirmedPercent = -1
        composeRule.setContent {
            ProgressBarPickerDialog(
                isDark = true,
                isArabic = true,
                accentColor = Color(0xFF0EA5E9),
                textColor = Color.White,
                metaColor = Color(0xFF94A3B8),
                onDismiss = {},
                onConfirmProgress = { pct, _ -> confirmedPercent = pct }
            )
        }

        composeRule.onNodeWithTag("progress_bar_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("progress_preset_75").performClick()
        composeRule.onNodeWithTag("progress_confirm_btn").performClick()
        composeRule.waitForIdle()

        assertEquals(75, confirmedPercent)
    }

    @Test
    fun statusBadgePickerAllowsChoosingStatusBeforeInsert() {
        var confirmedEmoji = ""
        var confirmedLabel = ""
        composeRule.setContent {
            StatusBadgePickerDialog(
                isDark = true,
                isArabic = true,
                accentColor = Color(0xFF0EA5E9),
                textColor = Color.White,
                metaColor = Color(0xFF94A3B8),
                onDismiss = {},
                onConfirmBadge = { emoji, label ->
                    confirmedEmoji = emoji
                    confirmedLabel = label
                }
            )
        }

        composeRule.onNodeWithTag("status_badge_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("status_badge_option_completed").performClick()
        composeRule.onNodeWithTag("status_badge_confirm_btn").performClick()
        composeRule.waitForIdle()

        assertEquals("✅", confirmedEmoji)
        assertEquals("مكتمل", confirmedLabel)
    }
}
