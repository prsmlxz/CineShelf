package com.cineshelf.app.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.cineshelf.app.ImmersiveModeController
import com.cineshelf.app.PipModeController
import com.cineshelf.app.data.FramePreviewSource
import com.cineshelf.app.data.PlayerPrefsStore
import com.cineshelf.app.data.SubtitlePrefsStore
import com.cineshelf.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Locale
import kotlin.math.abs

private enum class ActiveMenu { NONE, SUBTITLES, AUDIO, SLEEP_TIMER, PLAYBACK }
private enum class HudType { BRIGHTNESS, VOLUME }

private const val BOOST_SPEED = 2f

/** How close a decoded preview frame must be to the finger to be worth showing. */
private const val PREVIEW_MATCH_WINDOW_MS = 6_000L

@Composable
fun PlayerScreen(
    filePath: String,
    onBack: () -> Unit,
    onPlayNext: (String) -> Unit = {},
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val file = remember(filePath) { File(filePath) }
    val isInPip by PipModeController.isInPip

    // ExoPlayer's defaults are tuned for network streaming: it withholds playback
    // until several seconds are buffered, which makes a local file feel like it's
    // loading over the internet. 200ms of decoded video is plenty to start from
    // storage, and prioritising time over size stops a high-bitrate HEVC file
    // from tripping the byte-count ceiling before it reaches that threshold —
    // which is what made x265 files specifically slow to start.
    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(1_000, 20_000, 200, 400)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    // Decoder fallback matters for HEVC above all: many devices advertise a
    // hardware HEVC decoder that then refuses a particular profile/level, and
    // without fallback that's a hard playback error instead of a quiet switch to
    // the software decoder. Constant-bitrate seeking gives files with no seek
    // table (common in remuxes) usable scrubbing.
    val renderersFactory = remember {
        DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
    }
    val mediaSourceFactory = remember {
        DefaultMediaSourceFactory(
            context,
            DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        )
    }

    val subtitleFiles = remember(filePath) { viewModel.findSubtitleFiles(file) }
    val nextEpisode = remember(filePath) { viewModel.findNextEpisode(file) }

    val playerPrefsStore = remember { PlayerPrefsStore(context) }
    var playerPrefs by remember { mutableStateOf(playerPrefsStore.load()) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.fromFile(file))
                    .setSubtitleConfigurations(
                        subtitleFiles.mapIndexed { index, subFile ->
                            MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(subFile))
                                .setMimeType(mimeTypeForSubtitle(subFile.extension))
                                .setLabel(subFile.nameWithoutExtension)
                                .setSelectionFlags(if (index == 0) C.SELECTION_FLAG_DEFAULT else 0)
                                .build()
                        }
                    )
                    .build()
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var videoSize by remember { mutableStateOf<VideoSize?>(null) }
    var currentTracks by remember { mutableStateOf(Tracks.EMPTY) }
    var playbackEnded by remember { mutableStateOf(false) }

    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var lockHintVisible by remember { mutableStateOf(false) }
    var activeMenu by remember { mutableStateOf(ActiveMenu.NONE) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var speedBoosted by remember { mutableStateOf(false) }
    var aspectMode by remember { mutableStateOf(AspectMode.FIT) }
    var orientationMode by remember { mutableStateOf(OrientationMode.AUTO) }
    var selectedSubtitleKey by remember { mutableStateOf<String?>(null) }
    var selectedAudioKey by remember { mutableStateOf<String?>(null) }
    var sleepTimerEndAt by remember { mutableStateOf<Long?>(null) }
    var sleepTimerLabel by remember { mutableStateOf("Off") }

    var seekRipple by remember { mutableStateOf<SeekRipple?>(null) }
    var rippleToken by remember { mutableIntStateOf(0) }
    var hud by remember { mutableStateOf<HudType?>(null) }
    var brightnessLevel by remember { mutableFloatStateOf(0.6f) }
    var volumeLevel by remember { mutableFloatStateOf(0.5f) }

    // Scrub state. `scrubTargetMs` is the single source of truth for where the
    // finger is pointing, shared by the bar, the preview card and the seek.
    var scrubbing by remember { mutableStateOf(false) }
    var scrubTargetMs by remember { mutableLongStateOf(0L) }
    var scrubBaseMs by remember { mutableLongStateOf(0L) }
    var scrubFrame by remember { mutableStateOf<ImageBitmap?>(null) }

    var videoScale by remember { mutableFloatStateOf(1f) }
    var videoOffsetX by remember { mutableFloatStateOf(0f) }
    var videoOffsetY by remember { mutableFloatStateOf(0f) }

    val prefsStore = remember { SubtitlePrefsStore(context) }
    var subtitlePrefs by remember { mutableStateOf(prefsStore.load()) }
    var subtitleOffsetMs by remember { mutableLongStateOf(0L) }

    var autoResumedAtMs by remember { mutableStateOf<Long?>(null) }
    var nextEpisodeCountdown by remember { mutableStateOf<Int?>(null) }

    val frameSource = remember(filePath) { FramePreviewSource(file, scope) }

    // Resume silently. Asking meant every episode opened with a decision to make;
    // the toast is confirmation after the fact, not a prompt.
    LaunchedEffect(Unit) {
        val saved = viewModel.getInitialPosition(file)
        if (saved > 10_000L) {
            exoPlayer.seekTo(saved)
            currentPositionMs = saved
            autoResumedAtMs = saved
        }
    }
    LaunchedEffect(autoResumedAtMs) {
        if (autoResumedAtMs != null) {
            delay(2_600)
            autoResumedAtMs = null
        }
    }

    DisposableEffect(frameSource) {
        // Only accept a frame that's still near the finger. Async decodes finish
        // out of order, so an unfiltered callback lets a stale frame overwrite
        // the current one and the preview appears to stick.
        frameSource.start { positionMs, image ->
            if (abs(positionMs - scrubTargetMs) <= PREVIEW_MATCH_WINDOW_MS) scrubFrame = image
        }
        onDispose { frameSource.release() }
    }
    LaunchedEffect(durationMs) {
        if (durationMs > 0) frameSource.prewarm(durationMs)
    }

    val audioManager = remember {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    LaunchedEffect(Unit) {
        volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
        val currentBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        if (currentBrightness in 0f..1f) brightnessLevel = currentBrightness
    }

    // --- Fullscreen + keep-screen-on ---
    DisposableEffect(Unit) {
        ImmersiveModeController.immersive.value = true
        PipModeController.playerActive.value = true
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val insetsController = window?.let { WindowInsetsControllerCompat(it, view) }
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            PipModeController.playerActive.value = false
            PipModeController.paramsProvider = null
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) durationMs = exoPlayer.duration.coerceAtLeast(0)
                playbackEnded = state == Player.STATE_ENDED
            }
            override fun onTracksChanged(tracks: Tracks) { currentTracks = tracks }
            override fun onVideoSizeChanged(size: VideoSize) { videoSize = size }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // --- Position polling. Fast enough that the bar tracks playback smoothly,
    // and suspended entirely while scrubbing so it can't fight the finger. ---
    LaunchedEffect(scrubbing) {
        while (isActive && !scrubbing) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0)
            bufferedMs = exoPlayer.bufferedPosition.coerceAtLeast(0)
            delay(200)
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(5000)
            if (durationMs > 0) viewModel.saveProgress(file, exoPlayer.currentPosition, durationMs)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, activeMenu, locked, scrubbing) {
        if (controlsVisible && isPlaying && activeMenu == ActiveMenu.NONE && !locked && !scrubbing) {
            delay(3600)
            controlsVisible = false
        }
    }

    LaunchedEffect(rippleToken) {
        if (seekRipple != null) {
            delay(700)
            seekRipple = null
        }
    }
    LaunchedEffect(hud) { if (hud != null) { delay(900); hud = null } }
    LaunchedEffect(lockHintVisible) { if (lockHintVisible) { delay(1800); lockHintVisible = false } }

    LaunchedEffect(sleepTimerEndAt) {
        val endAt = sleepTimerEndAt ?: return@LaunchedEffect
        val remaining = endAt - System.currentTimeMillis()
        if (remaining > 0) {
            delay(remaining)
            exoPlayer.pause()
            sleepTimerEndAt = null
            sleepTimerLabel = "Off"
        }
    }

    // --- Next-episode autoplay with a countdown the user can cancel ---
    LaunchedEffect(playbackEnded) {
        if (playbackEnded && nextEpisode != null) {
            for (remaining in 8 downTo 1) {
                nextEpisodeCountdown = remaining
                delay(1000)
            }
            nextEpisodeCountdown = null
            onPlayNext(nextEpisode.absolutePath)
        }
    }

    // --- PiP: publish params so the Activity can auto-enter with real buttons ---
    LaunchedEffect(isPlaying, videoSize, playerPrefs.skipSeconds) {
        val skipSeconds = playerPrefs.skipSeconds
        PipModeController.paramsProvider = {
            PipActions.buildParams(context, isPlaying, videoSize, skipSeconds)
        }
        if (PipModeController.isInPip.value) {
            runCatching {
                activity?.setPictureInPictureParams(
                    PipActions.buildParams(context, isPlaying, videoSize, skipSeconds)
                )
            }
        }
    }
    DisposableEffect(playerPrefs.skipMs) {
        val skip = playerPrefs.skipMs
        val unregister = PipActions.registerReceiver(context) { control ->
            when (control) {
                PipActions.CONTROL_PLAY_PAUSE ->
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                PipActions.CONTROL_REWIND ->
                    exoPlayer.seekTo((exoPlayer.currentPosition - skip).coerceAtLeast(0))
                PipActions.CONTROL_FORWARD -> exoPlayer.seekTo(exoPlayer.currentPosition + skip)
            }
        }
        onDispose { unregister() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && !PipModeController.isInPip.value) {
                if (durationMs > 0) viewModel.saveProgress(file, exoPlayer.currentPosition, durationMs)
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (durationMs > 0) viewModel.saveProgress(file, exoPlayer.currentPosition, durationMs)
            exoPlayer.release()
        }
    }

    fun haptic() = view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)

    fun seekRelative(forward: Boolean) {
        val delta = if (forward) playerPrefs.skipMs else -playerPrefs.skipMs
        val target = (exoPlayer.currentPosition + delta).coerceIn(0, durationMs.coerceAtLeast(0))
        exoPlayer.seekTo(target)
        currentPositionMs = target
        rippleToken += 1
        seekRipple = SeekRipple(playerPrefs.skipSeconds, isLeft = !forward, token = rippleToken)
        haptic()
    }

    fun toggleLock() {
        locked = !locked
        lockHintVisible = false
        controlsVisible = !locked
        haptic()
    }

    fun applyOrientation(mode: OrientationMode) {
        orientationMode = mode
        activity?.requestedOrientation = when (mode) {
            OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    fun applyAspect(mode: AspectMode) {
        aspectMode = mode
        videoScale = 1f; videoOffsetX = 0f; videoOffsetY = 0f
    }

    fun enterPip() {
        val act = activity ?: return
        runCatching {
            act.enterPictureInPictureMode(
                PipActions.buildParams(context, isPlaying, videoSize, playerPrefs.skipSeconds)
            )
        }
    }

    fun selectSubtitle(key: String) {
        selectedSubtitleKey = key
        val params = exoPlayer.trackSelectionParameters.buildUpon()
        if (key == "off") {
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            val option = subtitleOptionsFrom(currentTracks).find { it.group.id + "-" + it.trackIndex == key }
            if (option != null) {
                params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(TrackSelectionOverride(option.group, option.trackIndex))
            }
        }
        exoPlayer.trackSelectionParameters = params.build()
        activeMenu = ActiveMenu.NONE
    }

    fun selectAudio(key: String) {
        selectedAudioKey = key
        val option = audioOptionsFrom(currentTracks).find { it.group.id + "-" + it.trackIndex == key }
        if (option != null) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                .setOverrideForType(TrackSelectionOverride(option.group, option.trackIndex))
                .build()
        }
        activeMenu = ActiveMenu.NONE
    }

    fun adjustBrightness(delta: Float) {
        val act = activity ?: return
        brightnessLevel = (brightnessLevel + delta).coerceIn(0.02f, 1f)
        val lp = act.window.attributes
        lp.screenBrightness = brightnessLevel
        act.window.attributes = lp
        hud = HudType.BRIGHTNESS
    }

    fun adjustVolume(delta: Float) {
        volumeLevel = (volumeLevel + delta).coerceIn(0f, 1f)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volumeLevel * maxVolume).toInt(), 0)
        hud = HudType.VOLUME
    }

    fun setSpeedBoost(on: Boolean) {
        if (speedBoosted == on) return
        speedBoosted = on
        exoPlayer.playbackParameters = PlaybackParameters(if (on) BOOST_SPEED else playbackSpeed)
        if (on) haptic()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    subtitleView?.setApplyEmbeddedStyles(false)
                    subtitleView?.setApplyEmbeddedFontSizes(false)
                }
            },
            update = { playerView ->
                playerView.resizeMode = when (aspectMode) {
                    AspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
                playerView.subtitleView?.let { sv ->
                    sv.setStyle(subtitlePrefs.toCaptionStyle())
                    sv.setFractionalTextSize(
                        androidx.media3.ui.SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitlePrefs.scale
                    )
                    val bottomDp = if (controlsVisible && !locked) 116 else 32
                    val bottomPx = with(density) {
                        (bottomDp * subtitlePrefs.bottomPaddingScale).dp.roundToPx()
                    }
                    sv.setPadding(sv.paddingLeft, sv.paddingTop, sv.paddingRight, bottomPx)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = videoScale
                    scaleY = videoScale
                    translationX = videoOffsetX
                    translationY = videoOffsetY
                }
        )

        if (isInPip) {
            // No overlay chrome in PiP — the system supplies the transport row.
            return@Box
        }

        if (isBuffering) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = AccentPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        // ------------------------------------------------------------------
        // Gesture layer.
        //
        // The previous build split the screen into three zones and bound the
        // show/hide tap to only the middle one, so ~60% of taps did nothing at
        // all. Now a single full-screen handler owns tap, double-tap and
        // long-press, and decides what a double-tap means from where it landed.
        // ------------------------------------------------------------------
        if (locked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { lockHintVisible = true })
                    }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Pinch zoom / pan. Deliberately hand-rolled instead of
                    // detectTransformGestures: that fires on single-finger pans
                    // too and would swallow the brightness/volume/scrub drags.
                    // This only engages — and only consumes — with two fingers down.
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.size >= 2) {
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    val next = (videoScale * zoom).coerceIn(1f, 3f)
                                    videoScale = next
                                    if (next <= 1.01f) {
                                        videoOffsetX = 0f
                                        videoOffsetY = 0f
                                    } else {
                                        val maxX = size.width * (next - 1f) / 2f
                                        val maxY = size.height * (next - 1f) / 2f
                                        videoOffsetX = (videoOffsetX + pan.x).coerceIn(-maxX, maxX)
                                        videoOffsetY = (videoOffsetY + pan.y).coerceIn(-maxY, maxY)
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                    .pointerInput(durationMs, playerPrefs) {
                        detectTapGestures(
                            // Holding runs at 2x; tryAwaitRelease is what gives the
                            // long-press a matching "released" edge to restore from.
                            onPress = {
                                tryAwaitRelease()
                                setSpeedBoost(false)
                            },
                            onLongPress = { setSpeedBoost(true) },
                            onTap = {
                                if (activeMenu != ActiveMenu.NONE) {
                                    activeMenu = ActiveMenu.NONE
                                } else {
                                    controlsVisible = !controlsVisible
                                }
                            },
                            onDoubleTap = { offset ->
                                val third = size.width / 3f
                                when {
                                    offset.x < third -> seekRelative(forward = false)
                                    offset.x > size.width - third -> seekRelative(forward = true)
                                    playerPrefs.centerTapTogglesPlayback ->
                                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                }
                            }
                        )
                    }
                    .pointerInput(durationMs) {
                        var accumDx = 0f
                        var accumDy = 0f
                        var axis: DragAxis? = null
                        var side: DragSide? = null

                        detectDragGestures(
                            onDragStart = {
                                accumDx = 0f; accumDy = 0f; axis = null; side = null
                                scrubBaseMs = exoPlayer.currentPosition
                            },
                            onDragEnd = {
                                if (axis == DragAxis.HORIZONTAL && scrubbing) {
                                    exoPlayer.seekTo(scrubTargetMs)
                                    currentPositionMs = scrubTargetMs
                                }
                                scrubbing = false
                                axis = null
                            },
                            onDragCancel = { scrubbing = false; axis = null }
                        ) { change, dragAmount ->
                            accumDx += dragAmount.x
                            accumDy += dragAmount.y
                            if (axis == null && (abs(accumDx) > 16f || abs(accumDy) > 16f)) {
                                axis = if (abs(accumDx) > abs(accumDy)) DragAxis.HORIZONTAL else DragAxis.VERTICAL
                                if (axis == DragAxis.VERTICAL) {
                                    side = if (change.position.x < size.width / 2f) DragSide.BRIGHTNESS else DragSide.VOLUME
                                }
                            }
                            when (axis) {
                                DragAxis.HORIZONTAL -> {
                                    change.consume()
                                    scrubbing = true
                                    controlsVisible = true
                                    val rangeMs = (durationMs * 0.25f).coerceIn(30_000f, 600_000f)
                                    val deltaMs = (accumDx / size.width) * rangeMs
                                    scrubTargetMs = (scrubBaseMs + deltaMs.toLong())
                                        .coerceIn(0, durationMs.coerceAtLeast(0))
                                    frameSource.request(scrubTargetMs)?.let { scrubFrame = it }
                                }
                                DragAxis.VERTICAL -> {
                                    change.consume()
                                    val delta = -dragAmount.y / size.height
                                    if (side == DragSide.BRIGHTNESS) adjustBrightness(delta) else adjustVolume(delta)
                                }
                                null -> {}
                            }
                        }
                    }
            )
        }

        seekRipple?.let { SeekRippleOverlay(it) }

        hud?.let {
            when (it) {
                HudType.BRIGHTNESS -> LevelHud(
                    Icons.Outlined.BrightnessMedium,
                    brightnessLevel,
                    "${(brightnessLevel * 100).toInt()}%"
                )
                HudType.VOLUME -> LevelHud(
                    Icons.AutoMirrored.Outlined.VolumeUp,
                    volumeLevel,
                    "${(volumeLevel * 100).toInt()}%"
                )
            }
        }

        if (speedBoosted) SpeedBoostBadge("${BOOST_SPEED.toInt()}× speed")

        autoResumedAtMs?.let { AutoResumeToast(formatTime(it)) }

        if (locked) {
            AnimatedVisibility(
                visible = lockHintVisible,
                enter = fadeIn(Motion.fade(160)) + scaleIn(Motion.bouncy(), initialScale = 0.85f),
                exit = fadeOut(Motion.fade(160)) + scaleOut(Motion.standard(), targetScale = 0.9f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .premiumPressableNoScale(onClick = { toggleLock() })
                        .glassPanelOverVideo(shape = CircleShape, baseAlpha = 0.55f, stroke = GlassStrokeBright),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.LockOpen,
                        contentDescription = "Unlock",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && !locked,
            enter = Motion.controlsEnter(),
            exit = Motion.controlsExit(),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerControls(
                title = file.nameWithoutExtension,
                isPlaying = isPlaying,
                positionMs = if (scrubbing) scrubTargetMs else currentPositionMs,
                durationMs = durationMs,
                bufferedMs = bufferedMs,
                skipSeconds = playerPrefs.skipSeconds,
                playbackAdjusted = playbackSpeed != 1f || aspectMode != AspectMode.FIT ||
                    orientationMode != OrientationMode.AUTO || sleepTimerEndAt != null,
                subtitlesOn = selectedSubtitleKey != "off",
                scrubbing = scrubbing,
                scrubFrame = scrubFrame,
                scrubDeltaMs = scrubTargetMs - scrubBaseMs,
                onBack = onBack,
                onLock = { toggleLock() },
                onPip = { enterPip() },
                onPlayPause = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                onSkipBack = { seekRelative(forward = false) },
                onSkipForward = { seekRelative(forward = true) },
                onOpenSubtitles = { activeMenu = ActiveMenu.SUBTITLES },
                onOpenAudio = { activeMenu = ActiveMenu.AUDIO },
                onOpenPlayback = { activeMenu = ActiveMenu.PLAYBACK },
                onScrubStart = {
                    scrubbing = true
                    scrubBaseMs = exoPlayer.currentPosition
                },
                onScrubMove = { target ->
                    scrubTargetMs = target
                    frameSource.request(target)?.let { scrubFrame = it }
                },
                onScrubEnd = { target ->
                    exoPlayer.seekTo(target)
                    currentPositionMs = target
                    scrubbing = false
                }
            )
        }

        // --- Next episode countdown ---
        AnimatedVisibility(
            visible = nextEpisodeCountdown != null,
            enter = slideInVertically(Motion.gentle()) { it } + fadeIn(Motion.fade()),
            exit = fadeOut(Motion.fade(150)),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Row(
                modifier = Modifier
                    .padding(Spacing.lg)
                    .glassPanelOverVideo(
                        shape = RoundedCornerShape(Radius.lg),
                        baseAlpha = 0.92f,
                        fill = SurfaceCardElevated.copy(alpha = 0.90f),
                        stroke = GlassStrokeBright
                    )
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.padding(end = Spacing.md)) {
                    Text(
                        "Up next in ${nextEpisodeCountdown ?: 0}s",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        nextEpisode?.nameWithoutExtension.orEmpty(),
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                PillButton("Cancel", filled = false) { nextEpisodeCountdown = null; playbackEnded = false }
                Spacer(Modifier.width(Spacing.xs))
                PillButton("Play now") { nextEpisode?.let { onPlayNext(it.absolutePath) } }
            }
        }

        when (activeMenu) {
            ActiveMenu.AUDIO -> {
                val audioOpts = audioOptionsFrom(currentTracks)
                val options = audioOpts.map { PopupOption(it.group.id + "-" + it.trackIndex, it.label) }
                val selected = selectedAudioKey
                    ?: audioOpts.find { it.selected }?.let { it.group.id + "-" + it.trackIndex }
                GlassPopupMenu(
                    icon = Icons.Outlined.Headphones,
                    title = "Audio track",
                    subtitle = "${options.size} embedded in this file",
                    options = options,
                    selectedKey = selected,
                    onSelect = { selectAudio(it) },
                    onDismiss = { activeMenu = ActiveMenu.NONE }
                )
            }
            ActiveMenu.SLEEP_TIMER -> GlassPopupMenu(
                icon = Icons.Outlined.Timer,
                title = "Sleep timer",
                subtitle = "Pause playback automatically",
                options = sleepTimerOptions.map { PopupOption(it.minutes?.toString() ?: "off", it.label) },
                selectedKey = if (sleepTimerEndAt == null) "off"
                else sleepTimerOptions.find { it.label == sleepTimerLabel }?.minutes?.toString(),
                onSelect = { key ->
                    if (key == "off") {
                        sleepTimerEndAt = null
                        sleepTimerLabel = "Off"
                    } else {
                        val minutes = key.toInt()
                        sleepTimerEndAt = System.currentTimeMillis() + minutes * 60_000L
                        sleepTimerLabel = sleepTimerOptions.find { it.minutes == minutes }?.label ?: "$minutes minutes"
                    }
                    activeMenu = ActiveMenu.NONE
                },
                onDismiss = { activeMenu = ActiveMenu.NONE }
            )
            ActiveMenu.PLAYBACK -> PlaybackSettingsSheet(
                speed = playbackSpeed,
                skipSeconds = playerPrefs.skipSeconds,
                aspectMode = aspectMode,
                orientationMode = orientationMode,
                sleepTimerLabel = sleepTimerLabel,
                hasNextEpisode = nextEpisode != null,
                onSpeedChange = { speed ->
                    playbackSpeed = speed
                    if (!speedBoosted) exoPlayer.playbackParameters = PlaybackParameters(speed)
                },
                onSkipSecondsChange = { seconds ->
                    playerPrefs = playerPrefs.copy(skipSeconds = seconds)
                    playerPrefsStore.save(playerPrefs)
                },
                onAspectChange = { applyAspect(it) },
                onOrientationChange = { applyOrientation(it) },
                onOpenSleepTimer = { activeMenu = ActiveMenu.SLEEP_TIMER },
                onNextEpisode = { nextEpisode?.let { onPlayNext(it.absolutePath) } },
                onDismiss = { activeMenu = ActiveMenu.NONE }
            )
            ActiveMenu.SUBTITLES -> {
                val trackOptions = subtitleOptionsFrom(currentTracks)
                val tracks = listOf(PopupOption(OFF_TRACK_KEY, "Off")) +
                    trackOptions.map { PopupOption(it.group.id + "-" + it.trackIndex, it.label) }
                SubtitleStyleSheet(
                    prefs = subtitlePrefs,
                    subtitleOffsetMs = subtitleOffsetMs,
                    tracks = tracks,
                    selectedTrackKey = selectedSubtitleKey
                        ?: trackOptions.find { it.selected }?.let { it.group.id + "-" + it.trackIndex }
                        ?: OFF_TRACK_KEY,
                    onSelectTrack = { selectSubtitle(it) },
                    onPrefsChange = {
                        subtitlePrefs = it
                        prefsStore.save(it)
                    },
                    onOffsetChange = { offset ->
                        // Media3 has no per-track delay API, so shift the whole
                        // playback position instead: the practical effect the user
                        // wants when captions run ahead of or behind the audio.
                        exoPlayer.seekTo((exoPlayer.currentPosition + (offset - subtitleOffsetMs)).coerceAtLeast(0))
                        subtitleOffsetMs = offset
                    },
                    onDismiss = { activeMenu = ActiveMenu.NONE }
                )
            }
            ActiveMenu.NONE -> {}
        }
    }
}

private fun mimeTypeForSubtitle(extension: String): String = when (extension.lowercase(Locale.US)) {
    "srt" -> MimeTypes.APPLICATION_SUBRIP
    "vtt" -> MimeTypes.TEXT_VTT
    "ssa", "ass" -> MimeTypes.TEXT_SSA
    else -> MimeTypes.APPLICATION_SUBRIP
}

private fun subtitleOptionsFrom(tracks: Tracks): List<TrackOption> =
    tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }.flatMap { group ->
        (0 until group.length).map { i ->
            val format = group.getTrackFormat(i)
            TrackOption(
                group = group.mediaTrackGroup,
                trackIndex = i,
                label = format.label ?: format.language?.uppercase(Locale.US) ?: "Subtitle ${i + 1}",
                selected = group.isTrackSelected(i)
            )
        }
    }

