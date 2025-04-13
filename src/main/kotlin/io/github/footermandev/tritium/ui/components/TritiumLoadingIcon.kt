package io.github.footermandev.tritium.ui.components

import io.github.footermandev.tritium.toRadians
import io.github.footermandev.tritium.ui.theme.TIcons
import java.awt.*
import javax.swing.JComponent
import javax.swing.Timer

class TritiumLoadingIcon(private val size: Int = 32) : JComponent() {
    private var progress = 0.0
    private var rotation = 0.0
    private val rotationSpeed = 2.0
    private val darkIcon = TIcons.DarkTritium.derive(size, size)
    private val lightIcon = TIcons.Tritium.derive(size, size)
    
    private val timer = Timer(16) { // ~60 FPS
        rotation = (rotation + rotationSpeed) % 360
        repaint()
    }

    init {
        preferredSize = Dimension(size, size)
        minimumSize = preferredSize
        maximumSize = preferredSize
        isOpaque = false
        background = Color(0, 0, 0, 0)
        timer.start()
    }

    fun setProgress(value: Double) {
        progress = value.coerceIn(0.0, 1.0)
        isVisible = true
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        if (!isVisible) return

        val g2 = g.create() as Graphics2D

        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)

            g2.color = Color(0,0,0,0)
            g2.fillRect(0,0, width, height)

            val x = (width - size) / 2
            val y = (height - size) / 2
            val centerX = width / 2.0
            val centerY = height / 2.0


            val darkG = g2.create() as Graphics2D
            darkG.rotate(rotation.toRadians(), centerX, centerY)
            darkIcon.paintIcon(this, darkG, x, y)
            darkG.dispose()

            val lightG = g2.create() as Graphics2D
            lightG.clipRect(x, y, (size * progress).toInt(), size)
            lightG.rotate(rotation.toRadians(), centerX, centerY)
            lightIcon.paintIcon(this, lightG, x, y)
            lightG.dispose()
        } finally { g2.dispose() }
    }

    override fun removeNotify() {
        super.removeNotify()
        timer.stop()
    }
}
