package compositor.input

import compositor.Compositor
import compositor.Timer
import compositor.input.CursorInputState.*
import wayland.KeyboardKeyState
import wayland.KeyboardKeyState.Pressed
import wayland.KeyboardKeyState.Released
import wayland.server.Listener
import wlroots.types.input.InputDevice
import wlroots.types.keyboard.KeyEvent
import wlroots.types.keyboard.Keyboard
import wlroots.types.keyboard.KeyboardModifier
import wlroots.types.keyboard.KeyboardModifier.Alt
import wlroots.util.Log
import xkbcommon.Keymap
import xkbcommon.XkbContext
import xkbcommon.XkbKey
import java.util.*


typealias Modifiers = EnumSet<KeyboardModifier>


class Keyboard(val compositor: Compositor, val wlrKeyboard: Keyboard) {

    val listeners: MutableList<Listener> = mutableListOf()
    val repeatTimer = RepeatTimer()
    var repeatKey: Int? = null


    init {
        val context = XkbContext.of(XkbContext.Flags.NO_FLAGS) ?: error("Failed creating XKB context")
        val keymap = context.keymapNewFromNames(null, Keymap.CompileFlags.NO_FLAGS)
            ?: error("Failed creating XKB keymap")
        wlrKeyboard.setKeymap(keymap)
        wlrKeyboard.setRepeatInfo(25, 200)

        with(wlrKeyboard.events) {
            listeners.add(key.add(::onKey))
            listeners.add(modifiers.add(::onModifiers))
            listeners.add(destroy.add(::onDestroy))
        }

        keymap.unref()
        context.unref()
    }


    fun keycodeToKeysym(keycode: Int): Int {
        return wlrKeyboard.xkbState.keyGetOneSym(keycode + 8)
    }


    fun tryCompositorShortcut(keysym: Int, modifiers: Modifiers, state: KeyboardKeyState): Boolean {
        var isCompositorShortcut = true

        when (compositor.captureMode.state) {
            // TODO: Move to passthrough?
            is Passthrough -> {
                if (state == Released) return false
                if (!modifiers.contains(Alt)) return false

                when (keysym) {
                    XkbKey.F1 -> compositor.windowSystem.focuser.focusNextWindow()
                    XkbKey.F2 -> compositor.terminalPath?.let { compositor.startProcess(it) }
                    XkbKey.F3 -> compositor.startProcess("/usr/bin/gthumb")
                    XkbKey.F9 -> compositor.windowSystem.focuser.getFocused()?.moveDiagonallyDown()
                    XkbKey.F11 -> compositor.windowSystem.focuser.getFocused()
                        ?.let { compositor.captureMode.transitionToMove(it, null, InitiatedWith.Keyboard) }

                    XkbKey.Num0 -> Log.init(Log.Importance.Silent)
                    XkbKey.Num1 -> Log.init(Log.Importance.Error)
                    XkbKey.Num2 -> Log.init(Log.Importance.Info)
                    XkbKey.Num3 -> Log.init(Log.Importance.Debug)

                    XkbKey.Escape -> compositor.stop()
                    else -> isCompositorShortcut = false
                }
            }

            is WindowMove, is WindowResize -> {
                isCompositorShortcut = compositor.captureMode.onKeyboardKey(keysym, state)
            }
        }

        if (isCompositorShortcut)
            repeatKey = keysym

        return isCompositorShortcut
    }


    //
    // Signal handlers
    //

    fun onKey(event: KeyEvent) {
        val keycode = event.keycode + 8
        val keysym = wlrKeyboard.xkbState.keyGetOneSym(keycode)
        val modifiers = wlrKeyboard.keyboardModifiers

        var propagateToClient = true

        // Do we have to cancel the repeat timer? If the key currently released was held down...
        if (keysym == repeatKey && event.state == Released) {
            repeatKey = null
            repeatTimer.stop()
            propagateToClient = false
        }

        // Try to handle the key as a compositor shortcut, if that fails forward it to the client
        val isCompositorShortcut = tryCompositorShortcut(keysym, modifiers, event.state)


        // If it's a compositor recognized shortcut, start the timer because the user might hold the key down
        // to try to repeat the action. Shortcuts handled internaly in the compositor are naturally not
        // propagated to the client.
        if (isCompositorShortcut) {
            repeatTimer.start(500)
            propagateToClient = false
        }

        if (propagateToClient) {
            compositor.seat.setKeyboard(wlrKeyboard)
            compositor.seat.keyboardNotifyKey(event.timeMsec, event.keycode, event.state)
        }
    }


    fun onModifiers(wlrKeyboard: Keyboard) {
        compositor.seat.setKeyboard(wlrKeyboard)
        compositor.seat.keyboardNotifyModifiers(wlrKeyboard.modifiers)
    }


    fun onDestroy(device: InputDevice) {
        // Remove listeners on the wlroots side.
        listeners.forEach { it.remove() }

        // Remove Java wrappers for listeners.
        listeners.clear()

        // TODO: Memory management: Take care of Signals/Listeners

        compositor.inputSystem.remove(this)
    }


    inner class RepeatTimer : Timer(compositor.display.eventLoop) {
        override fun callback() {
            require(repeatKey != null)
            tryCompositorShortcut(repeatKey!!, wlrKeyboard.keyboardModifiers, Pressed)
        }
    }
}