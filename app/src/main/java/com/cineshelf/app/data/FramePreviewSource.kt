package com.cineshelf.app.data

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File

/**
 * Supplies preview frames while the user scrubs.
 *
 * The naive approach — extract a frame per drag event — falls apart instantly:
 * MediaMetadataRetriever takes tens of milliseconds per frame, drag events
 * arrive every few milliseconds, and the requests pile into a queue that runs
 * further and further behind the finger. The result is preview images arriving
 * seconds after you've stopped moving.
 *
 * Two mechanisms fix that:
 *
 * 1. Conflation. Requests go into a CONFLATED channel, so a pending request is
 *    *replaced* by any newer one. The extractor thread always works on the most
 *    recent position the finger touched and silently drops everything stale.
 *    The queue can therefore never grow, no matter how fast the drag is.
 *
 * 2. Quantisation + cache. Positions are snapped to a coarse grid before
 *    lookup, so small finger movements reuse an already-decoded bitmap instead
 *    of triggering a fresh decode. Decoded frames live in a memory LRU, making
 *    repeat passes over the same region instant.
 *
 * Frames are decoded at a small scale, which is both faster and plenty for a
 * thumbnail-sized preview.
 */
class FramePreviewSource(
    private val videoFile: File,
    private val scope: CoroutineScope,
    private val stepMs: Long = 2_000L
) {
    private val cache = object : LruCache<Long, ImageBitmap>(48) {}
    private val requests = Channel<Long>(Channel.CONFLATED)

    @Volatile
    private var retriever: MediaMetadataRetriever? = null

    @Volatile
    private var released = false

    /** Latest successfully decoded frame, as (quantisedPositionMs, bitmap). */
    @Volatile
    var latest: Pair<Long, ImageBitmap>? = null
        private set

    private var onFrame: ((Long, ImageBitmap) -> Unit)? = null

    fun start(onFrame: (Long, ImageBitmap) -> Unit) {
        this.onFrame = onFrame
        scope.launch(Dispatchers.IO) {
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(videoFile.absolutePath)
                retriever = r
            } catch (_: Exception) {
                runCatching { r.release() }
                return@launch
            }
            for (positionMs in requests) {
                if (released) break
                val key = quantise(positionMs)
                val cached = cache.get(key)
                if (cached != null) {
                    latest = key to cached
                    onFrame(key, cached)
                } else {
                    val bitmap = decode(key)
                    if (bitmap != null) {
                        val image = bitmap.asImageBitmap()
                        cache.put(key, image)
                        latest = key to image
                        onFrame(key, image)
                    }
                }
            }
        }
    }

    /**
     * Ask for the frame nearest [positionMs]. Returns a cached frame
     * immediately when one exists, so the common case never round-trips
     * through the extractor thread at all.
     */
    fun request(positionMs: Long): ImageBitmap? {
        if (released) return null
        val key = quantise(positionMs)
        val cached = cache.get(key)
        if (cached != null) {
            latest = key to cached
            return cached
        }
        requests.trySend(positionMs)
        // Fall back to the most recent frame we have so the preview shows
        // *something* adjacent rather than flashing empty.
        return latest?.second
    }

    /**
     * Warm the cache with frames spread across the video so the first scrub
     * already has coverage instead of decoding from cold.
     */
    fun prewarm(durationMs: Long, count: Int = 12) {
        if (durationMs <= 0) return
        scope.launch(Dispatchers.IO) {
            for (i in 0 until count) {
                if (released) return@launch
                val t = durationMs * (i + 1) / (count + 1)
                val key = quantise(t)
                if (cache.get(key) == null) {
                    decode(key)?.let { cache.put(key, it.asImageBitmap()) }
                }
            }
        }
    }

    private fun quantise(positionMs: Long): Long =
        (positionMs / stepMs) * stepMs

    private fun decode(positionMs: Long): Bitmap? {
        val r = retriever ?: return null
        return try {
            // OPTION_CLOSEST_SYNC snaps to a keyframe: far cheaper than an exact
            // decode, and for a preview the difference is imperceptible.
            r.getScaledFrameAtTime(
                positionMs * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                PREVIEW_WIDTH,
                PREVIEW_HEIGHT
            )
        } catch (_: Exception) {
            null
        }
    }

    fun release() {
        released = true
        requests.close()
        runCatching { retriever?.release() }
        retriever = null
        cache.evictAll()
        latest = null
        onFrame = null
    }

    private companion object {
        const val PREVIEW_WIDTH = 320
        const val PREVIEW_HEIGHT = 180
    }
}
