package com.cineshelf.app.data

import android.content.Context
import android.os.Environment
import java.io.File
import java.util.regex.Pattern

/**
 * Owns the on-disk library at /Movies/CineShelf/<Show>/[Season N/]<file>,
 * including auto-detecting SxxExx style filenames and organizing them into
 * season subfolders without ever renaming the original file.
 */
class LibraryRepository(private val context: Context) {

    private val metadata = MetadataStore(context)

    private val videoExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "webm", "m4v", "3gp", "ts", "flv"
    )

    // Matches S03E01, s3e1, S03.E01, S03 E01, etc.
    private val seasonEpisodePattern: Pattern = Pattern.compile(
        "[Ss](\\d{1,2})[\\s._-]?[Ee](\\d{1,3})"
    )
    // Fallback: 3x01 style
    private val altPattern: Pattern = Pattern.compile(
        "(?<![0-9])(\\d{1,2})[xX](\\d{2,3})(?![0-9])"
    )

    val rootDir: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "CineShelf"
        ).apply { if (!exists()) mkdirs() }

    fun createShowFolder(name: String): Boolean {
        val safe = name.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
        if (safe.isEmpty()) return false
        val folder = File(rootDir, safe)
        return folder.exists() || folder.mkdirs()
    }

    fun deleteShowFolder(folder: File): Boolean {
        return folder.deleteRecursively()
    }

    /** Extracts (season, episode) from a filename, checking both naming conventions. */
    private fun parseSeasonEpisode(filename: String): Pair<Int, Int>? {
        val m1 = seasonEpisodePattern.matcher(filename)
        if (m1.find()) {
            val s = m1.group(1)?.toIntOrNull()
            val e = m1.group(2)?.toIntOrNull()
            if (s != null && e != null) return s to e
        }
        val m2 = altPattern.matcher(filename)
        if (m2.find()) {
            val s = m2.group(1)?.toIntOrNull()
            val e = m2.group(2)?.toIntOrNull()
            if (s != null && e != null) return s to e
        }
        return null
    }

    private fun isVideoFile(file: File): Boolean =
        file.isFile && file.extension.lowercase() in videoExtensions

    /**
     * Scans a show folder: moves any loose SxxExx files sitting directly in the
     * show root into their Season N subfolder (creating it if needed), then
     * builds the full ShowItem tree. Files already inside a "Season N" folder
     * are left in place. Files with no detectable pattern are treated as
     * standalone items (movies/specials).
     */
    fun scanShow(folder: File): ShowItem {
        organizeLooseEpisodes(folder)

        val seasons = mutableMapOf<Int, MutableList<VideoItem>>()
        val standalone = mutableListOf<VideoItem>()

        folder.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                val seasonNum = seasonFolderNumber(child.name)
                if (seasonNum != null) {
                    val episodes = child.listFiles()
                        ?.filter { isVideoFile(it) }
                        ?.mapNotNull { file -> buildVideoItem(file, seasonNum) }
                        ?.sortedBy { it.episodeNumber ?: Int.MAX_VALUE }
                        ?: emptyList()
                    if (episodes.isNotEmpty()) {
                        seasons.getOrPut(seasonNum) { mutableListOf() }.addAll(episodes)
                    }
                }
            } else if (isVideoFile(child)) {
                // Loose file with no season pattern -> standalone (movie/special)
                standalone.add(buildVideoItem(child, null, forceStandalone = true)!!)
            }
        }

        val seasonGroups = seasons.entries
            .sortedBy { it.key }
            .map { (num, eps) -> SeasonGroup(num, eps.sortedBy { it.episodeNumber ?: 0 }) }

        val poster = (standalone.firstOrNull() ?: seasonGroups.firstOrNull()?.episodes?.firstOrNull())
            ?.thumbnailPath

        return ShowItem(
            folder = folder,
            name = folder.name,
            seasons = seasonGroups,
            standalone = standalone.sortedBy { it.file.name.lowercase() },
            posterPath = poster
        )
    }

    fun scanLibrary(): List<ShowItem> {
        return rootDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { scanShow(it) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    private fun seasonFolderNumber(dirName: String): Int? {
        val m = Pattern.compile("(?i)season\\s*(\\d{1,2})").matcher(dirName)
        return if (m.find()) m.group(1)?.toIntOrNull() else null
    }

    private fun organizeLooseEpisodes(folder: File) {
        val looseFiles = folder.listFiles()?.filter { isVideoFile(it) } ?: return
        for (file in looseFiles) {
            val parsed = parseSeasonEpisode(file.name) ?: continue
            val (season, _) = parsed
            val seasonDir = File(folder, "Season $season").apply { if (!exists()) mkdirs() }
            val destination = File(seasonDir, file.name)
            if (!destination.exists()) {
                val oldPath = file.absolutePath
                if (file.renameTo(destination)) {
                    metadata.rekey(oldPath, destination.absolutePath)
                }
            }
        }
    }

    private fun buildVideoItem(file: File, seasonNumber: Int?, forceStandalone: Boolean = false): VideoItem? {
        if (!isVideoFile(file)) return null
        val parsed = if (forceStandalone) null else parseSeasonEpisode(file.name)
        val episodeNumber = parsed?.second
        val displayTitle = when {
            episodeNumber != null -> "Episode $episodeNumber"
            else -> file.nameWithoutExtension
        }
        val thumb = ThumbnailUtil.getOrCreateThumbnail(context, file)
        return VideoItem(
            file = file,
            displayTitle = displayTitle,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            watched = metadata.isWatched(file.absolutePath),
            positionMs = metadata.getPosition(file.absolutePath),
            durationMs = metadata.getDuration(file.absolutePath),
            thumbnailPath = thumb
        )
    }

    fun setWatched(item: VideoItem, watched: Boolean) = metadata.setWatched(item.id, watched)

    fun setProgress(item: VideoItem, positionMs: Long, durationMs: Long) =
        metadata.setProgress(item.id, positionMs, durationMs)

    fun deleteVideo(item: VideoItem): Boolean {
        val ok = item.file.delete()
        if (ok) metadata.remove(item.id)
        return ok
    }

    // --- Direct file-based helpers used by the player screen ---

    fun getInitialPosition(file: File): Long = metadata.getPosition(file.absolutePath)

    fun saveProgress(file: File, positionMs: Long, durationMs: Long) {
        if (durationMs > 0) metadata.setProgress(file.absolutePath, positionMs, durationMs)
    }
}
