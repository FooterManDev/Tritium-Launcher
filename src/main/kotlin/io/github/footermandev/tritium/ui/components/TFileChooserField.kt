package io.github.footermandev.tritium.ui.components

import io.github.footermandev.tritium.dim
import io.github.footermandev.tritium.ui.theme.TIcons
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class TFileChooserField(
    private val mode: Int = JFileChooser.FILES_ONLY,
    private val extensions: FileNameExtensionFilter? = null,
    initialText: String = "",
    columns: Int = 20,
    buttonIcon: Icon? = TIcons.OpenFile
): JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {

    val field = JTextField(initialText, columns)

    private val browseBtn = JButton().apply {
        val btnIcon = buttonIcon.apply {
            size = dim(20, field.height - 2)
        }
        this.icon = buttonIcon
        isContentAreaFilled = false
        border = BorderFactory.createEmptyBorder(0,4,0,4)
    }

    init {
        add(field)
        add(Box.createHorizontalStrut(8))
        add(browseBtn)

        browseBtn.addActionListener {
            val chooser = JFileChooser().apply { fileSelectionMode = mode; if(extensions != null) fileFilter = extensions }
            if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                val file = chooser.selectedFile
                if(file != null) {
                    field.text = file.path
                }
            }
        }
    }
}