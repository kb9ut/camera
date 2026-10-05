package com.ktakata.setcam.stamp

import org.junit.Assert.assertEquals
import org.junit.Test

class UprightTransformTest {

    private fun assertPoint(expectedX: Float, expectedY: Float, actual: Pair<Float, Float>) {
        assertEquals(expectedX, actual.first, 0.001f)
        assertEquals(expectedY, actual.second, 0.001f)
    }

    @Test
    fun `90度と270度では幅と高さが入れ替わる`() {
        assertEquals(Size2(1080, 1920), UprightTransform.uprightSize(1920, 1080, 90))
        assertEquals(Size2(1080, 1920), UprightTransform.uprightSize(1920, 1080, 270))
        assertEquals(Size2(1920, 1080), UprightTransform.uprightSize(1920, 1080, 0))
        assertEquals(Size2(1920, 1080), UprightTransform.uprightSize(1920, 1080, 180))
    }

    @Test
    fun `回転0はクロップ位置の平行移動だけ`() {
        val m = UprightTransform.uprightToBuffer(10, 20, 1920, 1080, 0)
        assertPoint(15f, 25f, UprightTransform.map(m, 5f, 5f))
    }

    @Test
    fun `回転90 正立の左上はバッファの左下、正立の右上はバッファの左上`() {
        val m = UprightTransform.uprightToBuffer(0, 0, 1920, 1080, 90)
        assertPoint(0f, 1080f, UprightTransform.map(m, 0f, 0f))
        assertPoint(0f, 0f, UprightTransform.map(m, 1080f, 0f))
    }

    @Test
    fun `回転180 正立の左上はバッファの右下`() {
        val m = UprightTransform.uprightToBuffer(0, 0, 1920, 1080, 180)
        assertPoint(1920f, 1080f, UprightTransform.map(m, 0f, 0f))
    }

    @Test
    fun `回転270 正立の左上はバッファの右上`() {
        val m = UprightTransform.uprightToBuffer(0, 0, 1920, 1080, 270)
        assertPoint(1920f, 0f, UprightTransform.map(m, 0f, 0f))
    }

    @Test
    fun `どの回転でも正立の四隅はクロップの四隅に写る`() {
        val left = 8; val top = 6; val cw = 1920; val ch = 1080
        val cropCorners = setOf(
            left.toFloat() to top.toFloat(), (left + cw).toFloat() to top.toFloat(),
            left.toFloat() to (top + ch).toFloat(), (left + cw).toFloat() to (top + ch).toFloat(),
        )
        for (r in listOf(0, 90, 180, 270)) {
            val size = UprightTransform.uprightSize(cw, ch, r)
            val m = UprightTransform.uprightToBuffer(left, top, cw, ch, r)
            val mapped = listOf(0f to 0f, size.width.toFloat() to 0f, 0f to size.height.toFloat(),
                size.width.toFloat() to size.height.toFloat())
                .map { UprightTransform.map(m, it.first, it.second) }
                .toSet()
            assertEquals("rotation $r", cropCorners, mapped)
        }
    }

    @Test
    fun `負の角度や360超えは正規化する`() {
        val a = UprightTransform.uprightToBuffer(0, 0, 1920, 1080, -90)
        val b = UprightTransform.uprightToBuffer(0, 0, 1920, 1080, 270)
        assertEquals(b.toList(), a.toList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `90の倍数以外は例外`() {
        UprightTransform.uprightToBuffer(0, 0, 1920, 1080, 45)
    }
}
