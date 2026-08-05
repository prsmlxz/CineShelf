package com.cineshelf.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cineshelf.app.data.MediaInfo
import com.cineshelf.app.data.SeasonGroup
import com.cineshelf.app.data.ShowItem
import com.cineshelf.app.data.VideoItem
import com.cineshelf.app.ui.components.MediaBadgeRow
import com.cineshelf.app.ui.components.MediaTile
import com.cineshelf.app.ui.components.SpecLine
import com.cineshelf.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ShowDetailScreen(
    folderPath: String,
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    viewModel: ShowDetailViewModel = viewModel()
) {
    LaunchedEffect(folderPath) { viewModel.load(folderPath) }

    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val hiddenIds = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()

    fun requestDelete(item: VideoItem) {
        hiddenIds.add(item.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Deleted \"${item.displayTitle}\"",
                actionLabel = "Undo",
                withDismissAction = false,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                hiddenIds.remove(item.id)
            } else {
                viewModel.commitDelete(item)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier
                        .padding(Spacing.md)
                        .navigationBarsPadding()
                        .clip(RoundedCornerShape(Radius.lg))
                        .background(SurfaceSheet),
                    containerColor = Color.Transparent,
                    contentColor = TextPrimary,
                    actionColor = AccentBright,
                    snackbarData = data
                )
            }
        },
        modifier = Modifier.auroraBackdrop()
    ) { padding ->
        // Only the bottom inset is consumed: the backdrop is supposed to run up
        // behind the status bar, so applying the top inset here would leave a
        // black band above it.
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            when {
                state.isLoading && state.show == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentGlow, strokeWidth = 2.dp)
                    }
                }
                state.show != null -> {
                    ShowContent(
                        show = state.show!!,
                        hiddenIds = hiddenIds,
                        onBack = onBack,
                        onPlay = { onPlay(it.file.absolutePath) },
                        onToggleWatched = { viewModel.toggleWatched(it) },
                        onDelete = { requestDelete(it) }
                    )
                }
            }
        }
    }
}

/**
 * Stateless body. Split out from [ShowDetailScreen] so screenshot tests can
 * render the real UI against fixture data, with no ViewModel or disk access.
 */
