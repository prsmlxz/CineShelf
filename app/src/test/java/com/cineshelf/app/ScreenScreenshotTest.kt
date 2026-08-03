package com.cineshelf.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cineshelf.app.data.ContinueWatchingEntry
import com.cineshelf.app.data.SeasonGroup
import com.cineshelf.app.data.ShowItem
import com.cineshelf.app.data.SubtitlePrefs
import com.cineshelf.app.data.VideoItem
import com.cineshelf.app.ui.detail.ShowContent
import com.cineshelf.app.ui.library.LibraryContent
import com.cineshelf.app.ui.library.LibraryUiState
import com.cineshelf.app.ui.permission.PermissionScreen
import com.cineshelf.app.ui.player.AspectMode
import com.cineshelf.app.ui.player.PlayerControls
import com.cineshelf.app.ui.player.SubtitleStyleSheet
import com.cineshelf.app.ui.theme.CineShelfTheme
import com.cineshelf.app.ui.theme.auroraBackdrop
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders every screen to a PNG on the JVM via Robolectric. This is the only
 * way to actually *look* at this UI without a device: the machine has no
 * virtualization, so no emulator is possible.
 *
 * These are recordings, not assertions — the point is producing images a human
 * can inspect, not locking in a golden that breaks on every design tweak.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, content: @androidx.compose.runtime.Composable BoxScope.() -> Unit) {
        composeRule.setContent {
            CineShelfTheme {
                Box(Modifier.fillMaxSize(), content = content)
            }
        }
        composeRule.onRoot().captureRoboImage(name)
    }

    @Test
    fun library() = capture("library.png") {
        LibraryContent(
            state = LibraryUiState(
                shows = Fixtures.shows,
                continueWatching = Fixtures.continueWatching,
                isLoading = false
            ),
            onOpenShow = {},
            onPlay = {},
            onRefresh = {},
            onCreateShow = {}
        )
    }

    @Test
    fun libraryEmpty() = capture("library-empty.png") {
        LibraryContent(
            state = LibraryUiState(isLoading = false),
            onOpenShow = {},
            onPlay = {},
            onRefresh = {},
            onCreateShow = {}
        )
    }

    @Test
    fun showDetail() = capture("show-detail.png") {
        Box(Modifier.fillMaxSize().auroraBackdrop()) {
            ShowContent(
                show = Fixtures.shows[0],
                hiddenIds = emptyList(),
                onBack = {},
                onPlay = {},
                onToggleWatched = {},
                onDelete = {}
            )
        }
    }

    @Test
    fun permission() = capture("permission.png") {
        PermissionScreen()
    }

    @Test
    fun playerControls() = capture("player-controls.png") {
        PlayerControls(
            title = "Breaking Bad — S01E04 · Cancer Man",
            isPlaying = true,
            positionMs = 22 * 60_000L + 14_000L,
            durationMs = 47 * 60_000L + 32_000L,
            bufferedMs = 30 * 60_000L,
            aspectMode = AspectMode.FIT,
            speedLabel = "1x",
            scrubbing = false,
            scrubFrame = null,
            scrubDeltaMs = 0L,
            hasNextEpisode = true,
            onBack = {}, onLock = {}, onPip = {}, onPlayPause = {},
            onSkipBack = {}, onSkipForward = {}, onNextEpisode = {},
            onOpenSubtitles = {}, onOpenSubtitleStyle = {}, onOpenAudio = {},
            onOpenSpeed = {}, onCycleAspect = {}, onCycleOrientation = {},
            onOpenSleepTimer = {}, onScrubStart = {}, onScrubMove = {}, onScrubEnd = {}
        )
    }

    @Test
    fun subtitleStyleSheet() = capture("subtitle-style.png") {
        SubtitleStyleSheet(
            prefs = SubtitlePrefs(),
            subtitleOffsetMs = 0L,
            onPrefsChange = {},
            onOffsetChange = {},
            onDismiss = {}
        )
    }
}

private object Fixtures {
    private fun episode(
        show: String,
        season: Int,
        number: Int,
        title: String,
        watched: Boolean = false,
        positionMs: Long = 0L
    ) = VideoItem(
        file = File("/storage/emulated/0/CineShelf/$show/S0${season}E${number}.mkv"),
        displayTitle = title,
        seasonNumber = season,
        episodeNumber = number,
        watched = watched,
        positionMs = positionMs,
        durationMs = 47 * 60_000L,
        lastPlayedAt = 0L,
        thumbnailPath = null
    )

    private val breakingBad = ShowItem(
        folder = File("/storage/emulated/0/CineShelf/Breaking Bad"),
        name = "Breaking Bad",
        seasons = listOf(
            SeasonGroup(
                seasonNumber = 1,
                episodes = listOf(
                    episode("Breaking Bad", 1, 1, "Pilot", watched = true),
                    episode("Breaking Bad", 1, 2, "Cat's in the Bag...", watched = true),
                    episode("Breaking Bad", 1, 3, "...And the Bag's in the River", positionMs = 18 * 60_000L),
                    episode("Breaking Bad", 1, 4, "Cancer Man"),
                    episode("Breaking Bad", 1, 5, "Gray Matter")
                )
            ),
            SeasonGroup(
                seasonNumber = 2,
                episodes = listOf(
                    episode("Breaking Bad", 2, 1, "Seven Thirty-Seven"),
                    episode("Breaking Bad", 2, 2, "Grilled")
                )
            )
        ),
        standalone = emptyList(),
        posterPath = null
    )

    private val severance = ShowItem(
        folder = File("/storage/emulated/0/CineShelf/Severance"),
        name = "Severance",
        seasons = listOf(
            SeasonGroup(
                seasonNumber = 1,
                episodes = listOf(
                    episode("Severance", 1, 1, "Good News About Hell", watched = true),
                    episode("Severance", 1, 2, "Half Loop", positionMs = 9 * 60_000L)
                )
            )
        ),
        standalone = emptyList(),
        posterPath = null
    )

    private val dune = ShowItem(
        folder = File("/storage/emulated/0/CineShelf/Dune"),
        name = "Dune: Part Two",
        seasons = emptyList(),
        standalone = listOf(
            VideoItem(
                file = File("/storage/emulated/0/CineShelf/Dune/Dune Part Two.mkv"),
                displayTitle = "Dune: Part Two",
                seasonNumber = null,
                episodeNumber = null,
                watched = false,
                positionMs = 41 * 60_000L,
                durationMs = 166 * 60_000L,
                lastPlayedAt = 0L,
                thumbnailPath = null
            )
        ),
        posterPath = null
    )

    private val blade = ShowItem(
        folder = File("/storage/emulated/0/CineShelf/Blade Runner 2049"),
        name = "Blade Runner 2049",
        seasons = emptyList(),
        standalone = listOf(
            VideoItem(
                file = File("/storage/emulated/0/CineShelf/Blade Runner 2049/br2049.mkv"),
                displayTitle = "Blade Runner 2049",
                seasonNumber = null,
                episodeNumber = null,
                watched = true,
                positionMs = 0L,
                durationMs = 164 * 60_000L,
                lastPlayedAt = 0L,
                thumbnailPath = null
            )
        ),
        posterPath = null
    )

    val shows = listOf(breakingBad, severance, dune, blade)

    val continueWatching = listOf(
        ContinueWatchingEntry(breakingBad, breakingBad.seasons[0].episodes[2]),
        ContinueWatchingEntry(severance, severance.seasons[0].episodes[1]),
        ContinueWatchingEntry(dune, dune.standalone[0])
    )
}
