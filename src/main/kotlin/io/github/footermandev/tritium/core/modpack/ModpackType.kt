package io.github.footermandev.tritium.core.modpack

import com.formdev.flatlaf.extras.FlatSVGIcon

abstract class ModpackType {
    abstract val id: String
    abstract val displayName: String
    abstract val icon: FlatSVGIcon
    abstract val webpage: String

    companion object {
        val types = mutableListOf<ModpackType>()
    }

    override fun toString(): String = id
}