package io.github.footermandev.tritium.keymap

import java.awt.AWTEvent.MOUSE_EVENT_MASK
import java.awt.KeyboardFocusManager
import java.awt.Toolkit

object KeymapService {
    val keymapMngr = KeymapMngr()
    val actionMngr = ActionMngr

    fun init() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(
            GlobalKeyDispatcher(keymapMngr, actionMngr)
        )

        Toolkit.getDefaultToolkit().addAWTEventListener(
            GlobalMouseDispatcher(keymapMngr, actionMngr),
            MOUSE_EVENT_MASK
        )

        if(keymapMngr.activeKeymap == null) {
            val defaultKeymap = Keymap("Default", mutableMapOf())
            keymapMngr.activeKeymap = defaultKeymap
        }
    }
}