package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.example.data.model.FabMenuStyle
import com.example.ui.components.FluffyBottomBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FabDragSelectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun quickTapOnPlusStillOpensMenu() {
        var openedNote = false
        composeRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                FluffyBottomBar(
                    isDark = false,
                    isArabic = false,
                    onNewNote = { openedNote = true },
                    onNewChecklist = {},
                    onNewVoiceNote = {},
                    onOpenScanner = {},
                    fabMenuStyle = FabMenuStyle.VERTICAL_STACK
                )
            }
        }

        composeRule.onNodeWithTag("floating_new_note_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("action_new_note_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("action_doc_scanner_btn").assertIsDisplayed()
        assertFalse(openedNote)
    }

    @Test
    fun pressAndDragToScannerTriggersScannerOnRelease_verticalStack() {
        var scannerOpened = 0
        composeRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                FluffyBottomBar(
                    isDark = false,
                    isArabic = false,
                    onNewNote = {},
                    onNewChecklist = {},
                    onNewVoiceNote = {},
                    onOpenScanner = { scannerOpened++ },
                    fabMenuStyle = FabMenuStyle.VERTICAL_STACK
                )
            }
        }

        val plusNode = composeRule.onNodeWithTag("floating_new_note_button")
        plusNode.performTouchInput {
            down(center)
            // Drag upward past touchSlop to expand menu
            moveBy(Offset(0f, -60f))
        }
        composeRule.waitForIdle()

        val targetBounds = composeRule.onNodeWithTag("action_doc_scanner_btn")
            .fetchSemanticsNode()
            .boundsInRoot
        val collapseNode = composeRule.onNodeWithTag("action_collapse_dock_btn")
        val anchorBounds = collapseNode.fetchSemanticsNode().boundsInRoot

        val deltaToScanner = targetBounds.center - anchorBounds.topLeft
        collapseNode.performTouchInput {
            moveTo(deltaToScanner)
        }
        composeRule.waitForIdle()

        collapseNode.performTouchInput {
            up()
        }
        composeRule.waitForIdle()

        assertEquals(1, scannerOpened)
    }

    @Test
    fun pressAndDragToNoteTriggersNoteOnRelease_originalCapsule() {
        var noteOpened = 0
        composeRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                FluffyBottomBar(
                    isDark = true,
                    isArabic = false,
                    onNewNote = { noteOpened++ },
                    onNewChecklist = {},
                    onNewVoiceNote = {},
                    onOpenScanner = {},
                    fabMenuStyle = FabMenuStyle.ORIGINAL
                )
            }
        }

        val dockNode = composeRule.onNodeWithTag("morphing_action_dock")
        dockNode.performTouchInput {
            down(center)
            moveBy(Offset(-60f, 0f))
        }
        composeRule.waitForIdle()

        val targetBounds = composeRule.onNodeWithTag("action_new_note_btn")
            .fetchSemanticsNode()
            .boundsInRoot
        val dockBounds = dockNode.fetchSemanticsNode().boundsInRoot

        val localTarget = targetBounds.center - dockBounds.topLeft
        dockNode.performTouchInput {
            moveTo(localTarget)
        }
        composeRule.waitForIdle()

        dockNode.performTouchInput {
            up()
        }
        composeRule.waitForIdle()

        assertEquals(1, noteOpened)
    }

    @Test
    fun pressAndDragIntoEmptySpaceCollapsesMenuWithoutTriggeringAction() {
        var anyTriggered = false
        composeRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                FluffyBottomBar(
                    isDark = false,
                    isArabic = true,
                    onNewNote = { anyTriggered = true },
                    onNewChecklist = { anyTriggered = true },
                    onNewVoiceNote = { anyTriggered = true },
                    onOpenScanner = { anyTriggered = true },
                    fabMenuStyle = FabMenuStyle.CONSTELLATION
                )
            }
        }

        val plusNode = composeRule.onNodeWithTag("floating_new_note_button")
        plusNode.performTouchInput {
            down(center)
            moveBy(Offset(350f, -400f))
            up()
        }
        composeRule.waitForIdle()

        assertFalse(anyTriggered)
        assertTrue(
            composeRule.onAllNodes(androidx.compose.ui.test.hasTestTag("action_new_note_btn"))
                .fetchSemanticsNodes()
                .isEmpty()
        )
    }

    @Test
    fun longPressOnPlusWithoutDraggingShowsMenuThenCollapsesOnRelease() {
        var anyTriggered = false
        composeRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                FluffyBottomBar(
                    isDark = false,
                    isArabic = false,
                    onNewNote = { anyTriggered = true },
                    onNewChecklist = { anyTriggered = true },
                    onNewVoiceNote = { anyTriggered = true },
                    onOpenScanner = { anyTriggered = true },
                    fabMenuStyle = FabMenuStyle.VERTICAL_STACK
                )
            }
        }

        val plusNode = composeRule.onNodeWithTag("floating_new_note_button")
        plusNode.performTouchInput {
            down(center)
        }
        composeRule.mainClock.advanceTimeBy(220L)
        composeRule.waitForIdle()

        // Menu is open while holding
        composeRule.onNodeWithTag("action_doc_scanner_btn").assertIsDisplayed()

        // Releasing without sliding to any action collapses the menu
        composeRule.onNodeWithTag("action_collapse_dock_btn").performTouchInput {
            up()
        }
        composeRule.waitForIdle()

        assertFalse(anyTriggered)
        assertTrue(
            composeRule.onAllNodes(androidx.compose.ui.test.hasTestTag("action_doc_scanner_btn"))
                .fetchSemanticsNodes()
                .isEmpty()
        )
    }

    @Test
    fun immediateSlideToScannerAndInstantReleaseTriggersWithoutWaiting() {
        var scannerOpened = 0
        composeRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                FluffyBottomBar(
                    isDark = false,
                    isArabic = false,
                    onNewNote = {},
                    onNewChecklist = {},
                    onNewVoiceNote = {},
                    onOpenScanner = { scannerOpened++ },
                    fabMenuStyle = FabMenuStyle.ORIGINAL
                )
            }
        }

        val dockNode = composeRule.onNodeWithTag("morphing_action_dock")
        dockNode.performTouchInput {
            down(center)
            // Drag left past touchSlop to enter drag mode and expand dock
            moveBy(Offset(-55f, 0f))
        }
        // Only advance 30ms (early in spring animation) before moving to target and releasing immediately
        composeRule.mainClock.advanceTimeBy(30L)
        composeRule.waitForIdle()

        val targetBounds = composeRule.onNodeWithTag("action_doc_scanner_btn")
            .fetchSemanticsNode()
            .boundsInRoot
        val dockBounds = dockNode.fetchSemanticsNode().boundsInRoot
        val localTarget = targetBounds.center - dockBounds.topLeft

        dockNode.performTouchInput {
            moveTo(localTarget)
            up()
        }
        composeRule.waitForIdle()

        assertEquals(1, scannerOpened)
    }
}
