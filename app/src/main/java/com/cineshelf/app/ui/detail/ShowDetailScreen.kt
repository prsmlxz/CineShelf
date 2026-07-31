package com.cineshelf.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
        containerColor = BackgroundPrimary,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = SurfaceCardElevated,
                    contentColor = TextPrimary,
                    actionColor = AccentPrimary,
                    shape = RoundedCornerShape(14.dp),
                    snackbarData = data
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(BackgroundPrimary)
                .padding(padding)
        ) {
            when {
                state.isLoading && state.show == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentPrimary)
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

@Composable
private fun ShowContent(
    show: ShowItem,
    hiddenIds: List<String>,
    onBack: () -> Unit,
    onPlay: (VideoItem) -> Unit,
    onToggleWatched: (VideoItem) -> Unit,
    onDelete: (VideoItem) -> Unit
) {
    val visibleStandalone = show.standalone.filter { it.id !in hiddenIds }
    val visibleSeasons = show.seasons.map { it.copy(episodes = it.episodes.filter { e -> e.id !in hiddenIds }) }
        .filter { it.episodes.isNotEmpty() }

    if (show.isSingleMovie && visibleStandalone.isNotEmpty()) {
        MovieHero(
            item = visibleStandalone.first(),
            showName = show.name,
            onBack = onBack,
            onPlay = onPlay
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            DetailHeader(name = show.name, subtitle = "${show.totalItemCount} items", onBack = onBack)
        }

        items(visibleSeasons, key = { "season-${it.seasonNumber}" }) { season: SeasonGroup ->
            SeasonRail(
                season = season,
                onPlay = onPlay,
                onToggleWatched = onToggleWatched,
                onDelete = onDelete
            )
        }

        if (visibleStandalone.isNotEmpty()) {
            item {
                MediaRail(
                    title = if (visibleSeasons.isEmpty()) "Videos" else "Movies & Specials",
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
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No videos here yet.\nDownload files into this folder on your device.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(name: String, subtitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, start = 8.dp, end = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        Spacer(Modifier.width(4.dp))
        Column {
            Text(name, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
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
        items = season.episodes,
        onPlay = onPlay,
        onToggleWatched = onToggleWatched,
        onDelete = onDelete
    )
}

@Composable
private fun MediaRail(
    title: String,
    items: List<VideoItem>,
    onPlay: (VideoItem) -> Unit,
    onToggleWatched: (VideoItem) -> Unit,
    onDelete: (VideoItem) -> Unit
) {
    Column(Modifier.padding(top = 18.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(start = 24.dp, bottom = 10.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { episode ->
                MediaTile(
                    item = episode,
                    onClick = { onPlay(episode) },
                    onToggleWatched = { onToggleWatched(episode) },
                    onDelete = { onDelete(episode) },
                    modifier = Modifier.width(168.dp)
                )
            }
        }
    }
}

@Composable
private fun MovieHero(
    item: VideoItem,
    showName: String,
    onBack: () -> Unit,
    onPlay: (VideoItem) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        DetailHeader(name = showName, subtitle = "Movie", onBack = onBack)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceCard)
                .clickable { onPlay(item) },
            contentAlignment = Alignment.Center
        ) {
            if (item.thumbnailPath != null) {
                coil.compose.AsyncImage(
                    model = File(item.thumbnailPath),
                    contentDescription = item.displayTitle,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            item.file.name,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
