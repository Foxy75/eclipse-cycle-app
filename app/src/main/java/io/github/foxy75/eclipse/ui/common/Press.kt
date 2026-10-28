package io.github.foxy75.eclipse.ui.common

import android.view.MotionEvent
import android.view.View

/** Press-scale micro-interaction: 0.97 / 80ms, no endless animation. */
object Press {

    fun attach(vararg views: View) {
        for (v in views) {
            v.setOnTouchListener { view, e ->
                if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                    view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80L).start()
                    view.postDelayed({
                        view.animate().scaleX(1f).scaleY(1f).setDuration(80L).start()
                    }, 130L)
                }
                false
            }
        }
    }
}