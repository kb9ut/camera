package com.ktakata.setcam.stamp

import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormats {
    /** 検証用の日時。ゾーン付きなので、タイムゾーンを使う書式も検証できる。 */
    private val SAMPLE = ZonedDateTime.of(2026, 10, 5, 5, 15, 30, 0, ZoneId.of("Asia/Tokyo"))

    fun isValid(pattern: String?): Boolean = formatOrNull(pattern, SAMPLE) != null

    fun formatOrNull(pattern: String?, time: ZonedDateTime, locale: Locale = Locale.getDefault()): String? {
        if (pattern.isNullOrBlank()) return null
        return try {
            // "[]" や "''" のように整形結果が空になる書式は、空のスタンプになるので無効にする。
            DateTimeFormatter.ofPattern(pattern, locale).format(time).takeIf { it.isNotBlank() }
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: DateTimeException) {
            null
        }
    }
}
