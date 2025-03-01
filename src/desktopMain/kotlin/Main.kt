import com.formdev.flatlaf.FlatDarkLaf
import model.settings.SettingsMngr
import org.slf4j.LoggerFactory
import javax.swing.SwingUtilities

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger(Constants.TR + "::Main")

    SettingsMngr.init()
    logger.info("Initial configuration: ${SettingsMngr.settings}")

    FlatDarkLaf.setup()
    /**
     * TODO: add logic to determine if the Dashboard should launch or the Project view.
     */
    SwingUtilities.invokeLater { MainFrame() }
}