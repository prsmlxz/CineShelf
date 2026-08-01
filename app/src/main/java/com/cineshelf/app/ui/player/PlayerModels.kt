package com.cineshelf.app.ui.player

import androidx.media3.common.TrackGroup

/** A single selectable subtitle or audio track, derived from the player's current tracks. */
data class TrackOption(
    val group: TrackGroup,
    val trackIndex: Int,
    val label: String,
    val selected: Boolean
)

/** Axis lock for the combined scrub / brightness / volume drag gesture. */
enum class DragAxis { HORIZONTAL, VERTICAL }

/** Which side of the screen a vertical drag started on. */
enum class DragSide { BRIGHTNESS, VOLUME }

enum class AspectMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    STRETCH("Stretch");

    fun next(): AspectMode = when (this) {
        FIT -> FILL
        FILL -> STRETCH
        STRETCH -> FIT
    }
}

enum class OrientationMode(val label: String) {
    AUTO("Auto"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape");

    fun next(): OrientationMode = when (this) {
        AUTO -> LANDSCAPE
        LANDSCAPE -> PORTRAIT
        PORTRAIT -> AUTO
    }
}

data class SleepTimerOption(val minutes: Int?, val label: String)

val sleepTimerOptions = listOf(
    SleepTimerOption(null, "Off"),
    SleepTimerOption(15, "15 minutes"),
    SleepTimerOption(30, "30 minutes"),
    SleepTimerOption(45, "45 minutes"),
    SleepTimerOption(60, "1 hour")
)

val speedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
