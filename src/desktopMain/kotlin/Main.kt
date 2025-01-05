import auth.AuthStorage
import auth.MCProfile
import auth.MicrosoftAuth
import com.formdev.flatlaf.FlatDarkLaf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import java.awt.Font
import java.awt.Image
import java.awt.Toolkit
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.*

fun main() {
    FlatDarkLaf.setup()
    SwingUtilities.invokeLater { MainFrame() }
}

class MainFrame : JFrame("Tritium Launcher") {
    private val authStatusLabel = JLabel("Not authenticated").apply {
        font = Font("Arial", Font.PLAIN, 16)
        horizontalAlignment = SwingConstants.CENTER
    }
    private val profileImgLabel = JLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
    }
    private val usernameLabel = JLabel().apply {
        font = Font("Arial", Font.BOLD, 16)
        horizontalAlignment = SwingConstants.CENTER
    }
    private val loginBtn = JButton("Login").apply {
        addActionListener { performLogin() }
    }

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(400, 300)
        setLocationRelativeTo(null)
        layout = BorderLayout()

        val contentPanel = JPanel(BorderLayout()).apply {
            add(profileImgLabel, BorderLayout.CENTER)
            add(usernameLabel, BorderLayout.SOUTH)
        }

        add(authStatusLabel, BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)
        add(loginBtn, BorderLayout.SOUTH)

        isVisible = true
    }

    private fun performLogin() {
        authStatusLabel.text = "Authenticating..."
        loginBtn.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = MicrosoftAuth.auth()
                if(profile != null) {
                    updateUIForAuthenticated(profile)
                } else showErrorDialog("Auth failed. User may not own Minecraft.")
            } catch (e: Exception) {
                e.printStackTrace()
                showErrorDialog("An error occurred: ${e.message}")
            } finally {
                loginBtn.isEnabled = true
                authStatusLabel.text = "Not authenticated"
            }
        }
    }

    private fun updateUIForAuthenticated(profile: MCProfile) {
        SwingUtilities.invokeLater {
            authStatusLabel.text = "Welcome, ${profile.name}"
            usernameLabel.text = profile.name

            val skinUrl = profile.skins.firstOrNull()?.url
            if (skinUrl != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val img = Toolkit.getDefaultToolkit().createImage(skinUrl.toUrl())
                        profileImgLabel.icon = ImageIcon(img.getScaledInstance(100, 100, Image.SCALE_SMOOTH))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else profileImgLabel.icon = null
        }
    }

    private fun showErrorDialog(message: String) {
        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE)
        }
    }
}