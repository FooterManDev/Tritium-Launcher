import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.util.SystemInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.slf4j.LoggerFactory
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger(Constants.TR + "::Main")
    logger.info("Starting with args: ${args.joinToString(" ")}")

    flatLaf()

    /**
     * TODO: add logic to determine if the Dashboard should launch or the Project view.
     */
    SwingUtilities.invokeLater {
        MainFrame()

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}

fun flatLaf() {

    if(SystemInfo.isLinux) {
        JFrame.setDefaultLookAndFeelDecorated(true)
        JDialog.setDefaultLookAndFeelDecorated(true)
        UIManager.put("TitlePane.showIcon", false)
        UIManager.put("TitlePane.titleText", "")
    }
    if(SystemInfo.isMacOS) {
        System.setProperty("apple.laf.useScreenMenuBar", "true")
    }

    UIManager.put(FlatClientProperties.USE_WINDOW_DECORATIONS, false)
    UIManager.put("TitlePane.useWindowDecorations", false)

    UIManager.put("Component.hideMnemonics", false)

    FlatDarkLaf.setup()
}