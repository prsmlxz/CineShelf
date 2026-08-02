package com.cineshelf.app.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.graphics.Color as AndroidColor
import android.media.AudioManager
import android.net.Uri
import android.util.Rational
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import com.cineshelf.app.ImmersiveModeController
import com.cineshelf.app.PipModeController
import com.cineshelf.app.buildPipRemoteActions
import com.cineshelf.app.data.SubtitleEdgeStyle
import com.cineshelf.app.data.SubtitleStyle
import com.cineshelf.app.data.ThumbnailUtil
import com.cineshelf.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.abs

private enum class ActiveMenu { NONE, SPEED, SUBTITLES, SUBTITLE_STYLE, AUDIO, SLEEP_TIMER }
private enum class HudType { BRIGHTNESS, VOLUME }

@Composable
fun PlayerScreen(
    filePath: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val touchSlopPx = LocalViewConfiguration.current.touchSlop
    val file = remember(filePath) { File(filePath) }
    val isInPip by PipModeController.isInPip

    // ExoPlayer's default LoadControl is tuned for network streaming — it withholds
    // playback until multiple seconds are buffered, which is exactly what makes local
    // file playback feel like it's loading over the internet even though the whole
    // file is already sitting on disk. These thresholds are milliseconds, not seconds.
    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 1_000,
                /* maxBufferMs = */ 20_000,
                /* bufferForPlaybackMs = */ 150,
                /* bufferForPlaybackAfterRebufferMs = */ 300
            )
            .build()
    }

    val subtitleFiles = remember(filePath) { viewModel.findSubtitleFiles(file) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
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
                val initialPosition = viewModel.getInitialPosition(file)
                prepare()
                if (initialPosition > 0) seekTo(initialPosition)
                playWhenReady = true
            }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var durationMs by remember { mutableStateOf(0L) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var bufferedPercentage by remember { mutableStateOf(0) }
    var isBuffering by remember { mutableStateOf(true) }
    var videoSize by remember { mutableStateOf<VideoSize?>(null) }
    var currentTracks by remember { mutableStateOf(Tracks.EMPTY) }

    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var lockHintVisible by remember { mutableStateOf(false) }
    var activeMenu by remember { mutableStateOf(ActiveMenu.NONE) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var aspectMode by remember { mutableStateOf(AspectMode.FIT) }
    var orientationMode by remember { mutableStateOf(OrientationMode.AUTO) }
    var selectedSubtitleKey by remember { mutableStateOf<String?>("off") }
    var selectedAudioKey by remember { mutableStateOf<String?>(null) }
    var sleepTimerEndAt by remember { mutableStateOf<Long?>(null) }
    var sleepTimerLabel by remember { mutableStateOf("Off") }
    var subtitleStyle by remember { mutableStateOf(viewModel.getSubtitleStyle()) }

    var isDraggingSlider by remember { mutableStateOf(false) }

    var seekBubble by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var hud by remember { mutableStateOf<HudType?>(null) }
    var brightnessLevel by remember { mutableStateOf(0.6f) }
    var volumeLevel by remember { mutableStateOf(0.5f) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubTargetMs by remember { mutableStateOf(0L) }
    var scrubDeltaMs by remember { mutableStateOf(0L) }
    var scrubThumbnails by remember { mutableStateOf<List<Pair<Long, String>>>(emptyList()) }

    // Generate a sparse-but-dense set of preview frames once we know the duration, off the
    // main thread. Cached on disk, so this is a one-time cost per video. 30 frames spread
    // across the runtime gives a genuinely frame-accurate *feel* while scrubbing without
    // paying to decode dozens more than anyone will ever land exactly on.
    LaunchedEffect(durationMs) {
        if (durationMs > 0 && scrubThumbnails.isEmpty()) {
            scrubThumbnails = withContext(Dispatchers.IO) {
                ThumbnailUtil.getOrCreateScrubThumbnails(context, file, durationMs)
            }
        }
    }

    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    // Initialize volume HUD level from the current system volume.
    LaunchedEffect(Unit) {
        volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
        val currentBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        if (currentBrightness in 0f..1f) brightnessLevel = currentBrightness
    }

    // --- Immersive fullscreen (robust: MainActivity reasserts this on focus/resume) ---
    DisposableEffect(Unit) {
        ImmersiveModeController.immersive.value = true
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val insetsController = window?.let { WindowInsetsControllerCompat(it, view) }
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            ImmersiveModeController.immersive.value = false
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Defensive re-hide: some OEM skins quietly bring the system bars back on almost any
    // layout/state change. Re-asserting hidden every time the control chrome toggles costs
    // nothing (it's a no-op if already hidden) and closes that gap without polling.
    LaunchedEffect(controlsVisible, locked) {
        val window = activity?.window
        if (window != null) {
            WindowInsetsControllerCompat(window, view).hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    // --- Player event listener ---
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) durationMs = exoPlayer.duration.coerceAtLeast(0)
            }
            override fun onTracksChanged(tracks: Tracks) { currentTracks = tracks }
            override fun onVideoSizeChanged(size: VideoSize) { videoSize = size }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // --- Position polling ---
    LaunchedEffect(Unit) {
        while (isActive) {
            if (!isDraggingSlider) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0)
                bufferedPercentage = exoPlayer.bufferedPercentage
            }
            delay(400)
        }
    }

    // --- Periodic progress save (also drives watched-marking + crash resilience) ---
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(5000)
            if (durationMs > 0) viewModel.saveProgress(file, exoPlayer.currentPosition, durationMs)
        }
    }

    // --- Auto-hide controls while playing ---
    LaunchedEffect(controlsVisible, isPlaying, activeMenu, locked) {
        if (controlsVisible && isPlaying && activeMenu == ActiveMenu.NONE && !locked) {
            delay(3200)
            controlsVisible = false
        }
    }

    LaunchedEffect(seekBubble) { if (seekBubble != null) { delay(600); seekBubble = null } }
    LaunchedEffect(hud) { if (hud != null) { delay(900); hud = null } }
    LaunchedEffect(lockHintVisible) { if (lockHintVisible) { delay(1800); lockHintVisible = false } }

    // --- Sleep timer countdown ---
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

    // --- Lifecycle-aware pause + final save ---
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && !isInPip) {
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

    val haptics = view

    fun seekRelative(deltaMs: Long, isLeft: Boolean) {
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0, durationMs.coerceAtLeast(0))
        exoPlayer.seekTo(target)
        currentPositionMs = target
        seekBubble = (if (deltaMs > 0) "+10s" else "-10s") to isLeft
        controlsVisible = true
        haptics.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun toggleLock() {
        locked = !locked
        lockHintVisible = false
        controlsVisible = !locked
        haptics.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun cycleAspect() {
        aspectMode = aspectMode.next()
        controlsVisible = true
    }

    fun cycleOrientation() {
        orientationMode = orientationMode.next()
        activity?.requestedOrientation = when (orientationMode) {
            OrientationMode.AUTO -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
            OrientationMode.LANDSCAPE -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            OrientationMode.PORTRAIT -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        controlsVisible = true
    }

    fun computePipAspectRatio(): Rational {
        val size = videoSize
        return if (size != null && size.width > 0 && size.height > 0) {
            val r = size.width.toFloat() / size.height.toFloat()
            val clamped = r.coerceIn(1f / 2.39f, 2.39f)
            if (clamped == r) Rational(size.width, size.height) else Rational((clamped * 100).toInt(), 100)
        } else {
            Rational(16, 9)
        }
    }

    fun buildPipParams(): PictureInPictureParams =
        PictureInPictureParams.Builder()
            .setAspectRatio(computePipAspectRatio())
            .setActions(buildPipRemoteActions(context, exoPlayer.isPlaying))
            .build()

    fun enterPip() {
        val act = activity ?: return
        try {
            act.enterPictureInPictureMode(buildPipParams())
        } catch (_: Exception) {
            // Not supported/permitted right now — nothing worth crashing over.
        }
    }

    fun handlePipAction(action: PipAction) {
        when (action) {
            PipAction.PLAY_PAUSE -> if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            PipAction.REWIND -> seekRelative(-10_000L, true)
            PipAction.FORWARD -> seekRelative(10_000L, false)
        }
        activity?.let {
            try { it.setPictureInPictureParams(buildPipParams()) } catch (_: Exception) {}
        }
    }

    // Bridge this screen's PiP state (aspect ratio, transport actions) to the Activity,
    // which is the only place that can actually call enterPictureInPictureMode(). Cleared
    // on disposal so a stale callback can never fire after leaving the player.
    DisposableEffect(Unit) {
        PipModeController.requestEnterPip = { enterPip() }
        PipModeController.onPipAction = { action -> handlePipAction(action) }
        onDispose {
            PipModeController.requestEnterPip = null
            PipModeController.onPipAction = null
        }
    }

    // Keep the system-drawn PiP play/pause icon in sync with real playback state.
    LaunchedEffect(isPlaying, isInPip) {
        if (isInPip) {
            activity?.let {
                try { it.setPictureInPictureParams(buildPipParams()) } catch (_: Exception) {}
            }
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

    fun updateSubtitleStyle(newStyle: SubtitleStyle) {
        subtitleStyle = newStyle
        viewModel.saveSubtitleStyle(newStyle)
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
                val bottomDp = if (controlsVisible && !locked) 96 else 28
                val bottomPx = with(density) { bottomDp.dp.roundToPx() }
                val sv = playerView.subtitleView
                sv?.setPadding(sv.paddingLeft, sv.paddingTop, sv.paddingRight, bottomPx)
                sv?.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitleStyle.sizeScale)
                sv?.setStyle(captionStyleFor(subtitleStyle))
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isInPip) {
            // Keep it bare in PiP — the system draws its own transport controls
            // from the RemoteActions we supply; there's no room or need for
            // our own overlay chrome on top of that.
            return@Box
        }

        if (isBuffering) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(30.dp))
            }
        }

        // --- Gesture layers ---
        if (locked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { lockHintVisible = true })
                    }
            )
        } else {
            // Three horizontal zones. Every zone toggles the controls on a single tap —
            // that's the interaction people reach for constantly, so it must never be a
            // dead spot. The two edge zones *additionally* recognize a double-tap for
            // 10s seek, which means they pay Android's standard tap/double-tap
            // disambiguation delay (~300ms); the center zone has no double-tap
            // registered on it at all, so it stays instant, with no wait.
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(0.32f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { if (activeMenu == ActiveMenu.NONE) controlsVisible = !controlsVisible },
                                onDoubleTap = { seekRelative(-10_000L, true) }
                            )
                        }
                )
                Box(
                    modifier = Modifier
                        .weight(0.36f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { if (activeMenu == ActiveMenu.NONE) controlsVisible = !controlsVisible }
                            )
                        }
                )
                Box(
                    modifier = Modifier
                        .weight(0.32f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { if (activeMenu == ActiveMenu.NONE) controlsVisible = !controlsVisible },
                                onDoubleTap = { seekRelative(10_000L, false) }
                            )
                        }
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(durationMs) {
                        var accumDx = 0f
                        var accumDy = 0f
                        var axis: DragAxis? = null
                        var side: DragSide? = null
                        var scrubBaseMs = 0L

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
                            if (axis == null) {
                                if (abs(accumDx) > touchSlopPx || abs(accumDy) > touchSlopPx) {
                                    axis = if (abs(accumDx) > abs(accumDy)) DragAxis.HORIZONTAL else DragAxis.VERTICAL
                                    if (axis == DragAxis.VERTICAL) {
                                        side = if (change.position.x < size.width / 2f) DragSide.BRIGHTNESS else DragSide.VOLUME
                                    }
                                }
                            }
                            when (axis) {
                                DragAxis.HORIZONTAL -> {
                                    change.consume()
                                    scrubbing = true
                                    val rangeMs = (durationMs * 0.25f).coerceIn(30_000f, 600_000f)
                                    val deltaMs = (accumDx / size.width) * rangeMs
                                    scrubTargetMs = (scrubBaseMs + deltaMs.toLong()).coerceIn(0, durationMs.coerceAtLeast(0))
                                    scrubDeltaMs = scrubTargetMs - scrubBaseMs
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

        seekBubble?.let { (text, isLeft) -> SeekBubble(text = text, isLeft = isLeft) }
        hud?.let {
            when (it) {
                HudType.BRIGHTNESS -> LevelHud(Icons.Outlined.BrightnessMedium, brightnessLevel, "${(brightnessLevel * 100).toInt()}%")
                HudType.VOLUME -> LevelHud(Icons.Outlined.VolumeUp, volumeLevel, "${(volumeLevel * 100).toInt()}%")
            }
        }
        if (scrubbing) {
            val deltaSec = scrubDeltaMs / 1000
            val sign = if (deltaSec >= 0) "+" else ""
            val nearestThumb = scrubThumbnails.minByOrNull { abs(it.first - scrubTargetMs) }?.second
            ScrubHud(targetTimeLabel = formatTime(scrubTargetMs), deltaLabel = "$sign${deltaSec}s", thumbnailPath = nearestThumb)
        }

        if (locked) {
            AnimatedVisibility(
                visible = lockHintVisible,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(180)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .premiumPressableNoScale(onClick = { toggleLock() })
                        .glassPanel(shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.LockOpen, contentDescription = "Unlock", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && !locked,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerControls(
                title = file.nameWithoutExtension,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                bufferedPercentage = bufferedPercentage,
                aspectMode = aspectMode,
                scrubThumbnails = scrubThumbnails,
                onBack = onBack,
                onLock = { toggleLock() },
                onPip = { enterPip() },
                onPlayPause = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                onSkipBack = { seekRelative(-10_000L, true) },
                onSkipForward = { seekRelative(10_000L, false) },
                onOpenSubtitles = { activeMenu = ActiveMenu.SUBTITLES },
                onOpenAudio = { activeMenu = ActiveMenu.AUDIO },
                onOpenSpeed = { activeMenu = ActiveMenu.SPEED },
                onCycleAspect = { cycleAspect() },
                onCycleOrientation = { cycleOrientation() },
                onOpenSleepTimer = { activeMenu = ActiveMenu.SLEEP_TIMER },
                onScrubStart = { isDraggingSlider = true },
                onScrubEnd = { ms ->
                    exoPlayer.seekTo(ms)
                    currentPositionMs = ms
                    isDraggingSlider = false
                }
            )
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
            ActiveMenu.SUBTITLES -> {
                val options = listOf(PopupOption("off", "Off")) +
                    subtitleOptionsFrom(currentTracks).map { PopupOption(it.group.id + "-" + it.trackIndex, it.label) }
                GlassPopupMenu(
                    title = "Subtitles",
                    options = options,
                    selectedKey = selectedSubtitleKey,
                    onSelect = { selectSubtitle(it) },
                    onDismiss = { activeMenu = ActiveMenu.NONE },
                    headerTrailing = {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .premiumPressableNoScale(onClick = { activeMenu = ActiveMenu.SUBTITLE_STYLE })
                                .glassPanel(shape = CircleShape, fill = GlassFillLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Subtitle style", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                )
            }
            ActiveMenu.SUBTITLE_STYLE -> SubtitleStyleSheet(
                style = subtitleStyle,
                onStyleChange = { updateSubtitleStyle(it) },
                onDismiss = { activeMenu = ActiveMenu.NONE }
            )
            ActiveMenu.AUDIO -> {
                val audioOpts = audioOptionsFrom(currentTracks)
                val options = audioOpts.map { PopupOption(it.group.id + "-" + it.trackIndex, it.label) }
                val selected = selectedAudioKey ?: audioOpts.find { it.selected }?.let { it.group.id + "-" + it.trackIndex }
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
                selectedKey = if (sleepTimerEndAt == null) "off" else sleepTimerOptions.find { it.label == sleepTimerLabel }?.minutes?.toString(),
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
            ActiveMenu.NONE -> {}
        }
    }
}

private fun captionStyleFor(style: SubtitleStyle): CaptionStyleCompat {
    val foreground = AndroidColor.WHITE
    return when (style.edgeStyle) {
        SubtitleEdgeStyle.OUTLINE -> CaptionStyleCompat(
            foreground, AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_OUTLINE, AndroidColor.argb(200, 0, 0, 0), null
        )
        SubtitleEdgeStyle.DROP_SHADOW -> CaptionStyleCompat(
            foreground, AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW, AndroidColor.argb(200, 0, 0, 0), null
        )
        SubtitleEdgeStyle.BACKGROUND_BOX -> CaptionStyleCompat(
            foreground, AndroidColor.argb(160, 0, 0, 0), AndroidColor.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_NONE, AndroidColor.TRANSPARENT, null
        )
        SubtitleEdgeStyle.NONE -> CaptionStyleCompat(
            foreground, AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_NONE, AndroidColor.TRANSPARENT, null
        )
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

@Composable
private fun PlayerControls(
    title: String,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPercentage: Int,
    aspectMode: AspectMode,
    scrubThumbnails: List<Pair<Long, String>>,
    onBack: () -> Unit,
    onLock: () -> Unit,
    onPip: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSpeed: () -> Unit,
    onCycleAspect: () -> Unit,
    onCycleOrientation: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onScrubStart: () -> Unit,
    onScrubEnd: (Long) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        // Subtle top gradient — not an opaque bar
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(ScrimMedium, Color.Transparent)))
                .padding(top = Spacing.xl, bottom = Spacing.xxl)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransportIconButton(icon = Icons.Filled.ArrowBackIosNew, contentDescription = "Back", iconSize = 15.dp, onClick = onBack)
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.xs)
                )
                TransportIconButton(icon = Icons.Outlined.PictureInPictureAlt, contentDescription = "Picture in picture", onClick = onPip)
                Spacer(Modifier.width(Spacing.xxs))
                TransportIconButton(icon = Icons.Filled.Lock, contentDescription = "Lock", onClick = onLock)
            }
        }

        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)
        ) {
            TransportIconButton(icon = Icons.Outlined.Replay10, contentDescription = "Back 10 seconds", size = 46.dp, iconSize = 22.dp, onClick = onSkipBack)
            PlayPauseButton(isPlaying = isPlaying, onClick = onPlayPause)
            TransportIconButton(icon = Icons.Outlined.Forward10, contentDescription = "Forward 10 seconds", size = 46.dp, iconSize = 22.dp, onClick = onSkipForward)
        }

        // Bottom gradient + quick-access row + scrubber
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, ScrimMedium)))
                .padding(top = Spacing.xxl, bottom = Spacing.lg, start = Spacing.md, end = Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickIcon(Icons.Outlined.Subtitles, "Subtitles", onOpenSubtitles)
                QuickIcon(Icons.Outlined.Audiotrack, "Audio", onOpenAudio)
                QuickIcon(Icons.Outlined.Speed, "Speed", onOpenSpeed)
                QuickIcon(Icons.Outlined.AspectRatio, aspectMode.label, onCycleAspect)
                QuickIcon(Icons.Outlined.ScreenRotation, "Rotate", onCycleOrientation)
                QuickIcon(Icons.Outlined.Timer, "Sleep timer", onOpenSleepTimer)
            }

            Spacer(Modifier.height(Spacing.sm))

            Timebar(
                positionMs = currentPositionMs,
                durationMs = durationMs,
                bufferedPercentage = bufferedPercentage,
                thumbnails = scrubThumbnails,
                onScrubStart = onScrubStart,
                onScrubMove = {},
                onScrubEnd = onScrubEnd,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    formatTime(currentPositionMs),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    formatTime(durationMs),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun QuickIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .premiumPressable(scaleDown = 0.88f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun TransportIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp = 36.dp,
    iconSize: androidx.compose.ui.unit.Dp = 18.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .premiumPressable(scaleDown = 0.88f, onClick = onClick)
            .glassPanel(shape = CircleShape, fill = GlassFillLight),
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
