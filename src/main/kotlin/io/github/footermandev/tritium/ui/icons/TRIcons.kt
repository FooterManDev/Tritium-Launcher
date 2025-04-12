package io.github.footermandev.tritium.ui.icons

import com.formdev.flatlaf.extras.FlatSVGIcon
import javax.swing.ImageIcon

object TRIcons {
    val Tritium = FlatSVGIcon("icons/tritium.svg")
    val TritiumPng = ImageIcon(javaClass.classLoader.getResource("icons/tritium.png"))

    val DarkTritium = FlatSVGIcon("icons/dark_tritium.svg")

    val Btn = ImageIcon(javaClass.classLoader.getResource("icons/btn.png"))

    val Run = FlatSVGIcon("icons/run.svg")

    val Cross = FlatSVGIcon("icons/cross.svg", 28, 28)
    val Min = FlatSVGIcon("icons/min.svg", 28, 28)
    val Max = FlatSVGIcon("icons/max.svg", 28, 28)

    val OpenFile = FlatSVGIcon("icons/open_file.svg", 16, 16)

    val CurseForge = FlatSVGIcon("icons/curseforge.svg")
    val Modrinth = FlatSVGIcon("icons/modrinth.svg")

    val Fabric = FlatSVGIcon("icons/fabric.svg")
}