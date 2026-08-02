package com.cineshelf.app.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Extracts a representative frame from a video file and caches it to disk so
 * we don't re-decode on every library scan. Cache key is derived from the
 * file's absolute path + last-modified timestamp so edits/replacements bust
 * the cache automatically.
 */
object ThumbnailUtil {

    private fun cacheDir(context: Context): File =
        File(context.cacheDir, "thumbs").apply { if (!exists()) mkdirs() }

    private fun keyFor(file: File): String {
        val raw = "${file.absolutePath}:${file.lastModified()}"
        val digest = MessageDigest.getInstance("MD5").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Returns a cached thumbnail path if present, otherwise generates one
     * synchronously. Callers should invoke this off the main thread.
     */
    fun getOrCreateThumbnail(context: Context, videoFile: File): String? {
        val dir = cacheDir(context)
        val cached = File(dir, "${keyFor(videoFile)}.jpg")
        if (cached.exists() && cached.length() > 0) return cached.absolutePath

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            // Grab a frame ~10% into the video so we skip black intro frames when possible.
            val frameTimeUs = if (durationMs > 0) (durationMs * 1000L) / 10 else 0L
            val bitmap: Bitmap? = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            if (bitmap != null) {
                FileOutputStream(cached).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
                }
                cached.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("ThumbnailUtil", "Failed to extract thumbnail for ${videoFile.name}", e)
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /**
     * Generates a sparse set of preview frames spread evenly across the video's
     * duration (not one per second — that would be far too slow to extract from
     * a local file on demand). Used to show a real thumbnail while scrubbing,
     * snapping to the nearest generated frame rather than the exact position.
     * Cached to disk per video so this only costs anything once.
     */
    fun getOrCreateScrubThumbnails(
        context: Context,
        videoFile: File,
        durationMs: Long,
        count: Int = 30
    ): List<Pair<Long, String>> {
        if (durationMs <= 0) return emptyList()
        val dir = cacheDir(context)
        val baseKey = keyFor(videoFile)
        val retriever = MediaMetadataRetriever()
        val results = mutableListOf<Pair<Long, String>>()
        try {
            retriever.setDataSource(videoFile.absolutePath)
            for (i in 0 until count) {
                val timeMs = (durationMs * (i + 1)) / (count + 1)
                val cached = File(dir, "${baseKey}_scrub_$i.jpg")
                if (cached.exists() && cached.length() > 0) {
                    results.add(timeMs to cached.absolutePath)
                    continue
                }
                val bitmap = try {
                    retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (e: Exception) {
                    null
                } ?: continue
                try {
                    FileOutputStream(cached).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out) }
                    results.add(timeMs to cached.absolutePath)
                } catch (_: Exception) {
                    // Skip this frame if it can't be written; the rest still work.
                }
            }
        } catch (e: Exception) {
            Log.w("ThumbnailUtil", "Failed to extract scrub thumbnails for ${videoFile.name}", e)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
        return results
    }
}
