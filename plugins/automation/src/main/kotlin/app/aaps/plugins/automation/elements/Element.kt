package app.aaps.plugins.automation.elements

import android.widget.LinearLayout

interface Element {

    fun addToLayout(root: LinearLayout)

    /**
     * Clean up any View references to prevent memory leaks.
     * Called when the dialog is destroyed.
     */
    fun cleanup() {}
}