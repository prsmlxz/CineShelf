package com.cineshelf.app.ui.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cineshelf.app.data.ContinueWatchingEntry
import com.cineshelf.app.data.ShowItem
import com.cineshelf.app.ui.theme.*
import java.io.File

@Composable
fun LibraryScreen(
    onOpenShow: (String) -> Unit,
    onPlay: (String) -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LibraryContent(
        state = state,
        onOpenShow = onOpenShow,
        onPlay = onPlay,
        onRefresh = { viewModel.refresh() },
        onCreateShow = { viewModel.createShow(it) }
    )
}

/**
 * Stateless body. Split out from [LibraryScreen] so the real UI can be
 * rendered against fixture data in screenshot tests, where no ViewModel or
 * device storage exists.
 */
@Composable
fun LibraryContent(
    state: LibraryUiState,
    onOpenShow: (String) -> Unit,
    onPlay: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreateShow: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .auroraBackdrop()
    ) {
        when {
            state.isLoading && state.shows.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGlow, strokeWidth = 2.dp)
                }
            }
            state.shows.isEmpty() -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = Spacing.lg)
                ) {
                    LibraryTopBar(
                        showCount = 0,
                        onRefresh = onRefresh,
                        onAdd = { showAddDialog = true }
                    )
                    EmptyLibrary(onAdd = { showAddDialog = true }, modifier = Modifier.weight(1f))
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = Spacing.lg,
                        end = Spacing.lg,
                        bottom = Spacing.xxxl
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    item(span = { GridItemSpan(2) }) {
                        LibraryTopBar(
                            showCount = state.shows.size,
                            onRefresh = onRefresh,
                            onAdd = { showAddDialog = true }
                        )
                    }

                    if (state.continueWatching.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            ContinueWatchingSection(
                                entries = state.continueWatching,
                                onPlay = { onPlay(it.absolutePath) }
                            )
                        }
                    }

                    item(span = { GridItemSpan(2) }) {
                        SectionHeader("My Shelf", "${state.shows.size} collections")
                    }

                    items(state.shows, key = { it.id }) { show ->
                        ShowTile(show = show, onClick = { onOpenShow(show.id) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddShowDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                onCreateShow(name)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun LibraryTopBar(showCount: Int, onRefresh: () -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.lg, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            // The wordmark carries the aurora gradient — the one place the
            // palette is stated outright rather than implied.
            Text("CineShelf", style = AuroraWordmark)
            Text(
                if (showCount == 0) "Your library" else "$showCount in your library",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        CircleGlassButton(Icons.Outlined.Refresh, "Refresh", onClick = onRefresh)
        Spacer(Modifier.width(Spacing.xs))
        CircleGlassButton(Icons.Outlined.Add, "Add show", accent = true, onClick = onAdd)
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xxs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        subtitle?.let {
            Text(
                it.uppercase(),
                style = SectionEyebrow,
                color = TextQuaternary,
                modifier = Modifier.padding(start = Spacing.sm, top = 3.dp)
            )
        }
    }
}

@Composable
private fun CircleGlassButton(
    icon: ImageVector,
    contentDescription: String,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .then(
                if (accent) Modifier.auroraGlow(AccentPrimary, radius = 12.dp, glowAlpha = 0.40f)
                else Modifier
            )
            .premiumPressable(scaleDown = 0.88f, onClick = onClick)
            .glassPanel(
                shape = CircleShape,
                fill = if (accent) AccentPrimary.copy(alpha = 0.24f) else GlassFill,
                stroke = if (accent) AccentPrimary.copy(alpha = 0.45f) else GlassStroke
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (accent) AccentGlow else TextSecondary,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun ContinueWatchingSection(entries: List<ContinueWatchingEntry>, onPlay: (File) -> Unit) {
    Column(Modifier.padding(top = Spacing.md)) {
        SectionHeader("Continue Watching", "pick up where you left off")
        Spacer(Modifier.height(Spacing.sm))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(entries, key = { it.episode.id }) { entry ->
                ContinueWatchingCard(entry = entry, onClick = { onPlay(entry.episode.file) })
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(entry: ContinueWatchingEntry, onClick: () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = entry.episode.progressFraction.coerceIn(0f, 1f),
        animationSpec = Motion.gentle(),
        label = "cw-progress"
    )
    Box(
        modifier = Modifier
            .width(232.dp)
            .aspectRatio(16f / 9.6f)
            .premiumPressable(scaleDown = 0.97f, onClick = onClick)
            .clip(RoundedCornerShape(Radius.lg))
            .background(SurfaceCard)
    ) {
        if (entry.episode.thumbnailPath != null) {
            AsyncImage(
                model = File(entry.episode.thumbnailPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(SurfaceCard, SurfaceRaised)))
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, ScrimMedium, ScrimStrong),
                        startY = 50f
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(46.dp)
                .glassPanel(
                    shape = CircleShape,
                    fill = ControlCircleFill,
                    stroke = ControlCircleStroke
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(23.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
        ) {
            Text(
                entry.show.name,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                entry.episode.displayTitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(Spacing.xs))
            ProgressRail(progress)
        }
    }
}

/** Thin two-layer progress rail with a glowing filled portion. */
@Composable
private fun ProgressRail(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(Radius.pill))
            .background(Color.White.copy(alpha = 0.18f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(
                    Brush.horizontalGradient(listOf(AccentPrimary, AccentGlow)),
                    RoundedCornerShape(Radius.pill)
                )
        )
    }
}

@Composable
private fun ShowTile(show: ShowItem, onClick: () -> Unit) {
    Column(modifier = Modifier.premiumPressable(scaleDown = 0.96f, onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 2.7f)
                .clip(RoundedCornerShape(Radius.lg))
                .background(SurfaceCard)
        ) {
            if (show.posterPath != null) {
                AsyncImage(
                    model = File(show.posterPath),
                    contentDescription = show.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(SurfaceCardElevated, SurfaceCard))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Movie,
                        contentDescription = null,
                        tint = TextQuaternary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, ScrimMedium), startY = 160f)
                    )
            )

            // Hairline inner edge — reads as a lit rim on the poster.
            Box(
                Modifier
                    .fillMaxSize()
                    .glassPanel(
                        shape = RoundedCornerShape(Radius.lg),
                        fill = Color.Transparent,
                        stroke = HairlineMid
                    )
            )

            if (show.totalItemCount > 0) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(Spacing.xs)) {
                    ProgressRail(show.watchedCount.toFloat() / show.totalItemCount.toFloat())
                }
            }
        }

        Spacer(Modifier.height(Spacing.xs))

        Text(
            show.name,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            if (show.totalItemCount == 0) "Empty"
            else "${show.totalItemCount} item${if (show.totalItemCount == 1) "" else "s"}",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

@Composable
private fun EmptyLibrary(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .auroraGlow(AccentPrimary, radius = 28.dp, glowAlpha = 0.35f)
                .glassPanel(shape = CircleShape, fill = GlassFillLight, stroke = GlassStrokeBright),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = AccentGlow,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            "Your shelf is empty",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Add a show or movie to create its folder, then download video files into it from your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.xl))
        Row(
            modifier = Modifier
                .auroraGlow(AccentPrimary, radius = 18.dp, glowAlpha = 0.45f)
                .premiumPressable(onClick = onAdd)
                .glassPanel(shape = RoundedCornerShape(Radius.pill), fill = AccentPrimary)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xxs))
            Text(
                "Add Show",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun AddShowDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .glassPanel(
                    shape = RoundedCornerShape(Radius.xl),
                    fill = SurfaceCardElevated,
                    stroke = GlassStrokeBright
                )
                .padding(Spacing.lg)
        ) {
            Text(
                "New Show or Movie",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                "This creates a folder on your device. Download video files into it and they'll show up here.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(Spacing.md))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("e.g. Breaking Bad") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(Radius.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = HairlineMid,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentGlow
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.lg))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "Cancel",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .premiumPressable(onClick = onDismiss)
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                )
                Spacer(Modifier.width(Spacing.sm))
                Row(
                    modifier = Modifier
                        .premiumPressable(
                            enabled = text.isNotBlank(),
                            onClick = { if (text.isNotBlank()) onConfirm(text) }
                        )
                        .glassPanel(
                            shape = RoundedCornerShape(Radius.pill),
                            fill = if (text.isNotBlank()) AccentPrimary else GlassFill
                        )
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                ) {
                    Text(
                        "Create",
                        color = if (text.isNotBlank()) Color.White else TextQuaternary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
