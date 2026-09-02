package com.wj.androidm3.business.countdown.data

import android.content.Context
import androidx.core.content.edit

class CountdownPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    var assistantEnabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)
        set(value) = preferences.edit { putBoolean(KEY_ENABLED, value) }

    var dockOnLeft: Boolean
        get() = preferences.getBoolean(KEY_DOCK_LEFT, false)
        set(value) = preferences.edit { putBoolean(KEY_DOCK_LEFT, value) }

    var verticalFraction: Float
        get() = preferences.getFloat(KEY_VERTICAL_FRACTION, 0.35f).coerceIn(0f, 1f)
        set(value) = preferences.edit { putFloat(KEY_VERTICAL_FRACTION, value.coerceIn(0f, 1f)) }

    companion object {
        private const val PREFERENCES_NAME = "countdown_assistant_preferences"
        private const val KEY_ENABLED = "assistant_enabled"
        private const val KEY_DOCK_LEFT = "dock_on_left"
        private const val KEY_VERTICAL_FRACTION = "vertical_fraction"
    }
}
