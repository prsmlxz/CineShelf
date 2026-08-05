package com.cineshelf.app.ui.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cineshelf.app.ui.theme.*

/**
 * Permission gate.
 *
 * One UI onboarding screens are quiet: a plain glyph, a clear sentence, and a
 * single filled action pinned near the bottom of the reading column. The
 * previous version led with a pulsing purple halo behind a glass disc, which
 * made a routine permission request look like an error state.
 */
@Composable
fun PermissionScreen() {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.xxl)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = TextQuaternary,
                modifier = Modifier.size(60.dp)
            )

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
                    .premiumPressable(scaleDown = 0.97f) {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(AccentPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Grant Access",
                    style = MaterialTheme.typography.labelLarge,
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
