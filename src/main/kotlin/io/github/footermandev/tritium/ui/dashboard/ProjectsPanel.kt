package io.github.footermandev.tritium.ui.dashboard

import io.github.footermandev.tritium.addEach
import io.github.footermandev.tritium.core.Project
import io.github.footermandev.tritium.core.ProjectDirWatcher
import io.github.footermandev.tritium.core.ProjectMngr
import io.github.footermandev.tritium.core.ProjectMngrListener
import io.github.footermandev.tritium.dim
import io.github.footermandev.tritium.emptyBorder
import io.github.footermandev.tritium.logger
import io.github.footermandev.tritium.ui.components.RoundedBorder
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridLayout
import java.awt.Insets
import javax.swing.*
import javax.swing.border.EmptyBorder

//TODO: Set up a file to contain user specified directories for projects, along with searching the default directory!
class ProjectsPanel : JPanel(), ProjectMngrListener {
    private val logger = logger()

    private val projectsList: JScrollPane
    private val projectsPanel: JPanel

    private val watcher: ProjectDirWatcher

    fun dispose() {
        ProjectMngr.removeListener(this)
        watcher.stop()
    }

    init {
        layout = BorderLayout()

        val btnPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(10,0,10,0)
            isOpaque = false
        }

        val btnsContainer = JPanel(GridLayout(1,3,10,10)).apply {
            border = EmptyBorder(0,20,0,20)
            isOpaque = false
        }

        val newProject = JButton("New Project").apply {
            margin = Insets(10, 20, 10, 20)
            preferredSize = dim(0, 40)

            addActionListener {
                val newProject = NewProjectFrame()
                newProject.isVisible = true
            }
        }
        val importProject = JButton("Import Project").apply {
            margin = Insets(10, 20, 10, 20)
            preferredSize = dim(0, 40)
        }

        val cloneFromGit = JButton("Clone from Git").apply {
            margin = Insets(10, 20, 10, 20)
            preferredSize = dim(0, 40)
        }

        btnsContainer.addEach(newProject, importProject, cloneFromGit)
        btnPanel.add(btnsContainer, BorderLayout.NORTH)

        add(btnPanel, BorderLayout.NORTH)

        projectsList = JScrollPane().apply {
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
            verticalScrollBar.unitIncrement = 10
            border = null

            projectsPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
            }
            viewport.view = projectsPanel
        }

        add(projectsList, BorderLayout.CENTER)

        watcher = ProjectDirWatcher(ProjectMngr.projectsDir.toPath())
        watcher.start { refresh() }

        refresh()
    }

    override fun onProjectCreated(project: Project) {
        refresh()
    }

    override fun onProjectOpened(project: Project) {}

    override fun onProjectDeleted(project: Project) {
        refresh()
    }

    override fun onProjectUpdated(project: Project) {
        refresh()
    }

    override fun onProjectFinishedLoading(projects: List<Project>) {}

    override fun onProjectFailedToGenerate(project: String, errorMsg: String, exception: Exception?) {}

    private fun refresh() {
        SwingUtilities.invokeLater {
            val updatedProjects = ProjectMngr.refreshProjects()
            projectsPanel.removeAll()

            projectsPanel.border = emptyBorder(20)

            if(updatedProjects.isEmpty()) {
                val label = JLabel("No available projects.")
                label.horizontalAlignment = SwingConstants.CENTER
                projectsPanel.layout = BorderLayout()
                projectsPanel.add(label, BorderLayout.CENTER)
            } else {
                projectsPanel.layout = BoxLayout(projectsPanel, BoxLayout.Y_AXIS)

                updatedProjects.forEach { p ->
                    logger.info("Listed {}", p.metadata.name)
                    val ui = ProjectUI(p)
                    ui.alignmentX = Component.LEFT_ALIGNMENT
                    ui.border = RoundedBorder(15)
                    ui.isOpaque = false

                    projectsPanel.add(ui)

                    projectsPanel.add(Box.createRigidArea(dim(0,10)))
                }
                if(projectsPanel.componentCount > 0) projectsPanel.remove(projectsPanel.componentCount - 1)
                projectsPanel.add(Box.createVerticalGlue())
            }

            projectsPanel.revalidate()
            projectsPanel.repaint()
            revalidate()
            repaint()
        }
    }
}