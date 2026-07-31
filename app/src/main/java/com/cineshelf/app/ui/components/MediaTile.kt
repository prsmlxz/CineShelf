package com.cineshelf.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceCard)
                .combinedClickable(
                    onClick = {
                        if (overlayVisible) overlayVisible = false else onClick()
                    },
                    onLongClick = { overlayVisible = true }
                )
        ) {
            if (item.thumbnailPath != null) {
                AsyncImage(
                    model = File(item.thumbnailPath),
                    contentDescription = item.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(SurfaceCard, SurfaceCardElevated))
                        )
                )
            }

            // Bottom gradient scrim for legibility
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                            startY = 40f
                        )
                    )
            )

            // Progress bar for partially watched items
            if (!item.watched && item.durationMs > 0 && item.positionMs > 0) {
                val progress = (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                )
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(AccentPrimary)
                )
            }

            if (item.watched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .background(AccentSuccess, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Watched",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            if (!overlayVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(38.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = overlayVisible,
                enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.9f),
                exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.9f)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    OverlayIconButton(
                        icon = Icons.Default.CheckCircle,
                        tint = if (item.watched) AccentSuccess else Color.White,
                        contentDescription = "Toggle watched",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        onClick = {
                            onToggleWatched()
                            overlayVisible = false
                        }
                    )
                    OverlayIconButton(
                        icon = Icons.Default.Delete,
                        tint = AccentDanger,
                        contentDescription = "Delete",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        onClick = {
                            onDelete()
                            overlayVisible = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            item.displayTitle,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            item.file.name,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OverlayIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .background(Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
    }
}
