import core.Project
import core.ProjectMngr
import model.WindowStateMngr
import ui.dashboard.AccountDashboard
import ui.dashboard.ProjectsList
import ui.dashboard.SettingsDashboard
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import kotlin.system.exitProcess

class MainFrame : JFrame() {
    init {
        title = "Tritium Launcher"
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        size = Dimension(800, 600)
        minimumSize = Dimension(600, 500)

        WindowStateMngr.restoreState(this@MainFrame, fromTR("window/state.json"))

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                WindowStateMngr.saveState(this@MainFrame, fromTR("window/state.json"))
                exitProcess(0)
            }
        })

        isVisible = true

        val activeProject = ProjectMngr.activeProject
        if(activeProject != null) {
            openProject(activeProject)
        } else {
            setupDashboard()
        }
    }

    private fun setupDashboard() {
        val dash = JPanel().apply {
            layout = BorderLayout()
            border = BorderFactory.createEmptyBorder(150, 150, 100, 150)
        }

        val navPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentY = Component.TOP_ALIGNMENT
        }

        val searchField = JTextField("Search...").apply {
            maximumSize = Dimension(275, 30)
            alignmentX = Component.CENTER_ALIGNMENT
            border = BorderFactory.createLineBorder(Color.GRAY, 1, true)
        }

        val actionsPanel = JPanel().apply {
            border = BorderFactory.createEmptyBorder()
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(JButton("New Project"))
            add(JButton("Open..."))
            add(JButton("Clone from Git"))
        }

        val projectList = ProjectsList()
        val settingsPanel = SettingsDashboard()
        val accountPanel = AccountDashboard()

        val header = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            alignmentX = Component.CENTER_ALIGNMENT
            add(navPanel, BorderLayout.WEST)
            add(Box.createRigidArea(Dimension(100, 0)), BorderLayout.WEST)
            add(searchField)
            add(Box.createRigidArea(Dimension(175, 0)), BorderLayout.EAST)
        }

        val viewsPanel = JPanel(CardLayout()).apply {
            add(projectList, "Projects")
            add(settingsPanel, "Settings")
            add(accountPanel, "Account")
            maximumSize = Dimension(500, Int.MAX_VALUE)
        }

        navPanel.apply {
            val cardLayout = viewsPanel.layout as CardLayout

            val projectsBtn = JButton("Projects").apply {
                addActionListener { cardLayout.show(viewsPanel, "Projects") }
                viewsPanel.maximumSize = Dimension(300, Int.MAX_VALUE) // This is used to ensure the list stays the correct size.
            }
            add(projectsBtn)

            val settingsBtn = JButton("Settings").apply {
                addActionListener { cardLayout.show(viewsPanel, "Settings") }
            }
            add(settingsBtn)

            val accountBtn = JButton("Account").apply {
                addActionListener { cardLayout.show(viewsPanel, "Account") }
            }
            add(accountBtn)
        }

        val content = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.CENTER_ALIGNMENT
            add(header, BorderLayout.PAGE_START)
            add(actionsPanel, BorderLayout.CENTER)
            add(viewsPanel, BorderLayout.PAGE_END)
            add(Box.createVerticalGlue(), BorderLayout.PAGE_END)
        }

        dash.add(content, BorderLayout.CENTER)
        dash.add(Box.createVerticalGlue(), BorderLayout.PAGE_END)
        dash.add(Box.createHorizontalStrut(50), BorderLayout.NORTH)
        dash.add(Box.createHorizontalStrut(50), BorderLayout.SOUTH)

        contentPane.removeAll()
        contentPane.add(dash, BorderLayout.CENTER)
        validate()
        repaint()
    }

    // TODO: Move to Object or Class. Does not do anything useful.
    private fun openProject(project: Project) {
        contentPane.removeAll()

        val splitter = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            dividerLocation = project.meta.editorDividerLoc
            resizeWeight = 0.3
        }

        contentPane.add(splitter, BorderLayout.CENTER)
        validate()
        repaint()
    }

    private fun quit() {
        dispose()
        exitProcess(0)
    }
}