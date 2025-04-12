package io.github.footermandev.tritium.core.modpack

import com.formdev.flatlaf.extras.FlatSVGIcon
import com.google.auto.service.AutoService
import io.github.footermandev.tritium.ui.icons.TRIcons

@AutoService(ModpackType::class)
data class CurseForge(
    override val id: String = "curseforge",
    override val displayName: String = "CurseForge",
    override val icon: FlatSVGIcon = TRIcons.CurseForge,
    override val webpage: String = "https://www.curseforge.com/"
) : ModpackType() {
    override fun toString(): String = id

    init {
        types.add(this)
    }
}