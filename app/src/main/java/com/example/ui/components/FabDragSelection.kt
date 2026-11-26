package com.example.ui.components

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * Coordinates press-and-drag selection from the floating '+' button across all 5 FAB menu styles.
 */
internal class FabDragSelectionController {
    var isDragSessionActive by mutableStateOf(false)
        private set

    var hoveredActionTag by mutableStateOf<String?>(null)
        private set

    var pointerRootOffset by mutableStateOf<Offset?>(null)
        private set

    var boundsVersion by mutableIntStateOf(0)
        private set

    private val actionBounds = mutableStateMapOf<String, Rect>()
    private var cancelBounds: Rect? = null

    fun registerActionBounds(tag: String, rect: Rect) {
        actionBounds[tag] = rect
        boundsVersion++
    }

    fun unregisterActionBounds(tag: String) {
        if (actionBounds.remove(tag) != null) {
            boundsVersion++
        }
    }

    fun registerCancelBounds(rect: Rect) {
        cancelBounds = rect
    }

    fun clearCancelBounds() {
        cancelBounds = null
    }

    fun beginDragSession(initialRootOffset: Offset?) {
        isDragSessionActive = true
        pointerRootOffset = initialRootOffset
        hoveredActionTag = null
    }

    fun updatePointer(
        rootOffset: Offset?,
        hitSlopPx: Float,
        topExtraSlopPx: Float = hitSlopPx,
        allowSelection: Boolean
    ): String? {
        pointerRootOffset = rootOffset
        if (!allowSelection || rootOffset == null) {
            hoveredActionTag = null
            return null
        }

        var bestTag: String? = null
        var bestDist = Float.MAX_VALUE
        actionBounds.forEach { (tag, rect) ->
            val expanded = Rect(
                left = rect.left - hitSlopPx,
                top = rect.top - topExtraSlopPx,
                right = rect.right + hitSlopPx,
                bottom = rect.bottom + hitSlopPx
            )
            if (expanded.contains(rootOffset)) {
                val dist = hypot(
                    (rootOffset.x - rect.center.x).toDouble(),
                    (rootOffset.y - rect.center.y).toDouble()
                ).toFloat()
                if (dist < bestDist) {
                    bestDist = dist
                    bestTag = tag
                }
            }
        }

        val cancelRect = cancelBounds
        if (cancelRect != null) {
            val distToCancel = hypot(
                (rootOffset.x - cancelRect.center.x).toDouble(),
                (rootOffset.y - cancelRect.center.y).toDouble()
            ).toFloat()
            val cancelRadius = maxOf(cancelRect.width, cancelRect.height) * 0.44f
            val directlyInsideBestRect = bestTag?.let { tag ->
                actionBounds[tag]?.contains(rootOffset) == true
            } ?: false
            if (distToCancel <= cancelRadius && !directlyInsideBestRect) {
                val closerActionExists = bestTag != null && bestDist < distToCancel * 0.85f
                if (!closerActionExists) {
                    hoveredActionTag = null
                    return null
                }
            }
        }

        hoveredActionTag = bestTag
        return bestTag
    }

    fun endDragSession() {
        isDragSessionActive = false
        hoveredActionTag = null
        pointerRootOffset = null
    }
}

@Composable
internal fun rememberFabDragSelectionController(): FabDragSelectionController {
    return remember { FabDragSelectionController() }
}

internal fun Modifier.fabDragTarget(
    controller: FabDragSelectionController,
    tag: String
): Modifier = composed {
    DisposableEffect(controller, tag) {
        onDispose {
            controller.unregisterActionBounds(tag)
        }
    }
    this.onGloballyPositioned { coords ->
        if (coords.isAttached) {
            controller.registerActionBounds(tag, coords.boundsInRoot())
        }
    }
}

internal fun Modifier.fabCancelTarget(
    controller: FabDragSelectionController,
    active: Boolean = true
): Modifier = composed {
    DisposableEffect(controller, active) {
        if (!active) {
            controller.clearCancelBounds()
        }
        onDispose {
            controller.clearCancelBounds()
        }
    }
    if (active) {
        this.onGloballyPositioned { coords ->
            if (coords.isAttached) {
                controller.registerCancelBounds(coords.boundsInRoot())
            }
        }
    } else {
        this
    }
}

/**
 * Intercepts a hold (>= 165ms) or drag (> touchSlop) starting on the collapsed '+' button,
 * expands the add menu, tracks the finger across registered action targets, and invokes
 * the hovered option on release (while keeping quick taps working as normal expand clicks).
 */
