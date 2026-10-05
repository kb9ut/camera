package com.ktakata.setcam

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class FileNamesTest {

    @Test
    fun `日時からファイル名を作る`() {
        val name = FileNames.videoFileName(LocalDateTime.of(2026, 10, 5, 5, 15, 30))
        assertEquals("setcam_20261005_051530.mp4", name)
    }

    @Test
    fun `月日時分秒はゼロ埋めする`() {
        val name = FileNames.videoFileName(LocalDateTime.of(2026, 1, 2, 3, 4, 5))
        assertEquals("setcam_20260102_030405.mp4", name)
    }
}
