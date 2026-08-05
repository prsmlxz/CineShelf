package com.cineshelf.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cineshelf.app.data.SubtitleEdge
import com.cineshelf.app.data.SubtitlePrefs
import com.cineshelf.app.data.SubtitleTextColor
import com.cineshelf.app.ui.components.MiniActionChip
import com.cineshelf.app.ui.components.RowDivider
import com.cineshelf.app.ui.components.SegmentedControl
import com.cineshelf.app.ui.components.SettingsGroup
import com.cineshelf.app.ui.components.SettingsRow
import com.cineshelf.app.ui.components.SliderRow
import com.cineshelf.app.ui.theme.AccentBright
import com.cineshelf.app.ui.theme.AccentPrimary
import com.cineshelf.app.ui.theme.GlassStrokeBright
import com.cineshelf.app.ui.theme.Radius
import com.cineshelf.app.ui.theme.ScrimMedium
import com.cineshelf.app.ui.theme.SectionEyebrow
import com.cineshelf.app.ui.theme.Spacing
import com.cineshelf.app.ui.theme.SurfaceCardElevated
import com.cineshelf.app.ui.theme.SurfaceSheet
import com.cineshelf.app.ui.theme.SurfaceRaised
import com.cineshelf.app.ui.theme.TextPrimary
import com.cineshelf.app.ui.theme.TextSecondary
import com.cineshelf.app.ui.theme.glassPanelOverVideo
import com.cineshelf.app.ui.theme.premiumPressableNoScale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The subtitle sheet: which track, and how it looks.
 *
 * Track selection and appearance used to be two separate dock icons opening two
 * separate popups, which meant "subtitles" was two destinations depending on
 * which aspect of them you wanted to change. They are one thing.
 *
 * The appearance controls are built from three affordances rather than five
 * identical stepper rows: sliders for the continuous values, segmented controls
 * for the small named sets, and stepper chips only for sync — the one value
 * unbounded in both directions.
 *
 * The live preview sits above the controls rather than below them so it stays
 * visible while a slider is being dragged.
 */
