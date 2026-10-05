package com.ktakata.setcam.stamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StampLayoutTest {
    private val w = 1080
    private val h = 1920
    private val textWidth = 300f
    private val ascent = -40f
    private val descent = 10f
    private val textSize = 40f

    private fun origin(position: StampPosition) =
        StampLayout.textOrigin(w, h, textWidth, ascent, descent, textSize, position)

    @Test
    fun `文字サイズは短辺に対する割合`() {
        assertEquals(43.2f, StampLayout.textSizePx(1080, 1920, 4), 0.001f)
        assertEquals(43.2f, StampLayout.textSizePx(1920, 1080, 4), 0.001f)
    }

    @Test
    fun `右下は余白（文字サイズの半分）を空けて置く`() {
        val o = origin(StampPosition.BOTTOM_RIGHT)
        assertEquals(760f, o.x, 0.001f)        // 1080 - 20 - 300
        assertEquals(1890f, o.baseline, 0.001f) // 1920 - 20 - 50 + 40
    }

    @Test
    fun `左上`() {
        val o = origin(StampPosition.TOP_LEFT)
        assertEquals(20f, o.x, 0.001f)
        assertEquals(60f, o.baseline, 0.001f)   // 20 + 40
    }

    @Test
    fun `中央`() {
        val o = origin(StampPosition.CENTER)
        assertEquals(390f, o.x, 0.001f)         // (1080 - 300) / 2
        assertEquals(975f, o.baseline, 0.001f)  // (1920 - 50) / 2 + 40
    }

    @Test
    fun `9 か所すべてでフレーム内に収まる`() {
        for (p in StampPosition.entries) {
            val o = origin(p)
            assertTrue("$p x", o.x >= 0f && o.x + textWidth <= w)
            assertTrue("$p y", o.baseline + ascent >= 0f && o.baseline + descent <= h)
        }
    }
}
