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
import androidx.compose.material.icons.outlined.PlayArrow
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cineshelf.app.data.SeasonGroup
import com.cineshelf.app.data.ShowItem
import com.cineshelf.app.data.VideoItem
import com.cineshelf.app.ui.components.MediaTile
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
                        .glassPanel(
                            shape = RoundedCornerShape(Radius.lg),
                            fill = SurfaceCardElevated,
                            stroke = GlassStrokeBright
                        ),
                    containerColor = Color.Transparent,
                    contentColor = TextPrimary,
                    actionColor = AccentGlow,
                    snackbarData = data
                )
            }
        },
        modifier = Modifier.auroraBackdrop()
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
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

    if (show.isSingleMovie && visibleStandalone.isNotEmpty()) {
        MovieHero(item = visibleStandalone.first(), showName = show.name, onBack = onBack, onPlay = onPlay)
        return
    }

    // The obvious next thing to watch: the first partly-watched episode,
    // else the first unwatched one. This is the button people actually want.
    val allInOrder = visibleSeasons.flatMap { it.episodes } + visibleStandalone
    val upNext = allInOrder.firstOrNull { it.isInProgress } ?: allInOrder.firstOrNull { !it.watched }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = Spacing.xxxl)
    ) {
        item {
            DetailHeader(
                name = show.name,
                subtitle = "${show.watchedCount} of ${show.totalItemCount} watched",
                progress = if (show.totalItemCount > 0)
                    show.watchedCount.toFloat() / show.totalItemCount.toFloat() else 0f,
                onBack = onBack
            )
        }

        if (upNext != null) {
            item { UpNextCard(item = upNext, onPlay = { onPlay(upNext) }) }
        }

        items(visibleSeasons, key = { "season-${it.seasonNumber}" }) { season: SeasonGroup ->
            SeasonRail(season = season, onPlay = onPlay, onToggleWatched = onToggleWatched, onDelete = onDelete)
        }

        if (visibleStandalone.isNotEmpty()) {
            item {
                MediaRail(
                    title = if (visibleSeasons.isEmpty()) "Videos" else "Movies & Specials",
                    count = visibleStandalone.size,
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
                        .padding(top = 80.dp, start = Spacing.xl, end = Spacing.xl),
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

@Composable
private fun DetailHeader(name: String, subtitle: String, progress: Float, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = Spacing.lg, start = Spacing.md, end = Spacing.xl, bottom = Spacing.xs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .premiumPressable(scaleDown = 0.88f, onClick = onBack)
                    .glassPanel(shape = CircleShape, fill = GlassFill, stroke = GlassStroke),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.displaySmall,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle.uppercase(),
                    style = SectionEyebrow,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
        if (progress > 0f) {
            Spacer(Modifier.height(Spacing.sm))
            Box(
                Modifier
                    .padding(start = 50.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(
                            Brush.horizontalGradient(listOf(AccentPrimary, AccentGlow)),
                            RoundedCornerShape(Radius.pill)
                        )
                )
            }
        }
    }
}

/**
 * Wide hero for the next episode. The thumbnail is used twice — once blurred
 * as an ambient wash behind the card, once crisp inside it — so the card picks
 * up the colour of the show it belongs to.
 */
@Composable
private fun UpNextCard(item: VideoItem, onPlay: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl, vertical = Spacing.md)
    ) {
        if (item.thumbnailPath != null) {
            AsyncImage(
                model = File(item.thumbnailPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .blur(38.dp)
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .premiumPressable(scaleDown = 0.98f, onClick = onPlay)
                .glassPanel(
                    shape = RoundedCornerShape(Radius.xl),
                    fill = SurfaceCardElevated.copy(alpha = 0.82f),
                    stroke = GlassStroke
                )
                .padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(118.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(SurfaceCard),
                contentAlignment = Alignment.Center
            ) {
                if (item.thumbnailPath != null) {
                    AsyncImage(
                        model = File(item.thumbnailPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    Modifier
                        .size(34.dp)
                        .glassPanel(shape = CircleShape, fill = ControlCircleFill, stroke = ControlCircleStroke),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    if (item.isInProgress) "RESUME" else "UP NEXT",
                    style = SectionEyebrow,
                    color = AccentGlow
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    item.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.isInProgress) {
                    Spacer(Modifier.height(Spacing.xs))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(item.progressFraction)
                                .background(
                                    Brush.horizontalGradient(listOf(AccentPrimary, AccentGlow)),
                                    RoundedCornerShape(Radius.pill)
                                )
                        )
                    }
                }
            }
            Spacer(Modifier.width(Spacing.xs))
        }
    }
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
        count = season.episodes.size,
        items = season.episodes,
        onPlay = onPlay,
        onToggleWatched = onToggleWatched,
        onDelete = onDelete
    )
}

@Composable
private fun MediaRail(
    title: String,
    count: Int,
    items: List<VideoItem>,
    onPlay: (VideoItem) -> Unit,
    onToggleWatched: (VideoItem) -> Unit,
    onDelete: (VideoItem) -> Unit
) {
    Column(Modifier.padding(top = Spacing.lg)) {
        Row(
            modifier = Modifier.padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(width = 3.dp, height = 15.dp)
                    .background(
                        Brush.verticalGradient(listOf(AccentGlow, AccentPrimary)),
                        RoundedCornerShape(Radius.pill)
                    )
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                "$count",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier
                    .glassPanel(shape = RoundedCornerShape(Radius.pill), fill = GlassFill)
                    .padding(horizontal = 7.dp, vertical = 1.dp)
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.xl),
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

@Composable
private fun MovieHero(item: VideoItem, showName: String, onBack: () -> Unit, onPlay: (VideoItem) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        DetailHeader(
            name = showName,
            subtitle = if (item.watched) "Movie · watched" else "Movie",
            progress = item.progressFraction,
            onBack = onBack
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl, vertical = Spacing.md)
                .aspectRatio(16f / 9f)
                .premiumPressable(scaleDown = 0.98f, onClick = { onPlay(item) })
                .clip(RoundedCornerShape(Radius.xl))
                .background(SurfaceCard),
            contentAlignment = Alignment.Center
        ) {
            if (item.thumbnailPath != null) {
                AsyncImage(
                    model = File(item.thumbnailPath),
                    contentDescription = item.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, ScrimMedium)))
            )
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .auroraGlow(AccentPrimary, radius = 16.dp, glowAlpha = 0.30f)
                    .glassPanel(shape = CircleShape, fill = ControlCircleFill, stroke = ControlCircleStroke),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .glassPanel(
                        shape = RoundedCornerShape(Radius.xl),
                        fill = Color.Transparent,
                        stroke = HairlineMid
                    )
            )
        }
        Text(
            item.file.name,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Spacing.xl)
        )
    }
}
