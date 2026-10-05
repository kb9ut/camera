package com.ktakata.setcam.stamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class DateFormatsTest {
    private val time = ZonedDateTime.of(2026, 10, 5, 5, 15, 30, 0, ZoneId.of("Asia/Tokyo"))

    @Test
    fun `既定の書式は 2026_10_5 5_15 になる`() {
        assertEquals("2026/10/5 5:15", DateFormats.formatOrNull("yyyy/M/d H:mm", time, Locale.ROOT))
    }

    @Test
    fun `秒付きゼロ埋め書式`() {
        assertEquals("2026-10-05 05:15:30", DateFormats.formatOrNull("yyyy-MM-dd HH:mm:ss", time, Locale.ROOT))
    }

    @Test
    fun `午前午後はロケールに従う`() {
        assertEquals("10/5/2026 5:15 AM", DateFormats.formatOrNull("M/d/yyyy h:mm a", time, Locale.US))
    }

    @Test
    fun `予約文字や未知の文字を含む書式は null`() {
        assertNull(DateFormats.formatOrNull("yyyy/M/d {", time, Locale.ROOT))
        assertNull(DateFormats.formatOrNull("bbb", time, Locale.ROOT))
    }

    @Test
    fun `空や null は null`() {
        assertNull(DateFormats.formatOrNull("", time, Locale.ROOT))
        assertNull(DateFormats.formatOrNull("   ", time, Locale.ROOT))
        assertNull(DateFormats.formatOrNull(null, time, Locale.ROOT))
    }

    @Test
    fun `isValid は正しい書式だけ true`() {
        assertTrue(DateFormats.isValid("yy.MM.dd"))
        assertFalse(DateFormats.isValid("yyyy/M/d {"))
        assertFalse(DateFormats.isValid(null))
    }
}
