package com.cineshelf.app.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Lightweight JSON key-value store (keyed by absolute file path) that persists
 * per-video watch state, playback position, and duration. Deliberately avoids
 * a full database dependency to keep the build simple and fast.
 */
class MetadataStore(context: Context) {

    private val storeFile = File(context.filesDir, "metadata.json")
    private val lock = ReentrantLock()
    private var cache: JSONObject = load()

    private fun load(): JSONObject {
        return try {
            if (storeFile.exists()) {
                JSONObject(storeFile.readText())
            } else {
                JSONObject()
            }
        } catch (e: Exception) {
            JSONObject()
        }
    }

    private fun persist() {
        try {
            storeFile.writeText(cache.toString())
        } catch (_: Exception) {
            // best-effort; not fatal if a single write fails
        }
    }

    fun isWatched(path: String): Boolean = lock.withLock {
        cache.optJSONObject(path)?.optBoolean("watched", false) ?: false
    }

    fun getPosition(path: String): Long = lock.withLock {
        cache.optJSONObject(path)?.optLong("positionMs", 0L) ?: 0L
    }

    fun getDuration(path: String): Long = lock.withLock {
        cache.optJSONObject(path)?.optLong("durationMs", 0L) ?: 0L
    }

    fun setWatched(path: String, watched: Boolean) = lock.withLock {
        val entry = cache.optJSONObject(path) ?: JSONObject().also { cache.put(path, it) }
        entry.put("watched", watched)
        if (watched) {
            entry.put("positionMs", 0L)
        }
        persist()
    }

    fun setProgress(path: String, positionMs: Long, durationMs: Long) = lock.withLock {
        val entry = cache.optJSONObject(path) ?: JSONObject().also { cache.put(path, it) }
        entry.put("positionMs", positionMs)
        entry.put("durationMs", durationMs)
        if (durationMs > 0 && positionMs >= durationMs * 0.92) {
            entry.put("watched", true)
            entry.put("positionMs", 0L)
        }
        persist()
    }

    fun remove(path: String) = lock.withLock {
        cache.remove(path)
        persist()
    }

    /** Called when a file is moved (e.g. into a season folder) so metadata follows it. */
    fun rekey(oldPath: String, newPath: String) = lock.withLock {
        val entry = cache.optJSONObject(oldPath)
        if (entry != null) {
            cache.remove(oldPath)
            cache.put(newPath, entry)
            persist()
        }
    }
}
