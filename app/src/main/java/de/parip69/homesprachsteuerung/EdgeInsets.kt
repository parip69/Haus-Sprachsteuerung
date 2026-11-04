package de.parip69.homesprachsteuerung

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.applySystemInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val types = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or
            WindowInsetsCompat.Type.ime()
        val padding = insets.getInsets(types)
        view.setPadding(padding.left, padding.top, padding.right, padding.bottom)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
