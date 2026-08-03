package com.cineshelf.app

import android.content.res.Configuration
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cineshelf.app.navigation.CineShelfNavHost
import com.cineshelf.app.ui.permission.PermissionScreen
import com.cineshelf.app.ui.theme.CineShelfTheme

class MainActivity : ComponentActivity() {

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

    /**
     * The reliable fix for "immersive mode doesn't stick": Android drops the
     * hidden system-bars state on focus changes, so we reassert it here
     * every time focus is regained.
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
     * Home / recents / app switch while the player is open drops into
     * Picture-in-Picture rather than pausing behind a black screen. The params
     * come from the player so the PiP window carries working transport buttons
     * — building them here with an empty builder is what left it with none.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!PipModeController.playerActive.value) return
        val params = PipModeController.paramsProvider?.invoke() ?: return
        try {
            enterPictureInPictureMode(params)
        } catch (_: Exception) {
            // PiP unsupported or not permitted in this state — falling back to a
            // normal background/pause is acceptable, not worth crashing over.
        }
    }
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
