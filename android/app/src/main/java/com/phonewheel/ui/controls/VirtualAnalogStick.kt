package com.phonewheel.ui.controls

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.phonewheel.model.StickId
import com.phonewheel.network.AxisSender
import kotlin.math.min

/**
 * Analógico virtual reutilizável, independente da tela.
 *
 * Responsabilidades:
 * - Área circular com knob móvel controlado por toque;
 * - Produzir valores normalizados X/Y em [-1.0, +1.0];
 * - Retorno automático ao centro ao soltar;
 * - Enviar apenas mudanças relevantes via [AxisSender] (opcional/nullable);
 * - Não conhece detalhes do Windows: apenas produz eventos
 *   LeftStickX/LeftStickY/RightStickX/RightStickY (via [StickId]).
 *
 * Configuração:
 * - [stickId]: qual analógico este componente representa (LEFT ou RIGHT);
 * - [axisSender]: camada de envio (pode ser `null` para uso sem rede).
 */
class VirtualAnalogStick @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var stickId: StickId = StickId.LEFT
    var axisSender: AxisSender? = null

        /**
             * Modo Volante: quando ativado, o toque não altera o valor (o giroscópio
             * controla o eixo X via [setWheelValue]) e o visual fica destacado.
             */
            var wheelMode: Boolean = false

            private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            private val model = AnalogStickModel()

    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    private var baseRadius = 0f
    private var knobRadius = 0f

    private val baseColor = Color.rgb(43, 43, 43)      // #2B2B2B
    private val baseBorderColor = Color.rgb(58, 58, 58) // #3A3A3A
    private val knobColor = Color.rgb(58, 58, 58)      // #3A3A3A
    private val knobActiveColor = Color.rgb(229, 57, 53) // #E53935
    private val knobBorderColor = Color.rgb(74, 74, 74) // #4A4A4A

    /**
     * Valores normalizados atuais (para leitura externa, ex.: testes).
     */
        val stickX: Float
        get() = model.x

        val stickY: Float
        get() = model.y

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        baseRadius = min(w, h) / 2f * 0.9f
        knobRadius = baseRadius * 0.35f
        // O knob percorre a base sem ultrapassar a borda.
        model.radius = baseRadius - knobRadius
    }

    /**
         * Define o valor do volante (Modo Volante): atualiza apenas o eixo X
         * normalizado em [-1.0, +1.0] e centraliza Y. Não envia eixos pela rede.
         */
        fun setWheelValue(normalizedX: Float) {
            val clamped = normalizedX.coerceIn(-1f, 1f)
            if (model.update(clamped * model.radius, 0f)) {
                invalidate()
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            // No Modo Volante o toque não altera o valor: o giroscópio controla o volante.
            if (wheelMode) return false

            when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
                    activePointerId = event.getPointerId(0)
                    updateStick(event.getX(0), event.getY(0))
                    performClick()
                }
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Primeiro ponteiro vence; ponteiros adicionais são ignorados.
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(activePointerId)
                if (index >= 0) {
                    updateStick(event.getX(index), event.getY(index))
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerId = event.getPointerId(event.actionIndex)
                if (pointerId == activePointerId) {
                    centerStick()
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activePointerId != MotionEvent.INVALID_POINTER_ID) {
                    centerStick()
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateStick(touchX: Float, touchY: Float) {
        val cx = width / 2f
        val cy = height / 2f
        if (model.update(touchX - cx, touchY - cy)) {
            axisSender?.send(stickId.xAxis, model.x)
            axisSender?.send(stickId.yAxis, model.y)
            invalidate()
        }
    }

    private fun centerStick() {
        if (model.center()) {
            axisSender?.sendCenter(stickId.xAxis)
            axisSender?.sendCenter(stickId.yAxis)
            invalidate()
        }
    }

        /**
         * Centraliza o analógico e limpa o ponteiro ativo.
         * Usado ao sair da tela/desconectar para não deixar o eixo "preso".
         */
        fun reset() {
            centerStick()
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
        if (baseRadius <= 0f) return

        // Base circular
        paint.style = Paint.Style.FILL
        paint.color = baseColor
        canvas.drawCircle(cx, cy, baseRadius, paint)

        // Borda da base (vermelha no Modo Volante)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
                paint.color = if (wheelMode) knobActiveColor else baseBorderColor
        canvas.drawCircle(cx, cy, baseRadius, paint)

        // Guias cruzadas
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = baseBorderColor
        canvas.drawLine(cx - baseRadius, cy, cx + baseRadius, cy, paint)
        canvas.drawLine(cx, cy - baseRadius, cx, cy + baseRadius, paint)

        // Knob
        val knobX = cx + model.x * model.radius
        val knobY = cy + model.y * model.radius
        val active = activePointerId != MotionEvent.INVALID_POINTER_ID

        paint.style = Paint.Style.FILL
                paint.color = if (active) knobActiveColor else if (wheelMode) knobActiveColor else knobColor
        canvas.drawCircle(knobX, knobY, knobRadius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = knobBorderColor
        canvas.drawCircle(knobX, knobY, knobRadius, paint)
    }
}