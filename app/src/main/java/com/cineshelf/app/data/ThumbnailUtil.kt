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
}
