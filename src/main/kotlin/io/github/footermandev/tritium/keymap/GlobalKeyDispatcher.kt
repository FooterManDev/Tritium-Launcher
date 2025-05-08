package io.github.footermandev.tritium.keymap

import java.awt.KeyEventDispatcher
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class GlobalKeyDispatcher(
    private val keymapMngr: KeymapMngr,
    private val actionMngr: ActionMngr
): KeyEventDispatcher {

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if(e.id != KeyEvent.KEY_PRESSED) return false

        val stroke = KeyStroke.getKeyStrokeForEvent(e)

        val actions = keymapMngr.activeKeymap?.getAllBindings()?.filter { (_, shortcuts) ->
            shortcuts.any {
                it is Shortcut.Keyboard && KeyStroke.getKeyStroke(it.keyStroke) == stroke
            }
        }?.keys.orEmpty()

        actions.forEach { actionId ->
            val action = actionMngr.get(actionId)
            if(action?.context == ActionContext.GLOBAL) {
                action.perform()
                return true
            }
        }

        return false
    }
}