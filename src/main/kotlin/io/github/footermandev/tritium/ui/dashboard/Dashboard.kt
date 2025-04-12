package io.github.footermandev.tritium.ui.dashboard

import io.github.footermandev.tritium.dim
import io.github.footermandev.tritium.ui.icons.TRIcons
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger(Dashboard::class.java)

/**
 * Includes a list of recently opened projects, quick navigation to update settings, and actions for creating or importing projects.
 * TODO: Opened if there are no projects, an active project was deleted, or on first launch.
 */
class Dashboard : JFrame() {
    private lateinit var cardLayout: CardLayout
    private lateinit var rightPanel: JPanel
    private val leftPanelBg = UIManager.getColor("Panel.background").darker()
    private val rightPanelBg = UIManager.getColor("Panel.background")

    init {
        layout = BorderLayout()
        title = "Tritium"
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        size = Dimension(650, 500)
        iconImage = TRIcons.TritiumPng.image
        setLocationRelativeTo(null) // Opens at the center of the screen
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                dispose()
                exitProcess(0)
            }
        })

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            resizeWeight = 0.01
            isOneTouchExpandable = false
            dividerSize = 0 // Invisible
            isEnabled = false // Immovable
        }
        add(splitPane, BorderLayout.CENTER)

        val leftPanel = JPanel().apply {
            layout = BorderLayout()
            background = leftPanelBg
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }
        splitPane.leftComponent = leftPanel

        // TODO: Add localization
        val btnBg = UIManager.getColor("Button.background")
        val projectsNavBtn = JButton("Projects").apply {
            addActionListener {
                cardLayout.show(rightPanel, "projects")
            }
            addPropertyChangeListener {
                background = if (isSelected) {
                    btnBg.darker()
                } else btnBg
            }
        }
        val accountNavBtn = JButton("Account").apply {
            addActionListener {
                cardLayout.show(rightPanel, "account")
            }
            addPropertyChangeListener {
                background = if (isSelected) {
                    btnBg.darker()
                } else btnBg
            }
        }
        val settingsNavBtn = JButton("Settings").apply {
            addActionListener {
                cardLayout.show(rightPanel, "settings")
            }
            addPropertyChangeListener {
                background = if (isSelected) {
                    btnBg.darker()
                } else btnBg
            }
        }

        val navPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = leftPanelBg
            alignmentX = Component.CENTER_ALIGNMENT
            add(Box.createRigidArea(dim(0, 10)))
            add(projectsNavBtn)
            add(Box.createRigidArea(dim(0, 15)))
            add(accountNavBtn)
            add(Box.createRigidArea(dim(0, 15)))
            add(settingsNavBtn)
        }

        val bottomPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = leftPanelBg
            add(AccInfo())
            add(Box.createRigidArea(dim(0, 10)))
            add(About())
        }

        leftPanel.add(navPanel, BorderLayout.NORTH)
        leftPanel.add(bottomPanel, BorderLayout.SOUTH)

        rightPanel = JPanel().apply {
            background = rightPanelBg
        }
        cardLayout = CardLayout()
        rightPanel.layout = cardLayout
        splitPane.rightComponent = rightPanel

        rightPanel.add(ProjectsPanel(), "projects")
        rightPanel.add(AccountPanel(), "account")
        rightPanel.add(SettingsPanel(), "settings")

        isVisible = true
    }

    // TODO
    class SettingsPanel : JPanel() {}
}