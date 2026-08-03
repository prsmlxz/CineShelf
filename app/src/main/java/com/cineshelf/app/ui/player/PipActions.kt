package com.cineshelf.app.ui.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.media3.common.VideoSize

/**
 * Picture-in-Picture transport controls.
 *
 * The original PiP window had no buttons at all, because
 * `PictureInPictureParams.Builder().build()` supplies none — the system does
 * not infer them from the player. They have to be declared explicitly as
 * [RemoteAction]s backed by broadcast PendingIntents, and re-declared every
 * time the play/pause state flips so the icon matches reality.
 */
object PipActions {

    const val ACTION_CONTROL = "com.cineshelf.app.PIP_CONTROL"
    const val EXTRA_CONTROL = "control"

    const val CONTROL_PLAY_PAUSE = 1
    const val CONTROL_REWIND = 2
    const val CONTROL_FORWARD = 3

    private fun remoteAction(
        context: Context,
        iconRes: Int,
        title: String,
        control: Int
    ): RemoteAction {
        val intent = Intent(ACTION_CONTROL)
            .setPackage(context.packageName)
            .putExtra(EXTRA_CONTROL, control)
        val pending = PendingIntent.getBroadcast(
            context,
            control,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteAction(Icon.createWithResource(context, iconRes), title, title, pending)
    }

    fun buildParams(
        context: Context,
        isPlaying: Boolean,
        videoSize: VideoSize?
    ): PictureInPictureParams {
        val ratio = aspectRatio(videoSize)
        val actions = listOf(
            remoteAction(
                context,
                android.R.drawable.ic_media_rew,
                "Rewind 10 seconds",
                CONTROL_REWIND
            ),
            remoteAction(
                context,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                CONTROL_PLAY_PAUSE
            ),
            remoteAction(
                context,
                android.R.drawable.ic_media_ff,
                "Forward 10 seconds",
                CONTROL_FORWARD
            )
        )
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(ratio)
            .setActions(actions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    /**
     * PiP rejects extreme ratios, so clamp into the accepted range rather than
     * letting the system throw on an ultra-wide source.
     */
    private fun aspectRatio(videoSize: VideoSize?): Rational {
        if (videoSize == null || videoSize.width <= 0 || videoSize.height <= 0) return Rational(16, 9)
        val raw = videoSize.width.toFloat() / videoSize.height.toFloat()
        val clamped = raw.coerceIn(1f / 2.39f, 2.39f)
        return if (clamped == raw) {
            Rational(videoSize.width, videoSize.height)
        } else {
            Rational((clamped * 1000).toInt(), 1000)
        }
    }

    /** Registers a receiver for the PiP action broadcasts, returning an unregister lambda. */
    fun registerReceiver(context: Context, onControl: (Int) -> Unit): () -> Unit {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != ACTION_CONTROL) return
                onControl(intent.getIntExtra(EXTRA_CONTROL, 0))
            }
        }
        val filter = IntentFilter(ACTION_CONTROL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        return { runCatching { context.unregisterReceiver(receiver) } }
    }
}
