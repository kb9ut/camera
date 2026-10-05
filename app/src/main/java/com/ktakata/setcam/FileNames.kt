package com.ktakata.setcam

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object FileNames {
    private val FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT)

    fun videoFileName(time: LocalDateTime): String = "sscam_${FORMAT.format(time)}.mp4"
}
