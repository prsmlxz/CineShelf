package com.cineshelf.app.ui.player

import android.app.Activity
import android.net.Uri
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.cineshelf.app.ui.theme.AccentPrimary
import com.cineshelf.app.ui.theme.TextPrimary
import com.cineshelf.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Locale

private val speedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
fun PlayerScreen(
    filePath: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val file = remember(filePath) { File(filePath) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
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
    var controlsVisible by remember { mutableStateOf(true) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var seekBubble by remember { mutableStateOf<String?>(null) }

    // --- Immersive fullscreen + keep screen on ---
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
       controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // --- Player event listener ---
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0)
                }
            }
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

    // --- Periodic progress save (covers watched-marking + crash resilience) ---
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(5000)
            if (durationMs > 0) {
                viewModel.saveProgress(file, exoPlayer.currentPosition, durationMs)
            }
        }
    }

    // --- Auto-hide controls while playing ---
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3500)
            controlsVisible = false
        }
    }

    // --- Seek feedback bubble auto-dismiss ---
    LaunchedEffect(seekBubble) {
        if (seekBubble != null) {
            delay(650)
            seekBubble = null
        }
    }

    // --- Lifecycle-aware pause/resume + final save ---
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (durationMs > 0) viewModel.saveProgress(file, exoPlayer.currentPosition, durationMs)
                    exoPlayer.pause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (durationMs > 0) viewModel.saveProgress(file, exoPlayer.currentPosition, durationMs)
            exoPlayer.release()
        }
    }

    fun seekRelative(deltaMs: Long) {
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0, durationMs.coerceAtLeast(0))
        exoPlayer.seekTo(target)
        currentPositionMs = target
        seekBubble = if (deltaMs > 0) "+10s" else "-10s"
        controlsVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture layer: tap to toggle controls, double-tap sides to seek
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible },
                        onDoubleTap = { offset ->
                            if (offset.x < size.width / 2f) seekRelative(-10_000L) else seekRelative(10_000L)
                        }
                    )
                }
        )

        seekBubble?.let { text ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50)),
                ) {
                    Text(
                        text,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerControls(
                title = file.nameWithoutExtension,
                isPlaying = isPlaying,
                currentPositionMs = if (isDraggingSlider) dragPosition.toLong() else currentPositionMs,
                durationMs = durationMs,
                bufferedPercentage = bufferedPercentage,
                playbackSpeed = playbackSpeed,
                showSpeedMenu = showSpeedMenu,
                onBack = onBack,
                onPlayPause = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                onSkipBack = { seekRelative(-10_000L) },
                onSkipForward = { seekRelative(10_000L) },
                onSpeedClick = { showSpeedMenu = true },
                onSpeedSelected = { speed ->
                    exoPlayer.playbackParameters = PlaybackParameters(speed)
                    playbackSpeed = speed
                    showSpeedMenu = false
                },
                onDismissSpeedMenu = { showSpeedMenu = false },
                onSliderStart = { isDraggingSlider = true },
                onSliderChange = { dragPosition = it },
                onSliderFinished = {
                    exoPlayer.seekTo(dragPosition.toLong())
                    isDraggingSlider = false
                }
            )
        }
    }
}

@Composable
private fun PlayerControls(
    title: String,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPercentage: Int,
    playbackSpeed: Float,
    showSpeedMenu: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onSpeedClick: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onDismissSpeedMenu: () -> Unit,
    onSliderStart: () -> Unit,
    onSliderChange: (Float) -> Unit,
    onSliderFinished: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        // Top scrim + bar
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
                .padding(top = 12.dp, bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )
                Box {
                    IconButton(onClick = onSpeedClick) {
                        Icon(Icons.Default.Speed, contentDescription = "Playback speed", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = onDismissSpeedMenu
                    ) {
                        speedOptions.forEach { speed ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (speed == 1f) "Normal" else "${speed}x",
                                        fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = { onSpeedSelected(speed) }
                            )
                        }
                    }
                }
            }
        }

        // Center transport controls
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            CircleIconButton(icon = Icons.Default.FastRewind, size = 46.dp, onClick = onSkipBack)
            CircleIconButton(
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                size = 68.dp,
                emphasized = true,
                onClick = onPlayPause
            )
            CircleIconButton(icon = Icons.Default.FastForward, size = 46.dp, onClick = onSkipForward)
        }

        // Bottom scrim + scrubber
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Slider(
                value = currentPositionMs.toFloat().coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
                onValueChange = {
                    onSliderStart()
                    onSliderChange(it)
                },
                onValueChangeFinished = onSliderFinished,
                valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = AccentPrimary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(currentPositionMs), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                Text(formatTime(durationMs), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    size: androidx.compose.ui.unit.Dp,
    emphasized: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (emphasized) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (emphasized) Color.Black else Color.White,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

private fun formatTime(ms: Long): String {
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
