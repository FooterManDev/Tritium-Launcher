package io.github.footermandev.tritium.core.modloader

import io.github.footermandev.tritium.Constants.TR_DIR
import io.github.footermandev.tritium.logger
import java.io.File
import java.net.URI
import javax.swing.ImageIcon

/**
 * Open class for ModLoader implementations.
 * Tritium will support NeoForge and Fabric. Support for other Mod Loaders may be added in the future.
 * Additionally, Plugins will be able to add their own ModLoader implementations.
 */
abstract class ModLoader {
    abstract val id: String
    abstract val displayName: String
    abstract val repository: URI
    abstract val oldestVersion: String
    abstract val icon: ImageIcon

    abstract suspend fun download(version: String): Boolean
    abstract suspend fun uninstall(version: String): Boolean
    abstract suspend fun getVersions(): List<String>
    abstract suspend fun getCompatibleVersions(version: String): List<String>

    abstract fun isInstalled(version: String): Boolean
    abstract fun getInstalled(): List<String>

    abstract suspend fun getLatest(): String?
    abstract suspend fun getDownloadUrl(version: String): URI?
    abstract suspend fun update(version: String): Boolean

    companion object {
        val INSTALL_DIR = File(TR_DIR, "loaders")

        val registry = mutableListOf<ModLoader>()
        fun register(loader: ModLoader) {
            val logger = logger()
            logger.debug("Registered ModLoader: {}", loader.displayName)
            registry.add(loader)
        }
    }

    override fun toString(): String = displayName
}