package io.github.foxy75.eclipse.ui.common

import android.view.View
import android.widget.LinearLayout
import io.github.foxy75.eclipse.R

/** Builds and updates a 6-dot PIN indicator. */
object PinDots {

    fun build(container: LinearLayout, count: Int = 6): List<View> {
        container.removeAllViews()
        val dots = ArrayList<View>(count)
        val size = container.resources.getDimensionPixelSize(R.dimen.pin_dot)
        val margin = container.resources.getDimensionPixelSize(R.dimen.space_8)
        for (i in 0 until count) {
            val v = View(container.context)
            val lp = LinearLayout.LayoutParams(size, size)
            lp.marginEnd = margin
            v.layoutParams = lp
            v.setBackgroundResource(R.drawable.bg_pin_dot)
            container.addView(v)
            dots.add(v)
        }
        return dots
    }

    fun render(dots: List<View>, filled: Int) {
        for (i in dots.indices) {
            dots[i].setBackgroundResource(
                if (i < filled) R.drawable.bg_pin_dot_filled else R.drawable.bg_pin_dot
            )
        }
    }
}