@Composable
fun ShowContent(
    show: ShowItem,
    hiddenIds: List<String>,
    onBack: () -> Unit,
    onPlay: (VideoItem) -> Unit,
    onToggleWatched: (VideoItem) -> Unit,
    onDelete: (VideoItem) -> Unit
) {
    val visibleStandalone = show.standalone.filter { it.id !in hiddenIds }
    val visibleSeasons = show.seasons
        .map { it.copy(episodes = it.episodes.filter { e -> e.id !in hiddenIds }) }
        .filter { it.episodes.isNotEmpty() }

    // The obvious next thing to watch: the first partly-watched item, else the
    // first unwatched one. This is what the hero's Play button targets.
    val allInOrder = visibleSeasons.flatMap { it.episodes } + visibleStandalone
    val upNext = allInOrder.firstOrNull { it.isInProgress }
        ?: allInOrder.firstOrNull { !it.watched }
        ?: allInOrder.firstOrNull()

    // For a single movie the file *is* the show, so its facts belong in the hero.
    // For a series the hero describes the episode you're about to resume.
    val heroItem = if (show.isSingleMovie) visibleStandalone.firstOrNull() else upNext

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.xxxl)
    ) {
        item {
            DetailHero(
                show = show,
                heroItem = heroItem,
                onBack = onBack,
                onPlay = { heroItem?.let(onPlay) }
            )
        }

        if (heroItem != null && heroItem.isInProgress) {
            item {
                ProgressPanel(
                    item = heroItem,
                    // On a series the panel has to say *which* episode is being
                    // resumed; on a movie there is only one thing it could mean.
                    episodeLabel = if (show.isSingleMovie) null else heroItem.displayTitle
                )
            }
        }

        // Only for a film. On a series this describes one arbitrary episode, and
        // pushing the season rails below a full spec card buries the thing the
        // page actually exists to show.
        if (show.isSingleMovie && heroItem?.mediaInfo != null) {
            item { SpecSheet(item = heroItem, info = heroItem.mediaInfo) }
        }

        items(visibleSeasons, key = { "season-${it.seasonNumber}" }) { season: SeasonGroup ->
            SeasonRail(season = season, onPlay = onPlay, onToggleWatched = onToggleWatched, onDelete = onDelete)
        }

        // A single movie is already fully represented by the hero; listing it
        // again below as a lone tile is just repetition.
        if (visibleStandalone.isNotEmpty() && !show.isSingleMovie) {
            item {
                MediaRail(
                    title = if (visibleSeasons.isEmpty()) "Videos" else "Movies & Specials",
                    countLabel = visibleStandalone.size.let { if (it == 1) "1 video" else "$it videos" },
                    items = visibleStandalone,
                    onPlay = onPlay,
                    onToggleWatched = onToggleWatched,
                    onDelete = onDelete
                )
            }
        }

        if (visibleSeasons.isEmpty() && visibleStandalone.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillParentMaxWidth()
                        .padding(top = Spacing.xxl, start = Spacing.xl, end = Spacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No videos here yet.\nDownload files into this folder on your device.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * The page header: a full-bleed backdrop that fades into the page, with the
 * poster, title, technical badges and the play button layered over its lower
 * half.
 *
 * The same frame is used for both backdrop and poster because that's all we
 * have — there is no artwork service here, only a frame grabbed from the file.
 * Blurring and darkening the backdrop copy is what keeps it from looking like
 * the same image printed twice: it becomes a colour wash, and the crisp poster
 * beside it is what the eye lands on.
 */
@Composable
private fun DetailHero(
    show: ShowItem,
    heroItem: VideoItem?,
    onBack: () -> Unit,
    onPlay: () -> Unit
) {
    val artwork = heroItem?.thumbnailPath ?: show.posterPath

    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(348.dp)
        ) {
            if (artwork != null) {
                AsyncImage(
                    model = File(artwork),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(20.dp)
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(SurfaceCardElevated, BackgroundPrimary)))
                )
            }
            // Two scrims: one flat pass to knock the backdrop back so white text
            // survives a bright frame, one vertical fade so the image dissolves
            // into the page rather than ending on a hard edge.
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)))
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.55f),
                            0.32f to Color.Transparent,
                            0.72f to BackgroundPrimary.copy(alpha = 0.86f),
                            1.0f to BackgroundPrimary
                        )
                    )
            )
        }

        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                Modifier.padding(start = Spacing.xs, top = Spacing.xxs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bare glyph on a 48dp target. The glass disc it used to sit in
                // was the only circle on the page and drew the eye away from the
                // artwork it was floating on.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .premiumPressableSoft(scaleDown = 0.90f, dimTo = 0.5f, onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xxl))

            Row(
                Modifier.padding(horizontal = Spacing.lg),
                verticalAlignment = Alignment.Bottom
            ) {
                PosterCard(artwork = artwork, name = show.name)
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f).padding(bottom = Spacing.xxs)) {
                    Text(
                        show.name,
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        libraryLine(show),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            MediaBadgeRow(
                info = heroItem?.mediaInfo,
                runtimeLabel = heroItem?.let { runtimeOf(it) },
                modifier = Modifier.padding(horizontal = Spacing.lg)
            )

            if (heroItem != null) {
                Spacer(Modifier.height(Spacing.md))
                PlayButton(item = heroItem, onPlay = onPlay, modifier = Modifier.padding(horizontal = Spacing.lg))
            }
        }
    }
}

/**
 * The single section-heading treatment for this page, matching the library's.
 * Title case, one size — "Continue watching" here against "Continue Watching"
 * two taps away read as two different apps.
 */
@Composable
private fun SectionHeading(text: String) {
    Text(text, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
}

/** 2:3 poster sitting on the backdrop's lower fade. */
@Composable
private fun PosterCard(artwork: String?, name: String) {
    Box(
        Modifier
            .width(108.dp)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(Radius.md))
            .background(SurfaceCard),
        contentAlignment = Alignment.Center
    ) {
        if (artwork != null) {
            AsyncImage(
                model = File(artwork),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Same monogram treatment as the library shelf, so a show without
            // artwork looks the same in both places rather than showing a
            // failed-image glyph here and a designed cover there.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(SurfaceCardElevated, SurfaceSunken))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?",
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 46.sp,
                    color = Color.White.copy(alpha = 0.11f)
                )
            }
        }
    }
}

/**
 * The one solid-accent element on the page. Everything else is text or plain
 * surface, so this is unambiguously the primary action — and it says what it
 * will actually do rather than just "Play".
 */
