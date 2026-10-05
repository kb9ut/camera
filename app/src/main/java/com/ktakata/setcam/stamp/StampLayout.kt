package com.ktakata.setcam.stamp

/** drawText に渡す座標（x は文字の左端、baseline はベースライン）。 */
data class TextOrigin(val x: Float, val baseline: Float)

object StampLayout {

    fun textSizePx(width: Int, height: Int, percent: Int): Float =
        minOf(width, height) * percent / 100f

    /**
     * 正立座標系（width × height）の中で、3×3 の位置に文字を置いたときの原点を返す。
     * 余白は文字サイズの半分。ascent は負の値（Paint.FontMetrics と同じ）。
     */
    fun textOrigin(
        width: Int,
        height: Int,
        textWidth: Float,
        ascent: Float,
        descent: Float,
        textSize: Float,
        position: StampPosition,
    ): TextOrigin {
        val margin = textSize / 2f
        val textHeight = descent - ascent
        val x = when (position.column) {
            0 -> margin
            1 -> (width - textWidth) / 2f
            else -> width - margin - textWidth
        }
        val top = when (position.row) {
            0 -> margin
            1 -> (height - textHeight) / 2f
            else -> height - margin - textHeight
        }
        return TextOrigin(x, top - ascent)
    }
}
