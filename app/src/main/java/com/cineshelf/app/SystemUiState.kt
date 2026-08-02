package com.cineshelf.app

import android.util.Rational
import androidx.compose.runtime.mutableStateOf
import com.cineshelf.app.ui.player.PipAction

/**
 * Single source of truth for whether the app should currently be in
 * immersive (system-bars-hidden) fullscreen mode.
 *
 * Why this exists: simply calling `WindowInsetsControllerCompat.hide(...)`
 * once when the player screen appears is not reliable on its own — Android
 * silently re-shows the system bars whenever the window loses and regains
 * focus (a notification shade pull, a screenshot, split-screen focus, and
 * on some OEM skins even routine recomposition). The fix is to reassert
 * the hidden state from the Activity's `onWindowFocusChanged`/`onResume`,
 * which needs to know, at the Activity level, whether we're currently on
 * the player screen. This object is that shared flag.
 */
object ImmersiveModeController {
    val immersive = mutableStateOf(false)
}

/**
 * Bridges the player (a Composable, which owns the ExoPlayer instance and
 * knows the video's aspect ratio and playback state) with the Activity
 * (the only place `enterPictureInPictureMode` and the PiP action broadcast
 * receiver can live). The player registers callbacks here while it's on
 * screen and clears them on disposal; the Activity only ever calls through
 * these, never touches ExoPlayer directly.
 */
object PipModeController {
    val isInPip = mutableStateOf(false)
    val isPlaying = mutableStateOf(false)
    val aspectRatio = mutableStateOf<Rational?>(null)

    /** Set by the player screen; invoked by MainActivity.onUserLeaveHint(). */
    var requestEnterPip: (() -> Unit)? = null

    /** Set by the player screen; invoked when a system PiP action button is tapped. */
    var onPipAction: ((PipAction) -> Unit)? = null
}
