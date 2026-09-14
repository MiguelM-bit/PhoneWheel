package com.phonewheel.ui.controls

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.phonewheel.network.ButtonSender
import kotlin.math.min

/**
 * D-pad virtual reutilizável, independente da tela.
 *
 * Responsabilidades:
 * - Quatro direções (Up/Down/Left/Right) com zonas por quadrante;
 * - Pressionar e liberar cada direção corretamente (via [ButtonSender],
 *   usando o [ButtonPacket] existente);
 * - Suporte a múltiplos toques simultâneos (um ponteiro por direção);
 * - Evitar eventos duplicados (via [ButtonStateTracker]).
 *
 * Configuração:
 * - [sender]: camada de envio (pode ser `null` para uso sem rede).
 */
class VirtualDPad @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var sender: ButtonSender? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pointerTracker = DPadPointerTracker()
    private val buttonTracker = ButtonStateTracker()

    private val baseColor = Color.rgb(43, 43, 43)      // #2B2B2B
    private val activeColor = Color.rgb(229, 57, 53)   // #E53935
    private val borderColor = Color.rgb(58, 58, 58)    // #3A3A3A
    private val textColor = Color.rgb(224, 224, 224)   // #E0E0E0

    /**
     * Direções atualmente pressionadas (para leitura externa, ex.: testes).
     */
    val pressedDirections: Set<DPadDirection>
        get() = DPadDirection.entries.filter { buttonTracker.isPressed(it.buttonId) }.toSet()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handlePointerDown(event, 0)
                performClick()
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                handlePointerDown(event, event.actionIndex)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(i)
                    val direction = directionAt(event.getX(i), event.getY(i))
                    val result = pointerTracker.onPointerMove(pointerId, direction)
                    result.released?.let { releaseDirection(it) }
                    result.pressed?.let { pressDirection(it) }
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerId = event.getPointerId(event.actionIndex)
                pointerTracker.onPointerUp(pointerId)?.let { releaseDirection(it) }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                releaseAll()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handlePointerDown(event: MotionEvent, index: Int) {
        val pointerId = event.getPointerId(index)
        val direction = directionAt(event.getX(index), event.getY(index)) ?: return
        pointerTracker.onPointerDown(pointerId, direction)?.let { pressDirection(it) }
    }

    private fun directionAt(x: Float, y: Float): DPadDirection? {
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) / 2f
        return DPadGeometry.directionAt(x - cx, y - cy, radius)
    }

    private fun pressDirection(direction: DPadDirection) {
        if (buttonTracker.press(direction.buttonId)) {
            sender?.press(direction.buttonId)
            invalidate()
        }
    }

    private fun releaseDirection(direction: DPadDirection) {
        if (buttonTracker.release(direction.buttonId)) {
            sender?.release(direction.buttonId)
            invalidate()
        }
    }

    /**
     * Libera todas as direções pressionadas (usado ao desconectar).
     */
    fun releaseAll() {
        for (direction in pointerTracker.releaseAll()) {
            releaseDirection(direction)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) / 2f
        if (radius <= 0f) return

        val armWidth = radius * 0.5f
        val armLength = radius * 0.95f

        // Cruz base (barra horizontal + vertical)
        paint.style = Paint.Style.FILL
        paint.color = baseColor
        canvas.drawRoundRect(
            RectF(cx - armLength, cy - armWidth / 2f, cx + armLength, cy + armWidth / 2f),
            armWidth / 2f, armWidth / 2f, paint
        )
        canvas.drawRoundRect(
            RectF(cx - armWidth / 2f, cy - armLength, cx + armWidth / 2f, cy + armLength),
            armWidth / 2f, armWidth / 2f, paint
        )

        // Destaque das direções ativas
        paint.color = activeColor
        if (buttonTracker.isPressed(DPadDirection.UP.buttonId)) {
            canvas.drawRoundRect(
                RectF(cx - armWidth / 2f, cy - armLength, cx + armWidth / 2f, cy),
                armWidth / 2f, armWidth / 2f, paint
            )
        }
        if (buttonTracker.isPressed(DPadDirection.DOWN.buttonId)) {
            canvas.drawRoundRect(
                RectF(cx - armWidth / 2f, cy, cx + armWidth / 2f, cy + armLength),
                armWidth / 2f, armWidth / 2f, paint
            )
        }
        if (buttonTracker.isPressed(DPadDirection.LEFT.buttonId)) {
            canvas.drawRoundRect(
                RectF(cx - armLength, cy - armWidth / 2f, cx, cy + armWidth / 2f),
                armWidth / 2f, armWidth / 2f, paint
            )
        }
        if (buttonTracker.isPressed(DPadDirection.RIGHT.buttonId)) {
            canvas.drawRoundRect(
                RectF(cx, cy - armWidth / 2f, cx + armLength, cy + armWidth / 2f),
                armWidth / 2f, armWidth / 2f, paint
            )
        }

        // Borda da cruz
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = borderColor
        canvas.drawRoundRect(
            RectF(cx - armLength, cy - armWidth / 2f, cx + armLength, cy + armWidth / 2f),
            armWidth / 2f, armWidth / 2f, paint
        )
        canvas.drawRoundRect(
            RectF(cx - armWidth / 2f, cy - armLength, cx + armWidth / 2f, cy + armLength),
            armWidth / 2f, armWidth / 2f, paint
        )

        // Setas indicadoras
        paint.style = Paint.Style.FILL
        paint.textSize = armWidth * 0.5f
        paint.textAlign = Paint.Align.CENTER
        paint.color = textColor
        val arrowBaseline = cy - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText("▲", cx, cy - armLength * 0.55f, paint)
        canvas.drawText("▼", cx, cy + armLength * 0.55f, paint)
        canvas.drawText("◀", cx - armLength * 0.55f, arrowBaseline, paint)
        canvas.drawText("▶", cx + armLength * 0.55f, arrowBaseline, paint)
    }
}