@Composable
fun BoxScope.SubtitleStyleSheet(
    prefs: SubtitlePrefs,
    subtitleOffsetMs: Long,
    tracks: List<PopupOption>,
    selectedTrackKey: String?,
    onSelectTrack: (String) -> Unit,
    onPrefsChange: (SubtitlePrefs) -> Unit,
    onOffsetChange: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val subtitlesOff = selectedTrackKey == OFF_TRACK_KEY
    SheetScaffold(
        icon = Icons.Outlined.ClosedCaption,
        title = "Subtitles",
        subtitle = if (tracks.size <= 1) "No subtitle tracks in this file"
        else "${tracks.size - 1} tracks available",
        onDismiss = onDismiss
    ) {
        SettingsGroup(title = "Track") {
            tracks.forEachIndexed { index, option ->
                if (index > 0) RowDivider()
                SettingsRow(
                    label = option.label,
                    value = option.trailing,
                    onClick = { onSelectTrack(option.key) },
                    trailing = if (option.key == selectedTrackKey) {
                        { SelectedCheck() }
                    } else null
                )
            }
        }

        // Everything below styles the captions, which is meaningless with none
        // showing. Dimmed rather than hidden so the sheet doesn't resize when
        // the track changes.
        Column(Modifier.alpha(if (subtitlesOff) 0.4f else 1f)) {
            Spacer(Modifier.height(Spacing.md))
            SubtitlePreviewStrip(prefs)

            Spacer(Modifier.height(Spacing.md))
            SettingsGroup(title = "Text") {
                SliderRow(
                    label = "Size",
                    value = scaleToFraction(prefs.scale),
                    valueLabel = "${(prefs.scale * 100).roundToInt()}%",
                    steps = subtitleScaleSteps.size,
                    onValueChange = { onPrefsChange(prefs.copy(scale = fractionToScale(it))) }
                )
                RowDivider()
                LabeledSegments(
                    label = "Colour",
                    options = SubtitleTextColor.values().toList(),
                    selected = prefs.textColor,
                    labelOf = { it.label },
                    onSelect = { onPrefsChange(prefs.copy(textColor = it)) }
                )
            }

            Spacer(Modifier.height(Spacing.md))
            SettingsGroup(
                title = "Legibility",
                footnote = "Outline reads best over bright scenes."
            ) {
                LabeledSegments(
                    label = "Edge",
                    options = SubtitleEdge.values().toList(),
                    selected = prefs.edge,
                    labelOf = { shortEdgeLabel(it) },
                    onSelect = { onPrefsChange(prefs.copy(edge = it)) }
                )
                RowDivider()
                SliderRow(
                    label = "Backdrop",
                    value = prefs.backgroundOpacity,
                    valueLabel = if (prefs.backgroundOpacity <= 0.01f) "Off"
                    else "${(prefs.backgroundOpacity * 100).roundToInt()}%",
                    steps = 5,
                    onValueChange = { onPrefsChange(prefs.copy(backgroundOpacity = it)) }
                )
            }

            Spacer(Modifier.height(Spacing.md))
            SettingsGroup(title = "Placement & Sync") {
                SliderRow(
                    label = "Height",
                    value = paddingToFraction(prefs.bottomPaddingScale),
                    valueLabel = "${(prefs.bottomPaddingScale * 100).roundToInt()}%",
                    steps = 7,
                    onValueChange = {
                        onPrefsChange(prefs.copy(bottomPaddingScale = fractionToPadding(it)))
                    }
                )
                RowDivider()
                Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Delay",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatOffset(subtitleOffsetMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (subtitleOffsetMs == 0L) TextSecondary else AccentBright
                        )
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
                    ) {
                        MiniActionChip("−1s", { onOffsetChange(subtitleOffsetMs - 1000L) }, Modifier.weight(1f))
                        MiniActionChip("−.1", { onOffsetChange(subtitleOffsetMs - 100L) }, Modifier.weight(1f))
                        MiniActionChip(
                            "Reset",
                            { onOffsetChange(0L) },
                            Modifier.weight(1f),
                            active = subtitleOffsetMs != 0L
                        )
                        MiniActionChip("+.1", { onOffsetChange(subtitleOffsetMs + 100L) }, Modifier.weight(1f))
                        MiniActionChip("+1s", { onOffsetChange(subtitleOffsetMs + 1000L) }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** The key used for the "no subtitles" entry in the track list. */
const val OFF_TRACK_KEY = "off"

@Composable
private fun SelectedCheck() {
    Icon(
        Icons.Outlined.Check,
        contentDescription = null,
        tint = AccentBright,
        modifier = Modifier.size(20.dp)
    )
}

/**
 * Playback behaviour sheet: speed, seek distance, framing, session.
 *
 * These were four separate dock icons opening four near-identical popups.
 * Folding them into one grouped sheet is what let the dock drop to four items.
 */
@Composable
fun BoxScope.PlaybackSettingsSheet(
    speed: Float,
    skipSeconds: Int,
    aspectMode: AspectMode,
    orientationMode: OrientationMode,
    sleepTimerLabel: String,
    hasNextEpisode: Boolean,
    onSpeedChange: (Float) -> Unit,
    onSkipSecondsChange: (Int) -> Unit,
    onAspectChange: (AspectMode) -> Unit,
    onOrientationChange: (OrientationMode) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onNextEpisode: () -> Unit,
    onDismiss: () -> Unit
) {
    SheetScaffold(
        icon = Icons.Outlined.Tune,
        title = "Playback",
        subtitle = "Speed, gestures and framing",
        onDismiss = onDismiss
    ) {
        SettingsGroup(title = "Speed") {
            ValueHeaderSegments(
                label = "Playback rate",
                valueLabel = if (speed == 1f) "Normal" else "${trimSpeed(speed)}×",
                valueIsDefault = speed == 1f,
                options = speedOptions,
                selected = speedOptions.minByOrNull { abs(it - speed) } ?: 1f,
                labelOf = { trimSpeed(it) },
                onSelect = onSpeedChange
            )
        }

        Spacer(Modifier.height(Spacing.md))
        SettingsGroup(
            title = "Gestures",
            footnote = "Double-tap the left or right edge of the video to skip."
        ) {
            ValueHeaderSegments(
                label = "Double-tap skip",
                valueLabel = "${skipSeconds}s",
                valueIsDefault = false,
                options = skipDurationOptions,
                selected = skipSeconds,
                labelOf = { "${it}s" },
                onSelect = onSkipSecondsChange
            )
        }

        Spacer(Modifier.height(Spacing.md))
        SettingsGroup(title = "Display") {
            LabeledSegments(
                label = "Aspect ratio",
                options = AspectMode.values().toList(),
                selected = aspectMode,
                labelOf = { it.label },
                onSelect = onAspectChange
            )
            RowDivider()
            LabeledSegments(
                label = "Orientation",
                options = OrientationMode.values().toList(),
                selected = orientationMode,
                labelOf = { it.label },
                onSelect = onOrientationChange
            )
        }

        Spacer(Modifier.height(Spacing.md))
        SettingsGroup(title = "Session") {
            SettingsRow(
                label = "Sleep timer",
                value = sleepTimerLabel,
                onClick = onOpenSleepTimer
            )
            if (hasNextEpisode) {
                RowDivider()
                SettingsRow(label = "Play next episode", onClick = onNextEpisode)
            }
        }
    }
}

/**
 * Shared chrome for the two tall settings sheets: scrim, opaque surface rounded
 * at the top only, grabber, header, and a scrolling body capped so the sheet
 * never covers the whole video.
 */
@Composable
private fun BoxScope.SheetScaffold(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    body: @Composable ColumnScope.() -> Unit
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
            .clip(RoundedCornerShape(topStart = Radius.xxl, topEnd = Radius.xxl))
            .background(SurfaceSheet)
            .navigationBarsPadding()
            .padding(bottom = Spacing.sm)
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally)) { SheetGrabber() }
        SheetHeader(icon = icon, title = title, subtitle = subtitle)
        // Capped so the sheet always leaves a strip of video visible above it —
        // a settings panel that fills the screen stops reading as a sheet.
        Column(
            modifier = Modifier
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            content = body
        )
    }
}

/** A settings row whose control is a full-width segmented picker under the label. */
@Composable
private fun <T> LabeledSegments(
    label: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        Spacer(Modifier.height(Spacing.xs))
        SegmentedControl(options = options, selected = selected, labelOf = labelOf, onSelect = onSelect)
    }
}

