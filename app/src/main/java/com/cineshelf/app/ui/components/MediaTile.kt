package com.cineshelf.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(Radius.md))
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
                        .background(Brush.linearGradient(listOf(SurfaceCard, SurfaceCardElevated)))
                )
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colors = listOf(Color.Transparent, ScrimStrong), startY = 50f))
            )

            if (item.watched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(19.dp)
                        .glassPanel(shape = CircleShape, fill = AccentSuccess.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = "Watched", tint = Color.White, modifier = Modifier.size(11.dp))
                }
            } else if (item.isInProgress) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                )
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(item.progressFraction)
                        .height(2.dp)
                        .background(AccentPrimary)
                )
            }

            if (!overlayVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(34.dp)
                        .glassPanel(shape = CircleShape, fill = ScrimMedium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = overlayVisible,
                enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.92f),
                exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.92f)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(ScrimStrong)
                ) {
                    OverlayIconButton(
                        icon = Icons.Outlined.CheckCircle,
                        tint = if (item.watched) AccentSuccess else Color.White,
                        contentDescription = "Toggle watched",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(7.dp),
                        onClick = {
                            onToggleWatched()
                            overlayVisible = false
                        }
                    )
                    OverlayIconButton(
                        icon = Icons.Outlined.DeleteOutline,
                        tint = AccentDanger,
                        contentDescription = "Delete",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 9.dp),
                        onClick = {
                            onDelete()
                            overlayVisible = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.xxs))

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
            .size(32.dp)
            .premiumPressable(scaleDown = 0.88f, onClick = onClick)
            .glassPanel(shape = CircleShape, fill = GlassFillLight),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(16.dp))
    }
}
