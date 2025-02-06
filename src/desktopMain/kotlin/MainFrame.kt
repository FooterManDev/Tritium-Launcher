import core.Project
import core.ProjectMngr
import model.WindowStateMngr
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JSplitPane
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