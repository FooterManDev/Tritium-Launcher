package io.github.footermandev.tritium.core

import com.akuleshov7.ktoml.exceptions.TomlEncodingException
import io.github.footermandev.tritium.core.mod.Mod
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

@Serializable
/**
 * @param name The name of the project
 * @param path The path to the project
 * @param icon The path to the icon for the project
 * @param version The version of the project
 * @param changelogFile The changelog specifications
 *
 * @param minecraftVersion The Minecraft version of the project
 *
 * @param modLoader The Mod Loader of the project
 * @param modLoaderVersion The Mod Loader version of the project
 *
 * @param type The type of the project
 *
 * @param mods The mods in the project
 *
 * @param authors The authors of the project
 *
 * @param titleColor The title color of the project. Supports hex color codes, via #, 0x or without prefixes.
 */
data class ProjectMetadata(
    var name: String,
    var path: String,
    var icon: String,
    var version: String,
    var changelogFile: ChangelogFileSpec?,

    var minecraftVersion: String,

    var modLoader: String,
    var modLoaderVersion: String,

    var type: String,

    var mods: List<Mod>,

    var authors: List<Pair<String, String>>, // Name, Title

    var titleColor: String?
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)

        fun generateFile(metadata: ProjectMetadata, path: File): Boolean {
            val fileName = "trmeta.json"
            val file = File(path, fileName)
            try {
                if(file.exists()) {
                    logger.warn("Attempted to generate existing metadata file.")
                    return false
                }
                file.writeText(Json.encodeToString(serializer(), metadata))
                logger.info("Generated {}", fileName)
                logger.info(
                    "Encoded Metadata:" +
                    "\n ${metadata.name}" +
                    "\n ${metadata.path}" +
                    "\n ${metadata.icon}" +
                    "\n ${metadata.version}" +
                    "\n ${metadata.changelogFile}" +
                    "\n ${metadata.minecraftVersion}" +
                    "\n ${metadata.modLoader}" +
                    "\n ${metadata.modLoaderVersion}" +
                    "\n ${metadata.type}" +
                    "\n ${metadata.mods}" +
                    "\n ${metadata.authors}"
                )
                return true
            } catch (e: TomlEncodingException) {
                logger.error("Error encoding metadata: {}", e.message, e)
                return false
            } catch (e: IOException) {
                logger.error("Error writing metadata: {}", e.message, e)
                return false
            }
        }
    }
}