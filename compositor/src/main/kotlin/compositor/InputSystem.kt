package compositor

import compositor.WindowSystem.UnderCursor
import wayland.KeyboardKeyState
import wayland.PointerButtonState
import wayland.SeatCapability
import wayland.server.Listener
import wayland.util.Edge
import wlroots.types.Cursor
import wlroots.types.input.*
import xkbcommon.Keymap
import xkbcommon.XkbContext
import xkbcommon.XkbKey


class InputSystem(val compositor: Compositor) {
    val cursor: Cursor
    val keyboards: MutableMap<Listener, Keyboard> = HashMap()
    val pointers: MutableMap<Listener, Pointer> = HashMap()


    init {
        compositor.backend.events.newInput.add(::onNewInput)

        cursor = Cursor.create().apply {
            attachOutputLayout(compositor.outputLayout)
            with(events) {
                motion.add(::onCursorMotion)
                motionAbsolute.add(::onCursorMotionAbsolute)
                button.add(::onCursorButton)
                axis.add(::onCursorAxis)
                frame.add(::onCursorFrame)
            }
        }
    }

    // **************************************************************************************************** //
    // Listeners: Input device lifecycle                                                                    //
    // **************************************************************************************************** //


    fun onNewInput(device: InputDevice) {
        when (device.type) {
            InputDevice.Type.KEYBOARD -> onNewKeyboard(Keyboard.fromInputDevice(device))
            InputDevice.Type.POINTER -> onNewPointer(Pointer.fromInputDevice(device))
            else -> error("Unsupported wlr_input_device_type: ${device.type}")
        }
    }


    fun onNewKeyboard(keyboard: Keyboard) {
        val context = XkbContext.of(XkbContext.Flags.NO_FLAGS)
            ?: error("Failed to create XKB context")
        val keymap = context.keymapNewFromNames(null, Keymap.CompileFlags.NO_FLAGS)
            ?: error("Failed to create XKB keymap")
        keyboard.setKeymap(keymap)
        keyboard.setRepeatInfo(25, 200)

        compositor.seat.setKeyboard(keyboard)

        with(keyboard) {
            keyboards[events.key.add(::onKeyboardKey)] = keyboard
            keyboards[events.modifiers.add(::onKeyboardModifiers)] = keyboard
            keyboards[base.events.destroy.add(::onKeyboardDestroy)] = keyboard
        }

        keymap.unref()
        context.unref()

        compositor.seat.addCapability(SeatCapability.KEYBOARD)
    }


    fun onKeyboardDestroy(listener: Listener, device: InputDevice) {
        // For the given keyboard, find all of its listeners and destroy them
        val keyboard = keyboards[listener]!!

        // All listeners added to this keyboard
        val listeners = keyboards.entries.filter { it.value == keyboard }

        // Remove listeners from their signals
        listeners.forEach { (listener, _) -> listener.remove() }

        // Remove listener->keyboard entries from hash map
        keyboards.entries.removeAll(listeners)

        require(listeners.size == 3) // TODO: Remove sanity check
        // TODO: Memory management: Close the confined arena here
    }


    fun onNewPointer(pointer: Pointer) {
        cursor.attachInputDevice(pointer.base())
        compositor.seat.addCapability(SeatCapability.POINTER)
    }


    // **************************************************************************************************** //
    // *** Listeners: Keyboard input: key press/release, modifiers                                      *** //
    // **************************************************************************************************** //

    fun onKeyboardKey(listener: Listener, event: KeyboardKeyEvent) {
        val keyboard = keyboards[listener]!!
        val keycode = event.keycode + 8
        val keysym = keyboard.xkbState().keyGetOneSym(keycode)

        // Should we swallow this key combo?
        var handledInCompositor = false

        if (keyboard.modifiers.containsAlt() && event.state == KeyboardKeyState.PRESSED) {
            when (keysym) {
                XkbKey.F1 -> {
                    compositor.windowSystem.focusNextToplevel()
                    handledInCompositor = true
                }

                XkbKey.F2 -> {
                    compositor.terminalPath?.let {
                        compositor.startProcess(it)
                    }
                    handledInCompositor = true
                }

                XkbKey.Escape -> {
                    compositor.stop()
                    handledInCompositor = true
                }
            }
        }

        if (keyboard.modifiers.containsLogo() && event.state == KeyboardKeyState.PRESSED) {
            when (keysym) {
                XkbKey.Insert -> {
                    println("Meta+Insert")
                    handledInCompositor = true
                }
            }
        }

        if (!handledInCompositor) {
            compositor.seat.setKeyboard(keyboard)
            compositor.seat.keyboardNotifyKey(event.timeMsec, event.keycode, event.state)
        }
    }


    fun onKeyboardModifiers(keyboard: Keyboard) {
        compositor.seat.setKeyboard(keyboard)
        compositor.seat.keyboardNotifyModifiers(keyboard.modifiers())
    }


    // **************************************************************************************************** //
    // *** Listeners: MOUSE input: button clicks, scrolling, moving the cursor                          *** //
    // **************************************************************************************************** //


    fun onCursorMotion(event: PointerMotionEvent) {
        cursor.move(event.pointer.base(), event.deltaX, event.deltaY)
        compositor.captureMode.onCursorMotion(event.timeMsec)
    }


    fun onCursorMotionAbsolute(event: PointerMotionAbsoluteEvent) {
        cursor.warpAbsolute(event.pointer.base(), event.x, event.y)
        compositor.captureMode.onCursorMotion(event.timeMsec)
    }


    fun onCursorButton(event: PointerButtonEvent) {
        compositor.captureMode.onCursorButton(event)
    }


    fun onCursorAxis(event: PointerAxisEvent) {
        compositor.seat.pointerNotifyAxis(event)
    }


    fun onCursorFrame(cursor: Cursor) {
        compositor.seat.pointerNotifyFrame()
    }
}

// TODO: Delete
enum class CursorMode {
    Move,
    Resize,
    Passthrough
}