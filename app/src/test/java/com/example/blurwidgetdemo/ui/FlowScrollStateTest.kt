package dev.revilodev.onewidget.ui

import org.junit.Assert.*
import org.junit.Test

class FlowScrollStateTest {
    @Test fun returningToTopRequiresANewPull() {
        val state = FlowScrollState()
        state.beginGesture(canScrollUp = true)
        assertFalse(state.mayExpand(canScrollUp = true, touch = true))
        assertFalse(state.mayExpand(canScrollUp = false, touch = true))
        assertFalse(state.mayExpand(canScrollUp = false, touch = false))
        state.beginGesture(canScrollUp = false)
        assertTrue(state.mayExpand(canScrollUp = false, touch = true))
        assertFalse(state.mayExpand(canScrollUp = false, touch = false))
    }

    @Test fun surfacesRequireBothIndependentStates() {
        assertFalse(FlowScrollState.showControlSurfaces(false, false))
        assertFalse(FlowScrollState.showControlSurfaces(false, true))
        assertFalse(FlowScrollState.showControlSurfaces(true, false))
        assertTrue(FlowScrollState.showControlSurfaces(true, true))
    }

    @Test fun reversingDirectionWithinOneGestureStillRequiresAFreshPull() {
        val state = FlowScrollState()
        state.beginGesture(canScrollUp = false)
        state.contentScrolled(canScrollUp = true)
        state.contentScrolled(canScrollUp = false)
        assertFalse(state.mayExpand(canScrollUp = false, touch = true))
        state.beginGesture(canScrollUp = false)
        assertTrue(state.mayExpand(canScrollUp = false, touch = true))
    }
}
