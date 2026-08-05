package com.cineshelf.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cineshelf.app.data.VideoItem
import com.cineshelf.app.ui.theme.*
import java.io.File

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MediaTile(
    item: VideoItem,
    onClick: () -> Unit,
    onToggleWatched: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var overlayVisible by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = Motion.standard(),
        label = "tile-scale"
    )
    // Watched items recede rather than disappear, so a finished season still
    // reads as a season instead of a wall of identical thumbnails.
    val contentAlpha by animateFloatAsState(
        targetValue = if (item.watched && !overlayVisible) 0.55f else 1f,
        animationSpec = Motion.gentle(),
        label = "tile-alpha"
    )

    Column(modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(Radius.lg))
                .background(SurfaceCard)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { if (overlayVisible) overlayVisible = false else onClick() },
                    onLongClick = { overlayVisible = true }
                )
        ) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                if (item.thumbnailPath != null) {
                    AsyncImage(
                        model = File(item.thumbnailPath),
                        contentDescription = item.displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Episode number set large and very dim, so a thumbnail-less
                    // rail still has something to distinguish one tile from the
                    // next instead of reading as identical empty rectangles.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(SurfaceCardElevated, SurfaceSunken))),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            item.episodeNumber?.toString()
                                ?: item.displayTitle.trim().take(1).uppercase(),
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.padding(start = Spacing.md)
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, ScrimStrong), startY = 60f)
                        )
                )
            }

            // Lit rim, the same hairline used on library posters.
            Box(
                Modifier
                    .fillMaxSize()
                    .glassPanel(
                        shape = RoundedCornerShape(Radius.lg),
                        fill = Color.Transparent,
                        stroke = HairlineMid
                    )
            )

            if (item.effectiveDurationMs > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .glassPanel(
                            shape = RoundedCornerShape(Radius.xs),
                            fill = Color.Black.copy(alpha = 0.55f),
                            stroke = HairlineLight
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        formatDuration(item.effectiveDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.88f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (item.watched) {
                // Glass and white, not green. A saturated status dot on every
                // finished tile was the loudest colour on the page, competing
                // with the accent for attention it doesn't deserve.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(21.dp)
                        .glassPanel(
                            shape = CircleShape,
                            fill = Color.Black.copy(alpha = 0.55f),
                            stroke = GlassStrokeBright
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = "Watched",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else if (item.isInProgress) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(Color.White.copy(alpha = 0.20f))
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

            if (!overlayVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(38.dp)
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
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = overlayVisible,
                enter = fadeIn(Motion.fade(160)) + scaleIn(Motion.standard(), initialScale = 0.92f),
                exit = fadeOut(Motion.fade(130)) + scaleOut(Motion.snap(), targetScale = 0.92f)
            ) {
                Box(Modifier.fillMaxSize().background(ScrimStrong)) {
                    OverlayIconButton(
                        icon = Icons.Outlined.CheckCircle,
                        tint = if (item.watched) AccentSuccess else Color.White,
                        contentDescription = "Toggle watched",
                        modifier = Modifier.align(Alignment.TopEnd).padding(7.dp),
                        onClick = {
                            onToggleWatched()
                            overlayVisible = false
                        }
                    )
                    OverlayIconButton(
                        icon = Icons.Outlined.DeleteOutline,
                        tint = AccentDanger,
                        contentDescription = "Delete",
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp),
                        onClick = {
                            onDelete()
                            overlayVisible = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.xs))

        Text(
            item.displayTitle,
            style = MaterialTheme.typography.titleSmall,
            color = if (item.watched) TextSecondary else TextPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // The filename used to sit here, which is noise — it's the thing the
        // title was derived from. Show what the file *is* instead.
        subtitleLine(item)?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** "S01E04 · 1080p · HEVC", dropping whatever isn't known. */
private fun subtitleLine(item: VideoItem): String? {
    val parts = mutableListOf<String>()
    if (item.seasonNumber != null && item.episodeNumber != null) {
        parts += "S%02dE%02d".format(item.seasonNumber, item.episodeNumber)
    }
    item.mediaInfo?.resolutionLabel?.let { parts += it }
    item.mediaInfo?.videoCodec?.let { parts += it }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun OverlayIconButton(
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .premiumPressable(scaleDown = 0.86f, onClick = onClick)
            .glassPanel(shape = CircleShape, fill = GlassFillStrong, stroke = GlassStrokeBright),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(17.dp))
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
