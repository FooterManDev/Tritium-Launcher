package io.github.footermandev.tritium.core.modpack

import com.formdev.flatlaf.extras.FlatSVGIcon
import com.google.auto.service.AutoService
import io.github.footermandev.tritium.core.modpack.ModpackType
import io.github.footermandev.tritium.ui.icons.TRIcons

@AutoService(ModpackType::class)
data class Modrinth(
    override val id: String = "modrinth",
    override val displayName: String = "Modrinth",
    override val icon: FlatSVGIcon = TRIcons.Modrinth,
    override val webpage: String = "https://modrinth.com/"
) : ModpackType() {
    override fun toString(): String = id

    init {
        types.add(this)
    }
}
