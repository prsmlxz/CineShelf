package com.cineshelf.app

import androidx.compose.runtime.mutableStateOf

/**
 * Single source of truth for whether the app should currently be in
 * immersive (system-bars-hidden) fullscreen mode.
 *
 * Why this exists: simply calling `WindowInsetsControllerCompat.hide(...)`
 * once when the player screen appears is not reliable on its own — Android
 * silently re-shows the system bars whenever the window loses and regains
 * focus (a notification shade pull, a screenshot, split-screen focus, and
 * on some OEM skins even routine recomposition). The fix is to reassert
 * the hidden state from the Activity's `onWindowFocusChanged`, which needs
 * to know, at the Activity level, whether we're currently on the player
 * screen. This object is that shared flag.
 */
object ImmersiveModeController {
    val immersive = mutableStateOf(false)
}

/** Tracks Picture-in-Picture state so the player can simplify its UI while mini. */
object PipModeController {
    val isInPip = mutableStateOf(false)
}
