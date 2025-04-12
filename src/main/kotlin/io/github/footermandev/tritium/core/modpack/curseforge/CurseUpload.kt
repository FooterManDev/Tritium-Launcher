package io.github.footermandev.tritium.core.modpack.curseforge

import io.github.footermandev.tritium.core.modpack.ReleaseType
import kotlinx.serialization.Serializable

@Serializable
data class CurseUpload(
    val changelog: String = "",
    val changelogType: CurseChangelogType = CurseChangelogType.TEXT,
    val displayName: String? = null,
    val parentFileID: Int? = null,
    val gameVersions: List<String> = emptyList(),
    val releaseType: ReleaseType = ReleaseType.RELEASE,
    val isMarkedForManualRelease: Boolean = false,
    val relations: List<CurseRelation> = emptyList()
)