package io.github.footermandev.tritium.ui.dashboard

import io.github.footermandev.tritium.dim
import io.github.footermandev.tritium.ui.theme.TColors
import io.github.footermandev.tritium.ui.theme.TIcons
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Component
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
    private var cardLayout: CardLayout
    private var rightPanel: JPanel
    private val leftPanelBg = UIManager.getColor("Panel.background").darker()
    private val rightPanelBg = UIManager.getColor("Panel.background")

    private var selected: JButton? = null
    private val selectedBG = TColors.accent

    init {
        layout = BorderLayout()
        title = "Tritium"
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        size = dim(650, 500)
        maximumSize = dim(650, 500)
        isResizable = false
        iconImage = TIcons.TritiumPng.image
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


        val projectsNavBtn = createNavBtn("Projects")
        val accountNavBtn = createNavBtn("Account")
        val settingsNavBtn = createNavBtn("Settings")

        selected = projectsNavBtn
        updateBtnAppearance(projectsNavBtn, true)

        projectsNavBtn.addActionListener {
            updateSelectedBtn(projectsNavBtn)
            cardLayout.show(rightPanel, "projects")
        }

        accountNavBtn.addActionListener {
            updateSelectedBtn(accountNavBtn)
            cardLayout.show(rightPanel, "account")
        }

        settingsNavBtn.addActionListener {
            updateSelectedBtn(settingsNavBtn)
            cardLayout.show(rightPanel, "settings")
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
        // TODO: Add settings panel
        //  rightPanel.add(SettingsPanel(), "settings")

        isVisible = true
    }

    private fun createNavBtn(label: String): JButton {
        return JButton(label).apply {
            isContentAreaFilled = false
            isBorderPainted = false
            isFocusPainted = false
            foreground = Color.WHITE
            horizontalAlignment = SwingConstants.LEFT
        }
    }

    private fun updateSelectedBtn(new: JButton) {
        selected?.let { updateBtnAppearance(it, false) }
        updateBtnAppearance(new, true)
        selected = new
    }

    private fun updateBtnAppearance(btn: JButton, isSelected: Boolean) {
        if(isSelected) {
            btn.isContentAreaFilled = true
            btn.background = selectedBG
        } else {
            btn.isContentAreaFilled = false
        }
    }
}