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
        fun onSensitivityChanged(level: Int)
        fun onToggleConfirmDisconnect(enabled: Boolean)
    }

    var callback: Callback? = null

    // --- State ---
    var invertWheel: Boolean = false
    var invertLeftX: Boolean = false
    var invertLeftY: Boolean = false
    var invertRightX: Boolean = false
    var invertRightY: Boolean = false
    var sensitivityLevel: Int = 1
    var confirmDisconnect: Boolean = true

    // --- Paints ---
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(24, 24, 24) // solid dark background
        style = Paint.Style.FILL
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(224, 224, 224)
        textSize = 26f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.LEFT
    }

    private val closeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(158, 158, 158)
        textSize = 28f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(158, 158, 158)
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.LEFT
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(224, 224, 224)
        textSize = 18f
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

    private val segmentActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(229, 57, 53)
        style = Paint.Style.FILL
    }

    private val segmentInactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(40, 40, 40)
        style = Paint.Style.FILL
    }

    private val segmentBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(60, 60, 60)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val segmentTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(180, 180, 180)
        textSize = 16f
        textAlign = Paint.Align.CENTER
    }

    private val segmentTextActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val recenterBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(229, 57, 53)
        style = Paint.Style.FILL
    }

    private val recenterTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f
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
        textSize = 16f
        textAlign = Paint.Align.LEFT
    }

    // --- Layout constants ---
    private val padHorizontal = 40f
    private val padTop = 20f
    private val toggleWidth = 56f
    private val toggleHeight = 30f
    private val toggleCorner = 15f
    private val segmentHeight = 40f
    private val segmentCornerRadius = 8f
    private val recenterHeight = 52f
    private val recenterCorner = 10f
    private val rowHeight = 56f
    private val sectionGap = 16f

    // Touch regions
    private var closeRegion = RectF()
    private var backRegion = RectF()
    private var recenterRegion = RectF()
    private var toggleRegions = mutableListOf<RectF>()
    private var segmentRegions = mutableListOf<RectF>()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeTouchRegions(w, h)
    }

    private fun computeTouchRegions(w: Int, h: Int) {
        toggleRegions.clear()
        segmentRegions.clear()

        val left = padHorizontal
        val right = w - padHorizontal

        // Close button: top-right
        closeRegion = RectF(right - 40f, 16f, right, 56f)

        // Back: top-left
        backRegion = RectF(left, 16f, left + 80f, 56f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val left = padHorizontal
        val right = w - padHorizontal

        // Full-screen background
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Top bar: ← Voltar | Configurações | ✕
        canvas.drawText("← Voltar", left, 44f, backPaint)
        canvas.drawText("Configurações", left + 120f, 44f, titlePaint)
        canvas.drawText("✕", closeRegion.centerX(), 46f, closeTextPaint)

        var y = 80f

        // Divider
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 20f

        // === VOLANTE ===
        canvas.drawText("VOLANTE", left, y + 14f, sectionPaint)
        y += 32f

        // Recentrar volante
        recenterRegion = RectF(left, y, right, y + recenterHeight)
        canvas.drawRoundRect(recenterRegion, recenterCorner, recenterCorner, recenterBgPaint)
        canvas.drawText("Recentrar volante", recenterRegion.centerX(), recenterRegion.centerY() + 6f, recenterTextPaint)
        y += recenterHeight + 12f

        // Inverter volante row
        val invertWheelRow = RectF(left, y, right, y + rowHeight)
        canvas.drawText("Inverter volante", left + 8f, y + rowHeight / 2f + 6f, labelPaint)
        toggleRegions.clear()
        toggleRegions.add(RectF(right - toggleWidth, y + (rowHeight - toggleHeight) / 2f, right, y + (rowHeight + toggleHeight) / 2f))
        drawToggle(canvas, toggleRegions[0], invertWheel)
        y += rowHeight

        // Divider
        y += 8f
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 20f

        // === ANALÓGICOS ===
        canvas.drawText("ANALÓGICOS", left, y + 14f, sectionPaint)
        y += 32f

        val stickLabels = listOf(
            "Inverter X — Analógico Esquerdo",
            "Inverter Y — Analógico Esquerdo",
            "Inverter X — Analógico Direito",
            "Inverter Y — Analógico Direito"
        )
        val stickValues = listOf(invertLeftX, invertLeftY, invertRightX, invertRightY)

        for (i in 0 until 4) {
            canvas.drawText(stickLabels[i], left + 8f, y + rowHeight / 2f + 6f, labelPaint)
            toggleRegions.add(RectF(right - toggleWidth, y + (rowHeight - toggleHeight) / 2f, right, y + (rowHeight + toggleHeight) / 2f))
            drawToggle(canvas, toggleRegions[1 + i], stickValues[i])
            y += rowHeight
        }

        // Divider
        y += 8f
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 20f

        // === SENSIBILIDADE ===
        canvas.drawText("SENSIBILIDADE DO VOLANTE", left, y + 14f, sectionPaint)
        y += 32f

        val segLabels = listOf("Baixa", "Normal", "Alta")
        val segWidth = (right - left) / 3f
        segmentRegions.clear()
        for (i in 0 until 3) {
            segmentRegions.add(RectF(left + i * segWidth, y, left + (i + 1) * segWidth, y + segmentHeight))
        }

        for (i in 0 until 3) {
            val isActive = sensitivityLevel == i
            val paint = if (isActive) segmentActivePaint else segmentInactivePaint
            val textPaint = if (isActive) segmentTextActivePaint else segmentTextPaint
            val rect = segmentRegions[i]
            val r = segmentCornerRadius

            canvas.save()
            canvas.clipRect(rect)
            canvas.drawRoundRect(rect, r, r, paint)
            canvas.restore()

            // Borders
            canvas.drawRect(rect, segmentBorderPaint)
            canvas.drawText(segLabels[i], rect.centerX(), rect.centerY() + 6f, textPaint)
        }
        y += segmentHeight + 12f

        // Divider
        y += 8f
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 20f

        // === DESCONEÇÃO ===
        canvas.drawText("DESCONEÇÃO", left, y + 14f, sectionPaint)
        y += 32f

        canvas.drawText("Confirmar antes de desconectar", left + 8f, y + rowHeight / 2f + 6f, labelPaint)
        toggleRegions.add(RectF(right - toggleWidth, y + (rowHeight - toggleHeight) / 2f, right, y + (rowHeight + toggleHeight) / 2f))
        drawToggle(canvas, toggleRegions[5], confirmDisconnect)
    }

    private fun drawToggle(canvas: Canvas, rect: RectF, isOn: Boolean) {
        val trackPaint = if (isOn) toggleBgOnPaint else toggleTrackPaint
        canvas.drawRoundRect(rect, toggleCorner, toggleCorner, trackPaint)

        val knobRadius = toggleHeight / 2f - 3f
        val knobCx = if (isOn) rect.right - toggleHeight / 2f else rect.left + toggleHeight / 2f
        canvas.drawCircle(knobCx, rect.centerY(), knobRadius, toggleKnobPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) return true

        val x = event.x
        val y = event.y

        // Close / Back
        if (closeRegion.contains(x, y) || backRegion.contains(x, y)) {
            callback?.onSettingsClose()
            return true
        }

        // Recenter
        if (recenterRegion.contains(x, y)) {
            callback?.onRecenterWheel()
            return true
        }

        // Toggle: Invert wheel (index 0)
        if (toggleRegions.isNotEmpty() && toggleRegions[0].contains(x, y)) {
            invertWheel = !invertWheel
            callback?.onToggleInvertWheel(invertWheel)
            invalidate()
            return true
        }

        // Toggles: Stick inversions (indices 1-4)
        for (i in 1..4) {
            if (i < toggleRegions.size && toggleRegions[i].contains(x, y)) {
                when (i) {
                    1 -> { invertLeftX = !invertLeftX; callback?.onToggleInvertLeftX(invertLeftX) }
                    2 -> { invertLeftY = !invertLeftY; callback?.onToggleInvertLeftY(invertLeftY) }
                    3 -> { invertRightX = !invertRightX; callback?.onToggleInvertRightX(invertRightX) }
                    4 -> { invertRightY = !invertRightY; callback?.onToggleInvertRightY(invertRightY) }
                }
                invalidate()
                return true
            }
        }

        // Sensitivity segments
        for (i in 0..2) {
            if (i < segmentRegions.size && segmentRegions[i].contains(x, y)) {
                if (sensitivityLevel != i) {
                    sensitivityLevel = i
                    callback?.onSensitivityChanged(i)
                    invalidate()
                }
                return true
            }
        }

        // Toggle: Confirm disconnect (index 5)
        if (toggleRegions.size > 5 && toggleRegions[5].contains(x, y)) {
            confirmDisconnect = !confirmDisconnect
            callback?.onToggleConfirmDisconnect(confirmDisconnect)
            invalidate()
            return true
        }

        // Consume all other touches
        return true
    }
}
