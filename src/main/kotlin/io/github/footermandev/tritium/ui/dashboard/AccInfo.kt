package io.github.footermandev.tritium.ui.dashboard

import io.github.footermandev.tritium.Constants
import io.github.footermandev.tritium.auth.ProfileMngr
import io.github.footermandev.tritium.loadImage
import io.github.footermandev.tritium.logger
import io.github.footermandev.tritium.ui.components.TritiumLoadingIcon
import io.github.footermandev.tritium.ui.components.addEach
import java.awt.Dimension
import java.awt.Font
import javax.swing.*

internal class AccInfo : JPanel() {
    private val logger = logger()
    private val loadingIcon = TritiumLoadingIcon(25).apply {
        alignmentX = CENTER_ALIGNMENT
    }

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        preferredSize = Dimension(75, 60)
        minimumSize = preferredSize
        maximumSize = preferredSize
        isOpaque = false
        alignmentX = CENTER_ALIGNMENT

        add(Box.createVerticalGlue())
        add(JPanel().apply {
            isOpaque = false
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(Box.createHorizontalGlue())
            add(loadingIcon)
            add(Box.createHorizontalGlue())
        })
        add(Box.createVerticalGlue())

        loadingIcon.setProgress(0.0)

        ProfileMngr.addProgressListener { progress ->
            SwingUtilities.invokeLater {
                loadingIcon.setProgress(progress)
                if (progress >= 1.0) {
                    Timer(500) {
                        loadingIcon.isVisible = false
                    }.apply {
                        isRepeats = false
                        start()
                    }
                }
            }
        }

        ProfileMngr.addListener { p ->
            SwingUtilities.invokeLater {
                removeAll()
                
                if(p != null) {
                    add(Box.createVerticalGlue())
                    val face = JLabel(loadImage(Constants.FACE_URL + p.id, 25, 25, true)).apply {
                        alignmentX = CENTER_ALIGNMENT
                    }
                    val name = JLabel(p.name).apply { 
                        font = Font("Arial", Font.BOLD, 10)
                        alignmentX = CENTER_ALIGNMENT
                        preferredSize = Dimension(75, 15)
                        horizontalAlignment = SwingConstants.CENTER
                    }

                    addEach(face, name)
                    add(Box.createVerticalGlue())
                } else {
                    logger.warn("No profile found, cannot populate UI.")
                    add(Box.createVerticalGlue())
                    add(JLabel("No profile found").apply {
                        font = Font("Arial", Font.PLAIN, 10)
                        alignmentX = CENTER_ALIGNMENT
                    })
                    add(Box.createVerticalGlue())
                }
                revalidate()
                repaint()
            }
        }
    }
}