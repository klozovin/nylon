package compositor

import wayland.KeyboardKeyState
import wayland.server.Listener
import wlroots.types.input.InputDevice
import wlroots.types.keyboard.Keyboard
import wlroots.types.keyboard.KeyEvent
import wlroots.types.keyboard.KeyboardModifier
import xkbcommon.Keymap
import xkbcommon.XkbContext
import xkbcommon.XkbKey

class NyKeyboard(val compositor: Compositor, val wlrKeyboard: Keyboard) {

    val listeners: MutableList<Listener> = arrayListOf()


    init {
        val context = XkbContext.of(XkbContext.Flags.NO_FLAGS) ?: error("Failed creating XKB context")
        val keymap = context.keymapNewFromNames(null, Keymap.CompileFlags.NO_FLAGS)
            ?: error("Failed creating XKB keymap")
        wlrKeyboard.setKeymap(keymap)
        wlrKeyboard.setRepeatInfo(25, 200)

        with(wlrKeyboard) {
            listeners.add(events.key.add(::onKey))
            listeners.add(events.modifiers.add(::onModifiers))
            listeners.add(base.events.destroy.add(::onDestroy))
        }

        keymap.unref()
        context.unref()
    }


    fun onKey(event: KeyEvent) {
        val keyboard = wlrKeyboard
        val keycode = event.keycode + 8
        val keysym = keyboard.getXkbState().keyGetOneSym(keycode)

        // Propagate the key to the focused client? Only if the compositor doesn't handle that key combo.
        var propagateKey = false

        val keyboardModifiers = keyboard.getKeyboardModifiers()

        if (keyboardModifiers.contains(KeyboardModifier.Alt) && event.state == KeyboardKeyState.PRESSED) {
            when (keysym) {
                XkbKey.F1 -> compositor.windowSystem.focusNextToplevel()

                XkbKey.F2 -> {
                    compositor.terminalPath?.let {
                        compositor.startProcess(it)
                    }
                }

                XkbKey.Escape -> compositor.stop()

                else -> propagateKey = true

            }
        }

        if (keyboardModifiers.contains(KeyboardModifier.Logo) && event.state == KeyboardKeyState.PRESSED) {
            when (keysym) {
                XkbKey.Insert -> println("Meta+Insert")

                else -> propagateKey = true
            }
        }

        if (propagateKey) {
            compositor.seat.setKeyboard(keyboard)
            compositor.seat.keyboardNotifyKey(event.timeMsec, event.keycode, event.state)
        }
    }


    fun onModifiers(wlrKeyboard: Keyboard) {
        compositor.seat.setKeyboard(wlrKeyboard)
        compositor.seat.keyboardNotifyModifiers(wlrKeyboard.getModifiers())
    }


    fun onDestroy(device: InputDevice) {
        listeners.forEach { it.remove() }
        compositor.inputSystem.nyKeyboards.remove(this)
        assert(listeners.size == 3)
    }
}