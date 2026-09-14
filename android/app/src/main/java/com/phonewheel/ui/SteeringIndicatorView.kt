package com.phonewheel.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * Indicador horizontal do volante.
 *
 * Representa o valor normalizado do steering em [-1.0, +1.0]:
 * - trilho horizontal;
 * - marcador central fixo;
 * - indicador móvel que acompanha o valor;
 * - setas de direção esquerda/direita (a seta do lado ativo acende).
 */
class SteeringIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val trackColor = Color.rgb(43, 43, 43)          // #2B2B2B
    private val centerMarkerColor = Color.rgb(158, 158, 158) // #9E9E9E
    private val indicatorColor = Color.rgb(229, 57, 53)     // #E53935
    private val arrowColor = Color.rgb(158, 158, 158)       // #9E9E9E
    private val arrowActiveColor = Color.rgb(229, 57, 53)   // #E53935

    private var value = 0f

    /**
     * Define o valor normalizado do volante em [-1.0, +1.0].
     */
    fun setValue(newValue: Float) {
        val clamped = newValue.coerceIn(-1f, 1f)
        if (clamped != value) {
            value = clamped
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val cy = h / 2f
        val trackHeight = h * 0.28f
        val trackTop = (h - trackHeight) / 2f
        val trackLeft = w * 0.14f
        val trackRight = w * 0.86f
        val trackWidth = trackRight - trackLeft

        // Trilho
        paint.style = Paint.Style.FILL
        paint.color = trackColor
        canvas.drawRoundRect(
            trackLeft, trackTop, trackRight, trackTop + trackHeight,
            trackHeight / 2f, trackHeight / 2f, paint
        )

        // Marcador central fixo
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = centerMarkerColor
        canvas.drawLine(
            w / 2f, trackTop - h * 0.12f,
            w / 2f, trackTop + trackHeight + h * 0.12f, paint
        )

        // Indicador móvel (acompanha o valor normalizado)
        val indicatorX = trackLeft + (value + 1f) / 2f * trackWidth
        paint.style = Paint.Style.FILL
        paint.color = indicatorColor
        canvas.drawCircle(indicatorX, cy, trackHeight * 0.75f, paint)

        // Setas de direção
        drawArrow(canvas, w * 0.07f, cy, pointingLeft = true, active = value < 0f)
        drawArrow(canvas, w * 0.93f, cy, pointingLeft = false, active = value > 0f)
    }

    private fun drawArrow(canvas: Canvas, x: Float, y: Float, pointingLeft: Boolean, active: Boolean) {
        val size = height * 0.16f
        val path = Path()
        if (pointingLeft) {
            path.moveTo(x, y)
            path.lineTo(x + size, y - size)
            path.lineTo(x + size, y + size)
        } else {
            path.moveTo(x, y)
            path.lineTo(x - size, y - size)
            path.lineTo(x - size, y + size)
        }
        path.close()

        paint.style = Paint.Style.FILL
        paint.color = if (active) arrowActiveColor else arrowColor
        canvas.drawPath(path, paint)
    }
}