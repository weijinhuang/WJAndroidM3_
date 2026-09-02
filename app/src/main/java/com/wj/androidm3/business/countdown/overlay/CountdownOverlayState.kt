package com.wj.androidm3.business.countdown.overlay

enum class CountdownOverlayState {
    HIDDEN_HANDLE,
    COMPACT_TIMER,
    EXPANDED_BUTTON,
    MENU
}

object CountdownOverlayStateReducer {
    fun collapsed(hasRunningCountdown: Boolean): CountdownOverlayState =
        if (hasRunningCountdown) CountdownOverlayState.COMPACT_TIMER
        else CountdownOverlayState.HIDDEN_HANDLE

    fun onPrimaryTap(current: CountdownOverlayState): CountdownOverlayState = when (current) {
        CountdownOverlayState.HIDDEN_HANDLE,
        CountdownOverlayState.COMPACT_TIMER -> CountdownOverlayState.EXPANDED_BUTTON
        CountdownOverlayState.EXPANDED_BUTTON -> CountdownOverlayState.MENU
        CountdownOverlayState.MENU -> CountdownOverlayState.MENU
    }

    fun onDismiss(hasRunningCountdown: Boolean): CountdownOverlayState =
        collapsed(hasRunningCountdown)
}
