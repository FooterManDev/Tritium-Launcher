package io.github.footermandev.tritium.ui.theme

import java.awt.Color
import javax.swing.UIManager

object TColors {
    private fun comp(str: String): Color? = UIManager.getColor("Component.$str")

    val accent = comp("accentColor")
}