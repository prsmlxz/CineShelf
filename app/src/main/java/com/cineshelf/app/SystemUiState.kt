package com.cineshelf.app

import android.app.PictureInPictureParams
import androidx.compose.runtime.mutableStateOf

/**
 * Whether the app should currently be in immersive (system-bars-hidden) mode.
 *
 * Calling `WindowInsetsControllerCompat.hide(...)` once is not reliable on its
 * own — Android re-shows the system bars whenever the window loses and regains
 * focus (notification shade, screenshot, split-screen, and on some OEM skins
 * routine recomposition). The Activity reasserts the hidden state from
 * `onWindowFocusChanged`, reading this flag.
 *
 * Defaults to true: the whole app is fullscreen, not just the player.
 */
object ImmersiveModeController {
    val immersive = mutableStateOf(true)
}

/** Tracks Picture-in-Picture state so the player can simplify its UI while mini. */
object PipModeController {
    val isInPip = mutableStateOf(false)

    /** True only while the player screen is on top — gates auto-PiP on leave. */
    val playerActive = mutableStateOf(false)

    /**
     * Supplied by the player so the Activity can enter PiP with real transport
     * actions. Without this, PiP shows a window with no play button.
     */
    @Volatile
    var paramsProvider: (() -> PictureInPictureParams)? = null
}
