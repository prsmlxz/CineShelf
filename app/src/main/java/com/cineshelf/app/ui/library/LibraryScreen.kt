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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FolderOpen
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
import androidx.compose.ui.unit.sp
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
                    LibraryTopBar(onRefresh = onRefresh, onAdd = { showAddDialog = true })
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
                        LibraryTopBar(onRefresh = onRefresh, onAdd = { showAddDialog = true })
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
                        SectionHeader("My Shelf")
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

/**
 * One UI large-title header. The title sits on its own line with real space
 * above it, and the actions align to the baseline rather than floating in
 * circles beside it — the two glass discs here were the first thing you saw on
 * opening the app, ahead of the artwork.
 */
@Composable
private fun LibraryTopBar(onRefresh: () -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xxl, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("CineShelf", style = AuroraWordmark)
        }
        BarIconButton(Icons.Outlined.Refresh, "Refresh", onClick = onRefresh)
        BarIconButton(Icons.Outlined.Add, "Add show", onClick = onAdd)
    }
}

/**
 * Section label. No coloured rule beside it, and no second line: the subtitles
 * that used to sit here ("pick up where you left off", "4 collections") restated
 * the heading or counted something already on screen, and a caption under every
 * heading flattens the hierarchy it was meant to create.
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineLarge,
        color = TextPrimary,
        modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.xxs)
    )
}

/** A bare 48dp glyph button for the header. */
@Composable
private fun BarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .premiumPressableSoft(scaleDown = 0.90f, dimTo = 0.5f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = TextPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ContinueWatchingSection(entries: List<ContinueWatchingEntry>, onPlay: (File) -> Unit) {
    Column(Modifier.padding(top = Spacing.md)) {
        SectionHeader("Continue Watching")
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
            PosterMonogram(entry.show.name, compact = true)
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
        // No play disc. The whole card is the play target, and a translucent
        // circle floating over the artwork is the tell of a generic media grid —
        // it hides the frame it sits on and repeats on every card.
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

/**
 * Progress rail. Flat accent, not a gradient — this is a measurement, and a bar
 * that changes hue along its length reads as ornament.
 */
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
                .background(AccentPrimary, RoundedCornerShape(Radius.pill))
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
                PosterMonogram(show.name)
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, ScrimMedium), startY = 160f)
                    )
            )

            // No rim. Artwork carries its own edge against a black background;
            // a hairline around every poster turns the shelf into a wireframe.

            if (show.seasons.isNotEmpty()) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.xs)
                        .clip(RoundedCornerShape(Radius.xs))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${show.seasons.size}S",
                        style = BadgeLabel,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            if (show.totalItemCount > 0 && show.watchedCount > 0) {
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
            shelfSubtitle(show),
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

/**
 * Stand-in for missing poster art: the show's initial, set very large and very
 * dim over a soft gradient.
 *
 * A small centred "no image" glyph is what made these cards read as broken —
 * it looks like a failed load. A monogram at poster scale reads as a designed
 * cover, and because it differs per show the shelf stops looking like a grid of
 * identical empty rectangles.
 */
@Composable
private fun PosterMonogram(name: String, compact: Boolean = false) {
    val initial = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(SurfaceCardElevated, SurfaceSunken))
            ),
        // Landscape cards carry a centred play button, so the letter moves to the
        // leading edge rather than sitting directly behind it.
        contentAlignment = if (compact) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            initial,
            style = MaterialTheme.typography.displayLarge,
            fontSize = if (compact) 40.sp else 72.sp,
            color = Color.White.copy(alpha = 0.09f),
            modifier = if (compact) Modifier.padding(start = Spacing.md) else Modifier
        )
    }
}

private fun shelfSubtitle(show: ShowItem): String = when {
    show.totalItemCount == 0 -> "No videos yet"
    show.watchedCount == show.totalItemCount -> "Watched"
    show.watchedCount > 0 -> "${show.watchedCount} of ${show.totalItemCount} watched"
    show.totalItemCount == 1 -> "1 item"
    else -> "${show.totalItemCount} items"
}

@Composable
private fun EmptyLibrary(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // A plain oversized glyph, not an icon inside a glowing disc. The disc
        // added a second shape to look at and said nothing the icon didn't.
        Icon(
            Icons.Outlined.FolderOpen,
            contentDescription = null,
            tint = TextQuaternary,
            modifier = Modifier.size(56.dp)
        )
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
                .premiumPressable(scaleDown = 0.97f, onClick = onAdd)
                .clip(RoundedCornerShape(Radius.pill))
                .background(AccentPrimary)
                .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                "Add Show",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
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
                .clip(RoundedCornerShape(Radius.xl))
                .background(SurfaceSheet)
                .padding(Spacing.lg)
        ) {
            Text(
                "New Show or Movie",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
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
