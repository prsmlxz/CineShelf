package com.cineshelf.app.data

import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.util.Locale

/** One audio track as advertised by the container. */
data class AudioTrackInfo(
    val language: String?,
    val codec: String,
    val channelCount: Int
) {
    /** "English · Dolby Digital 5.1" */
    val label: String
        get() = buildString {
            append(language ?: "Undetermined")
            append(" · ")
            append(codec)
            channelLabel?.let { append(" $it") }
        }

    val channelLabel: String?
        get() = when (channelCount) {
            0 -> null
            1 -> "Mono"
            2 -> "Stereo"
            6 -> "5.1"
            8 -> "7.1"
            else -> "${channelCount}ch"
        }
}

/**
 * Everything the detail page shows about a file that isn't in its name: how big
 * the picture is, what encoded it, what the audio is, how many subtitle tracks
 * are embedded.
 *
 * Read from the container rather than guessed from the filename, because release
 * names lie constantly — a file called `...1080p.x264...` is frequently neither.
 */
data class MediaInfo(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val videoCodec: String?,
    val hdrFormat: String?,
    val audioTracks: List<AudioTrackInfo>,
    val subtitleTrackCount: Int,
    val fileSizeBytes: Long
) {
    /**
     * Matched on the long edge so anamorphic and letterboxed masters land in the
     * right bucket — a 2.39:1 4K film is 3840x1608, whose height would otherwise
     * read as below 1080p.
     */
    val resolutionLabel: String?
        get() {
            val longEdge = maxOf(width, height)
            val shortEdge = minOf(width, height)
            if (longEdge <= 0) return null
            return when {
                longEdge >= 3400 -> "4K"
                longEdge >= 2400 -> "1440p"
                longEdge >= 1800 -> "1080p"
                longEdge >= 1200 -> "720p"
                shortEdge > 0 -> "${shortEdge}p"
                else -> null
            }
        }

    val hasDolbyAudio: Boolean
        get() = audioTracks.any { it.codec.contains("Dolby", ignoreCase = true) }

    /** The track the player will most likely pick: the widest one. */
    val primaryAudio: AudioTrackInfo?
        get() = audioTracks.maxByOrNull { it.channelCount }

    val fileSizeLabel: String?
        get() {
            if (fileSizeBytes <= 0) return null
            val gb = fileSizeBytes / 1_073_741_824.0
            return if (gb >= 1.0) String.format(Locale.US, "%.1f GB", gb)
            else String.format(Locale.US, "%.0f MB", fileSizeBytes / 1_048_576.0)
        }

    val runtimeLabel: String?
        get() {
            if (durationMs <= 0) return null
            val totalMinutes = durationMs / 60_000
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
}

/**
 * Reads [MediaInfo] straight out of a container's track table.
 *
 * MediaExtractor is used rather than MediaMetadataRetriever because the
 * retriever only reports the *selected* video track's dimensions and knows
 * nothing about how many audio or subtitle tracks exist — which is most of what
 * the detail page wants to show.
 */
object MediaInfoExtractor {

    fun extract(file: File): MediaInfo? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)

            var durationMs = 0L
            var width = 0
            var height = 0
            var videoCodec: String? = null
            var hdrFormat: String? = null
            val audio = mutableListOf<AudioTrackInfo>()
            var subtitleCount = 0

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()

                format.optLong(MediaFormat.KEY_DURATION)?.let { us ->
                    durationMs = maxOf(durationMs, us / 1000)
                }

                when {
                    mime.startsWith("video/") -> {
                        width = maxOf(width, format.optInt(MediaFormat.KEY_WIDTH) ?: 0)
                        height = maxOf(height, format.optInt(MediaFormat.KEY_HEIGHT) ?: 0)
                        videoCodec = videoCodecName(mime)
                        hdrFormat = hdrFormat ?: hdrFormatOf(format, mime)
                    }
                    mime.startsWith("audio/") -> audio += AudioTrackInfo(
                        language = languageOf(format),
                        codec = audioCodecName(mime),
                        channelCount = format.optInt(MediaFormat.KEY_CHANNEL_COUNT) ?: 0
                    )
                    mime.startsWith("text/") || mime.startsWith("application/") -> subtitleCount++
                }
            }

            MediaInfo(
                durationMs = durationMs,
                width = width,
                height = height,
                videoCodec = videoCodec,
                hdrFormat = hdrFormat,
                audioTracks = audio,
                subtitleTrackCount = subtitleCount,
                fileSizeBytes = file.length()
            )
        } catch (e: Exception) {
            Log.w("MediaInfoExtractor", "Could not read ${file.name}", e)
            null
        } finally {
            runCatching { extractor.release() }
        }
    }

    /**
     * HDR is inferred from the transfer function, which is the only signal in the
     * container that survives remuxing. Dolby Vision is its own mime type rather
     * than a transfer value.
     */
    private fun hdrFormatOf(format: MediaFormat, mime: String): String? {
        if (mime.equals("video/dolby-vision", ignoreCase = true)) return "Dolby Vision"
        return when (format.optInt(MediaFormat.KEY_COLOR_TRANSFER)) {
            MediaFormat.COLOR_TRANSFER_ST2084 -> "HDR10"
            MediaFormat.COLOR_TRANSFER_HLG -> "HLG"
            else -> null
        }
    }

    private fun videoCodecName(mime: String): String = when (mime.lowercase(Locale.US)) {
        "video/hevc" -> "HEVC"
        "video/avc" -> "H.264"
        "video/av01" -> "AV1"
        "video/x-vnd.on2.vp9" -> "VP9"
        "video/x-vnd.on2.vp8" -> "VP8"
        "video/mp4v-es" -> "MPEG-4"
        "video/dolby-vision" -> "Dolby Vision"
        else -> mime.substringAfter('/').uppercase(Locale.US)
    }

    private fun audioCodecName(mime: String): String = when (mime.lowercase(Locale.US)) {
        "audio/mp4a-latm" -> "AAC"
        "audio/ac3" -> "Dolby Digital"
        "audio/eac3", "audio/eac3-joc" -> "Dolby Digital+"
        "audio/true-hd" -> "Dolby TrueHD"
        "audio/vnd.dts" -> "DTS"
        "audio/vnd.dts.hd" -> "DTS-HD"
        "audio/opus" -> "Opus"
        "audio/vorbis" -> "Vorbis"
        "audio/flac" -> "FLAC"
        "audio/mpeg" -> "MP3"
        else -> mime.substringAfter('/').uppercase(Locale.US)
    }

    /** Containers write "und" for unknown; treat that as absent rather than a name. */
    private fun languageOf(format: MediaFormat): String? {
        val raw = format.optString("language")?.takeIf { it.isNotBlank() && it != "und" } ?: return null
        return runCatching { Locale(raw).getDisplayLanguage(Locale.US) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() && !it.equals(raw, ignoreCase = true) }
            ?: raw.uppercase(Locale.US)
    }

    // MediaFormat throws rather than returning null for absent keys, and
    // containsKey+get at every call site would triple the length of extract().
    private fun MediaFormat.optInt(key: String): Int? =
        if (containsKey(key)) runCatching { getInteger(key) }.getOrNull() else null

    private fun MediaFormat.optLong(key: String): Long? =
        if (containsKey(key)) runCatching { getLong(key) }.getOrNull() else null

    private fun MediaFormat.optString(key: String): String? =
        if (containsKey(key)) runCatching { getString(key) }.getOrNull() else null
}
