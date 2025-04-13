package io.github.footermandev.tritium.core

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.TomlDecodingException
import io.github.footermandev.tritium.core.modloader.ModLoader
import io.github.footermandev.tritium.core.modpack.ModpackType
import io.github.footermandev.tritium.fromTR
import io.github.footermandev.tritium.logger
import io.github.footermandev.tritium.toPath
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.prefs.Preferences
import kotlin.io.path.copyTo
import kotlin.io.path.createTempDirectory
import kotlin.io.path.name
import kotlin.io.path.pathString

object ProjectMngr {
    private val logger = logger()
    private val listeners = mutableListOf<ProjectMngrListener>()
    private val _projects = mutableListOf<Project>()
    val projects: List<Project>
        get() {
            if(_projects.isEmpty()) {
                getProjectsFromBaseDir()
            }
            return _projects
        }
    var activeProject: Project? = null
    val projectsDir = File(fromTR(), "projects")

    fun addListener(listener: ProjectMngrListener) { listeners.add(listener) }
    fun removeListener(listener: ProjectMngrListener) { listeners.remove(listener) }
    private fun notifyProjectCreated(project: Project) {
        listeners.forEach { it.onProjectCreated(project) }
    }
    private fun notifyProjectFailedToGenerate(project: String, errorMsg: String, exception: Exception?) {
        listeners.forEach { it.onProjectFailedToGenerate(project, errorMsg, exception) }
    }
    private fun notifyProjectOpened(project: Project) {
        listeners.forEach { it.onProjectOpened(project) }
    }

    private fun getProjectsFromBaseDir(): MutableList<Project> {
        _projects.clear()
        logger.info("Loading projects...")
        projectsDir.listFiles()?.forEach { dir ->
            if(dir.isDirectory) {
                val metadataFile = File(dir, "trmeta.json")
                if(metadataFile.exists()) {
                    try {
                        val metadata = Json.decodeFromString(ProjectMetadata.serializer(), metadataFile.readText())
                        _projects.add(Project(metadata))
                        logger.info("Loaded project {}", metadata.name)
                    } catch (e: Exception) {
                        logger.error("Error decoding metadata file: {}", e.message, e)
                    }
                }
            }
        }
        return _projects
    }

    fun getProject(name: String): Project? {
        return projects.find { it.metadata.name == name }
    }

    fun getProject(path: Path): Project? {
        return projects.find { it.metadata.path.toPath() == path }
    }

    fun openProject(project: Project) {
        if(activeProject !== project) {
            activeProject = project
            notifyProjectOpened(project)
        }
    }

    fun saveActiveProject() {
        activeProject?.let { project ->
            val prefs = Preferences.userRoot().node("project-mngr")
            prefs.put("active-project", project.metadata.path.toPath().pathString)
        }
    }

    fun loadActiveProject() {
        val prefs = Preferences.userRoot().node("project-mngr")
        val activeProjectPath = prefs.get("active-project", "")

        if(activeProjectPath.isNotEmpty()) {
            val projectDir = File(activeProjectPath)
            if(projectDir.exists()) {
                val metadataFile = File(projectDir, "trmeta.toml")
                if(metadataFile.exists()) {
                    try {
                        val metadata = Toml.decodeFromString(ProjectMetadata.serializer(), metadataFile.readText())
                        val project = Project(metadata)
                        openProject(project)
                    } catch (e: TomlDecodingException) {
                        logger.error("Error decoding metadata file: {}", e.message, e)
                    }
                }
            }
        }
    }

    fun refreshProjects(): MutableList<Project> {
        return getProjectsFromBaseDir()
    }

