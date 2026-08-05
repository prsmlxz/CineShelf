package com.cineshelf.app.data

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

/**
 * Supplies preview frames while the user scrubs.
 *
 * The naive approach — extract a frame per drag event — falls apart instantly:
 * MediaMetadataRetriever takes tens of milliseconds per frame, drag events
 * arrive every few milliseconds, and the requests pile into a queue that runs
 * further and further behind the finger. The result is preview images arriving
 * seconds after you've stopped moving.
 *
 * Mechanisms:
 *
 * 1. Conflation. Requests go into a CONFLATED channel, so a pending request is
 *    *replaced* by any newer one. The extractor thread always works on the most
 *    recent position the finger touched and silently drops everything stale.
 *
 * 2. Quantisation + cache. Positions snap to a grid before lookup, so small
 *    finger movements reuse an already-decoded bitmap. The grid widens for long
 *    files so a feature-length video is covered by roughly as many frames as the
 *    cache can actually hold.
 *
 * 3. Nearest-neighbour fallback. A miss returns the closest cached frame *by
 *    position*, and only within a bounded window. Previously this returned
 *    whatever was decoded last regardless of where it came from, which is why
 *    the preview would lock onto one frame and never move again once decoding
 *    started failing.
 *
 * 4. Retriever recovery. Some HEVC/x265 files put MediaMetadataRetriever into a
 *    state where every subsequent extract returns null. A run of failures tears
 *    the retriever down and builds a fresh one rather than silently giving up
 *    for the rest of the session.
 */
class FramePreviewSource(
    private val videoFile: File,
    private val scope: CoroutineScope
) {
    private val lock = Any()

    /** Access-ordered LRU. A plain LruCache can't be searched by nearest key. */
    private val cache = object : LinkedHashMap<Long, ImageBitmap>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, ImageBitmap>) =
            size > MAX_CACHED_FRAMES
    }

    private val requests = Channel<Long>(Channel.CONFLATED)

    @Volatile
    private var retriever: MediaMetadataRetriever? = null

    @Volatile
    private var released = false

    @Volatile
    private var stepMs: Long = DEFAULT_STEP_MS

    /** Consecutive null decodes. A run of these means the retriever is wedged. */
    @Volatile
    private var failureStreak = 0

    private var onFrame: ((Long, ImageBitmap) -> Unit)? = null

    fun start(onFrame: (Long, ImageBitmap) -> Unit) {
        this.onFrame = onFrame
        scope.launch(Dispatchers.IO) {
            if (!openRetriever()) return@launch
            for (positionMs in requests) {
                if (released) break
                val key = quantise(positionMs)
                val cached = synchronized(lock) { cache[key] }
                if (cached != null) {
                    onFrame(key, cached)
                    continue
                }
                val bitmap = decodeWithRecovery(key)
                if (bitmap != null) {
                    val image = bitmap.asImageBitmap()
                    synchronized(lock) { cache[key] = image }
                    onFrame(key, image)
                }
            }
        }
    }

    /**
     * Ask for the frame nearest [positionMs].
     *
     * Returns the exact frame when cached; otherwise the nearest cached frame
     * within [FALLBACK_WINDOW_STEPS] grid steps, so the preview shows something
     * positionally honest while the real decode runs. Returns null when nothing
     * close enough exists — the caller shows a placeholder, which is far better
     * than a confidently wrong frame from elsewhere in the film.
     */
    fun request(positionMs: Long): ImageBitmap? {
        if (released) return null
        val key = quantise(positionMs)
        synchronized(lock) { cache[key] }?.let { return it }
        requests.trySend(positionMs)
        return nearestCached(key)
    }

    /**
     * Widen the sampling grid to suit the file length, then warm the cache with
     * frames spread across the video so the first scrub already has coverage.
     *
     * Called once duration is known. Changing the grid invalidates every
     * existing key, so the cache is dropped rather than left holding entries
     * that can never be hit again.
     */
    fun prewarm(durationMs: Long, count: Int = 16) {
        if (durationMs <= 0 || released) return
        val step = (durationMs / TARGET_GRID_POINTS).coerceIn(MIN_STEP_MS, MAX_STEP_MS)
        if (step != stepMs) {
            stepMs = step
            synchronized(lock) { cache.clear() }
        }
        scope.launch(Dispatchers.IO) {
            if (!openRetriever()) return@launch
            for (i in 0 until count) {
                if (released) return@launch
                val key = quantise(durationMs * (i + 1) / (count + 1))
                if (synchronized(lock) { cache.containsKey(key) }) continue
                val bitmap = decodeWithRecovery(key) ?: continue
                val image = bitmap.asImageBitmap()
                synchronized(lock) { cache[key] = image }
            }
        }
    }

    private fun nearestCached(key: Long): ImageBitmap? {
        val window = stepMs * FALLBACK_WINDOW_STEPS
        return synchronized(lock) {
            var bestKey: Long? = null
            var bestDistance = Long.MAX_VALUE
            for (candidate in cache.keys) {
                val distance = abs(candidate - key)
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestKey = candidate
                }
            }
            if (bestKey != null && bestDistance <= window) cache[bestKey] else null
        }
    }

    private fun quantise(positionMs: Long): Long = (positionMs / stepMs) * stepMs

    /** Opens the retriever if it isn't already open. Safe to call repeatedly. */
    private fun openRetriever(): Boolean {
        if (released) return false
        if (retriever != null) return true
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(videoFile.absolutePath)
            retriever = r
            failureStreak = 0
            true
        } catch (_: Exception) {
            runCatching { r.release() }
            false
        }
    }

    private fun decodeWithRecovery(positionMs: Long): Bitmap? {
        decode(positionMs)?.let {
            failureStreak = 0
            return it
        }
        if (++failureStreak < FAILURES_BEFORE_RESET) return null

        // The retriever is wedged. Tear it down and try once more on a fresh one.
        failureStreak = 0
        runCatching { retriever?.release() }
        retriever = null
        if (!openRetriever()) return null
        return decode(positionMs)
    }

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
        synchronized(lock) { cache.clear() }
        onFrame = null
    }

    private companion object {
        const val PREVIEW_WIDTH = 256
        const val PREVIEW_HEIGHT = 144

        /** ~256x144 ARGB_8888 is ~147KB, so this caps the cache near 13MB. */
        const val MAX_CACHED_FRAMES = 90

        const val DEFAULT_STEP_MS = 4_000L
        const val MIN_STEP_MS = 2_000L
        const val MAX_STEP_MS = 12_000L

        /** Grid points to aim for across the whole file, before clamping. */
        const val TARGET_GRID_POINTS = 220L

        /** How far a fallback frame may be from the requested position. */
        const val FALLBACK_WINDOW_STEPS = 3

        const val FAILURES_BEFORE_RESET = 3
    }
}
