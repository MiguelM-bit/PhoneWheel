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

    private val baseColor = Color.rgb(35, 35, 35)         // #232323 - darker, cleaner
    private val activeColor = Color.rgb(229, 57, 53)      // #E53935 - red accent
    private val borderColor = Color.rgb(60, 60, 60)       // #3C3C3C - subtle border
    private val activeBorderColor = Color.rgb(255, 82, 82) // #FF5252 - brighter red border for active
    private val textColor = Color.rgb(224, 224, 224)      // #E0E0E0

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

        val armWidth = radius * 0.45f
        val armLength = radius * 0.95f
        val cornerRadius = armWidth / 2f

        // Helper to compute the RectF for a specific direction's arm
        fun armRect(direction: DPadDirection): RectF = when (direction) {
            DPadDirection.UP -> RectF(cx - armWidth / 2f, cy - armLength, cx + armWidth / 2f, cy)
            DPadDirection.DOWN -> RectF(cx - armWidth / 2f, cy, cx + armWidth / 2f, cy + armLength)
            DPadDirection.LEFT -> RectF(cx - armLength, cy - armWidth / 2f, cx, cy + armWidth / 2f)
            DPadDirection.RIGHT -> RectF(cx, cy - armWidth / 2f, cx + armLength, cy + armWidth / 2f)
        }

        fun isPressed(dir: DPadDirection) = buttonTracker.isPressed(dir.buttonId)

        // --- Layer 1: Outer glow for active directions (subtle red halo behind arms) ---
        val glowSize = armWidth * 0.12f
        paint.style = Paint.Style.FILL
        for (dir in DPadDirection.entries) {
            if (isPressed(dir)) {
                paint.color = Color.argb(50, 229, 57, 53) // subtle transparent red
                val rect = armRect(dir)
                canvas.drawRoundRect(
                    RectF(
                        rect.left - glowSize, rect.top - glowSize,
                        rect.right + glowSize, rect.bottom + glowSize
                    ),
                    cornerRadius + glowSize, cornerRadius + glowSize, paint
                )
            }
        }

        // --- Layer 2: Base cross fill (horizontal + vertical bars) ---
        paint.color = baseColor
        canvas.drawRoundRect(
            RectF(cx - armLength, cy - armWidth / 2f, cx + armLength, cy + armWidth / 2f),
            cornerRadius, cornerRadius, paint
        )
        canvas.drawRoundRect(
            RectF(cx - armWidth / 2f, cy - armLength, cx + armWidth / 2f, cy + armLength),
            cornerRadius, cornerRadius, paint
        )

        // --- Layer 3: Active direction fills (higher opacity/saturation) ---
        paint.color = activeColor
        for (dir in DPadDirection.entries) {
            if (isPressed(dir)) {
                canvas.drawRoundRect(armRect(dir), cornerRadius, cornerRadius, paint)
            }
        }

        // --- Layer 4: Inner highlight for active directions (subtle lighter shade) ---
        val highlightInset = armWidth * 0.15f
        val highlightRadius = maxOf(0f, cornerRadius - highlightInset)
        paint.color = Color.argb(55, 255, 170, 170) // subtle pinkish-white overlay
        for (dir in DPadDirection.entries) {
            if (isPressed(dir)) {
                val rect = armRect(dir)
                canvas.drawRoundRect(
                    RectF(
                        rect.left + highlightInset, rect.top + highlightInset,
                        rect.right - highlightInset, rect.bottom - highlightInset
                    ),
                    highlightRadius, highlightRadius, paint
                )
            }
        }

        // --- Layer 5: Base border (stroke 2f, subtle borderColor) ---
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = borderColor
        canvas.drawRoundRect(
            RectF(cx - armLength, cy - armWidth / 2f, cx + armLength, cy + armWidth / 2f),
            cornerRadius, cornerRadius, paint
        )
        canvas.drawRoundRect(
            RectF(cx - armWidth / 2f, cy - armLength, cx + armWidth / 2f, cy + armLength),
            cornerRadius, cornerRadius, paint
        )

        // --- Layer 6: Active arm borders (stroke 3f, brighter activeBorderColor) ---
        paint.strokeWidth = 3f
        paint.color = activeBorderColor
        for (dir in DPadDirection.entries) {
            if (isPressed(dir)) {
                canvas.drawRoundRect(armRect(dir), cornerRadius, cornerRadius, paint)
            }
        }

        // --- Layer 7: Arrow indicators ---
        paint.style = Paint.Style.FILL
        paint.textSize = armWidth * 0.55f
        paint.textAlign = Paint.Align.CENTER
        paint.color = textColor
        val arrowBaseline = cy - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText("▲", cx, cy - armLength * 0.55f, paint)
        canvas.drawText("▼", cx, cy + armLength * 0.55f, paint)
        canvas.drawText("◀", cx - armLength * 0.55f, arrowBaseline, paint)
        canvas.drawText("▶", cx + armLength * 0.55f, arrowBaseline, paint)
    }
}