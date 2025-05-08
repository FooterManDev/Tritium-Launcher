package io.github.footermandev.tritium

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.util.SystemInfo
import com.microsoft.aad.msal4j.SilentParameters
import io.github.footermandev.tritium.auth.MSAL
import io.github.footermandev.tritium.auth.MicrosoftAuth
import io.github.footermandev.tritium.auth.ProfileMngr
import io.github.footermandev.tritium.core.Project
import io.github.footermandev.tritium.core.ProjectMngr
import io.github.footermandev.tritium.core.ProjectMngrListener
import io.github.footermandev.tritium.keymap.KeymapService
import io.github.footermandev.tritium.ui.dashboard.Dashboard
import io.github.footermandev.tritium.ui.project.ProjectFrame
import io.github.footermandev.tritium.ui.theme.TIcons
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.swing.SwingUtilities
import javax.swing.UIManager

val mainLogger: Logger = LoggerFactory.getLogger(Constants.TR + "::Main")

fun main(args: Array<String>) {
    mainLogger.info("Starting with args: ${args.joinToString(" ")}")

    // Start the MSAL process
    attemptAutoSignIn()

    // Set up FlatLaf stuff
    flatLaf()

    // Set up keyboard stuff
    KeymapService.init()

    ProjectMngr.addListener(object : ProjectMngrListener {
        override fun onProjectCreated(project: Project) {}
        override fun onProjectOpened(project: Project) {
            ProjectFrame(project)
        }
        override fun onProjectDeleted(project: Project) {}
        override fun onProjectUpdated(project: Project) {}
        override fun onProjectFinishedLoading(projects: List<Project>) {}
        override fun onProjectFailedToGenerate(
            project: String,
            errorMsg: String,
            exception: Exception?
        ) {}
    })

    ProjectMngr.loadActiveProject()

    SwingUtilities.invokeLater {
        if(ProjectMngr.activeProject == null) Dashboard()
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun attemptAutoSignIn() {
    GlobalScope.launch {
        try {
            val accounts = MSAL.app.accounts.await()
            val account = accounts.iterator().asSequence().firstOrNull()
            if(account != null) {
                val scopes = setOf("XboxLive.signin", "offline_access")
                val params = SilentParameters.builder(scopes, account).build()
                val result = MSAL.app.acquireTokenSilently(params).await()

                mainLogger.info("Silent sign-in succeeded.")
                val mcToken = MicrosoftAuth().getMCToken(result.accessToken())
                ProfileMngr.Cache.init(mcToken)
            } else {
                mainLogger.info("No accounts available.")
                return@launch
            }
        } catch (e: Exception) {
            mainLogger.error("Sign-in failed: ${e.message}", e)
            throw e
        }
    }
}

fun flatLaf() {
    if(SystemInfo.isMacOS) {
        System.setProperty("apple.laf.useScreenMenuBar", "true")
    }
    UIManager.put("Component.hideMnemonics", false)

    FlatDarkLaf.setup()

    UIManager.put("TitlePane.closeIcon", TIcons.WindowIcons.Close)
    UIManager.put("TitlePane.iconifyIcon", TIcons.WindowIcons.Iconify)
    UIManager.put("TitlePane.maximizeIcon", TIcons.WindowIcons.RestoreMax)
    UIManager.put("TitlePane.restoreIcon", TIcons.WindowIcons.RestoreMin)
}