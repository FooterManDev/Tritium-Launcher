package io.github.footermandev.tritium.ui.dashboard

import io.github.footermandev.tritium.auth.MicrosoftAuth
import io.github.footermandev.tritium.compareMCVersions
import io.github.footermandev.tritium.core.ChangelogFileSpec
import io.github.footermandev.tritium.core.Project
import io.github.footermandev.tritium.core.ProjectMngr
import io.github.footermandev.tritium.core.ProjectMngrListener
import io.github.footermandev.tritium.core.modloader.ModLoader
import io.github.footermandev.tritium.core.modpack.ModpackType
import io.github.footermandev.tritium.emptyBorder
import io.github.footermandev.tritium.insets
import io.github.footermandev.tritium.toPath
import io.github.footermandev.tritium.ui.elements.TFileChooserField
import io.github.footermandev.tritium.ui.icons.TRIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.filechooser.FileNameExtensionFilter

class NewProjectFrame : JFrame() {
    private val logger = LoggerFactory.getLogger(NewProjectFrame::class.java)

    val nameField = JTextField(30)

    val iconField = TFileChooserField(
        mode = JFileChooser.FILES_ONLY,
        extensions = FileNameExtensionFilter("Image Files", "png", "jpg", "jpeg")
    )

    val mcVerField = JComboBox<String>().apply {
        CoroutineScope(Dispatchers.IO).launch {
            val versions = MicrosoftAuth.getMinecraftVersions()
            SwingUtilities.invokeLater {
                versions?.forEach { v ->
                    addItem(v?.id)
                }
            }
        }

        addActionListener {
            updateLoaderVerField()
        }
    }

    val modLoaderField = JComboBox<ModLoader>().apply {
        val loaders = ServiceLoader.load(ModLoader::class.java).toList()
        loaders.forEach {
            logger.info("Found ModLoader on request: {}", it)
            addItem(it)
        }

        addActionListener {
            updateLoaderVerField()
        }
    }

    private fun updateLoaderVerField() {
        val loader = modLoaderField.selectedItem as ModLoader
        val mcVer = mcVerField.selectedItem as String

        if(compareMCVersions(mcVer, loader.oldestVersion)) {
            CoroutineScope(Dispatchers.IO).launch {
                val versions = mcVer.let { loader.getCompatibleVersions(it) }
                SwingUtilities.invokeLater {
                    modLoaderVerField.removeAllItems()
                    versions.forEach {
                        modLoaderVerField.addItem(it)
                    }
                }
            }
        } else {
            SwingUtilities.invokeLater {
                modLoaderVerField.removeAllItems()
                modLoaderVerField.addItem("No available versions")
            }
        }
    }

    val modLoaderVerField = JComboBox<String>()

    val typeField = JComboBox<ModpackType>().apply {
        val types = ServiceLoader.load(ModpackType::class.java).toList()
        types.forEach {
            logger.info("Found ModpackType on request: {}", it)
            addItem(it)
        }
    }

    val renameIconField = JCheckBox("Rename Icon if Copied").apply { isEnabled = false }
    val copyIconField = JCheckBox("Copy Icon into Project").apply {
        addActionListener { renameIconField.isEnabled = this.isSelected }
    }

    val directoryOverrideField = JTextField(ProjectMngr.projectsDir.absolutePath)


    val changelogFileNameField = JTextField().apply {
        isEnabled = false
    }
    val changelogFileDirField = JTextField().apply {
        isEnabled = false
    }
    val changelogFileTypeField = JTextField("md")

    val createChangelogField = JCheckBox("Create Changelog File").apply {
        addActionListener {
            changelogFileNameField.isEnabled = this.isSelected
            changelogFileDirField.isEnabled = this.isSelected
            changelogFileTypeField.isEnabled = this.isSelected
        }
    }

    val directoryNameField = JTextField()

    init {
        title = "New Project"
        iconImage = TRIcons.TritiumPng.image
        defaultCloseOperation = DISPOSE_ON_CLOSE

        val panel = JPanel(GridBagLayout()).apply {
            border = emptyBorder(16)
        }
        contentPane = panel

        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.BOTH
            weighty = 1.0
            insets = insets(8)
        }

