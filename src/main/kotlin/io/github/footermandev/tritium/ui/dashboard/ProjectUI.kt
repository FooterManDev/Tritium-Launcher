package io.github.footermandev.tritium.ui.dashboard

import io.github.footermandev.tritium.*
import io.github.footermandev.tritium.core.Project
import io.github.footermandev.tritium.ui.components.TRoundedBorder
import io.github.footermandev.tritium.ui.components.scale
import io.github.footermandev.tritium.ui.theme.TColors.accent
import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
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
    private var selected = false
    var hoverColor: Color? = accent

    init {
        layout = GridBagLayout()
        border = EmptyBorder(10, 10, 10, 10)
        maximumSize = dim(Int.MAX_VALUE, 80)
        preferredSize = dim(300, 80)
        minimumSize = dim(200, 80)
        alignmentX = Component.LEFT_ALIGNMENT
        isFocusable = true

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
            font = font.scale(0.8f)
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
                if(!isFocusOwner) {
                    requestFocusInWindow()
                }
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

        addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) {
                selected = true
                repaint()
            }
            override fun focusLost(e: FocusEvent?) {
                selected = false
                hovered = false
                repaint()
            }
        })
    }

    override fun paint(g: Graphics) {
        if(hovered || selected) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

            val roundRadius = (border as? TRoundedBorder)?.radius ?: 0

            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f)
            g2.color = hoverColor
            g2.fillRoundRect(0,0, width - 1, height - 1, roundRadius, roundRadius)
            g2.dispose()
        }
        super.paint(g)
    }
}