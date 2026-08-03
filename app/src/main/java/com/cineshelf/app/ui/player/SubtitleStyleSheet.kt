package com.cineshelf.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cineshelf.app.data.SubtitleEdge
import com.cineshelf.app.data.SubtitlePrefs
import com.cineshelf.app.data.SubtitleTextColor
import com.cineshelf.app.ui.theme.*
import kotlin.math.roundToInt

/**
 * The subtitle appearance sheet. Every control writes through immediately so
 * the change is visible on the video behind the sheet as it is made — no
 * apply/confirm step.
 */
@Composable
fun BoxScope.SubtitleStyleSheet(
    prefs: SubtitlePrefs,
    subtitleOffsetMs: Long,
    onPrefsChange: (SubtitlePrefs) -> Unit,
    onOffsetChange: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrimMedium)
            .premiumPressableNoScale(onClick = onDismiss)
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(Spacing.sm)
            .glassPanel(
                shape = RoundedCornerShape(Radius.xxl),
                fill = SurfaceCardElevated.copy(alpha = 0.96f),
                stroke = GlassStrokeBright
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = Spacing.sm)
                .size(width = 36.dp, height = 4.dp)
                .background(HairlineStrong, RoundedCornerShape(Radius.pill))
        )
        Text(
            "Subtitle Style",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(Spacing.md))
        SheetLabel("Size", "${(prefs.scale * 100).roundToInt()}%")
        StepRow(
            options = subtitleScaleSteps.map { it to "${(it * 100).roundToInt()}%" },
            selected = prefs.scale,
            onSelect = { onPrefsChange(prefs.copy(scale = it)) }
        )

        Spacer(Modifier.height(Spacing.md))
        SheetLabel("Edge", prefs.edge.label)
        StepRow(
            options = SubtitleEdge.entries.map { it to it.label },
            selected = prefs.edge,
            onSelect = { onPrefsChange(prefs.copy(edge = it)) }
        )

        Spacer(Modifier.height(Spacing.md))
        SheetLabel("Color", prefs.textColor.label)
        StepRow(
            options = SubtitleTextColor.entries.map { it to it.label },
            selected = prefs.textColor,
            onSelect = { onPrefsChange(prefs.copy(textColor = it)) }
        )

        Spacer(Modifier.height(Spacing.md))
        SheetLabel("Background", if (prefs.backgroundOpacity <= 0f) "None" else "${(prefs.backgroundOpacity * 100).roundToInt()}%")
        StepRow(
            options = subtitleBackgroundSteps.map { it to if (it <= 0f) "None" else "${(it * 100).roundToInt()}%" },
            selected = prefs.backgroundOpacity,
            onSelect = { onPrefsChange(prefs.copy(backgroundOpacity = it)) }
        )

        Spacer(Modifier.height(Spacing.md))
        SheetLabel("Sync", formatOffset(subtitleOffsetMs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OffsetChip("-1s") { onOffsetChange(subtitleOffsetMs - 1000L) }
            OffsetChip("-0.1s") { onOffsetChange(subtitleOffsetMs - 100L) }
            OffsetChip("Reset") { onOffsetChange(0L) }
            OffsetChip("+0.1s") { onOffsetChange(subtitleOffsetMs + 100L) }
            OffsetChip("+1s") { onOffsetChange(subtitleOffsetMs + 1000L) }
        }

        Spacer(Modifier.height(Spacing.md))
        SubtitlePreviewStrip(prefs)
        Spacer(Modifier.height(Spacing.sm))
    }
}

@Composable
private fun SheetLabel(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.labelMedium, color = AccentGlow)
    }
}

@Composable
private fun <T> StepRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
    ) {
        options.forEach { (value, label) ->
            val isActive = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(if (isActive) AccentSoft else GlassFill)
                    .then(
                        if (isActive) Modifier.auroraGlow(
                            color = AccentPrimary,
                            radius = 8.dp,
                            glowAlpha = 0.30f,
                            cornerRadius = Radius.sm
                        ) else Modifier
                    )
                    .premiumPressableNoScale(onClick = { onSelect(value) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) TextPrimary else TextTertiary,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RowScope.OffsetChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .clip(RoundedCornerShape(Radius.sm))
            .background(GlassFill)
            .premiumPressableNoScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

/** Live sample of the chosen style, so the choice can be judged without dismissing. */
@Composable
private fun SubtitlePreviewStrip(prefs: SubtitlePrefs) {
    val textColor = Color(prefs.textColor.argb)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(Radius.md))
            .background(Brush.horizontalGradient(listOf(Color.Black, SurfaceRaised, Color.Black))),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.xs))
                .background(Color.Black.copy(alpha = prefs.backgroundOpacity.coerceIn(0f, 1f)))
                .padding(horizontal = Spacing.xs, vertical = 2.dp)
        ) {
            Text(
                "Preview subtitle text",
                color = textColor,
                fontWeight = if (prefs.edge == SubtitleEdge.NONE) FontWeight.Normal else FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * prefs.scale
                )
            )
        }
    }
}

private fun formatOffset(ms: Long): String {
    if (ms == 0L) return "In sync"
    val sign = if (ms > 0) "+" else "-"
    val abs = kotlin.math.abs(ms)
    return "$sign${abs / 1000}.${(abs % 1000) / 100}s"
}

private val subtitleScaleSteps = listOf(0.7f, 0.85f, 1.0f, 1.2f, 1.45f, 1.75f)
private val subtitleBackgroundSteps = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
