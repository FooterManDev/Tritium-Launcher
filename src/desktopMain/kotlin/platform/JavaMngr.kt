package platform

import model.settings.TRSettings
import java.io.File
import javax.swing.JOptionPane

/**
 * Used for finding and selecting Java versions.
 * This eventually needs to be changed to detect Java installations from anywhere in the user's home directory, and other system directories.
 * It may include functions for installing Java versions in the future.
 * TODO: This is not final! Must be properly done in the near future.
 */
object JavaMngr {
    fun findAll(): Map<String, List<File>> {
        val results = mutableMapOf<String, MutableList<File>>()

        val dirs: List<File> = when {
            Platform.isWin -> listOf(
                File("C:\\Program Files\\Java"),
                File("C:\\Program Files (x86)\\Java")
            )
            Platform.isMac -> listOf(
                File("/Library/Java/JavaVirtualMachines"),
                File("${Platform.userHome}/Library/Java/JavaVirtualMachines")
            )
            else -> listOf(File("/usr/lib/jvm")) // Linux and others
        }

        for(dir in dirs) {
            if(!dir.exists() || !dir.isDirectory) continue
            dir.listFiles()?.forEach { candidate ->
                val bin = File(candidate, "bin")
                val jFile = if(Platform.isWin)
                    File(bin, "java.exe")
                else
                    File(bin, "java")
                if(jFile.exists() && jFile.canExecute()) {
                    val lower = candidate.name.lowercase()
                    when {
                        "8" in lower -> results.getOrPut("8") { mutableListOf() }.add(candidate)
                        "16" in lower -> results.getOrPut("16") { mutableListOf() }.add(candidate)
                        "17" in lower -> results.getOrPut("17") { mutableListOf() }.add(candidate)
                        "21" in lower -> results.getOrPut("21") { mutableListOf() }.add(candidate)
                    }
                }
            }
        }

        return results
    }

    fun promptForJavaSelection(candidates: Map<String, List<File>>): TRSettings.Java {
        val selected = TRSettings.Java()
        val j8 = candidates["8"]
        val j16 = candidates["16"]
        val j17 = candidates["17"]
        val j21 = candidates["21"]

        fun selectForVersion(ver: String, candidates: List<File>?): String? {
            if(candidates.isNullOrEmpty()) return null
            val paths = candidates.map { it.absolutePath }.toTypedArray()
            val selectedValue = JOptionPane.showInputDialog(
                null,
                "Select Java $ver install:",
                "Java $ver",
                JOptionPane.QUESTION_MESSAGE,
                null,
                paths,
                paths.first()
            )
            return selectedValue as? String
        }

        val j8Path = selectForVersion("8", j8)
        val j16Path = selectForVersion("16", j16)
        val j17Path = selectForVersion("17", j17)
        val j21Path = selectForVersion("21", j21)

        return TRSettings.Java(j8Path, j16Path, j17Path, j21Path)
    }
}