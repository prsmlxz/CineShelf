package com.cineshelf.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cineshelf.app.data.MediaInfo
import com.cineshelf.app.ui.theme.BadgeLabel
import com.cineshelf.app.ui.theme.HairlineMid
import com.cineshelf.app.ui.theme.Radius
import com.cineshelf.app.ui.theme.Spacing
import com.cineshelf.app.ui.theme.TextPrimary
import com.cineshelf.app.ui.theme.TextSecondary
import com.cineshelf.app.ui.theme.TextTertiary

// ---------------------------------------------------------------------------
// Technical badges.
//
// These sit in one row under a title and answer "what am I about to play" —
// resolution, codec, HDR, surround. Only two of them are ever accented (HDR and
// Dolby), because those are the two a person actually scans for; the rest are
// outlined so the row reads as information rather than as a set of buttons.
// ---------------------------------------------------------------------------

/** Outlined badge — the default weight. Neutral, quiet, informational. */
@Composable
fun MetaBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(Radius.xs))
            .background(Color.White.copy(alpha = 0.055f))
            .padding(horizontal = Spacing.xs, vertical = 5.dp)
    ) {
        Text(text, style = BadgeLabel, color = TextSecondary)
    }
}

/**
 * Emphasised badge, reserved for HDR and Dolby.
 *
 * Monochrome, like everything else in the row — it separates itself by
 * luminance, not by hue. Accent colour is reserved for selection, progress and
 * the primary action; spending it on a capability label makes "HDR" compete
 * with the Play button directly beneath it.
 */
@Composable
fun FeatureBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(Radius.xs))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = Spacing.xs, vertical = 5.dp)
    ) {
        Text(text, style = BadgeLabel, color = TextPrimary)
    }
}

/**
 * The full technical row for a file, wrapping onto a second line on narrow
 * screens rather than clipping the tail. Renders nothing at all when the
 * container told us nothing — an empty row of placeholder chips is worse than
 * no row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaBadgeRow(
    info: MediaInfo?,
    modifier: Modifier = Modifier,
    runtimeLabel: String? = null
) {
    if (info == null && runtimeLabel == null) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
    ) {
        runtimeLabel?.let { MetaBadge(it) }
        info?.resolutionLabel?.let { MetaBadge(it) }
        info?.videoCodec?.let { MetaBadge(it) }
        info?.hdrFormat?.let { FeatureBadge(it) }
        if (info?.hasDolbyAudio == true) FeatureBadge("Dolby")
        info?.primaryAudio?.channelLabel?.let { MetaBadge(it) }
    }
}

/**
 * One line of "icon · label · value" used for the spec list under a title.
 * Deliberately not a SettingsRow: nothing here is tappable, so it carries no
 * row background or press affordance.
 */
@Composable
fun SpecLine(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary
        )
        Spacer(Modifier.width(Spacing.xs))
        Box(
            Modifier
                .size(width = 1.dp, height = 10.dp)
                .background(HairlineMid)
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
