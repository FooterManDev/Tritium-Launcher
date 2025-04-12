package io.github.footermandev.tritium.ui.dashboard

import io.github.footermandev.tritium.Constants
import io.github.footermandev.tritium.dim
import io.github.footermandev.tritium.sizedImg
import io.github.footermandev.tritium.ui.icons.TRIcons
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.UIManager

/**
 * Displays the Icon, Title and version number.
 * Not designed for use elsewhere.
 */
internal class About : JPanel() {
    private val bg = UIManager.getColor("Panel.background").darker()
    init {
        layout = BorderLayout()
        size = dim(60, 25)
        background = bg
        alignmentX = CENTER_ALIGNMENT

        val img = JLabel(sizedImg(TRIcons.TritiumPng.image, 25, 25))
        val textPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = bg
        }
        val title = JLabel(Constants.TR).apply {
            font = Font("Arial", Font.BOLD, 12)
        }
        // TODO: This is a placeholder, using a text file.
        val version = JLabel(javaClass.classLoader.getResource("version.txt")?.readText().orEmpty()).apply {
            font = Font("Arial", Font.PLAIN, 10)
            foreground = Color(100, 100, 100)
        }

        textPanel.add(title)
        textPanel.add(version)
        add(img, BorderLayout.WEST)
        add(textPanel, BorderLayout.CENTER)
    }
}