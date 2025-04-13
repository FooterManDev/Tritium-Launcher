package io.github.footermandev.tritium.ui.components

import java.awt.*
import javax.swing.border.Border

class TRoundedBorder(val radius: Int) : Border {
    var border: Boolean = false

    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        if(!border) return

        (g as? Graphics2D)?.apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            color = c.foreground
            drawRoundRect(x,y, width - 1, height - 1, radius, radius)
        } ?: g.drawRoundRect(x,y, width - 1, height - 1, radius, radius)
    }

    override fun getBorderInsets(c: Component): Insets = Insets(radius + 1, radius + 1, radius + 2, radius)

    override fun isBorderOpaque(): Boolean = true
}