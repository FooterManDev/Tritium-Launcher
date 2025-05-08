package io.github.footermandev.tritium.keymap

interface TAction {
    val id: String
    val name: String
    val context: ActionContext
    fun perform()
}