        val leftPanel = JPanel(GridBagLayout())
        val leftGbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = insets(8)
        }
        var leftRow = 0


        leftRow = addFormRow(leftPanel, leftGbc, leftRow,
            "Name",
            "The name of your project.",
            nameField
        )
        leftRow = addFormRow(leftPanel, leftGbc, leftRow,
            "Icon",
            "Your Project Image.",
            iconField
        )
        leftRow = addFormRow(leftPanel, leftGbc, leftRow,
            "Minecraft Version",
            "The version of Minecraft you want to use.",
            mcVerField
        )
        leftRow = addFormRow(leftPanel, leftGbc, leftRow,
            "Mod Loader",
            "The Mod Loader you want to use.",
            modLoaderField
        )
        leftRow = addFormRow(leftPanel, leftGbc, leftRow,
            "Mod Loader Version",
            "The version of the Mod Loader you want to use.",
            modLoaderVerField
        )
        leftRow = addFormRow(leftPanel, leftGbc, leftRow,
            "Modpack Type",
            "The Project Type.",
            typeField
        )

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.5
        panel.add(leftPanel, gbc)

        gbc.gridx = 1
        gbc.weightx = 0.0
        val divider = JSeparator(SwingConstants.VERTICAL)
        val divPanel = JPanel(BorderLayout()).apply {
            border = EmptyBorder(0,16,0,16)
            add(divider, BorderLayout.CENTER)
        }
        panel.add(divPanel, gbc)

        val rightPanel = JPanel(GridBagLayout())
        val rightGbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = insets(8)
        }
        var rightRow = 0

        rightRow = addFormRow(rightPanel, rightGbc, rightRow,
            "Copy Icon",
            "Copy the icon into the Project directory.",
            copyIconField
        )
        rightRow = addFormRow(rightPanel, rightGbc, rightRow,
            "Rename Icon",
            "If Copied, rename the image file.",
            renameIconField
        )
        rightRow = addFormRow(rightPanel, rightGbc, rightRow,
            "Project Directory",
            "The Project directory.",
            directoryOverrideField
        )
        rightRow = addFormRow(rightPanel, rightGbc, rightRow,
            "Project Directory Name",
            "The Project directory name.",
            directoryNameField
        )
        rightRow = addFormRow(rightPanel, rightGbc, rightRow,
            "Create Changelog",
            "Create a Changelog file.",
            createChangelogField
        )
        rightRow = addFormRow(rightPanel, rightGbc, rightRow,
            "Changelog Name",
            "The Changelog file name.",
            changelogFileNameField
        )
        rightRow = addFormRow(rightPanel, rightGbc, rightRow,
            "Changelog Directory",
            "The Changelog file directory.",
            changelogFileDirField
        )
        rightRow = addFormRow(rightPanel, rightGbc, rightRow,
            "Changelog File Type",
            "The Changelog file type.",
            changelogFileTypeField
        )

        gbc.gridx = 2
        gbc.weightx = 0.5
        panel.add(rightPanel, gbc)

        val cancel = JButton("Cancel").apply {
            addActionListener {
                dispose()
            }
        }

        val create = JButton("Create").apply {
            addActionListener {
                val changelog = if(createChangelogField.isSelected) {
                    ChangelogFileSpec(
                        changelogFileNameField.text,
                        changelogFileDirField.text.toPath(),
                        changelogFileTypeField.text
                    )
                } else null

                val overrideDirectory = if(directoryOverrideField.text.isNotEmpty()) {
                    File(directoryOverrideField.text)
                } else null

                val directoryNameOverride = directoryNameField.text.ifEmpty { null }

                try {
                    val gen = ProjectMngr.generateProject(
                        nameField.text,
                        iconField.field.text.toPath(),
                        mcVerField.selectedItem as String,
                        modLoaderField.selectedItem as ModLoader,
                        modLoaderVerField.selectedItem as String,
                        typeField.selectedItem as ModpackType,
                        copyIconField.isSelected,
                        renameIconField.isSelected,
                        overrideDirectory,
                        changelog,
                        directoryNameOverride
                    )

                    if(gen) {
                        ProjectMngr.addListener(object : ProjectMngrListener {
                            override fun onProjectCreated(project: Project) {
                                dispose()
                            }

                            override fun onProjectOpened(project: Project) {}

                            override fun onProjectDeleted(project: Project) {}

                            override fun onProjectUpdated(project: Project) {}

                            override fun onProjectFinishedLoading(projects: List<Project>) {}

                            override fun onProjectFailedToGenerate(
                                project: String,
                                errorMsg: String,
                                exception: Exception?
                            ) {}

                        })
                    }

                } catch (e: Exception) {
                    logger.error("Failed to generate project.", e)
                }
                dispose()
            }
        }

        val btnPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            add(cancel)
            add(create)
        }

        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 3
        gbc.weightx = 1.0
        gbc.anchor = GridBagConstraints.LINE_END
        panel.add(btnPanel, gbc)

        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }

    fun addFormRow(
        panel: JPanel,
        gbc: GridBagConstraints,
        rowIndex: Int,
        labelText: String,
        descText: String,
        component: JComponent
    ): Int {
        var row = rowIndex
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        gbc.anchor = GridBagConstraints.LINE_END
        panel.add(JLabel(labelText), gbc)

        gbc.gridx = 1
        gbc.gridy = row
        gbc.weightx = 1.0
        gbc.anchor = GridBagConstraints.LINE_START
        panel.add(component, gbc)
        row++

        if(descText.isNotEmpty()) {
            gbc.gridx = 1
            gbc.gridy = row
            gbc.weightx = 1.0
            gbc.anchor = GridBagConstraints.LINE_START
            panel.add(JLabel(descText).apply { font = font.deriveFont(font.size2D - 2f) }, gbc)
            row++
        }

        return row
    }

}
