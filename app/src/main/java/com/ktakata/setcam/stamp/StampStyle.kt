package com.ktakata.setcam.stamp

/** 3×3 グリッド上の位置。column / row は 0〜2（左上が 0,0）。 */
enum class StampPosition(val column: Int, val row: Int) {
    TOP_LEFT(0, 0), TOP_CENTER(1, 0), TOP_RIGHT(2, 0),
    MIDDLE_LEFT(0, 1), CENTER(1, 1), MIDDLE_RIGHT(2, 1),
    BOTTOM_LEFT(0, 2), BOTTOM_CENTER(1, 2), BOTTOM_RIGHT(2, 2);

    companion object {
        fun of(column: Int, row: Int): StampPosition =
            entries.first { it.column == column && it.row == row }
    }
}

enum class StampFont { GOTHIC, MINCHO, MONO, DIGITAL }

/** 文字色と、読みやすくするための影の色。 */
enum class StampColor(val argb: Int, val shadowArgb: Int) {
    WHITE(0xFFFFFFFF.toInt(), 0x99000000.toInt()),
    BLACK(0xFF000000.toInt(), 0x99FFFFFF.toInt()),
    YELLOW(0xFFFFEB3B.toInt(), 0x99000000.toInt()),
    ORANGE(0xFFFF9800.toInt(), 0x99000000.toInt()),
    RED(0xFFF44336.toInt(), 0x99000000.toInt()),
    GREEN(0xFF4CAF50.toInt(), 0x99000000.toInt()),
}

data class StampStyle(
    val pattern: String,
    val position: StampPosition,
    val font: StampFont,
    val color: StampColor,
    /** フレーム短辺に対する文字サイズ（%）。 */
    val sizePercent: Int,
) {
    companion object {
        const val DEFAULT_PATTERN = "yyyy/M/d H:mm"
        val DEFAULT = StampStyle(
            pattern = DEFAULT_PATTERN,
            position = StampPosition.BOTTOM_RIGHT,
            font = StampFont.GOTHIC,
            color = StampColor.WHITE,
            sizePercent = 4,
        )
    }
}
