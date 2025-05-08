package io.github.footermandev.tritium.keymap

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Shortcut {
    @Serializable
    @SerialName("keyboard")
    data class Keyboard(val keyStroke: String) : Shortcut()

    @Serializable
    @SerialName("mouse")
    data class Mouse(val button: Int, val modifiers: Int, val clickCount: Int) : Shortcut()
}

@Serializable
data class SerializableKeymap(
    val name: String,
    val parentName: String? = null,
    val bindings: MutableMap<String, List<Shortcut>>
) {
    fun toKeymap(allKeymaps: Map<String, Keymap>): Keymap = Keymap(
        name,
        bindings.mapValues { it.value.toMutableList() }.toMutableMap(),
        parentName?.let { allKeymaps[it] }
    )
}