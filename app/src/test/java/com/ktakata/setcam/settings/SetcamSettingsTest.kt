package com.ktakata.setcam.settings

import com.ktakata.setcam.stamp.StampColor
import com.ktakata.setcam.stamp.StampFont
import com.ktakata.setcam.stamp.StampPosition
import com.ktakata.setcam.stamp.StampStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetcamSettingsTest {

    @Test
    fun `何も保存されていなければ既定値`() {
        val s = SetcamSettings.from(emptyMap<String, Any>())
        assertEquals(Resolution.FHD, s.resolution)
        assertEquals(StampStyle.DEFAULT, s.stamp)
    }

    @Test
    fun `日時表示 OFF なら stamp は null`() {
        val s = SetcamSettings.from(mapOf(SetcamSettings.KEY_STAMP_ENABLED to false))
        assertNull(s.stamp)
    }

    @Test
    fun `解像度を読む、未知の値は FHD`() {
        assertEquals(Resolution.UHD, SetcamSettings.from(mapOf(SetcamSettings.KEY_RESOLUTION to "UHD")).resolution)
        assertEquals(Resolution.FHD, SetcamSettings.from(mapOf(SetcamSettings.KEY_RESOLUTION to "8K")).resolution)
    }

    @Test
    fun `プリセット書式を使う`() {
        val s = SetcamSettings.from(mapOf(SetcamSettings.KEY_FORMAT_PRESET to "yy.MM.dd"))
        assertEquals("yy.MM.dd", s.stamp!!.pattern)
    }

    @Test
    fun `カスタム選択時は正しいカスタム書式を使う`() {
        val s = SetcamSettings.from(
            mapOf(
                SetcamSettings.KEY_FORMAT_PRESET to SetcamSettings.PRESET_CUSTOM,
                SetcamSettings.KEY_FORMAT_CUSTOM to "HH:mm:ss",
            ),
        )
        assertEquals("HH:mm:ss", s.stamp!!.pattern)
    }

    @Test
    fun `カスタム書式が不正なら既定の書式`() {
        val s = SetcamSettings.from(
            mapOf(
                SetcamSettings.KEY_FORMAT_PRESET to SetcamSettings.PRESET_CUSTOM,
                SetcamSettings.KEY_FORMAT_CUSTOM to "yyyy {",
            ),
        )
        assertEquals(StampStyle.DEFAULT_PATTERN, s.stamp!!.pattern)
    }

    @Test
    fun `位置・フォント・色・大きさを読む`() {
        val s = SetcamSettings.from(
            mapOf(
                SetcamSettings.KEY_POSITION to "TOP_LEFT",
                SetcamSettings.KEY_FONT to "DIGITAL",
                SetcamSettings.KEY_COLOR to "ORANGE",
                SetcamSettings.KEY_SIZE to 7,
            ),
        )
        val stamp = s.stamp!!
        assertEquals(StampPosition.TOP_LEFT, stamp.position)
        assertEquals(StampFont.DIGITAL, stamp.font)
        assertEquals(StampColor.ORANGE, stamp.color)
        assertEquals(7, stamp.sizePercent)
    }

    @Test
    fun `未知の列挙値は既定値に戻す`() {
        val stamp = SetcamSettings.from(
            mapOf(
                SetcamSettings.KEY_POSITION to "SOMEWHERE",
                SetcamSettings.KEY_FONT to 42,
                SetcamSettings.KEY_COLOR to null,
            ),
        ).stamp!!
        assertEquals(StampStyle.DEFAULT.position, stamp.position)
        assertEquals(StampStyle.DEFAULT.font, stamp.font)
        assertEquals(StampStyle.DEFAULT.color, stamp.color)
    }

    @Test
    fun `大きさは 2〜10 に収める`() {
        assertEquals(10, SetcamSettings.from(mapOf(SetcamSettings.KEY_SIZE to 50)).stamp!!.sizePercent)
        assertEquals(2, SetcamSettings.from(mapOf(SetcamSettings.KEY_SIZE to 0)).stamp!!.sizePercent)
    }
}
