package io.github.footermandev.tritium.ui.dashboard

import io.github.footermandev.tritium.Constants
import io.github.footermandev.tritium.auth.MCProfile
import io.github.footermandev.tritium.auth.MicrosoftAuth
import io.github.footermandev.tritium.auth.ProfileMngr
import io.github.footermandev.tritium.insets
import io.github.footermandev.tritium.loadImage
import io.github.footermandev.tritium.logger
import kotlinx.coroutines.*
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

/**
 * Contains account information, and a way to sign in.
 *
 * Displays the user's Minecraft skin's face, MC username, and MC UUID.
 */
@OptIn(DelicateCoroutinesApi::class)
internal class AccountPanel() : JPanel() {
    private val logger = logger()
    @Volatile private var isLoading: Boolean = true

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        logger.info("Initializing account panel...")
        refreshUI()

        ProfileMngr.addListener { p ->
            logger.info("Profile listener triggered. Profile: ${p?.name}")
            isLoading = false
            SwingUtilities.invokeLater {
                refreshUI()
            }
        }

        GlobalScope.launch(Dispatchers.IO) {
            logger.info("Initial profile check started")
            val profile = ProfileMngr.Cache.get()
            if(profile != null) {
                isLoading = false
            }
            logger.info("Initial profile check finished. Profile: ${profile?.name}")
            SwingUtilities.invokeLater {
                refreshUI()
            }
        }
    }

    private fun createSignInPanel(): JPanel {
        return JPanel().apply {
            val btn = JButton("Sign in")
            btn.addActionListener {
                logger.info("Sign in button clicked")
                GlobalScope.launch(Dispatchers.IO) {
                    MicrosoftAuth.newSignIn { _ ->
                        SwingUtilities.invokeLater {
                            logger.info("Sign-in callback received.")
                            isLoading = false
                            refreshUI()
                        }
                    }
                }
            }
            add(btn)
        }
    }

    private fun refreshUI() {
        logger.info("Refreshing UI")
        removeAll()
        val profile = runBlocking { ProfileMngr.Cache.get() }
        if(profile != null) add(updateUIWithProfile(profile))
        else add(createSignInPanel())
        revalidate()
        repaint()
    }

    private fun updateUIWithProfile(profile: MCProfile): JPanel {
        val accPanel = JPanel().apply {
            layout = GridBagLayout()
            val gbc = GridBagConstraints().apply {
                fill = GridBagConstraints.HORIZONTAL
                insets = insets(10)
                gridx = 0
                gridy = 0
                anchor = GridBagConstraints.CENTER
            }

            val face = loadImage(Constants.FACE_URL + profile.id, 100, 100, true)
            add(JLabel(face), gbc)

            val userName = JLabel(profile.name).apply {
                font = font.deriveFont(18f)
            }
            gbc.gridy = 1
            add(userName, gbc)

            val uuid = JLabel(profile.id).apply {
                font = font.deriveFont(18f)
            }
            gbc.gridy = 2
            add(uuid, gbc)

            val signOutBtn = JButton("Sign out").apply {
                addActionListener {
                    MicrosoftAuth.signOut()
                    isLoading = false
                    refreshUI()
                }
            }
            gbc.gridy = 3
            add(signOutBtn, gbc)
        }
        return accPanel
    }
}