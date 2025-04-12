package io.github.footermandev.tritium.core

import io.github.footermandev.tritium.model.serializers.JavaPathSerializer
import kotlinx.serialization.Serializable
import java.nio.file.Path

/**
 * @param fileName The name of the changelog file.
 * @param path The location of the changelog file.
 * @param type The type of the changelog file. Defaults to Markdown.
 */
@Serializable
data class ChangelogFileSpec(
    val fileName: String,
    @Serializable(with = JavaPathSerializer::class)
    val path: Path? = null,
    val type: String = "md"
)