@Composable
private fun PlayButton(item: VideoItem, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val resuming = item.isInProgress
    Row(
        modifier = modifier
            .fillMaxWidth()
            .premiumPressable(scaleDown = 0.97f, onClick = onPlay)
            .clip(RoundedCornerShape(Radius.pill))
            .background(AccentPrimary)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            if (resuming) "Resume from ${formatClock(item.positionMs)}" else "Play",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

/**
 * The technical detail list. Separate from the badge row above: badges are for
 * glancing, this is for checking — so it holds the things you'd only look up
 * deliberately, like how many subtitle tracks are embedded.
 */
@Composable
private fun SpecSheet(item: VideoItem, info: MediaInfo) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        SectionHeading("Details")
        Spacer(Modifier.height(Spacing.xs))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.lg))
                .background(SurfaceCard)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
        ) {
            runtimeOf(item)?.let { SpecLine(Icons.Outlined.Schedule, "Runtime", it) }

            if (info.width > 0 && info.height > 0) {
                SpecLine(
                    Icons.Outlined.Videocam,
                    "Video",
                    listOfNotNull(
                        "${info.width}×${info.height}",
                        info.videoCodec,
                        info.hdrFormat
                    ).joinToString(" · ")
                )
            }

            info.primaryAudio?.let { track ->
                SpecLine(
                    Icons.Outlined.Headphones,
                    if (info.audioTracks.size > 1) "Audio (${info.audioTracks.size} tracks)" else "Audio",
                    track.label
                )
            }

            SpecLine(
                Icons.Outlined.ClosedCaption,
                "Subtitles",
                when (info.subtitleTrackCount) {
                    0 -> "None embedded"
                    1 -> "1 track"
                    else -> "${info.subtitleTrackCount} tracks"
                }
            )

            info.fileSizeLabel?.let {
                SpecLine(Icons.Outlined.Storage, "File", "$it · ${item.file.extension.uppercase()}")
            }
        }
    }
}

/**
 * "Continue watching" for the hero item — how far in you are, and how much is
 * left. Only drawn when there is genuine progress; a 0% bar on an untouched
 * file would just be a decorative empty line.
 */
@Composable
private fun ProgressPanel(item: VideoItem, episodeLabel: String?) {
    val remainingMs = (item.effectiveDurationMs - item.positionMs).coerceAtLeast(0L)
    val percent = (item.progressFraction * 100).toInt()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
    ) {
        SectionHeading("Continue Watching")
        Spacer(Modifier.height(Spacing.xs))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.lg))
                .background(SurfaceCard)
                .padding(Spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    episodeLabel ?: "$percent% watched",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${runtimeLabelOf(remainingMs)} left",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(item.progressFraction)
                        .background(AccentPrimary, RoundedCornerShape(Radius.pill))
                )
            }
        }
    }
}

private fun libraryLine(show: ShowItem): String = when {
    show.isSingleMovie -> "Movie"
    show.seasons.isNotEmpty() -> {
        val seasons = show.seasons.size
        "$seasons season${if (seasons == 1) "" else "s"} · " +
            "${show.watchedCount} of ${show.totalItemCount} watched"
    }
    else -> "${show.totalItemCount} item${if (show.totalItemCount == 1) "" else "s"}"
}

private fun runtimeOf(item: VideoItem): String? {
    val ms = item.effectiveDurationMs
    if (ms <= 0) return null
    return runtimeLabelOf(ms)
}

private fun runtimeLabelOf(ms: Long): String {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatClock(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Composable
private fun SeasonRail(
    season: SeasonGroup,
    onPlay: (VideoItem) -> Unit,
    onToggleWatched: (VideoItem) -> Unit,
    onDelete: (VideoItem) -> Unit
) {
    MediaRail(
        title = season.title,
        countLabel = season.episodes.size.let { if (it == 1) "1 episode" else "$it episodes" },
        items = season.episodes,
        onPlay = onPlay,
        onToggleWatched = onToggleWatched,
        onDelete = onDelete
    )
}

@Composable
private fun MediaRail(
    title: String,
    countLabel: String,
    items: List<VideoItem>,
    onPlay: (VideoItem) -> Unit,
    onToggleWatched: (VideoItem) -> Unit,
    onDelete: (VideoItem) -> Unit
) {
    Column(Modifier.padding(top = Spacing.md)) {
        Row(
            modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.sm),
            verticalAlignment = Alignment.Bottom
        ) {
            SectionHeading(title)
            Spacer(Modifier.width(Spacing.xs))
            // Named, not a bare numeral. "Season 1  5" made the reader guess
            // what was being counted.
            Text(
                countLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            items(items, key = { it.id }) { episode ->
                MediaTile(
                    item = episode,
                    onClick = { onPlay(episode) },
                    onToggleWatched = { onToggleWatched(episode) },
                    onDelete = { onDelete(episode) },
                    modifier = Modifier.width(182.dp)
                )
            }
        }
    }
}
