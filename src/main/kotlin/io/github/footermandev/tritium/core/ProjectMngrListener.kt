package io.github.footermandev.tritium.core

interface ProjectMngrListener {
    fun onProjectCreated(project: Project)
    fun onProjectOpened(project: Project)
    fun onProjectDeleted(project: Project)
    fun onProjectUpdated(project: Project)
    fun onProjectFinishedLoading(projects: List<Project>)
    fun onProjectFailedToGenerate(project: String, errorMsg: String, exception: Exception?)
}