package com.cineshelf.app.data

import java.io.File

/**
 * A single playable video file on disk, with derived display info and
 * persisted watch metadata merged in.
 */
data class VideoItem(
    val file: File,
    val displayTitle: String,       // e.g. "Episode 1" or the raw filename for a movie
    val seasonNumber: Int?,         // null if this isn't part of a season
    val episodeNumber: Int?,
    val watched: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAt: Long,
    val thumbnailPath: String?,
    val mediaInfo: MediaInfo? = null
) {
    val id: String get() = file.absolutePath
    val progressFraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val isInProgress: Boolean get() = !watched && positionMs > 15_000L && durationMs > 0

    /**
     * Duration from the container when playback hasn't recorded one yet, so a
     * never-opened file still shows its runtime.
     */
    val effectiveDurationMs: Long
        get() = if (durationMs > 0) durationMs else mediaInfo?.durationMs ?: 0L
}

data class SeasonGroup(
    val seasonNumber: Int,
    val episodes: List<VideoItem>
) {
    val title: String get() = "Season $seasonNumber"
}

/**
 * A show/movie folder as it appears on the library shelf.
 */
data class ShowItem(
    val folder: File,
    val name: String,
    val seasons: List<SeasonGroup>,
    val standalone: List<VideoItem>, // files with no season/episode pattern (movies, specials)
    val posterPath: String?
) {
    val id: String get() = folder.absolutePath
    val totalItemCount: Int get() = standalone.size + seasons.sumOf { it.episodes.size }
    val watchedCount: Int get() = standalone.count { it.watched } + seasons.sumOf { s -> s.episodes.count { it.watched } }
    val isSingleMovie: Boolean get() = seasons.isEmpty() && standalone.size == 1
    val allEpisodes: List<VideoItem> get() = standalone + seasons.flatMap { it.episodes }
}

/** A single row entry for the "Continue Watching" rail: which show it belongs to + which episode. */
data class ContinueWatchingEntry(
    val show: ShowItem,
    val episode: VideoItem
)
