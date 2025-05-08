package io.github.footermandev.tritium.keymap

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

data class Keymap(
    val name: String,
    val bindings: MutableMap<String, MutableList<Shortcut>>,
    val parent: Keymap? = null
) {
    private val json = Json {
        prettyPrint = true
        classDiscriminator = "type"
    }

    fun getShortcuts(actionId: String): List<Shortcut> = bindings[actionId] ?: parent?.getShortcuts(actionId) ?: emptyList()

    fun addShortcut(actionId: String, shortcut: Shortcut) { bindings.computeIfAbsent(actionId) { mutableListOf() }.add(shortcut) }

    fun getAllBindings(): Map<String, List<Shortcut>> {
        val combined = parent?.getAllBindings()?.toMutableMap() ?: mutableMapOf()
        bindings.forEach { (id, list) -> combined.merge(id, list) { old, new -> old + new} }
        return combined
    }

    fun findConflicts(shortcut: Shortcut): List<String> =
        getAllBindings().filter { (_, shortcuts) ->
            shortcuts.any {
                when(it) {
                    is Shortcut.Keyboard -> it == shortcut
                    is Shortcut.Mouse -> it == shortcut
                }
            }
        }.map { it.key }

    fun toSerializable(): SerializableKeymap = SerializableKeymap(
        name,
        parent?.name,
        bindings.mapValues { it.value.toList() }.toMutableMap()
    )

    fun saveKeymapToFile(keymap: Keymap, file: File) {
        val encoded = json.encodeToString(keymap.toSerializable())
        file.writeText(encoded)
    }

    fun loadKeymapFromFile(file: File, allKeymaps: Map<String, Keymap>): Keymap {
        val serial = json.decodeFromString<SerializableKeymap>(file.readText())
        return serial.toKeymap(allKeymaps)
    }
}