internal fun Modifier.fabPressAndDragAnchor(
    controller: FabDragSelectionController,
    isExpanded: Boolean,
    expandProgress: Float,
    actions: List<FabActionItem>,
    hitSlopDp: Dp = 22.dp,
    topExtraSlopDp: Dp = 26.dp,
    onExpand: () -> Unit,
    onCollapse: () -> Unit
): Modifier = composed {
    val isExpandedState by rememberUpdatedState(isExpanded)
    val actionsState by rememberUpdatedState(actions)
    val onExpandState by rememberUpdatedState(onExpand)
    val onCollapseState by rememberUpdatedState(onCollapse)
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val hitSlopPx = with(density) { hitSlopDp.toPx() }
    val topExtraSlopPx = with(density) { topExtraSlopDp.toPx() }
    var anchorCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(
        controller.isDragSessionActive,
        controller.pointerRootOffset,
        controller.boundsVersion,
        expandProgress
    ) {
        if (controller.isDragSessionActive) {
            val prevTag = controller.hoveredActionTag
            val newTag = controller.updatePointer(
                rootOffset = controller.pointerRootOffset,
                hitSlopPx = hitSlopPx,
                topExtraSlopPx = topExtraSlopPx,
                allowSelection = true
            )
            if (newTag != null && newTag != prevTag) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    this
        .onGloballyPositioned { coords ->
            anchorCoords = coords
        }
        .pointerInput(controller) {
            val touchSlop = viewConfiguration.touchSlop
            val holdThresholdMs = 165L
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial
                )
                if (isExpandedState) {
                    return@awaitEachGesture
                }

                val downRootPos = anchorCoords?.takeIf { it.isAttached }?.localToRoot(down.position)
                val startTimeMs = SystemClock.uptimeMillis()
                var dragModeEntered = false
                var maxDragDistance = 0f
                var lastLocalPos = down.position

                while (!dragModeEntered) {
                    val elapsed = SystemClock.uptimeMillis() - startTimeMs
                    val remaining = (holdThresholdMs - elapsed).coerceAtLeast(1L)
                    val event = withTimeoutOrNull(remaining) {
                        awaitPointerEvent(PointerEventPass.Initial)
                    }
                    if (event == null) {
                        dragModeEntered = true
                        break
                    }
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    lastLocalPos = change.position
                    val dist = (change.position - down.position).getDistance()
                    if (dist > maxDragDistance) {
                        maxDragDistance = dist
                    }
                    if (!change.pressed || change.changedToUpIgnoreConsumed()) {
                        return@awaitEachGesture
                    }
                    if (dist > touchSlop) {
                        dragModeEntered = true
                        change.consume()
                        break
                    }
                }

                if (!dragModeEntered) {
                    return@awaitEachGesture
                }

                val initialRoot = anchorCoords?.takeIf { it.isAttached }?.localToRoot(lastLocalPos) ?: downRootPos
                controller.beginDragSession(initialRoot)
                if (!isExpandedState) {
                    onExpandState()
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: event.changes.firstOrNull()
                            ?: break

                        val rootPos = anchorCoords?.takeIf { it.isAttached }?.localToRoot(change.position)
                        if (rootPos != null && downRootPos != null) {
                            val travel = (rootPos - downRootPos).getDistance()
                            if (travel > maxDragDistance) maxDragDistance = travel
                        } else {
                            val localDist = (change.position - down.position).getDistance()
                            if (localDist > maxDragDistance) maxDragDistance = localDist
                        }

                        if (!change.pressed || change.changedToUpIgnoreConsumed()) {
                            change.consume()
                            val finalTag = controller.updatePointer(
                                rootOffset = rootPos ?: controller.pointerRootOffset,
                                hitSlopPx = hitSlopPx,
                                topExtraSlopPx = topExtraSlopPx,
                                allowSelection = true
                            ) ?: controller.hoveredActionTag
                            val matchedAction = actionsState.firstOrNull { it.testTag == finalTag }
                            if (matchedAction != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                matchedAction.onClick()
                            } else {
                                onCollapseState()
                            }
                            break
                        }

                        change.consume()
                        val prevTag = controller.hoveredActionTag
                        val newTag = controller.updatePointer(
                            rootOffset = rootPos,
                            hitSlopPx = hitSlopPx,
                            topExtraSlopPx = topExtraSlopPx,
                            allowSelection = true
                        )
                        if (newTag != null && newTag != prevTag) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                } finally {
                    controller.endDragSession()
                }
            }
        }
}
