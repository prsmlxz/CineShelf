package com.cineshelf.app.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
    ) {
        when {
            state.isLoading && state.shows.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentPrimary, strokeWidth = 2.dp)
                }
            }
            state.shows.isEmpty() -> {
                Column(Modifier.fillMaxSize()) {
                    LibraryTopBar(showCount = 0, onRefresh = { viewModel.refresh() }, onAdd = { showAddDialog = true })
                    EmptyLibrary(onAdd = { showAddDialog = true }, modifier = Modifier.weight(1f))
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(Spacing.lg, 0.dp, Spacing.lg, Spacing.xxxl),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(2) }) {
                        LibraryTopBar(
                            showCount = state.shows.size,
                            onRefresh = { viewModel.refresh() },
                            onAdd = { showAddDialog = true }
                        )
                    }

                    if (state.continueWatching.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            ContinueWatchingSection(
                                entries = state.continueWatching,
                                onPlay = { onPlay(it.file.absolutePath) }
                            )
                        }
                    }

                    item(span = { GridItemSpan(2) }) {
                        Text(
                            "My Shelf",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs)
                        )
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
                viewModel.createShow(name)
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
            .padding(top = Spacing.xl, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("CineShelf", style = LargeTitle, color = TextPrimary)
            Text(
                if (showCount == 0) "Your library" else "$showCount in your library",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        CircleGlassButton(icon = Icons.Outlined.Refresh, contentDescription = "Refresh", onClick = onRefresh)
        Spacer(Modifier.width(Spacing.xs))
        CircleGlassButton(icon = Icons.Outlined.Add, contentDescription = "Add show", accent = true, onClick = onAdd)
    }
}

@Composable
private fun CircleGlassButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .premiumPressable(onClick = onClick)
            .glassPanel(shape = CircleShape, fill = if (accent) AccentPrimary.copy(alpha = 0.18f) else GlassFillLight),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (accent) AccentPrimary else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ContinueWatchingSection(entries: List<ContinueWatchingEntry>, onPlay: (java.io.File) -> Unit) {
    Column(Modifier.padding(top = Spacing.md)) {
        Text(
            "Continue Watching",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = Spacing.sm)
        )
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
    Column(
        modifier = Modifier
            .width(210.dp)
            .premiumPressable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9.4f)
                .clip(RoundedCornerShape(Radius.md))
        ) {
            if (entry.episode.thumbnailPath != null) {
                AsyncImage(
                    model = File(entry.episode.thumbnailPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(SurfaceCard))
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, ScrimStrong), startY = 60f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .glassPanel(shape = CircleShape, fill = ScrimMedium),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
            ) {
                Text(
                    entry.show.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    entry.episode.displayTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.18f))
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(entry.episode.progressFraction)
                    .height(2.dp)
                    .background(AccentPrimary)
            )
        }
    }
}

@Composable
private fun ShowTile(show: ShowItem, onClick: () -> Unit) {
    Column(modifier = Modifier.premiumPressable(scaleDown = 0.97f, onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
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
                        .background(Brush.linearGradient(listOf(SurfaceCard, SurfaceCardElevated))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Movie, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(26.dp))
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, ScrimSoft), startY = 90f))
            )

            if (show.totalItemCount > 0) {
                val progress = show.watchedCount.toFloat() / show.totalItemCount.toFloat()
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.14f))
                )
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(AccentPrimary)
                )
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
            if (show.totalItemCount == 0) "Empty" else "${show.totalItemCount} item${if (show.totalItemCount == 1) "" else "s"}",
            style = MaterialTheme.typography.bodySmall,
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
                .size(76.dp)
                .glassPanel(shape = CircleShape, fill = GlassFillLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(Spacing.lg))
        Text("Your shelf is empty", style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
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
                .premiumPressable(onClick = onAdd)
                .glassPanel(shape = RoundedCornerShape(Radius.pill), fill = AccentPrimary)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xxs))
            Text("Add Show", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AddShowDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .glassPanel(shape = RoundedCornerShape(Radius.xl), fill = SurfaceCardElevated)
                .padding(Spacing.lg)
        ) {
            Text("New Show or Movie", style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
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
                shape = RoundedCornerShape(Radius.sm),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = HairlineMid,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentPrimary
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
                        .premiumPressable(enabled = text.isNotBlank(), onClick = { if (text.isNotBlank()) onConfirm(text) })
                        .glassPanel(shape = RoundedCornerShape(Radius.sm), fill = if (text.isNotBlank()) AccentPrimary else GlassFillLight)
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                ) {
                    Text(
                        "Create",
                        color = if (text.isNotBlank()) Color.White else TextTertiary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
