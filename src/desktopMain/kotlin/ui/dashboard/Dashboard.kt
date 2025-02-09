package ui.dashboard

import Constants
import dim
import sizedImg
import java.awt.*
import javax.swing.*

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
        defaultCloseOperation = EXIT_ON_CLOSE
        size = Dimension(650, 500)
        iconImage = ImageIcon(javaClass.classLoader.getResource("images/icon.png")).image
        setLocationRelativeTo(null) // Opens at the center of the screen

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            resizeWeight = 0.01
            isOneTouchExpandable = false
            dividerSize = 0 // Invisible
            isEnabled = false // Immovable
        }
        add(splitPane, BorderLayout.CENTER)

        val leftPanel = JPanel().apply {
            layout = BorderLayout()
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            background = leftPanelBg
        }
        splitPane.leftComponent = leftPanel

        // TODO: Add localization
        val btnBg = UIManager.getColor("Button.background")
        val projectsNavBtn = JButton("Projects").apply {
            addActionListener {
                cardLayout.show(rightPanel, "projects")
            }
            addPropertyChangeListener {
                background = if(isSelected) {
                    btnBg.darker()
                } else btnBg
            }
        }
        val accountNavBtn = JButton("Account").apply {
            addActionListener {
                cardLayout.show(rightPanel, "account")
            }
            addPropertyChangeListener {
                background = if(isSelected) {
                    btnBg.darker()
                } else btnBg
            }
        }
        val settingsNavBtn = JButton("Settings").apply {
            addActionListener {
                cardLayout.show(rightPanel, "settings")
            }
            addPropertyChangeListener {
                background = if(isSelected) {
                    btnBg.darker()
                } else btnBg
            }
        }

        val navPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = leftPanelBg
            alignmentX = Component.CENTER_ALIGNMENT
            add(projectsNavBtn)
            add(Box.createRigidArea(dim(0, 10)))
            add(accountNavBtn)
            add(Box.createRigidArea(dim(0, 10)))
            add(settingsNavBtn)
        }

        leftPanel.add(navPanel, BorderLayout.NORTH)
        leftPanel.add(About(), BorderLayout.SOUTH)

        rightPanel = JPanel().apply {
            background = rightPanelBg
        }
        cardLayout = CardLayout()
        rightPanel.layout = cardLayout
        splitPane.rightComponent = rightPanel

        rightPanel.add(ProjectPanel(), "projects")
        rightPanel.add(AccountPanel(), "account")
        rightPanel.add(SettingsPanel(), "settings")




        isVisible = true
    }

    // TODO
    class ProjectPanel : JPanel() {

    }

    // TODO
    class AccountPanel : JPanel() {

    }

    // TODO
    class SettingsPanel : JPanel() {}

    /**
     * Displays the Icon, Title and version number.
     * Not designed for use elsewhere.
     */
    private class About : JPanel() {
        private val bg = UIManager.getColor("Panel.background").darker()
        init {
            layout = BorderLayout()
            size = dim(60, 25)
            background = bg
            alignmentX = Component.CENTER_ALIGNMENT

            val img = JLabel(sizedImg(javaClass.classLoader.getResource("images/icon.png"), 25, 25))
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
}