package com.cineshelf.app.ui.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cineshelf.app.ui.theme.*

@Composable
fun PermissionScreen() {
    val context = LocalContext.current

    // A slow breath on the glow: the only moving thing on an otherwise static
    // screen, enough to make it read as alive rather than as an error page.
    val transition = rememberInfiniteTransition(label = "permission-breathe")
    val glow by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = Motion.emphasized),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 34.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CINESHELF", style = SectionEyebrow, color = AccentGlow)

            Spacer(Modifier.height(Spacing.xl))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .auroraGlow(AccentPrimary, radius = 34.dp, glowAlpha = glow)
                    .glassPanel(shape = CircleShape, fill = GlassFillLight, stroke = GlassStrokeBright),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = AccentGlow,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(Spacing.xl))

            Text(
                "Access your files",
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(Spacing.sm))

            Text(
                "CineShelf plays videos straight from your device storage. Grant file access so it can " +
                    "create show folders and find what you download into them.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(Spacing.xxl))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .auroraGlow(AccentPrimary, radius = 20.dp, glowAlpha = 0.45f, cornerRadius = 27.dp)
                    .premiumPressable(scaleDown = 0.97f) {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                    .glassPanel(
                        shape = RoundedCornerShape(Radius.pill),
                        fill = AccentPrimary,
                        stroke = GlassStrokeBright
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Grant Access",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(Spacing.md))

            Text(
                "Nothing leaves your device.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }
    }
}
