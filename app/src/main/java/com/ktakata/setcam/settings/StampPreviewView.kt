package com.ktakata.setcam.settings

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.ktakata.setcam.stamp.StampRenderer
import com.ktakata.setcam.stamp.StampStyle
import java.time.ZonedDateTime

/** 縦長 9:16 の灰色の枠に、録画と同じ StampRenderer で日時を描く。 */
class StampPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val backgroundPaint = Paint().apply { color = 0xFF808080.toInt() }
    private var renderer: StampRenderer? = null

    var style: StampStyle? = null
        set(value) {
            field = value
            renderer = value?.let { StampRenderer(context, it) }
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(height * 9 / 16, height)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        renderer?.draw(canvas, width, height, ZonedDateTime.now())
    }
}
