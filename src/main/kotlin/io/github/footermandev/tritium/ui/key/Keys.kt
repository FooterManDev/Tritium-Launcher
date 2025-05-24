package io.github.footermandev.tritium.ui.key

import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JMenu

/**
 * Wrapper singleton for [KeyEvent]
 */
@Suppress("ObjectPropertyName")
object Keys {
    val A           = Key(KeyEvent.VK_A)
    val B           = Key(KeyEvent.VK_B)
    val C           = Key(KeyEvent.VK_C)
    val D           = Key(KeyEvent.VK_D)
    val E           = Key(KeyEvent.VK_E)
    val F           = Key(KeyEvent.VK_F)
    val G           = Key(KeyEvent.VK_G)
    val H           = Key(KeyEvent.VK_H)
    val I           = Key(KeyEvent.VK_I)
    val J           = Key(KeyEvent.VK_J)
    val K           = Key(KeyEvent.VK_K)
    val L           = Key(KeyEvent.VK_L)
    val M           = Key(KeyEvent.VK_M)
    val N           = Key(KeyEvent.VK_N)
    val O           = Key(KeyEvent.VK_O)
    val P           = Key(KeyEvent.VK_P)
    val Q           = Key(KeyEvent.VK_Q)
    val R           = Key(KeyEvent.VK_R)
    val S           = Key(KeyEvent.VK_S)
    val T           = Key(KeyEvent.VK_T)
    val U           = Key(KeyEvent.VK_U)
    val V           = Key(KeyEvent.VK_V)
    val W           = Key(KeyEvent.VK_W)
    val X           = Key(KeyEvent.VK_X)
    val Y           = Key(KeyEvent.VK_Y)
    val Z           = Key(KeyEvent.VK_Z)

    val `1`         = Key(KeyEvent.VK_1)
    val `2`         = Key(KeyEvent.VK_2)
    val `3`         = Key(KeyEvent.VK_3)
    val `4`         = Key(KeyEvent.VK_4)
    val `5`         = Key(KeyEvent.VK_5)
    val `6`         = Key(KeyEvent.VK_6)
    val `7`         = Key(KeyEvent.VK_7)
    val `8`         = Key(KeyEvent.VK_8)
    val `9`         = Key(KeyEvent.VK_9)
    val `0`         = Key(KeyEvent.VK_0)

    val F1          = Key(KeyEvent.VK_F1)
    val F2          = Key(KeyEvent.VK_F2)
    val F3          = Key(KeyEvent.VK_F3)
    val F4          = Key(KeyEvent.VK_F4)
    val F5          = Key(KeyEvent.VK_F5)
    val F6          = Key(KeyEvent.VK_F6)
    val F7          = Key(KeyEvent.VK_F7)
    val F8          = Key(KeyEvent.VK_F8)
    val F9          = Key(KeyEvent.VK_F9)
    val F10         = Key(KeyEvent.VK_F10)
    val F11         = Key(KeyEvent.VK_F11)
    val F12         = Key(KeyEvent.VK_F12)
    val F13         = Key(KeyEvent.VK_F13)
    val F14         = Key(KeyEvent.VK_F14)
    val F15         = Key(KeyEvent.VK_F15)
    val F16         = Key(KeyEvent.VK_F16)
    val F17         = Key(KeyEvent.VK_F17)
    val F18         = Key(KeyEvent.VK_F18)
    val F19         = Key(KeyEvent.VK_F19)
    val F20         = Key(KeyEvent.VK_F20)
    val F21         = Key(KeyEvent.VK_F21)
    val F22         = Key(KeyEvent.VK_F22)
    val F23         = Key(KeyEvent.VK_F23)
    val F24         = Key(KeyEvent.VK_F24)

    val BACKTICK    = Key(KeyEvent.VK_BACK_QUOTE)
    val SLASH       = Key(KeyEvent.VK_SLASH)
    val BACKSLASH   = Key(KeyEvent.VK_BACK_SLASH)
    val ASTERISK    = Key(KeyEvent.VK_ASTERISK)
    val SUBTRACT    = Key(KeyEvent.VK_SUBTRACT)
    val PLUS        = Key(KeyEvent.VK_PLUS)
    val LBRACE      = Key(KeyEvent.VK_BRACELEFT)
    val RBRACE      = Key(KeyEvent.VK_BRACERIGHT)
    val LBRACKET    = Key(KeyEvent.VK_OPEN_BRACKET)
    val RBRACKET    = Key(KeyEvent.VK_CLOSE_BRACKET)
    val DECIMAL     = Key(KeyEvent.VK_DECIMAL)
    val PERIOD      = Key(KeyEvent.VK_PERIOD)
    val COMMA       = Key(KeyEvent.VK_COMMA)
    val COLON       = Key(KeyEvent.VK_COLON)
    val DOUBLEQUOTE = Key(KeyEvent.VK_QUOTEDBL)

    val ENTER       = Key(KeyEvent.VK_ENTER)
    val ESCAPE      = Key(KeyEvent.VK_ESCAPE)
    val SPACE       = Key(KeyEvent.VK_SPACE)
    val BACKSPACE   = Key(KeyEvent.VK_BACK_SPACE)
    val SHIFT       = Key(KeyEvent.VK_SHIFT)
    val CTRL        = Key(KeyEvent.VK_CONTROL)
    val ALT         = Key(KeyEvent.VK_ALT)
    val TAB         = Key(KeyEvent.VK_TAB)
    val CAPS        = Key(KeyEvent.VK_CAPS_LOCK)
    val WINDOWS     = Key(KeyEvent.VK_WINDOWS)
    val UP          = Key(KeyEvent.VK_UP)
    val DOWN        = Key(KeyEvent.VK_DOWN)
    val LEFT        = Key(KeyEvent.VK_LEFT)
    val RIGHT       = Key(KeyEvent.VK_RIGHT)
    val DELETE      = Key(KeyEvent.VK_DELETE)
    val HOME        = Key(KeyEvent.VK_HOME)
    val END         = Key(KeyEvent.VK_END)
    val PGUP        = Key(KeyEvent.VK_PAGE_UP)
    val PGDOWN      = Key(KeyEvent.VK_PAGE_DOWN)
    val PRINT       = Key(KeyEvent.VK_PRINTSCREEN)
    val SCROLLLOCK  = Key(KeyEvent.VK_SCROLL_LOCK)
    val PAUSEBREAK  = Key(KeyEvent.VK_PAUSE)
    val NUMLOCK     = Key(KeyEvent.VK_NUM_LOCK)

    infix fun JMenu.setMnemonic(key: Key) {mnemonic = key.code }

    infix fun JComponent.onKeyPressed(key: Key) = KeyBinder(this, key)

    class KeyBinder(private val comp: JComponent, private val key: Key) {
        infix fun then(action: () -> Unit) {
            comp.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if(e.keyCode == key.code) action()
                }
            })
        }
    }
}