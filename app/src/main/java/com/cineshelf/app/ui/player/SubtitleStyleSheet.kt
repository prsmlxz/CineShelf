package com.cineshelf.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineshelf.app.data.SubtitleEdgeStyle
import com.cineshelf.app.data.SubtitleStyle
import com.cineshelf.app.ui.theme.*

private data class SizePreset(val label: String, val scale: Float)
private val sizePresets = listOf(
    SizePreset("S", 0.8f),
    SizePreset("M", 1.0f),
    SizePreset("L", 1.25f),
    SizePreset("XL", 1.5f)
)

private data class EdgePreset(val label: String, val style: SubtitleEdgeStyle)
private val edgePresets = listOf(
    EdgePreset("Outline", SubtitleEdgeStyle.OUTLINE),
    EdgePreset("Shadow", SubtitleEdgeStyle.DROP_SHADOW),
    EdgePreset("Box", SubtitleEdgeStyle.BACKGROUND_BOX),
    EdgePreset("None", SubtitleEdgeStyle.NONE)
)

/**
 * Size + outline settings for subtitles, with a live preview line so the
 * effect is obvious before it's applied to the actual video underneath.
 */
@Composable
fun BoxScope.SubtitleStyleSheet(
    style: SubtitleStyle,
    onStyleChange: (SubtitleStyle) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .premiumPressableNoScale(onClick = onDismiss)
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(Spacing.md)
            .glassPanel(shape = RoundedCornerShape(Radius.xl))
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
    ) {
        Text(
            "Subtitle Style",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(Spacing.md))

        // Live preview, styled to approximate the real caption rendering.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(Radius.md))
                .padding(vertical = Spacing.lg),
            contentAlignment = Alignment.Center
        ) {
            val previewModifier = if (style.edgeStyle == SubtitleEdgeStyle.BACKGROUND_BOX) {
                Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(Radius.sm))
                    .padding(horizontal = Spacing.xs, vertical = 2.dp)
            } else {
                Modifier
            }
            val previewStyle = if (style.edgeStyle == SubtitleEdgeStyle.OUTLINE || style.edgeStyle == SubtitleEdgeStyle.DROP_SHADOW) {
                MaterialTheme.typography.bodyLarge.copy(
                    shadow = Shadow(color = Color.Black, blurRadius = 8f, offset = Offset(0f, 2f))
                )
            } else {
                MaterialTheme.typography.bodyLarge
            }
            Text(
                "The quick brown fox",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = (15 * style.sizeScale).sp,
                style = previewStyle,
                modifier = previewModifier
            )
        }

        Spacer(Modifier.height(Spacing.lg))
        Text("Size", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        Spacer(Modifier.height(Spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            sizePresets.forEach { preset ->
                StylePresetChip(
                    label = preset.label,
                    selected = preset.scale == style.sizeScale,
                    onClick = { onStyleChange(style.copy(sizeScale = preset.scale)) }
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Text("Outline", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        Spacer(Modifier.height(Spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            edgePresets.forEach { preset ->
                StylePresetChip(
                    label = preset.label,
                    selected = preset.style == style.edgeStyle,
                    onClick = { onStyleChange(style.copy(edgeStyle = preset.style)) }
                )
            }
        }
    }
}

@Composable
private fun StylePresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .premiumPressable(scaleDown = 0.94f, onClick = onClick)
            .background(
                if (selected) AccentPrimary else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(Radius.pill)
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Text(
            label,
            color = if (selected) Color.White else TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
