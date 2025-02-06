package model.settings

import com.akuleshov7.ktoml.Toml
import kotlinx.coroutines.*
import platform.Platform
import java.io.File
import java.io.InputStream
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService

/**
 * Used for managing the Launcher's settings.
 * TODO: This is not the final Settings system!
 */
object SettingsMngr {

    private val fileDir: File = Platform.getSettingsDir()
    private val file: File = File(fileDir, "settings.toml")

    @Volatile
    var settings: TRSettings = TRSettings()
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init() {
        if(!fileDir.exists()) fileDir.mkdirs()

        val defaults: TRSettings = loadDefault()

        settings = if(file.exists()) {
            try {
                val userSettingsContent = file.readText()
                val userSettings = Toml.decodeFromString(TRSettings.serializer(), userSettingsContent)
                mergeFiles(defaults, userSettings)
            } catch (e: Exception) {
                println("Error reading user settings: ${e.message}. Using default settings.")
                defaults
            }
        } else defaults

        save()

        startWatcher()
    }

    private fun loadDefault(): TRSettings {
        val stream: InputStream? = this::class.java.classLoader.getResourceAsStream("default_settings.toml")
        return if(stream != null) {
            val defaultContent = stream.bufferedReader().readText()
            Toml.decodeFromString(TRSettings.serializer(), defaultContent)
        } else TRSettings()
    }

    private fun mergeFiles(defaults: TRSettings, user: TRSettings): TRSettings {
        return TRSettings(
            ver = maxOf(defaults.ver, user.ver)
        )
    }

    fun save() {
        try {
            val str = Toml.encodeToString(TRSettings.serializer(), settings)
            file.writeText(str)
        } catch (e: Exception) {
            println("Error saving settings: ${e.message}")
        }
    }

    fun update(new: TRSettings) {
        settings = new
        save()
    }

    // Dynamic reloading
    private fun startWatcher() {
        scope.launch {
            try {
                val service: WatchService = FileSystems.getDefault().newWatchService()
                fileDir.toPath().register(service, StandardWatchEventKinds.ENTRY_MODIFY)

                while (isActive) {
                    val key = service.take()
                    for(e in key.pollEvents()) {
                        if(e.kind() == StandardWatchEventKinds.OVERFLOW) continue

                        val changed = e.context() as? Path ?: continue
                        if(changed.toString() == file.name) {
                            println("Settings file changed; reloading.")
                            reload()
                        }
                    }
                    if(!key.reset()) break
                }
            } catch (e: Exception) {
                println("File watcher encountered an error: ${e.message}")
            }
        }
    }

    private fun reload() {
        try {
            val userSettingsContent = file.readText()
            val userSettings = Toml.decodeFromString(TRSettings.serializer(), userSettingsContent)
            val default = loadDefault()
            settings = mergeFiles(default, userSettings)
            println("Settings reloaded: $settings")
        } catch (e: Exception) {
            println("Error reloading settings: ${e.message}")
        }
    }
}