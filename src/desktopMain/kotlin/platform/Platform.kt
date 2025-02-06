package platform

import Constants
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
object Platform {
    val osName: String = System.getProperty("os.name").lowercase()
    val userHome: String = System.getProperty("user.home")
    val tempDir: File = File(System.getProperty("java.io.tmpdir"))

    val sysConfigDir: File by lazy {
        when {
            osName.contains("win") ->
                File(System.getenv("APPDATA") ?: "$userHome\\AppData\\Roaming")
            osName.contains("mac") ->
                File("$userHome/Library/Application Support")
            else ->
                File("$userHome/.config")
        }
    }

    fun getConfigDir(): File {
        val dir = File(sysConfigDir, Constants.TR)
        if(!dir.exists()) dir.mkdirs()
        return dir
    }

    val isWin: Boolean = osName.contains("win")
    val isMac: Boolean = osName.contains("mac")

    override fun toString(): String {
        return "OS: $osName, \n" +
                "User Home: $userHome, \n" +
                "Temp Dir: ${tempDir.absolutePath}"
    }
}