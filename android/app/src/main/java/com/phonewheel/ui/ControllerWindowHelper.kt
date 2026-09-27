package com.phonewheel.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.Build
import android.view.Display
import android.view.Surface
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.phonewheel.R

/**
 * Política compartilhada de janela para as telas de controle e prévia.
 *
 * Responsabilidades:
 * - Travar o landscape atual (não virar 180° durante o uso);
 * - Aplicar tela cheia de jogo com APIs modernas;
 * - Excluir gestos do sistema apenas nas áreas dos controles de borda;
 * - Aplicar padding mínimo de recorte (display cutout).
 *
 * Não usa lock task, kiosk nem APIs obsoletas de fullscreen.
 */
object ControllerWindowHelper {

    private val EDGE_CONTROL_IDS = intArrayOf(
        R.id.buttonLT,
        R.id.buttonRT,
        R.id.buttonLB,
        R.id.buttonRB,
        R.id.dpad,
        R.id.leftStick,
        R.id.rightStick
    )

    /**
     * Mapeia a rotação da tela para a orientação a travar.
     *
     * Retrato (`ROTATION_0` / `ROTATION_180`) retorna `null` para o caller
     * manter `sensorLandscape` até a janela realmente estar em landscape.
     */
    fun orientationForRotation(rotation: Int): Int? = when (rotation) {
        Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        else -> null
    }

    /**
     * Trava a Activity no landscape atual. Se ainda estiver em retrato,
     * mantém `sensorLandscape` para o sistema completar a rotação.
     */
    fun lockCurrentLandscape(activity: Activity) {
        val rotation = currentDisplayRotation(activity)
        val orientation = orientationForRotation(rotation)
        activity.requestedOrientation = orientation
            ?: ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    /**
     * Tela cheia de jogo: conteúdo sob as barras, barras escondidas,
     * swipe temporário para reaparecer, recorte nas bordas curtas.
     */
    fun applyImmersiveMode(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val attributes = window.attributes
            attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = attributes
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    /**
     * Padding mínimo pelo recorte da tela, para os controles não ficarem
     * atrás do notch. Não adiciona loops; reage a insets.
     */
    fun applyCutoutPadding(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            view.setPadding(cutout.left, cutout.top, cutout.right, cutout.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    /**
     * Exclui gestos do sistema apenas nas áreas dos controles próximos às
     * bordas (LT/RT, analógicos, D-Pad). Não cobre a tela inteira.
     */
    fun applySystemGestureExclusion(root: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        if (root.getTag(R.id.tag_controller_gesture_exclusion) == true) {
            updateGestureExclusionRects(root, EDGE_CONTROL_IDS)
            return
        }
        root.setTag(R.id.tag_controller_gesture_exclusion, true)

        root.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            updateGestureExclusionRects(view, EDGE_CONTROL_IDS)
        }
        viewOrPost(root) { updateGestureExclusionRects(root, EDGE_CONTROL_IDS) }
    }

    private fun viewOrPost(view: View, action: () -> Unit) {
        if (view.isLaidOut) {
            action()
        } else {
            view.post(action)
        }
    }

    private fun updateGestureExclusionRects(root: View, controlIds: IntArray) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val rootLocation = IntArray(2)
        root.getLocationOnScreen(rootLocation)
        val rects = ArrayList<Rect>(controlIds.size)

        for (id in controlIds) {
            val control = root.findViewById<View>(id) ?: continue
            if (control.visibility != View.VISIBLE || control.width <= 0 || control.height <= 0) {
                continue
            }
            val location = IntArray(2)
            control.getLocationOnScreen(location)
            rects.add(
                Rect(
                    location[0] - rootLocation[0],
                    location[1] - rootLocation[1],
                    location[0] - rootLocation[0] + control.width,
                    location[1] - rootLocation[1] + control.height
                )
            )
        }

        root.systemGestureExclusionRects = rects
    }

    @Suppress("DEPRECATION")
    private fun currentDisplayRotation(activity: Activity): Int {
        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            activity.windowManager.defaultDisplay
        }
        return display?.rotation ?: Surface.ROTATION_0
    }
}
