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
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.cineshelf.app.ImmersiveModeController
import com.cineshelf.app.PipModeController
import com.cineshelf.app.data.FramePreviewSource
import com.cineshelf.app.data.SubtitlePrefs
import com.cineshelf.app.data.SubtitlePrefsStore
import com.cineshelf.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Locale
import kotlin.math.abs

private enum class ActiveMenu { NONE, SPEED, SUBTITLE_TRACK, AUDIO, SLEEP_TIMER, SUBTITLE_STYLE }
private enum class HudType { BRIGHTNESS, VOLUME }

private const val BOOST_SPEED = 2f

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

    // ExoPlayer's default LoadControl is tuned for network streaming — it withholds
    // playback until multiple seconds are buffered, which makes local file playback
    // feel like it's loading over the internet. These thresholds are milliseconds.
    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(1_000, 20_000, 150, 300)
            .build()
    }

    val subtitleFiles = remember(filePath) { viewModel.findSubtitleFiles(file) }
    val nextEpisode = remember(filePath) { viewModel.findNextEpisode(file) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
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

    var seekBubble by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
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

    var resumePromptMs by remember { mutableStateOf<Long?>(null) }
    var nextEpisodeCountdown by remember { mutableStateOf<Int?>(null) }

    val frameSource = remember(filePath) { FramePreviewSource(file, scope) }

    // --- Resume prompt: offer rather than silently jump ---
    LaunchedEffect(Unit) {
        val saved = viewModel.getInitialPosition(file)
        if (saved > 10_000L) resumePromptMs = saved
    }
    LaunchedEffect(resumePromptMs) {
        if (resumePromptMs != null) {
            delay(7_000)
            resumePromptMs = null
        }
    }

    DisposableEffect(frameSource) {
        frameSource.start { _, image -> scrubFrame = image }
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

    LaunchedEffect(seekBubble) { if (seekBubble != null) { delay(650); seekBubble = null } }
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
    LaunchedEffect(isPlaying, videoSize) {
        PipModeController.paramsProvider = { PipActions.buildParams(context, isPlaying, videoSize) }
        if (PipModeController.isInPip.value) {
            runCatching {
                activity?.setPictureInPictureParams(PipActions.buildParams(context, isPlaying, videoSize))
            }
        }
    }
    DisposableEffect(Unit) {
        val unregister = PipActions.registerReceiver(context) { control ->
            when (control) {
                PipActions.CONTROL_PLAY_PAUSE ->
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                PipActions.CONTROL_REWIND -> exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0))
                PipActions.CONTROL_FORWARD -> exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L)
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

    fun seekRelative(deltaMs: Long, isLeft: Boolean) {
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0, durationMs.coerceAtLeast(0))
        exoPlayer.seekTo(target)
        currentPositionMs = target
        seekBubble = (if (deltaMs > 0) "+10s" else "-10s") to isLeft
        haptic()
    }

    fun toggleLock() {
        locked = !locked
        lockHintVisible = false
        controlsVisible = !locked
        haptic()
    }

    fun cycleAspect() {
        aspectMode = aspectMode.next()
        videoScale = 1f; videoOffsetX = 0f; videoOffsetY = 0f
        controlsVisible = true
    }

    fun cycleOrientation() {
        orientationMode = orientationMode.next()
        activity?.requestedOrientation = when (orientationMode) {
            OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        controlsVisible = true
    }

    fun enterPip() {
        val act = activity ?: return
        runCatching { act.enterPictureInPictureMode(PipActions.buildParams(context, isPlaying, videoSize)) }
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
                    val bottomDp = if (controlsVisible && !locked) 108 else 32
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
                    color = AccentGlow,
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
                    .pointerInput(durationMs) {
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
                                    offset.x < third -> seekRelative(-10_000L, true)
                                    offset.x > size.width - third -> seekRelative(10_000L, false)
                                    else -> if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
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

        seekBubble?.let { (text, isLeft) ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(Motion.fade(120)) + scaleIn(Motion.bouncy(), initialScale = 0.8f),
                exit = fadeOut(Motion.fade(150)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize()) { SeekBubble(text = text, isLeft = isLeft) }
            }
        }

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

        if (speedBoosted) SpeedBoostBadge("${BOOST_SPEED.toInt()}x speed")

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
                        .auroraGlow(color = AccentPrimary, radius = 16.dp, glowAlpha = 0.35f)
                        .premiumPressableNoScale(onClick = { toggleLock() })
                        .glassPanel(shape = CircleShape, fill = ScrimStrong, stroke = GlassStrokeBright),
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
            enter = fadeIn(Motion.fade(200)),
            exit = fadeOut(Motion.fade(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerControls(
                title = file.nameWithoutExtension,
                isPlaying = isPlaying,
                positionMs = if (scrubbing) scrubTargetMs else currentPositionMs,
                durationMs = durationMs,
                bufferedMs = bufferedMs,
                aspectMode = aspectMode,
                speedLabel = if (playbackSpeed == 1f) "1x" else "${playbackSpeed}x",
                scrubbing = scrubbing,
                scrubFrame = scrubFrame,
                scrubDeltaMs = scrubTargetMs - scrubBaseMs,
                hasNextEpisode = nextEpisode != null,
                onBack = onBack,
                onLock = { toggleLock() },
                onPip = { enterPip() },
                onPlayPause = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                onSkipBack = { seekRelative(-10_000L, true) },
                onSkipForward = { seekRelative(10_000L, false) },
                onNextEpisode = { nextEpisode?.let { onPlayNext(it.absolutePath) } },
                onOpenSubtitles = { activeMenu = ActiveMenu.SUBTITLE_TRACK },
                onOpenSubtitleStyle = { activeMenu = ActiveMenu.SUBTITLE_STYLE },
                onOpenAudio = { activeMenu = ActiveMenu.AUDIO },
                onOpenSpeed = { activeMenu = ActiveMenu.SPEED },
                onCycleAspect = { cycleAspect() },
                onCycleOrientation = { cycleOrientation() },
                onOpenSleepTimer = { activeMenu = ActiveMenu.SLEEP_TIMER },
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

        // --- Resume prompt ---
        AnimatedVisibility(
            visible = resumePromptMs != null,
            enter = slideInVertically(Motion.gentle()) { it } + fadeIn(Motion.fade()),
            exit = slideOutVertically(Motion.standard()) { it } + fadeOut(Motion.fade(150)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val target = resumePromptMs ?: 0L
            Row(
                modifier = Modifier
                    .padding(bottom = 140.dp, start = Spacing.md, end = Spacing.md)
                    .glassPanel(
                        shape = RoundedCornerShape(Radius.lg),
                        fill = SurfaceCardElevated.copy(alpha = 0.94f),
                        stroke = GlassStrokeBright
                    )
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Resume from ${formatTime(target)}?",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Start over otherwise",
                        color = TextTertiary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                PillButton("Resume") {
                    exoPlayer.seekTo(target)
                    resumePromptMs = null
                }
            }
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
                    .auroraGlow(color = AccentPrimary, radius = 22.dp, glowAlpha = 0.35f)
                    .glassPanel(
                        shape = RoundedCornerShape(Radius.lg),
                        fill = SurfaceCardElevated.copy(alpha = 0.96f),
                        stroke = GlassStrokeBright
                    )
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.padding(end = Spacing.md)) {
                    Text(
                        "Up next in ${nextEpisodeCountdown ?: 0}s",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        nextEpisode?.nameWithoutExtension.orEmpty(),
                        color = TextTertiary,
                        style = MaterialTheme.typography.labelSmall,
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
            ActiveMenu.SPEED -> GlassPopupMenu(
                title = "Playback Speed",
                options = speedOptions.map { PopupOption(it.toString(), if (it == 1f) "Normal" else "${it}x") },
                selectedKey = playbackSpeed.toString(),
                onSelect = { key ->
                    val speed = key.toFloat()
                    exoPlayer.playbackParameters = PlaybackParameters(speed)
                    playbackSpeed = speed
                    activeMenu = ActiveMenu.NONE
                },
                onDismiss = { activeMenu = ActiveMenu.NONE }
            )
            ActiveMenu.SUBTITLE_TRACK -> {
                val trackOptions = subtitleOptionsFrom(currentTracks)
                val options = listOf(PopupOption("off", "Off")) +
                    trackOptions.map { PopupOption(it.group.id + "-" + it.trackIndex, it.label) }
                val selected = selectedSubtitleKey
                    ?: trackOptions.find { it.selected }?.let { it.group.id + "-" + it.trackIndex }
                    ?: "off"
                GlassPopupMenu(
                    title = "Subtitles",
                    options = options,
                    selectedKey = selected,
                    onSelect = { selectSubtitle(it) },
                    onDismiss = { activeMenu = ActiveMenu.NONE }
                )
            }
            ActiveMenu.AUDIO -> {
                val audioOpts = audioOptionsFrom(currentTracks)
                val options = audioOpts.map { PopupOption(it.group.id + "-" + it.trackIndex, it.label) }
                val selected = selectedAudioKey
                    ?: audioOpts.find { it.selected }?.let { it.group.id + "-" + it.trackIndex }
                GlassPopupMenu(
                    title = "Audio",
                    options = options,
                    selectedKey = selected,
                    onSelect = { selectAudio(it) },
                    onDismiss = { activeMenu = ActiveMenu.NONE }
                )
            }
            ActiveMenu.SLEEP_TIMER -> GlassPopupMenu(
                title = "Sleep Timer",
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
            ActiveMenu.SUBTITLE_STYLE -> SubtitleStyleSheet(
                prefs = subtitlePrefs,
                subtitleOffsetMs = subtitleOffsetMs,
                onPrefsChange = {
                    subtitlePrefs = it
                    prefsStore.save(it)
                },
                onOffsetChange = { offset ->
                    subtitleOffsetMs = offset
                    // Media3 has no per-track delay API, so shift the whole
                    // playback position instead: the practical effect the user
                    // wants when captions run ahead of or behind the audio.
                    exoPlayer.seekTo((exoPlayer.currentPosition + (offset - subtitleOffsetMs)).coerceAtLeast(0))
                },
                onDismiss = { activeMenu = ActiveMenu.NONE }
            )
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
    aspectMode: AspectMode,
    speedLabel: String,
    scrubbing: Boolean,
    scrubFrame: ImageBitmap?,
    scrubDeltaMs: Long,
    hasNextEpisode: Boolean,
    onBack: () -> Unit,
    onLock: () -> Unit,
    onPip: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onNextEpisode: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenSubtitleStyle: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSpeed: () -> Unit,
    onCycleAspect: () -> Unit,
    onCycleOrientation: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onScrubStart: () -> Unit,
    onScrubMove: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        // Top scrim — a gradient, never an opaque bar, so the video stays the hero.
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(ScrimStrong, Color.Transparent)))
                .statusBarsPadding()
                .padding(top = Spacing.sm, bottom = Spacing.xxl)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransportIconButton(
                    icon = Icons.Filled.ArrowBackIosNew,
                    contentDescription = "Back",
                    iconSize = 15.dp,
                    onClick = onBack
                )
                Text(
                    title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
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
                    icon = Icons.Filled.Lock,
                    contentDescription = "Lock controls",
                    onClick = onLock
                )
            }
        }

        // Centre transport.
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)
        ) {
            TransportIconButton(
                icon = Icons.Outlined.Replay10,
                contentDescription = "Back 10 seconds",
                size = 50.dp,
                iconSize = 24.dp,
                onClick = onSkipBack
            )
            PlayPauseButton(isPlaying = isPlaying, onClick = onPlayPause)
            TransportIconButton(
                icon = Icons.Outlined.Forward10,
                contentDescription = "Forward 10 seconds",
                size = 50.dp,
                iconSize = 24.dp,
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
                val deltaSec = scrubDeltaMs / 1000
                ScrubPreviewCard(
                    frame = scrubFrame,
                    timeLabel = formatTime(positionMs),
                    deltaLabel = "${if (deltaSec >= 0) "+" else ""}${deltaSec}s"
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
                Text(
                    formatTime(positionMs),
                    color = TextPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "-${formatTime((durationMs - positionMs).coerceAtLeast(0))}",
                    color = TextTertiary,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(Modifier.height(Spacing.sm))

            // The quick row sits below the bar on its own glass dock. Bare
            // floating icons over video read as unfinished; the dock groups
            // them and gives the whole overlay a bottom edge.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassPanel(
                        shape = RoundedCornerShape(Radius.xl),
                        fill = GlassFill,
                        stroke = HairlineLight
                    )
                    .padding(vertical = Spacing.xxs),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickIcon(Icons.Outlined.Subtitles, "Subtitles", onClick = onOpenSubtitles)
                QuickIcon(Icons.Outlined.ClosedCaption, "Style", onClick = onOpenSubtitleStyle)
                QuickIcon(Icons.Outlined.Audiotrack, "Audio", onClick = onOpenAudio)
                QuickIcon(
                    Icons.Outlined.Speed,
                    speedLabel,
                    active = speedLabel != "1x",
                    onClick = onOpenSpeed
                )
                QuickIcon(
                    Icons.Outlined.AspectRatio,
                    aspectMode.label,
                    active = aspectMode != AspectMode.FIT,
                    onClick = onCycleAspect
                )
                QuickIcon(Icons.Outlined.ScreenRotation, "Rotate", onClick = onCycleOrientation)
                QuickIcon(Icons.Outlined.Timer, "Sleep", onClick = onOpenSleepTimer)
                if (hasNextEpisode) {
                    QuickIcon(Icons.Outlined.SkipNext, "Next", onClick = onNextEpisode)
                }
            }
        }
    }
}

/**
 * The play/pause control: a plain glyph on a low-opacity gray disc, with the
 * disc brightening and a soft aurora glow blooming while playing. The old
 * solid-white circle read as a stock placeholder against the video.
 */
@Composable
private fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    val glow by animateFloatAsState(
        targetValue = if (isPlaying) 0.20f else 0.10f,
        animationSpec = Motion.standard(),
        label = "playpause-glow"
    )
    Box(
        modifier = Modifier
            .size(72.dp)
            .auroraGlow(color = AccentPrimary, radius = 10.dp, glowAlpha = glow)
            .premiumPressable(scaleDown = 0.90f, onClick = onClick)
            .glassPanel(
                shape = CircleShape,
                fill = ControlCircleFill,
                stroke = ControlCircleStroke
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun PillButton(label: String, filled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .then(
                if (filled) Modifier.auroraGlow(color = AccentPrimary, radius = 10.dp, glowAlpha = 0.35f)
                else Modifier
            )
            .premiumPressable(onClick = onClick)
            .glassPanel(
                shape = CircleShape,
                fill = if (filled) AccentPrimary else GlassFill,
                stroke = if (filled) GlassStrokeBright else GlassStroke
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * One item on the bottom dock. The label always renders, so every icon sits on
 * the same baseline — captioning only the stateful ones left the row ragged.
 */
@Composable
private fun QuickIcon(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (active) AccentGlow else TextPrimary.copy(alpha = 0.88f),
        animationSpec = Motion.standard(),
        label = "quick-tint"
    )
    Column(
        modifier = Modifier
            .premiumPressable(scaleDown = 0.86f, onClick = onClick)
            .padding(horizontal = Spacing.xxs, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = if (active) tint else TextTertiary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun TransportIconButton(
    icon: ImageVector,
    contentDescription: String,
    size: Dp = 40.dp,
    iconSize: Dp = 19.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .premiumPressable(scaleDown = 0.88f, onClick = onClick)
            .glassPanel(shape = CircleShape, fill = GlassFillLight, stroke = GlassStroke),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(iconSize))
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