/** Same, but with the current value called out to the right of the label. */
@Composable
private fun <T> ValueHeaderSegments(
    label: String,
    valueLabel: String,
    valueIsDefault: Boolean,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (valueIsDefault) TextSecondary else AccentBright
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        SegmentedControl(options = options, selected = selected, labelOf = labelOf, onSelect = onSelect)
    }
}

/**
 * Live sample of the chosen style. The ramp stands in for video so a bright and a
 * dark background are both visible at once — a style that reads well on black
 * often disappears over a bright scene.
 *
 * One continuous dark-to-light sweep rather than the five-stop symmetric ramp it
 * had before: that version put a hard bright band down the middle of the strip,
 * which read as a rendering artefact instead of as a stand-in for a scene.
 */
@Composable
private fun SubtitlePreviewStrip(prefs: SubtitlePrefs) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .clip(RoundedCornerShape(Radius.md))
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Black, SurfaceRaised, Color(0xFF7A7A85))
                )
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            "Preview",
            style = SectionEyebrow,
            color = Color.White.copy(alpha = 0.30f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = Spacing.sm, top = Spacing.xs)
        )
        Box(
            modifier = Modifier
                .padding(bottom = (10f * prefs.bottomPaddingScale).dp.coerceAtLeast(4.dp))
                .clip(RoundedCornerShape(Radius.xs))
                .background(Color.Black.copy(alpha = prefs.backgroundOpacity.coerceIn(0f, 1f)))
                .padding(horizontal = Spacing.xs, vertical = 2.dp)
        ) {
            Text(
                "The quick brown fox",
                color = Color(prefs.textColor.argb),
                fontWeight = if (prefs.edge == SubtitleEdge.NONE) FontWeight.Normal else FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * prefs.scale
                )
            )
        }
    }
}

private fun shortEdgeLabel(edge: SubtitleEdge): String = when (edge) {
    SubtitleEdge.NONE -> "None"
    SubtitleEdge.OUTLINE -> "Outline"
    SubtitleEdge.DROP_SHADOW -> "Shadow"
    SubtitleEdge.RAISED -> "Raised"
    SubtitleEdge.DEPRESSED -> "Sunken"
}

private fun trimSpeed(value: Float): String =
    if (value % 1f == 0f) "${value.toInt()}" else "$value"

private fun formatOffset(ms: Long): String {
    if (ms == 0L) return "In sync"
    val sign = if (ms > 0) "+" else "−"
    val magnitude = abs(ms)
    return "$sign${magnitude / 1000}.${(magnitude % 1000) / 100}s"
}

// Size is exposed as a slider but stored as a multiplier, so the two map through
// a fixed step list — arbitrary fractional sizes look accidental.
private val subtitleScaleSteps = listOf(0.7f, 0.85f, 1.0f, 1.2f, 1.45f, 1.75f)

private fun scaleToFraction(scale: Float): Float {
    val index = subtitleScaleSteps.indices.minByOrNull { abs(subtitleScaleSteps[it] - scale) } ?: 2
    return index.toFloat() / (subtitleScaleSteps.size - 1)
}

private fun fractionToScale(fraction: Float): Float {
    val index = (fraction * (subtitleScaleSteps.size - 1)).roundToInt()
        .coerceIn(0, subtitleScaleSteps.lastIndex)
    return subtitleScaleSteps[index]
}

// Vertical placement runs 60%..180% of the default inset.
private const val PADDING_MIN = 0.6f
private const val PADDING_MAX = 1.8f

private fun paddingToFraction(value: Float): Float =
    ((value - PADDING_MIN) / (PADDING_MAX - PADDING_MIN)).coerceIn(0f, 1f)

private fun fractionToPadding(fraction: Float): Float =
    PADDING_MIN + (PADDING_MAX - PADDING_MIN) * fraction.coerceIn(0f, 1f)
