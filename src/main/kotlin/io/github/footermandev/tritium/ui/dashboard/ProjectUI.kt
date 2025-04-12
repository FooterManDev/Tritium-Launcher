package io.github.footermandev.tritium.ui.dashboard

import io.github.footermandev.tritium.*
import io.github.footermandev.tritium.core.Project
import io.github.footermandev.tritium.ui.components.RoundedBorder
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.nio.file.Path
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class ProjectUI(val project: Project) : JPanel() {
    private val meta = project.metadata
    private var hovered = false
    var hoverColor: Color? = Color.LIGHT_GRAY

    init {
        layout = GridBagLayout()
        border = EmptyBorder(10, 10, 10, 10)
        maximumSize = dim(Int.MAX_VALUE, 80)
        preferredSize = dim(300, 80)
        minimumSize = dim(200, 80)
        alignmentX = Component.LEFT_ALIGNMENT

        val gbc = GridBagConstraints().apply {
            insets = Insets(0, 5, 0, 5)
            anchor = GridBagConstraints.CENTER
            weighty = 1.0
            fill = GridBagConstraints.VERTICAL
            weightx = 0.0
        }

        val iconPath = if (File(meta.icon).isAbsolute) {
            // If it's an absolute path, use it directly
            Path.of(meta.icon)
        } else {
            // If it's a relative path, resolve it against the project directory
            Path.of(meta.path).resolve(meta.icon)
        }
        val icon = JLabel(ImageIcon(iconPath.toImage()?.getHighQualityScaledInstance(50, 50)))
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridheight = 2
        gbc.anchor = GridBagConstraints.CENTER
        add(icon, gbc)

        val title = JLabel(meta.name).apply {
            foreground = meta.titleColor?.parseColor() ?: Color.WHITE
        }
        gbc.gridx = 1
        gbc.gridy = 0
        gbc.gridheight = 1
        gbc.weightx = 1.0
        gbc.anchor = GridBagConstraints.WEST
        add(title, gbc)

        val path = JLabel(meta.path.shortenHome()).apply {

            font = font.deriveFont(font.size2D * 0.8f)
        }
        gbc.gridx = 1
        gbc.gridy = 1
        gbc.anchor = GridBagConstraints.WEST
        add(path, gbc)

        val mcVer = JLabel(meta.minecraftVersion)
        gbc.gridx = 2
        gbc.gridy = 0
        gbc.gridheight = 1
        gbc.weightx = 1.0
        gbc.anchor = GridBagConstraints.EAST
        add(mcVer, gbc)

        val loader = JLabel(meta.modLoader)
        gbc.gridx = 2
        gbc.gridy = 1
        gbc.anchor = GridBagConstraints.EAST
        add(loader, gbc)

        gbc.gridx = 1
        gbc.gridy = 0
        gbc.gridheight = 2
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        add(JLabel(""), gbc)

        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                hovered = true
                repaint()
            }

            override fun mouseExited(e: MouseEvent?) {
                hovered = false
                repaint()
            }

            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
            }
        })
    }

    override fun paint(g: Graphics) {
        super.paint(g)
        if(hovered) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

            val roundRadius = (border as? RoundedBorder)?.radius ?: 0

            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f)
            g2.color = hoverColor
            g2.fillRoundRect(0,0, width - 1, height - 1, roundRadius, roundRadius)
            g2.dispose()
        }
    }
}