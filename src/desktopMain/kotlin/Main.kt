import com.formdev.flatlaf.FlatDarkLaf
import javax.swing.SwingUtilities

fun main() {
    FlatDarkLaf.setup()
    SwingUtilities.invokeLater { MainFrame() }
}