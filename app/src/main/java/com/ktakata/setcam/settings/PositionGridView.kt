package com.ktakata.setcam.settings

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.android.material.color.MaterialColors
import com.ktakata.setcam.stamp.StampPosition

/** 縦長の枠を 3×3 に区切り、選ばれたマスを塗る。タップでマスを選ぶ。 */
class PositionGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var selected: StampPosition = StampPosition.BOTTOM_RIGHT
        set(value) {
            field = value
            invalidate()
        }

    var onSelect: ((StampPosition) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = 0xFF9E9E9E.toInt()
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(this@PositionGridView, androidx.appcompat.R.attr.colorPrimary)
    }

    override fun onDraw(canvas: Canvas) {
        val cellW = width / 3f
        val cellH = height / 3f
        val inset = 3f * density
        fillPaint.alpha = if (isEnabled) 255 else 90
        canvas.drawRect(
            selected.column * cellW + inset, selected.row * cellH + inset,
            (selected.column + 1) * cellW - inset, (selected.row + 1) * cellH - inset,
            fillPaint,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), linePaint)
        for (i in 1..2) {
            canvas.drawLine(i * cellW, 0f, i * cellW, height.toFloat(), linePaint)
            canvas.drawLine(0f, i * cellH, width.toFloat(), i * cellH, linePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                val column = (event.x / (width / 3f)).toInt().coerceIn(0, 2)
                val row = (event.y / (height / 3f)).toInt().coerceIn(0, 2)
                performClick()
                onSelect?.invoke(StampPosition.of(column, row))
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
