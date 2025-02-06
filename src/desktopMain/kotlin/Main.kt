import com.formdev.flatlaf.FlatDarkLaf
import model.settings.SettingsMngr
import javax.swing.SwingUtilities

fun main() {

    SettingsMngr.init()
    println("Initial configuration: ${SettingsMngr.settings}")

    FlatDarkLaf.setup()
    SwingUtilities.invokeLater { MainFrame() }
}