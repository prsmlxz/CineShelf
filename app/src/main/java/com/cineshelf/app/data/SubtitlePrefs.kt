package com.cineshelf.app.data

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.media3.ui.CaptionStyleCompat

/** How the subtitle text edge is drawn. */
enum class SubtitleEdge(val label: String, val mediaValue: Int) {
    NONE("None", CaptionStyleCompat.EDGE_TYPE_NONE),
    OUTLINE("Outline", CaptionStyleCompat.EDGE_TYPE_OUTLINE),
    DROP_SHADOW("Shadow", CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW),
    RAISED("Raised", CaptionStyleCompat.EDGE_TYPE_RAISED),
    DEPRESSED("Depressed", CaptionStyleCompat.EDGE_TYPE_DEPRESSED)
}

enum class SubtitleTextColor(val label: String, val argb: Int) {
    WHITE("White", AndroidColor.WHITE),
    SOFT_WHITE("Soft", AndroidColor.rgb(232, 232, 240)),
    YELLOW("Yellow", AndroidColor.rgb(255, 222, 89)),
    CYAN("Cyan", AndroidColor.rgb(120, 235, 255))
}

/**
 * User-adjustable subtitle appearance. Sizes are expressed as a multiplier of
 * the player's default text size so they scale correctly on any screen.
 */
data class SubtitlePrefs(
    val scale: Float = 1.0f,
    val edge: SubtitleEdge = SubtitleEdge.OUTLINE,
    val textColor: SubtitleTextColor = SubtitleTextColor.WHITE,
    val backgroundOpacity: Float = 0f,
    val bottomPaddingScale: Float = 1f
) {
    fun toCaptionStyle(): CaptionStyleCompat {
        val bgAlpha = (backgroundOpacity.coerceIn(0f, 1f) * 255).toInt()
        return CaptionStyleCompat(
            textColor.argb,
            AndroidColor.argb(bgAlpha, 0, 0, 0),
            AndroidColor.TRANSPARENT,
            edge.mediaValue,
            AndroidColor.argb(230, 0, 0, 0),
            null
        )
    }
}

/** Persists [SubtitlePrefs] so choices survive app restarts. */
class SubtitlePrefsStore(context: Context) {

    private val prefs = context.getSharedPreferences("subtitle_prefs", Context.MODE_PRIVATE)

    fun load(): SubtitlePrefs = SubtitlePrefs(
        scale = prefs.getFloat(KEY_SCALE, 1.0f),
        edge = runCatching { SubtitleEdge.valueOf(prefs.getString(KEY_EDGE, null) ?: "OUTLINE") }
            .getOrDefault(SubtitleEdge.OUTLINE),
        textColor = runCatching { SubtitleTextColor.valueOf(prefs.getString(KEY_COLOR, null) ?: "WHITE") }
            .getOrDefault(SubtitleTextColor.WHITE),
        backgroundOpacity = prefs.getFloat(KEY_BG, 0f),
        bottomPaddingScale = prefs.getFloat(KEY_BOTTOM, 1f)
    )

    fun save(value: SubtitlePrefs) {
        prefs.edit()
            .putFloat(KEY_SCALE, value.scale)
            .putString(KEY_EDGE, value.edge.name)
            .putString(KEY_COLOR, value.textColor.name)
            .putFloat(KEY_BG, value.backgroundOpacity)
            .putFloat(KEY_BOTTOM, value.bottomPaddingScale)
            .apply()
    }

    private companion object {
        const val KEY_SCALE = "scale"
        const val KEY_EDGE = "edge"
        const val KEY_COLOR = "color"
        const val KEY_BG = "bg_opacity"
        const val KEY_BOTTOM = "bottom_padding"
    }
}
