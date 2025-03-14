package ui.elements

import dim
import ui.icons.TRIcons
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.*

class TRTitlePane(private val frame: JFrame) : JPanel() {
    private val closeIcon = TRIcons.Cross
    private val minimizeIcon = TRIcons.Min
    private val maximizeIcon = TRIcons.Max
    private val titleForeground = UIManager.getColor("TitlePane.titleForeground")
    private val titleFont = UIManager.getFont("TitlePane.titleFont")

    private var dragOffset = Point()
    private val decorationHeight = 32

    init {
        layout = BorderLayout()
        isOpaque = false

        add(createBtnsPanel(), BorderLayout.EAST)

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                dragOffset = e.point
            }

            override fun mouseDragged(e: MouseEvent) {
                val newLoc = Point(
                    frame.x + e.x - dragOffset.x,
                    frame.y + e.y - dragOffset.y
                )
                frame.location = newLoc
            }
        })

        preferredSize = dim(0, decorationHeight)
    }

    private fun createBtnsPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT, 0,0))
        panel.isOpaque = false

        panel.add(createWindowBtn(minimizeIcon, "Minimize") {
            frame.state = Frame.ICONIFIED
        })
        panel.add(createWindowBtn(maximizeIcon, "Maximize") {
            toggleMaximize(it)
        })
        panel.add(createWindowBtn(closeIcon, "Close") {
            frame.dispose()
        })

        return panel
    }

    private fun createWindowBtn(icon: Icon, tooltip: String, action: (JButton) -> Unit): JButton {
        val btn = JButton(icon).apply {
            toolTipText = tooltip
            preferredSize = dim(30, 30)
            isContentAreaFilled = false
            border = BorderFactory.createEmptyBorder(5,5,5,5)
            isFocusPainted = false
        }
        btn.addActionListener { action(btn) }
        return btn
    }

    private fun toggleMaximize(btn: JButton) {
        if(frame.extendedState and Frame.MAXIMIZED_BOTH == 0) {
            frame.extendedState = Frame.MAXIMIZED_BOTH
            btn.icon = maximizeIcon //TODO: Add a restore icon
        } else {
            frame.extendedState = Frame.NORMAL
            btn.icon = maximizeIcon
        }
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val bgCol = UIManager.getColor("TitlePane.background") ?: Color(60,60,60)
        val gradient = GradientPaint(0f, 0f, bgCol, width.toFloat(), 0f, bgCol.darker())
        g2.paint = gradient

        val arc = UIManager.getInt("TitlePane.decorationArc").takeIf { it > 0 } ?: 15

        val shape = RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), arc.toFloat(), arc.toFloat())
        g2.fill(shape)

        g2.dispose()
    }
}