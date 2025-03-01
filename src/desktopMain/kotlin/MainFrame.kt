import auth.MSAL
import auth.MicrosoftAuth
import auth.ProfileMngr
import com.microsoft.aad.msal4j.SilentParameters
import core.Project
import core.ProjectMngr
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import model.WindowStateMngr
import org.slf4j.LoggerFactory
import ui.dashboard.Dashboard
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JSplitPane
import kotlin.system.exitProcess

class MainFrame : JFrame() {
    private val scopes = setOf("XboxLive.signin", "offline_access")

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        title = "Tritium"
        defaultCloseOperation = DO_NOTHING_ON_CLOSE // This is set for saving the window state
        size = Dimension(800, 600)
        minimumSize = Dimension(600, 500)
        iconImage = ImageIcon(javaClass.classLoader.getResource("images/icon.png")).image

        attemptAutoSignIn()

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

    @OptIn(DelicateCoroutinesApi::class)
    private fun attemptAutoSignIn() {
        GlobalScope.launch {
            try {
                val accounts = MSAL.app.accounts.await()
                val account = accounts.iterator().asSequence().firstOrNull()
                if(account != null) {
                    val params = SilentParameters.builder(scopes, account).build()
                    val result = MSAL.app.acquireTokenSilently(params).await()

                    logger.info("Silent sign-in succeeded.")
                    val mcToken = MicrosoftAuth.getMCToken(result.accessToken())
                    ProfileMngr.Cache.init(mcToken)
                } else {
                    logger.info("No accounts available.")
                    return@launch
                }
            } catch (e: Exception) {
                logger.error("Sign-in failed: ${e.message}", e)
                throw e
            }
        }
    }

    // TODO
    private fun setupDashboard() {
        isVisible = false
        val dashboard = Dashboard()
        dashboard.isVisible = true
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