private fun audioOptionsFrom(tracks: Tracks): List<TrackOption> =
    tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }.flatMap { group ->
        (0 until group.length).map { i ->
            val format = group.getTrackFormat(i)
            TrackOption(
                group = group.mediaTrackGroup,
                trackIndex = i,
                label = format.label ?: format.language?.uppercase(Locale.US) ?: "Track ${i + 1}",
                selected = group.isTrackSelected(i)
            )
        }
    }

/**
 * The control layer over the video. Public so screenshot tests can render it
 * standalone — it takes no player instance, only values.
 */
@Composable
fun PlayerControls(
    title: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    skipSeconds: Int,
    playbackAdjusted: Boolean,
    subtitlesOn: Boolean,
    scrubbing: Boolean,
    scrubFrame: ImageBitmap?,
    scrubDeltaMs: Long,
    onBack: () -> Unit,
    onLock: () -> Unit,
    onPip: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenPlayback: () -> Unit,
    onScrubStart: () -> Unit,
    onScrubMove: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        // Dim the whole picture while the controls are up. A real backdrop blur
        // can't composite over the video SurfaceView, so the depth cue is this
        // even dim plus the two edge gradients — the same trick that makes the
        // controls readable over a bright scene without an opaque bar.
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.26f)))

        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(ScrimStrong, Color.Transparent)))
                .statusBarsPadding()
                .padding(top = Spacing.xs, bottom = Spacing.xxl)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransportIconButton(
                    icon = Icons.Outlined.ArrowBackIosNew,
                    contentDescription = "Back",
                    iconSize = 16.dp,
                    onClick = onBack
                )
                Text(
                    title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.sm)
                )
                TransportIconButton(
                    icon = Icons.Outlined.PictureInPictureAlt,
                    contentDescription = "Picture in picture",
                    onClick = onPip
                )
                Spacer(Modifier.width(Spacing.xs))
                TransportIconButton(
                    icon = Icons.Outlined.Lock,
                    contentDescription = "Lock controls",
                    onClick = onLock
                )
            }
        }

        // Centre transport. One dominant element, two quiet ones: the skip
        // glyphs carry no disc at all, so the play button is the only thing with
        // a surface behind it and the eye goes straight there.
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            SkipButton(
                icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                contentDescription = "Back $skipSeconds seconds",
                onClick = onSkipBack
            )
            PlayPauseButton(isPlaying = isPlaying, onClick = onPlayPause)
            SkipButton(
                icon = Icons.Outlined.KeyboardDoubleArrowRight,
                contentDescription = "Forward $skipSeconds seconds",
                onClick = onSkipForward
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, ScrimStrong)))
                .navigationBarsPadding()
                .padding(top = Spacing.xxl, bottom = Spacing.sm, start = Spacing.md, end = Spacing.md)
        ) {
            // Preview card floats above the bar, tracking the thumb.
            val fraction = if (durationMs > 0) {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f
            ScrubPreviewAnchor(
                visible = scrubbing,
                fraction = fraction,
                previewWidth = 176.dp
            ) {
                ScrubPreviewCard(
                    frame = scrubFrame,
                    timeLabel = formatTime(positionMs),
                    deltaLabel = formatSeekDelta(scrubDeltaMs)
                )
            }

            CineScrubber(
                positionMs = positionMs,
                durationMs = durationMs,
                bufferedMs = bufferedMs,
                onScrubStart = onScrubStart,
                onScrubMove = onScrubMove,
                onScrubEnd = onScrubEnd
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xxs),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(positionMs), color = TextPrimary, style = TimeLabel)
                Text(
                    "-${formatTime((durationMs - positionMs).coerceAtLeast(0))}",
                    color = TextTertiary,
                    style = TimeLabel
                )
            }

            Spacer(Modifier.height(Spacing.sm))

            // Three actions, not eight. Speed, aspect, rotation, sleep timer and
            // next-episode all live in the Playback sheet, and subtitle track
            // selection merged into the Subtitles sheet.
            //
            // No panel behind them. The bottom scrim already separates this row
            // from the picture; a bordered pill on top of it was a second
            // container doing the same job, and it made three plain actions look
            // like a floating widget.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xxs),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockAction(
                    Icons.Outlined.ClosedCaption,
                    "Subtitles",
                    active = subtitlesOn,
                    onClick = onOpenSubtitles
                )
                DockAction(Icons.Outlined.Headphones, "Audio", onClick = onOpenAudio)
                DockAction(
                    Icons.Outlined.Tune,
                    "Playback",
                    active = playbackAdjusted,
                    onClick = onOpenPlayback
                )
            }
        }
    }
}