    /**
     * Generates a new Modded MC Instance, and generates the Metadata file.
     *
     * @param name The name of the Instance.
     * @param icon The icon of the Instance.
     * @param minecraftVersion The Minecraft version of the Instance.
     * @param modLoader The Mod Loader of the Instance.
     * @param modLoaderVersion The Mod Loader version of the Instance.
     * @param modpackType The type of the Modpack.
     * @param copyIconIntoProject If true copies the specified Icon to the Project directory.
     * @param renameIconIfCopy If [copyIconIntoProject] is true, this will rename the icon to "icon".
     * @param directoryOverride Defines where the project will be generated.
     * @param createChangelogFile Defines the name and path of the changelog file. If the [Path] is null, it will generate the changelog file in the Project dir.
     * @param directoryNameOverride Defines the name of the Instance directory, but not its name.
     */
    fun generateProject(
        name: String,
        icon: Path,
        minecraftVersion: String,
        modLoader: ModLoader,
        modLoaderVersion: String,
        modpackType: ModpackType,
        copyIconIntoProject: Boolean? = false,
        renameIconIfCopy: Boolean? = false,
        directoryOverride: File? = null,
        createChangelogFile: ChangelogFileSpec? = null,
        directoryNameOverride: String? = null
    ): Boolean {
        val finalDir = File(directoryOverride ?: projectsDir, directoryNameOverride ?: name)
        if (finalDir.exists()) {
            logger.error("Project directory already exists: {}", finalDir.path)
            notifyProjectFailedToGenerate(name, "Project directory already exists", null)
            return false
        }

        val tempDir = createTempDirectory("tritium_project_${name}_temp").toFile()
        try {
            var iconPath: Path
            val iconExtension = icon.fileName.toString().substringAfterLast(".", "")

            if (copyIconIntoProject == true) {
                try {
                    val copiedIcon = icon.copyTo(tempDir.toPath().resolve(icon.name), overwrite = true)
                    iconPath = copiedIcon
                    logger.info("Copied icon to temporary location: {}", iconPath)

                    if (renameIconIfCopy == true) {
                        val newIconPath = tempDir.toPath().resolve("icon${if (iconExtension.isNotEmpty()) ".$iconExtension" else ""}")
                        Files.move(copiedIcon, newIconPath, StandardCopyOption.REPLACE_EXISTING)
                        iconPath = newIconPath
                        logger.info("Renamed icon to: {}", iconPath)
                    }
                } catch (e: Exception) {
                    logger.error("Failed to handle project icon", e)
                    tempDir.deleteRecursively()
                    notifyProjectFailedToGenerate(name, "Failed to handle project icon", e)
                    return false
                }
            }

            val metadata = ProjectMetadata(
                name = name,
                path = finalDir.path,
                icon = if (copyIconIntoProject == true) {
                    // Store path relative to project directory
                    if (renameIconIfCopy == true) "icon${if (iconExtension.isNotEmpty()) ".$iconExtension" else ""}" else icon.fileName.toString()
                } else {
                    icon.toAbsolutePath().toString()
                },
                version = "",
                changelogFile = createChangelogFile,
                minecraftVersion = minecraftVersion,
                modLoader = modLoader.id,
                modLoaderVersion = modLoaderVersion,
                type = modpackType.id,
                mods = emptyList(),
                authors = emptyList(),
                null
            )

            try {
                val generated = ProjectMetadata.generateFile(metadata, tempDir)
                if (!generated) {
                    throw IOException("Failed to generate metadata file")
                }
            } catch (e: Exception) {
                logger.error("Failed to generate metadata file for $name", e)
                tempDir.deleteRecursively()
                notifyProjectFailedToGenerate(name, "Failed to generate metadata file", e)
                return false
            }

            if (createChangelogFile != null) {
                try {
                    val changelogFile = File(tempDir, createChangelogFile.fileName + "." + createChangelogFile.type)
                    changelogFile.createNewFile()
                    logger.info("Created changelog: {}", changelogFile.name)
                } catch (e: Exception) {
                    logger.error("Failed to create changelog file", e)
                    tempDir.deleteRecursively()
                    notifyProjectFailedToGenerate(name, "Failed to create changelog file", e)
                    return false
                }
            }

            // If we got here, everything succeeded in the temp directory
            // Move the temp directory to the final location
            try {
                try {
                    Files.move(tempDir.toPath(), finalDir.toPath(), StandardCopyOption.ATOMIC_MOVE)
                } catch (e: AtomicMoveNotSupportedException) {
                    // Fallback to non-atomic move if atomic move isn't supported (e.g., across filesystems)
                    tempDir.copyRecursively(finalDir, overwrite = true)
                    tempDir.deleteRecursively()
                }
            } catch (e: Exception) {
                logger.error("Failed to move project to final location", e)
                tempDir.deleteRecursively()
                notifyProjectFailedToGenerate(name, "Failed to move project to final location", e)
                return false
            }

            val project = Project(metadata)
            logger.info("Successfully generated project: {}", finalDir.name)
            notifyProjectCreated(project)
            return true
        } catch (e: Exception) {
            logger.error("Unexpected error while generating project", e)
            tempDir.deleteRecursively()
            notifyProjectFailedToGenerate(name, "Unexpected error while generating project", e)
            return false
        }
    }
}