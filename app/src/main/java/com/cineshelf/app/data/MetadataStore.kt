package com.cineshelf.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Lightweight JSON key-value store (keyed by absolute file path) that persists
 * per-video watch state, playback position, duration, and last-played time.
 * Deliberately avoids a full database dependency to keep the build simple.
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

    fun getLastPlayedAt(path: String): Long = lock.withLock {
        cache.optJSONObject(path)?.optLong("lastPlayedAt", 0L) ?: 0L
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
        entry.put("lastPlayedAt", System.currentTimeMillis())
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

    /**
     * Reads cached container facts, or null on a miss.
     *
     * Stamped with the file's lastModified so a replaced file (same path, new
     * encode) doesn't keep reporting the old encode's resolution and codec.
     */
    fun getMediaInfo(path: String, lastModified: Long): MediaInfo? = lock.withLock {
        val entry = cache.optJSONObject(path)?.optJSONObject("media") ?: return null
        if (entry.optLong("stamp", -1L) != lastModified) return null
        val tracks = entry.optJSONArray("audio")
        MediaInfo(
            durationMs = entry.optLong("durationMs", 0L),
            width = entry.optInt("width", 0),
            height = entry.optInt("height", 0),
            videoCodec = entry.optString("videoCodec").takeIf { it.isNotEmpty() },
            hdrFormat = entry.optString("hdrFormat").takeIf { it.isNotEmpty() },
            audioTracks = (0 until (tracks?.length() ?: 0)).mapNotNull { i ->
                tracks?.optJSONObject(i)?.let {
                    AudioTrackInfo(
                        language = it.optString("lang").takeIf { s -> s.isNotEmpty() },
                        codec = it.optString("codec"),
                        channelCount = it.optInt("ch", 0)
                    )
                }
            },
            subtitleTrackCount = entry.optInt("subs", 0),
            fileSizeBytes = entry.optLong("size", 0L)
        )
    }

    fun putMediaInfo(path: String, lastModified: Long, info: MediaInfo) = lock.withLock {
        val entry = cache.optJSONObject(path) ?: JSONObject().also { cache.put(path, it) }
        entry.put(
            "media",
            JSONObject().apply {
                put("stamp", lastModified)
                put("durationMs", info.durationMs)
                put("width", info.width)
                put("height", info.height)
                put("videoCodec", info.videoCodec ?: "")
                put("hdrFormat", info.hdrFormat ?: "")
                put("subs", info.subtitleTrackCount)
                put("size", info.fileSizeBytes)
                put(
                    "audio",
                    JSONArray().apply {
                        info.audioTracks.forEach { track ->
                            put(
                                JSONObject().apply {
                                    put("lang", track.language ?: "")
                                    put("codec", track.codec)
                                    put("ch", track.channelCount)
                                }
                            )
                        }
                    }
                )
            }
        )
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
