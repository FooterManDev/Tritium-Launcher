package io.github.footermandev.tritium.keymap

import io.github.footermandev.tritium.keymap.KeymapService.actionMngr
import java.awt.AWTEvent
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent

class GlobalMouseDispatcher(
    private val keymapManager: KeymapMngr,
    private val actionManager: ActionMngr
) : AWTEventListener {

    override fun eventDispatched(event: AWTEvent) {
        if (event !is MouseEvent) return
        if (event.id != MouseEvent.MOUSE_PRESSED) return

        val shortcut = Shortcut.Mouse(
            button = event.button,
            modifiers = event.modifiersEx,
            clickCount = event.clickCount
        )

        val actions = keymapManager.activeKeymap?.getAllBindings()?.filter { (_, shortcuts) ->
            shortcuts.any {
                it is Shortcut.Mouse &&
                        it.button == shortcut.button &&
                        it.modifiers == shortcut.modifiers &&
                        it.clickCount == shortcut.clickCount
            }
        }?.keys.orEmpty()

        for (actionId in actions) {
            val action = actionMngr.get(actionId)
            if (action?.context == ActionContext.GLOBAL) {
                action.perform()
                event.consume()
                break
            }
        }
    }
}