/**
 * The play/pause control.
 *
 * No disc. A One UI transport is glyph-only over the picture — a translucent
 * circle behind the icon is the single clearest tell of a generic player, and
 * it fights the video for the centre of the frame.
 *
 * Emphasis instead comes from three things the skip glyphs don't have: size
 * (48dp against 30dp), full-opacity white against 72%, and a soft radial
 * darkening behind it so the glyph stays legible over a blown-out scene. The
 * darkening has no edge, so it reads as shading rather than as a button.
 *
 * Solid glyphs, not outlined. Outlined.Pause is already two solid bars, so the
 * outlined pair rendered the same control at two different weights depending on
 * state — and a hollow play triangle reads as a disabled control.
 */
@Composable
private fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .premiumPressableSoft(scaleDown = 0.92f, dimTo = 0.6f, onClick = onClick)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.34f), Color.Transparent),
                        center = center,
                        radius = size.minDimension * 0.55f
                    ),
                    radius = size.minDimension * 0.55f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * Skip back/forward. Same glyph-only treatment at ~60% of the play button's
 * optical weight — present when you look for it, invisible when you don't.
 * 56dp keeps the target comfortably above the 48dp minimum.
 */
@Composable
private fun SkipButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .premiumPressableSoft(scaleDown = 0.90f, dimTo = 0.5f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.72f),
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun PillButton(label: String, filled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .premiumPressable(onClick = onClick)
            .glassPanel(
                shape = CircleShape,
                fill = if (filled) AccentPrimary else GlassFill,
                stroke = if (filled) GlassStrokeBright else GlassStroke
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * One item on the bottom dock. The 56dp minimum height is what makes the target
 * comfortable — the visible glyph is only 24dp, but the row it sits in was
 * previously ~40dp tall, under the Android minimum.
 */
@Composable
private fun DockAction(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (active) AccentBright else Color.White.copy(alpha = 0.88f),
        animationSpec = Motion.standard(),
        label = "dock-tint"
    )
    Column(
        modifier = Modifier
            .premiumPressableSoft(scaleDown = 0.94f, dimTo = 0.55f, onClick = onClick)
            .sizeIn(minWidth = 72.dp, minHeight = 56.dp)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) tint else TextSecondary,
            maxLines = 1
        )
    }
}

/**
 * Top-bar action. Glyph only, on a 48dp target — the discs these used to sit in
 * added three competing circles to the top of the frame for no information gain.
 * The top scrim already provides the contrast that made the discs necessary.
 */
@Composable
private fun TransportIconButton(
    icon: ImageVector,
    contentDescription: String,
    size: Dp = 48.dp,
    iconSize: Dp = 21.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .premiumPressableSoft(scaleDown = 0.90f, dimTo = 0.55f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * The scrub delta, as "+1:35" rather than "+95s".
 *
 * Raw seconds were rendered with the badge's letter-spacing applied between the
 * digits, so a two-figure value read as two separate numbers ("+9 5s"). Clock
 * form also matches the timestamp directly above it.
 */
internal fun formatSeekDelta(deltaMs: Long): String {
    val sign = if (deltaMs < 0) "-" else "+"
    val totalSeconds = kotlin.math.abs(deltaMs) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) {
        String.format(Locale.US, "%s%d:%02d", sign, minutes, seconds)
    } else {
        "$sign${seconds}s"
    }
}

internal fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
