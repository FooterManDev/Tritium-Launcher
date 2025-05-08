package io.github.footermandev.tritium.keymap

class KeymapMngr {
    private val keymaps = mutableMapOf<String, Keymap>()
    private val listeners = mutableListOf<() -> Unit>()
    var activeKeymap: Keymap? = null
        set(value) {
            field = value
            listeners.forEach { it() }
        }

    fun registerKeymap(keymap: Keymap) { keymaps[keymap.name] = keymap }

    fun getKeymap(name: String): Keymap? = keymaps[name]

    fun removeKeymap(name: String) = {
        keymaps.remove(name)
        if(activeKeymap?.name == name) activeKeymap = null
    }

    fun getAllKeymaps(): List<Keymap> = keymaps.values.toList()

    fun hasKeymap(name: String): Boolean = keymaps.containsKey(name)

    fun setActiveKeymapByName(name: String): Boolean {
        val map = keymaps[name]
        return if(map != null) {
            activeKeymap = map
            true
        } else false
    }

    fun addListener(listener: () -> Unit) = listeners.add(listener)
}