package com.cineshelf.app.data

import android.content.Context

/**
 * How subtitle text is outlined/backed so it stays readable over bright
 * scenes. Maps directly onto Media3's CaptionStyleCompat edge types.
 */
enum class SubtitleEdgeStyle {
    OUTLINE,
    DROP_SHADOW,
    BACKGROUND_BOX,
    NONE
}

data class SubtitleStyle(
    val sizeScale: Float = 1f,          // multiplier on the default caption size
    val edgeStyle: SubtitleEdgeStyle = SubtitleEdgeStyle.OUTLINE
)

/**
 * Persists the user's subtitle size/outline preference across videos and
 * app restarts. Deliberately plain SharedPreferences — this is two small
 * values, not worth a JSON file or a new dependency.
 */
class SubtitlePrefs(context: Context) {

    private val prefs = context.getSharedPreferences("subtitle_prefs", Context.MODE_PRIVATE)

    fun get(): SubtitleStyle {
        val scale = prefs.getFloat(KEY_SIZE, 1f)
        val edge = try {
            SubtitleEdgeStyle.valueOf(prefs.getString(KEY_EDGE, SubtitleEdgeStyle.OUTLINE.name) ?: SubtitleEdgeStyle.OUTLINE.name)
        } catch (_: IllegalArgumentException) {
            SubtitleEdgeStyle.OUTLINE
        }
        return SubtitleStyle(scale, edge)
    }

    fun save(style: SubtitleStyle) {
        prefs.edit()
            .putFloat(KEY_SIZE, style.sizeScale)
            .putString(KEY_EDGE, style.edgeStyle.name)
            .apply()
    }

    private companion object {
        const val KEY_SIZE = "size_scale"
        const val KEY_EDGE = "edge_style"
    }
}
