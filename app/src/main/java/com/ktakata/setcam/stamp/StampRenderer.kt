package com.ktakata.setcam.stamp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.ktakata.setcam.R
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 正立座標系（width × height）の Canvas に日時を描く。
 * OverlayEffect（録画）と設定画面のプレビューの両方で使う。
 */
class StampRenderer(context: Context, private val style: StampStyle) {

    private val formatter = DateTimeFormatter.ofPattern(style.pattern, Locale.getDefault())
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = this@StampRenderer.style.color.argb
        typeface = resolveTypeface(context, this@StampRenderer.style.font)
    }
    private val fontMetrics = Paint.FontMetrics()
    private var lastEpochSecond = Long.MIN_VALUE
    private var lastText = ""

    fun draw(canvas: Canvas, width: Int, height: Int, now: ZonedDateTime = ZonedDateTime.now()) {
        val textSize = StampLayout.textSizePx(width, height, style.sizePercent)
        if (paint.textSize != textSize) {
            paint.textSize = textSize
            paint.setShadowLayer(textSize * 0.08f, 0f, textSize * 0.04f, style.color.shadowArgb)
        }
        val text = textFor(now)
        paint.getFontMetrics(fontMetrics)
        val origin = StampLayout.textOrigin(
            width, height, paint.measureText(text), fontMetrics.ascent, fontMetrics.descent, textSize, style.position,
        )
        canvas.drawText(text, origin.x, origin.baseline, paint)
    }

    /** 毎フレーム文字列を作らないよう、秒が変わったときだけ整形し直す。 */
    private fun textFor(now: ZonedDateTime): String {
        val second = now.toEpochSecond()
        if (second != lastEpochSecond) {
            lastEpochSecond = second
            lastText = formatter.format(now)
        }
        return lastText
    }

    companion object {
        fun resolveTypeface(context: Context, font: StampFont): Typeface = when (font) {
            StampFont.GOTHIC -> Typeface.SANS_SERIF
            StampFont.MINCHO -> Typeface.SERIF
            StampFont.MONO -> Typeface.MONOSPACE
            StampFont.DIGITAL -> ResourcesCompat.getFont(context, R.font.dseg7_classic_regular) ?: Typeface.MONOSPACE
        }
    }
}
