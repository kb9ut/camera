package com.ktakata.setcam.settings

import com.ktakata.setcam.stamp.DateFormats
import com.ktakata.setcam.stamp.StampStyle

enum class Resolution { FHD, UHD, HD }

/** 保存された設定を型付きにしたもの。stamp が null なら日時を表示しない。 */
data class SetcamSettings(val resolution: Resolution, val stamp: StampStyle?) {

    companion object {
        const val KEY_RESOLUTION = "resolution"
        const val KEY_STAMP_ENABLED = "stamp_enabled"
        const val KEY_FORMAT_PRESET = "stamp_format_preset"
        const val KEY_FORMAT_CUSTOM = "stamp_format_custom"
        const val KEY_POSITION = "stamp_position"
        const val KEY_FONT = "stamp_font"
        const val KEY_COLOR = "stamp_color"
        const val KEY_SIZE = "stamp_size"

        const val PRESET_CUSTOM = "custom"
        val PRESET_PATTERNS = listOf(
            "yyyy/M/d H:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yy.MM.dd",
            "M/d/yyyy h:mm a",
        )
        const val MIN_SIZE = 2
        const val MAX_SIZE = 10

        /** SharedPreferences.getAll() の結果から作る。不正な値は既定値に戻す。 */
        fun from(values: Map<String, *>): SetcamSettings {
            val resolution = enumOr(values[KEY_RESOLUTION], Resolution.FHD)
            val enabled = values[KEY_STAMP_ENABLED] as? Boolean ?: true
            if (!enabled) return SetcamSettings(resolution, null)

            val default = StampStyle.DEFAULT
            val preset = values[KEY_FORMAT_PRESET] as? String ?: default.pattern
            val chosen = if (preset == PRESET_CUSTOM) values[KEY_FORMAT_CUSTOM] as? String else preset
            val pattern = chosen?.takeIf { DateFormats.isValid(it) } ?: default.pattern
            val size = ((values[KEY_SIZE] as? Int) ?: default.sizePercent).coerceIn(MIN_SIZE, MAX_SIZE)

            return SetcamSettings(
                resolution,
                StampStyle(
                    pattern = pattern,
                    position = enumOr(values[KEY_POSITION], default.position),
                    font = enumOr(values[KEY_FONT], default.font),
                    color = enumOr(values[KEY_COLOR], default.color),
                    sizePercent = size,
                ),
            )
        }

        private inline fun <reified E : Enum<E>> enumOr(raw: Any?, default: E): E =
            enumValues<E>().firstOrNull { it.name == raw } ?: default
    }
}
