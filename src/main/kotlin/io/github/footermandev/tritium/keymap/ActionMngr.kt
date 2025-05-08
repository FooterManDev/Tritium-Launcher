package io.github.footermandev.tritium.keymap

object ActionMngr {
    private val actions = mutableMapOf<String, TAction>()

    fun register(action: TAction) = { actions[action.id] = action }

    fun get(id: String): TAction? = actions[id]
    fun perform(id: String) = actions[id]?.perform()
}