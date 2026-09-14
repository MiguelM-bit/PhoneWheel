package com.phonewheel.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * View customizada que desenha um volante (aro, raios, cubo e marcador)
 * e gira conforme o ângulo de direção, espelhando o desenho do app Windows.
 */
class SteeringWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Cores (mesma paleta do WPF)
    private val outerRingColor = Color.rgb(43, 43, 43)      // #2B2B2B
    private val innerRingColor = Color.rgb(58, 58, 58)      // #3A3A3A
    private val wheelFillColor = Color.rgb(21, 21, 21)      // #151515
    private val spokeColor = Color.rgb(43, 43, 43)          // #2B2B2B
    private val hubColor = Color.rgb(58, 58, 58)            // #3A3A3A
    private val hubInnerColor = Color.rgb(74, 74, 74)       // #4A4A4A
    private val markerColor = Color.rgb(229, 57, 53)        // #E53935

    private var angle = 0f

    /**
     * Define o ângulo de rotação do volante em graus.
     */
    fun setAngle(newAngle: Float) {
        if (angle != newAngle) {
            angle = newAngle
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) / 2f
        if (radius <= 0f) return

        canvas.save()
        canvas.rotate(angle, cx, cy)

        // Aro externo
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = radius * 0.15f
        paint.color = outerRingColor
        canvas.drawCircle(cx, cy, radius - paint.strokeWidth / 2f, paint)

        // Preenchimento interno
        paint.style = Paint.Style.FILL
        paint.color = wheelFillColor
        canvas.drawCircle(cx, cy, radius * 0.8f, paint)

        // Aro interno (contorno)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = innerRingColor
        canvas.drawCircle(cx, cy, radius * 0.8f, paint)

        // Raios
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = radius * 0.09f
        paint.color = spokeColor
        paint.strokeCap = Paint.Cap.ROUND
        // Raio superior
        canvas.drawLine(cx, cy, cx, cy - radius * 0.8f, paint)
        // Raio direito
        canvas.drawLine(cx, cy, cx + radius * 0.68f, cy - radius * 0.4f, paint)
        // Raio esquerdo
        canvas.drawLine(cx, cy, cx - radius * 0.68f, cy - radius * 0.4f, paint)

        // Cubo central
        paint.style = Paint.Style.FILL
        paint.color = hubColor
        canvas.drawCircle(cx, cy, radius * 0.19f, paint)
        paint.color = hubInnerColor
        canvas.drawCircle(cx, cy, radius * 0.13f, paint)

        // Marcador superior (gira com o volante)
        paint.color = markerColor
        canvas.drawCircle(cx, cy - radius * 0.85f, radius * 0.045f, paint)

        canvas.restore()
    }
}