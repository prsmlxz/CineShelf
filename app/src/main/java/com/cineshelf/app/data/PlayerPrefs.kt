package com.cineshelf.app.data

import android.content.Context

/**
 * Playback behaviour the user can tune, separate from subtitle appearance.
 */
data class PlayerPrefs(
    /** Distance a double-tap seeks, in seconds. */
    val skipSeconds: Int = 10,
    /** Whether a double-tap in the screen centre toggles play/pause. */
    val centerTapTogglesPlayback: Boolean = true
) {
    val skipMs: Long get() = skipSeconds * 1_000L
}

class PlayerPrefsStore(context: Context) {

    private val prefs = context.getSharedPreferences("player_prefs", Context.MODE_PRIVATE)

    fun load(): PlayerPrefs = PlayerPrefs(
        skipSeconds = prefs.getInt(KEY_SKIP, 10),
        centerTapTogglesPlayback = prefs.getBoolean(KEY_CENTER_TAP, true)
    )

    fun save(value: PlayerPrefs) {
        prefs.edit()
            .putInt(KEY_SKIP, value.skipSeconds)
            .putBoolean(KEY_CENTER_TAP, value.centerTapTogglesPlayback)
            .apply()
    }

    private companion object {
        const val KEY_SKIP = "skip_seconds"
        const val KEY_CENTER_TAP = "center_tap_toggles"
    }
}
