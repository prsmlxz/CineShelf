package com.cineshelf.app

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cineshelf.app.navigation.CineShelfNavHost
import com.cineshelf.app.ui.permission.PermissionScreen
import com.cineshelf.app.ui.player.PipAction
import com.cineshelf.app.ui.theme.CineShelfTheme

private const val ACTION_PIP_CONTROL = "com.cineshelf.app.PIP_ACTION"
private const val EXTRA_PIP_ACTION = "extra_pip_action"
private const val REQUEST_REWIND = 10
private const val REQUEST_PLAY_PAUSE = 11
private const val REQUEST_FORWARD = 12

class MainActivity : ComponentActivity() {

    private var pipActionReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            CineShelfTheme {
                RootGate()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val actionOrdinal = intent?.getIntExtra(EXTRA_PIP_ACTION, -1) ?: return
                val action = PipAction.entries.getOrNull(actionOrdinal) ?: return
                PipModeController.onPipAction?.invoke(action)
            }
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(ACTION_PIP_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        pipActionReceiver = receiver
    }

    override fun onStop() {
        pipActionReceiver?.let { unregisterReceiver(it) }
        pipActionReceiver = null
        super.onStop()
    }

    /**
     * The reliable fix for "immersive mode doesn't stick": Android drops the
     * hidden system-bars state on focus changes, so we reassert it here
     * every time focus is regained, driven by whichever screen is currently
     * active (tracked via ImmersiveModeController). onResume is a second,
     * belt-and-suspenders trigger for the cases where focus doesn't change
     * but the bars still got reset (e.g. returning from split-screen, some
     * OEM gesture-nav skins).
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applySystemBarsState()
    }

    override fun onResume() {
        super.onResume()
        applySystemBarsState()
    }

    private fun applySystemBarsState() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (ImmersiveModeController.immersive.value) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PipModeController.isInPip.value = isInPictureInPictureMode
    }

    /**
     * Called when the user presses Home / switches apps / opens recents while
     * this Activity is in the foreground. If the player is currently active,
     * drop into Picture-in-Picture instead of just pausing behind a black
     * screen — this is what every flagship video app does on "leave". The
     * actual PictureInPictureParams (aspect ratio, transport actions) are
     * built by the player screen itself, since it's the only place that
     * knows the video's dimensions and playback state.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (ImmersiveModeController.immersive.value) {
            try {
                PipModeController.requestEnterPip?.invoke()
                    ?: enterPictureInPictureMode(PictureInPictureParams.Builder().build())
            } catch (_: Exception) {
                // PiP not supported/permitted in this state — falling back to a normal
                // background/pause is an acceptable degradation, not worth crashing over.
            }
        }
    }
}

/**
 * Builds the three system-drawn PiP transport buttons (rewind / play-pause /
 * forward). Called from the player screen, which owns the live playback
 * state, whenever PictureInPictureParams need to be (re)built — both on
 * initial entry and whenever play/pause state flips while already in PiP.
 */
fun buildPipRemoteActions(context: Context, isPlaying: Boolean): List<RemoteAction> {
    fun action(requestCode: Int, iconRes: Int, title: String, pipAction: PipAction): RemoteAction {
        val intent = Intent(ACTION_PIP_CONTROL)
            .setPackage(context.packageName)
            .putExtra(EXTRA_PIP_ACTION, pipAction.ordinal)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return RemoteAction(Icon.createWithResource(context, iconRes), title, title, pendingIntent)
    }

    return listOf(
        action(REQUEST_REWIND, android.R.drawable.ic_media_rew, "Rewind 10 seconds", PipAction.REWIND),
        action(
            REQUEST_PLAY_PAUSE,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "Pause" else "Play",
            PipAction.PLAY_PAUSE
        ),
        action(REQUEST_FORWARD, android.R.drawable.ic_media_ff, "Forward 10 seconds", PipAction.FORWARD)
    )
}

@Composable
private fun RootGate() {
    var hasAccess by remember { mutableStateOf(hasAllFilesAccess()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccess = hasAllFilesAccess()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (hasAccess) {
        CineShelfNavHost(modifier = Modifier.fillMaxSize())
    } else {
        PermissionScreen()
    }
}

private fun hasAllFilesAccess(): Boolean = Environment.isExternalStorageManager()
