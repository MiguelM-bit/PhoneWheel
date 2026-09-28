package com.phonewheel.ui.controls

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Overlay de configurações do controller — tela cheia.
 *
 * Preenche toda a tela com fundo escuro semi-transparente e exibe
 * as configurações organizadas em seções, com controles grandes
 * e fáceis de tocar. Bloqueia interações com os controles subjacentes.
 *
 * Desenhado inteiramente via Canvas (sem inflação XML).
 */
class SettingsOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface Callback {
        fun onSettingsClose()
        fun onRecenterWheel()
        fun onToggleInvertWheel(enabled: Boolean)
        fun onToggleInvertLeftX(enabled: Boolean)
        fun onToggleInvertLeftY(enabled: Boolean)
        fun onToggleInvertRightX(enabled: Boolean)
        fun onToggleInvertRightY(enabled: Boolean)
        fun onSensitivityChanged(value: Float)
        fun onToggleConfirmDisconnect(enabled: Boolean)
    }

    var callback: Callback? = null

    private val density = resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    // --- State ---
    var invertWheel: Boolean = false
    var invertLeftX: Boolean = false
    var invertLeftY: Boolean = false
    var invertRightX: Boolean = false
    var invertRightY: Boolean = false
    var sensitivityValue: Float = 2.0f
    var confirmDisconnect: Boolean = true

    // --- Scroll / gesture state ---
    private var scrollOffsetY: Float = 0f
    private var contentHeight: Float = 0f
    private var downX: Float = 0f
    private var downY: Float = 0f
    private var downScrollY: Float = 0f
    private var dragging: Boolean = false
    private var moved: Boolean = false
    private var sliderDragging: Boolean = false

    // --- Paints ---
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(24, 24, 24) // solid dark background
        style = Paint.Style.FILL
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(224, 224, 224)
        textSize = 26f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.LEFT
    }

    private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(158, 158, 158)
        textSize = 14f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.LEFT
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(224, 224, 224)
        textSize = 18f * density
        textAlign = Paint.Align.LEFT
    }

    private val toggleBgOnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(229, 57, 53)
        style = Paint.Style.FILL
    }

    private val toggleTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(55, 55, 55)
        style = Paint.Style.FILL
    }

    private val toggleKnobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(240, 240, 240)
        style = Paint.Style.FILL
    }

    private val recenterBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(229, 57, 53)
        style = Paint.Style.FILL
    }

    private val recenterTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(50, 50, 50)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val backPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(158, 158, 158)
        textSize = 16f * density
        textAlign = Paint.Align.LEFT
    }

    private val sliderValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 16f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    // --- Layout constants ---
    private val padHorizontal = 40f * density
    private val padTop = 20f * density
    private val padBottom = 40f * density
    private val labelInset = 8f * density
    private val toggleWidth = 56f * density
    private val toggleHeight = 30f * density
    private val toggleCorner = 15f * density
    private val recenterHeight = 52f * density
    private val recenterCorner = 10f * density
    private val rowHeight = 56f * density
    private val headerBaselineY = 44f * density
    private val headerBottomY = 80f * density
    private val titleOffsetX = 120f * density
    private val sliderRowHeight = 72f * density
    private val sliderTrackHeight = 8f * density
    private val sliderThumbRadius = 14f * density
    private val sliderValueTextSize = sliderValuePaint.textSize

    // Touch regions
    private val backRegion = RectF(padHorizontal, 16f * density, padHorizontal + 100f * density, 56f * density)
    private var recenterRegion = RectF()
    private var invertWheelToggleRegion = RectF()
    private val stickToggleRegions = mutableListOf<RectF>()
    private var confirmDisconnectToggleRegion = RectF()
    private var sliderRegion = RectF()
    private var sliderTrackLeft = 0f
    private var sliderTrackRight = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val left = padHorizontal
        val right = w - padHorizontal

        // Full-screen background
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Top bar: ← Voltar | Configurações
        canvas.drawText("← Voltar", left, headerBaselineY, backPaint)
        canvas.drawText("Configurações", left + titleOffsetX, headerBaselineY, titlePaint)
        canvas.drawLine(left, headerBottomY, right, headerBottomY, dividerPaint)

        canvas.save()
        canvas.clipRect(0f, headerBottomY, w, h)
        canvas.translate(0f, -scrollOffsetY)

        var y = headerBottomY + padTop

        // === VOLANTE ===
        canvas.drawText("VOLANTE", left, y + 14f * density, sectionPaint)
        y += 32f * density

        // Recentrar volante
        recenterRegion = RectF(left, y, right, y + recenterHeight)
        canvas.drawRoundRect(recenterRegion, recenterCorner, recenterCorner, recenterBgPaint)
        canvas.drawText("Recentrar volante", recenterRegion.centerX(), recenterRegion.centerY() + 6f * density, recenterTextPaint)
        y += recenterHeight + 12f * density

        // Inverter volante row
        canvas.drawText("Inverter volante", left + labelInset, y + rowHeight / 2f + 6f * density, labelPaint)
        invertWheelToggleRegion = RectF(right - toggleWidth, y + (rowHeight - toggleHeight) / 2f, right, y + (rowHeight + toggleHeight) / 2f)
        drawToggle(canvas, invertWheelToggleRegion, invertWheel)
        y += rowHeight

        // Divider
        y += 8f * density
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 20f * density

        // === ANALÓGICOS ===
        canvas.drawText("ANALÓGICOS", left, y + 14f * density, sectionPaint)
        y += 32f * density

        val stickLabels = listOf(
            "Inverter X — Analógico Esquerdo",
            "Inverter Y — Analógico Esquerdo",
            "Inverter X — Analógico Direito",
            "Inverter Y — Analógico Direito"
        )
        val stickValues = listOf(invertLeftX, invertLeftY, invertRightX, invertRightY)

        stickToggleRegions.clear()
        for (i in 0 until 4) {
            canvas.drawText(stickLabels[i], left + labelInset, y + rowHeight / 2f + 6f * density, labelPaint)
            stickToggleRegions.add(RectF(right - toggleWidth, y + (rowHeight - toggleHeight) / 2f, right, y + (rowHeight + toggleHeight) / 2f))
            drawToggle(canvas, stickToggleRegions[i], stickValues[i])
            y += rowHeight
        }

        // Divider
        y += 8f * density
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 20f * density

        // === SENSIBILIDADE DO VOLANTE ===
        canvas.drawText("SENSIBILIDADE DO VOLANTE", left, y + 14f * density, sectionPaint)
        y += 32f * density

        sliderTrackLeft = left
        sliderTrackRight = right
        sliderRegion = RectF(left - sliderThumbRadius, y, right + sliderThumbRadius, y + sliderRowHeight)

        val fraction = ((sensitivityValue - SENSITIVITY_MIN) / (SENSITIVITY_MAX - SENSITIVITY_MIN)).coerceIn(0f, 1f)
        val valueX = left + fraction * (right - left)
        val trackCy = y + 44f * density
        val trackTop = trackCy - sliderTrackHeight / 2f
        val trackBottom = trackCy + sliderTrackHeight / 2f

        canvas.drawRoundRect(left, trackTop, right, trackBottom, sliderTrackHeight / 2f, sliderTrackHeight / 2f, toggleTrackPaint)
        if (valueX > left) {
            canvas.drawRoundRect(left, trackTop, valueX, trackBottom, sliderTrackHeight / 2f, sliderTrackHeight / 2f, toggleBgOnPaint)
        }
        canvas.drawCircle(valueX, trackCy, sliderThumbRadius, toggleKnobPaint)

        val valueText = String.format(Locale.US, "%.1fx", sensitivityValue)
        canvas.drawText(valueText, valueX.coerceIn(left, right), y + sliderValueTextSize, sliderValuePaint)

        y += sliderRowHeight + 12f * density

        // Divider
        y += 8f * density
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 20f * density

        // === DESCONEÇÃO ===
        canvas.drawText("DESCONEÇÃO", left, y + 14f * density, sectionPaint)
        y += 32f * density

        canvas.drawText("Confirmar antes de desconectar", left + labelInset, y + rowHeight / 2f + 6f * density, labelPaint)
        confirmDisconnectToggleRegion = RectF(right - toggleWidth, y + (rowHeight - toggleHeight) / 2f, right, y + (rowHeight + toggleHeight) / 2f)
        drawToggle(canvas, confirmDisconnectToggleRegion, confirmDisconnect)
        y += rowHeight

        y += padBottom
        contentHeight = y

        canvas.restore()

        val limit = maxScroll()
        if (scrollOffsetY > limit) {
            scrollOffsetY = limit
            invalidate()
        }
    }

    private fun drawToggle(canvas: Canvas, rect: RectF, isOn: Boolean) {
        val trackPaint = if (isOn) toggleBgOnPaint else toggleTrackPaint
        canvas.drawRoundRect(rect, toggleCorner, toggleCorner, trackPaint)

        val knobRadius = toggleHeight / 2f - 3f
        val knobCx = if (isOn) rect.right - toggleHeight / 2f else rect.left + toggleHeight / 2f
        canvas.drawCircle(knobCx, rect.centerY(), knobRadius, toggleKnobPaint)
    }

    private fun maxScroll(): Float = (contentHeight - height).coerceAtLeast(0f)

    private fun updateSensitivityFromX(touchX: Float) {
        val span = sliderTrackRight - sliderTrackLeft
        if (span <= 0f) return

        val fraction = ((touchX - sliderTrackLeft) / span).coerceIn(0f, 1f)
        val raw = SENSITIVITY_MIN + fraction * (SENSITIVITY_MAX - SENSITIVITY_MIN)
        val value = ((raw * 10f).roundToInt() / 10f).coerceIn(SENSITIVITY_MIN, SENSITIVITY_MAX)

        if (value != sensitivityValue) {
            sensitivityValue = value
            callback?.onSensitivityChanged(value)
            invalidate()
        }
    }

    private fun handleContentTap(x: Float, contentY: Float) {
        if (recenterRegion.contains(x, contentY)) {
            callback?.onRecenterWheel()
            return
        }

        if (invertWheelToggleRegion.contains(x, contentY)) {
            invertWheel = !invertWheel
            callback?.onToggleInvertWheel(invertWheel)
            invalidate()
            return
        }

        for (i in stickToggleRegions.indices) {
            if (stickToggleRegions[i].contains(x, contentY)) {
                when (i) {
                    0 -> { invertLeftX = !invertLeftX; callback?.onToggleInvertLeftX(invertLeftX) }
                    1 -> { invertLeftY = !invertLeftY; callback?.onToggleInvertLeftY(invertLeftY) }
                    2 -> { invertRightX = !invertRightX; callback?.onToggleInvertRightX(invertRightX) }
                    3 -> { invertRightY = !invertRightY; callback?.onToggleInvertRightY(invertRightY) }
                }
                invalidate()
                return
            }
        }

        if (confirmDisconnectToggleRegion.contains(x, contentY)) {
            confirmDisconnect = !confirmDisconnect
            callback?.onToggleConfirmDisconnect(confirmDisconnect)
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downScrollY = scrollOffsetY
                dragging = false
                moved = false
                sliderDragging = false

                if (backRegion.contains(event.x, event.y)) {
                    callback?.onSettingsClose()
                    return true
                }

                val contentY = event.y + scrollOffsetY
                if (event.y >= headerBottomY && sliderRegion.contains(event.x, contentY)) {
                    sliderDragging = true
                    updateSensitivityFromX(event.x)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY

                if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                    moved = true
                }

                if (sliderDragging) {
                    if (abs(dy) > touchSlop && abs(dy) > abs(dx)) {
                        sliderDragging = false
                        dragging = true
                    } else {
                        updateSensitivityFromX(event.x)
                        return true
                    }
                }

                if (!dragging && abs(dy) > touchSlop) {
                    dragging = true
                }
                if (dragging) {
                    scrollOffsetY = (downScrollY - dy).coerceIn(0f, maxScroll())
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (sliderDragging) {
                    sliderDragging = false
                    return true
                }

                if (!moved && event.y >= headerBottomY) {
                    handleContentTap(event.x, event.y + scrollOffsetY)
                }
                dragging = false
                moved = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                moved = false
                sliderDragging = false
                return true
            }
        }
        return true
    }

    companion object {
        private const val SENSITIVITY_MIN = 0.5f
        private const val SENSITIVITY_MAX = 3.0f
    }
}
