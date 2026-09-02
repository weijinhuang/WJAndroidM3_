package com.wj.androidm3.business.countdown

import com.wj.androidm3.business.countdown.overlay.CountdownOverlayState
import com.wj.androidm3.business.countdown.overlay.CountdownOverlayStateReducer
import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownOverlayStateReducerTest {
    @Test
    fun noRunningTaskCollapsesToHiddenHandle() {
        assertEquals(
            CountdownOverlayState.HIDDEN_HANDLE,
            CountdownOverlayStateReducer.onDismiss(hasRunningCountdown = false)
        )
    }

    @Test
    fun runningTaskCollapsesToCompactTimer() {
        assertEquals(
            CountdownOverlayState.COMPACT_TIMER,
            CountdownOverlayStateReducer.onDismiss(hasRunningCountdown = true)
        )
    }

    @Test
    fun tapsExpandThenOpenMenu() {
        val expanded = CountdownOverlayStateReducer.onPrimaryTap(CountdownOverlayState.HIDDEN_HANDLE)
        val menu = CountdownOverlayStateReducer.onPrimaryTap(expanded)

        assertEquals(CountdownOverlayState.EXPANDED_BUTTON, expanded)
        assertEquals(CountdownOverlayState.MENU, menu)
    }
}
