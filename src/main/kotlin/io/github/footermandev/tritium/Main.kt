package io.github.footermandev.tritium

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.util.SystemInfo
import com.microsoft.aad.msal4j.SilentParameters
import io.github.footermandev.tritium.auth.MSAL
import io.github.footermandev.tritium.auth.MicrosoftAuth
import io.github.footermandev.tritium.auth.ProfileMngr
import io.github.footermandev.tritium.core.ProjectMngr
import io.github.footermandev.tritium.ui.dashboard.Dashboard
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

    attemptAutoSignIn()

    flatLaf()

    StartupEventMngr.launchAllEvents()

    SwingUtilities.invokeLater {
        if(ProjectMngr.activeProject != null) {
            mainLogger.info("Loading active project.")
            ProjectMngr.loadActiveProject()
        } else {
            mainLogger.info("Opening Dashboard, no active project.")
            Dashboard().isVisible = true
        }
    }
}

fun flatLaf() {

    if(SystemInfo.isLinux) {
        UIManager.put("TitlePane.showIcon", false)
        UIManager.put("TitlePane.titleText", "")
    }
    if(SystemInfo.isMacOS) {
        System.setProperty("apple.laf.useScreenMenuBar", "true")
    }
    UIManager.put("Component.hideMnemonics", false)

    FlatDarkLaf.setup()
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
                val mcToken = MicrosoftAuth.getMCToken(result.accessToken())
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