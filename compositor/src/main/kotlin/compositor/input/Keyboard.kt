package compositor.input

import compositor.Compositor
import wayland.KeyboardKeyState
import wayland.server.Listener
import wlroots.types.input.InputDevice
import wlroots.types.keyboard.KeyEvent
import wlroots.types.keyboard.Keyboard
import wlroots.types.keyboard.KeyboardModifier
import xkbcommon.Keymap
import xkbcommon.XkbContext
import xkbcommon.XkbKey

class Keyboard(val compositor: Compositor, val wlrKeyboard: Keyboard) {

    val listeners: MutableList<Listener> = mutableListOf()


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
        println("key: $event")
        val keycode = event.keycode + 8
        val keysym = wlrKeyboard.getXkbState().keyGetOneSym(keycode)
        val modifiers = wlrKeyboard.getKeyboardModifiers()


        if (compositor.captureMode.state is CursorState.WindowMove) {
//            compositor.captureMode.onKey(event)
            println("NOPE NOPE NOPE")
            return
        }

        // Propagate the key to the focused client? Only if the compositor doesn't handle that key combo.
        var propagateKey = false
        when {
            modifiers.contains(KeyboardModifier.Alt) && event.state == KeyboardKeyState.PRESSED -> {
                when (keysym) {
                    XkbKey.F1 -> compositor.windowSystem.focusNextToplevel()

                    XkbKey.F2 -> compositor.terminalPath?.let {
                        compositor.startProcess(it)
                    }

                    XkbKey.Escape -> compositor.stop()
                }
            }

            modifiers.contains(KeyboardModifier.Logo) && event.state == KeyboardKeyState.PRESSED -> {
                when (keysym) {
                    XkbKey.Insert -> println("Meta+Insert")
                }

            }

            else -> propagateKey = true
        }

        if (propagateKey) {
            println("Propagatin key: $event")
            compositor.seat.setKeyboard(wlrKeyboard)
            compositor.seat.keyboardNotifyKey(event.timeMsec, event.keycode, event.state)
        } else {
            println("NOT propagatin: $event")
        }
    }


    fun onModifiers(wlrKeyboard: Keyboard) {
        compositor.seat.setKeyboard(wlrKeyboard)
        compositor.seat.keyboardNotifyModifiers(wlrKeyboard.getModifiers())
    }


    fun onDestroy(device: InputDevice) {
        // Remove listeners on the wlroots side.
        listeners.forEach { it.remove() }

        // Remove Java wrappers for listeners.
        listeners.clear()

        // TODO: Memory management: Take care of Signals/Listeners

        // Remove the `Keyboard` object from the input system, and check that it succeeded in that.
        val removed = compositor.inputSystem.keyboards.remove(this)
        assert(removed)
    }
}