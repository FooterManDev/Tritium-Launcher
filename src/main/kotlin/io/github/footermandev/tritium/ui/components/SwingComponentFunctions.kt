package io.github.footermandev.tritium.ui.components

import java.awt.Component
import java.awt.Font
import java.awt.Insets
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

/**
 * Simplifies the [Insets] constructor.
 */
fun insets(all: Int) = Insets(all, all, all, all)

/**
 * Simplifies the [javax.swing.border.EmptyBorder] constructor
 */
fun emptyBorder(all: Int) = EmptyBorder(all, all, all, all)

fun JPanel.addEach(vararg components: Component) {
    components.forEach { add(it) }
}

fun JPanel.addEach(components: Array<out Component>, constraints: Any) {
    components.forEach { add(it, constraints) }
}

fun Font.scale(scale: Float): Font {
    return deriveFont(this.size2D * scale)
}