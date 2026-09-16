package com.phonewheel.ui.controls

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.phonewheel.network.ButtonSender
import kotlin.math.min

/**
 * Botão virtual reutilizável, independente da tela.
 *
 * Responsabilidades:
 * - Detectar pressionamento e liberação (primeiro ponteiro vence);
 * - Fornecer feedback visual (pressionado/solto);
 * - Enviar apenas mudanças de estado via [ButtonSender] (opcional/nullable,
 *   permitindo uso sem rede).
 *
 * Configuração:
 * - [buttonId]: identificador digital do protocolo (A=0, B=1, X=2, Y=3,
 *   LB=4, RB=5, LS=6, RS=7, Back=8, Start=9);
 * - [label]: texto exibido no centro do botão;
 * - [sender]: camada de envio (pode ser `null` para uso sem rede).
 */
class VirtualButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var buttonId: Int = 0
    var label: String = ""
    var sender: ButtonSender? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tracker = ButtonStateTracker()

    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    private val baseColor = Color.rgb(35, 35, 35)        // #232323 - darker, cleaner
    private val pressedColor = Color.rgb(229, 57, 53)    // #E53935 - red accent
    private val borderColor = Color.rgb(60, 60, 60)      // #3C3C3C - subtle border
    private val pressedBorderColor = Color.rgb(229, 57, 53) // #E53935 - red border when pressed
    private val textColor = Color.rgb(224, 224, 224)     // #E0E0E0

    /**
     * Estado atual do botão (para leitura externa, ex.: testes).
     */
        val pressed: Boolean
        get() = tracker.isPressed(buttonId)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
                    activePointerId = event.getPointerId(0)
                    setPressedState(true)
                    performClick()
                }
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Primeiro ponteiro vence; ponteiros adicionais são ignorados.
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // Mantém pressionado enquanto o ponteiro ativo estiver na tela.
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerId = event.getPointerId(event.actionIndex)
                if (pointerId == activePointerId) {
                    setPressedState(false)
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activePointerId != MotionEvent.INVALID_POINTER_ID) {
                    setPressedState(false)
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun setPressedState(pressed: Boolean) {
        val changed = if (pressed) tracker.press(buttonId) else tracker.release(buttonId)
        if (changed) {
            if (pressed) sender?.press(buttonId) else sender?.release(buttonId)
            invalidate()
        }
    }

        /**
         * Libera o botão se estiver pressionado e limpa o ponteiro ativo.
         * Usado ao sair da tela/desconectar para não deixar o botão "preso".
         */
        fun release() {
            if (tracker.isPressed(buttonId)) {
                setPressedState(false)
            }
            activePointerId = MotionEvent.INVALID_POINTER_ID
        }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) / 2f - 2f
        if (radius <= 0f) return

        val pressed = tracker.isPressed(buttonId)

        // Visual radius: 90% when pressed (scale down), full size when not pressed
        val visualRadius = if (pressed) radius * 0.9f else radius

        // Outer glow ring when pressed (subtle semi-transparent circle behind)
        if (pressed) {
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(40, 229, 57, 53) // semi-transparent red glow
            canvas.drawCircle(cx, cy, radius * 1.05f, paint)
        }

        // Main circle
        paint.style = Paint.Style.FILL
        paint.color = if (pressed) pressedColor else baseColor
        canvas.drawCircle(cx, cy, visualRadius, paint)

        // Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (pressed) 3f else 2f
        paint.color = if (pressed) pressedBorderColor else borderColor
        canvas.drawCircle(cx, cy, visualRadius, paint)

        // Label
        if (label.isNotEmpty()) {
            paint.style = Paint.Style.FILL
            paint.color = if (pressed) Color.WHITE else textColor
            paint.textSize = radius * 0.65f
            paint.textAlign = Paint.Align.CENTER
            val baseline = cy - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(label, cx, baseline, paint)
        }
    }
}