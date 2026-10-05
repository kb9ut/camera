package com.ktakata.setcam.stamp

data class Size2(val width: Int, val height: Int)

/**
 * バッファ（センサー向き）のクロップ領域を、rotationDegrees だけ時計回りに回すと正立する。
 * ここでは逆向き、つまり「正立座標 (u, v) → バッファ座標 (x, y)」の行列を作る。
 */
object UprightTransform {

    fun uprightSize(cropWidth: Int, cropHeight: Int, rotationDegrees: Int): Size2 =
        if (normalize(rotationDegrees) % 180 == 0) Size2(cropWidth, cropHeight)
        else Size2(cropHeight, cropWidth)

    /** android.graphics.Matrix.setValues() 用の、行優先の 3×3 行列。 */
    fun uprightToBuffer(
        cropLeft: Int,
        cropTop: Int,
        cropWidth: Int,
        cropHeight: Int,
        rotationDegrees: Int,
    ): FloatArray {
        val l = cropLeft.toFloat()
        val t = cropTop.toFloat()
        val w = cropWidth.toFloat()
        val h = cropHeight.toFloat()
        // x = a*u + b*v + c,  y = d*u + e*v + f
        return when (normalize(rotationDegrees)) {
            0 -> floatArrayOf(1f, 0f, l, 0f, 1f, t, 0f, 0f, 1f)
            90 -> floatArrayOf(0f, 1f, l, -1f, 0f, t + h, 0f, 0f, 1f)
            180 -> floatArrayOf(-1f, 0f, l + w, 0f, -1f, t + h, 0f, 0f, 1f)
            else -> floatArrayOf(0f, -1f, l + w, 1f, 0f, t, 0f, 0f, 1f) // 270
        }
    }

    fun map(matrix: FloatArray, x: Float, y: Float): Pair<Float, Float> =
        (matrix[0] * x + matrix[1] * y + matrix[2]) to (matrix[3] * x + matrix[4] * y + matrix[5])

    private fun normalize(degrees: Int): Int {
        require(degrees % 90 == 0) { "rotationDegrees must be a multiple of 90: $degrees" }
        return ((degrees % 360) + 360) % 360
    }
}
