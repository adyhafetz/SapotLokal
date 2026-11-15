package com.sapotlokal.ui.vendor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.sapotlokal.R
import kotlin.math.max

private fun Context.dp(v: Float) = v * resources.displayMetrics.density

/** Simple bar chart in the brand colours: soft-blue track, cocoa bar. No chart library needed. */
class BarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values: List<Int> = emptyList()
    private var labels: List<String> = emptyList()

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.sapot_blue) }
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.sapot_primary) }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.sapot_text_secondary)
        textSize = context.dp(11f)
        textAlign = Paint.Align.CENTER
    }

    fun setData(values: List<Int>, labels: List<String>) {
        this.values = values
        this.labels = labels
        invalidate()
    }

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(
            resolveSize(suggestedMinimumWidth, w),
            resolveSize(context.dp(190f).toInt(), h)
        )
    }

    override fun onDraw(c: Canvas) {
        if (values.isEmpty()) return
        val labelH = context.dp(22f)
        val chartH = height - labelH
        val slot = width.toFloat() / values.size
        val barW = slot * 0.46f
        val maxV = max(values.maxOrNull() ?: 0, 1)
        val r = context.dp(8f)

        values.forEachIndexed { i, v ->
            val cx = slot * i + slot / 2f
            val rect = RectF(cx - barW / 2f, 0f, cx + barW / 2f, chartH)
            c.drawRoundRect(rect, r, r, track)
            if (v > 0) {
                val h = max(chartH * v / maxV, r * 2f)
                c.drawRoundRect(RectF(rect.left, chartH - h, rect.right, chartH), r, r, bar)
            }
            labels.getOrNull(i)?.let { c.drawText(it, cx, height - context.dp(5f), text) }
        }
    }
}

/** Smooth area/line chart for the monthly trend. */
class LineChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values: List<Int> = emptyList()
    private var labels: List<String> = emptyList()

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.sapot_primary)
        style = Paint.Style.STROKE
        strokeWidth = context.dp(2.5f)
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.sapot_blue)
        alpha = 150
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.sapot_primary) }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.sapot_border)
        strokeWidth = context.dp(1f)
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.sapot_text_secondary)
        textSize = context.dp(11f)
    }

    fun setData(values: List<Int>, labels: List<String>) {
        this.values = values
        this.labels = labels
        invalidate()
    }

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(
            resolveSize(suggestedMinimumWidth, w),
            resolveSize(context.dp(190f).toInt(), h)
        )
    }

    override fun onDraw(c: Canvas) {
        if (values.size < 2) return
        val left = context.dp(30f)
        val right = width - context.dp(26f)
        val top = context.dp(8f)
        val bottom = height - context.dp(24f)
        val maxV = max(values.maxOrNull() ?: 0, 3)

        // Horizontal grid lines with y labels (0 up to max)
        for (i in 0..3) {
            val y = bottom - (bottom - top) * i / 3f
            c.drawLine(left, y, right, y, grid)
            text.textAlign = Paint.Align.RIGHT
            c.drawText((maxV * i / 3).toString(), left - context.dp(6f), y + context.dp(4f), text)
        }

        val step = (right - left) / (values.size - 1)
        val xs = values.indices.map { left + step * it }
        val ys = values.map { bottom - (bottom - top) * it / maxV }

        val path = Path().apply {
            moveTo(xs[0], ys[0])
            for (i in 1 until xs.size) {
                val mx = (xs[i - 1] + xs[i]) / 2f
                cubicTo(mx, ys[i - 1], mx, ys[i], xs[i], ys[i])
            }
        }
        val area = Path(path).apply {
            lineTo(xs.last(), bottom)
            lineTo(xs.first(), bottom)
            close()
        }
        c.drawPath(area, fill)
        c.drawPath(path, line)
        xs.indices.forEach { c.drawCircle(xs[it], ys[it], context.dp(3.5f), dot) }

        text.textAlign = Paint.Align.CENTER
        labels.forEachIndexed { i, l -> c.drawText(l, xs[i], height - context.dp(6f), text) }
    }
}
