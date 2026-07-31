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
    val thumbnailPath: String?
) {
    val id: String get() = file.absolutePath
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
}
