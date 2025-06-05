package compositor

import compositor.WindowSystem.UnderCursor
import wayland.KeyboardKeyState
import wayland.PointerButtonState
import wayland.SeatCapability
import wayland.server.Listener
import wlroots.types.Cursor
import wlroots.types.input.*
import xkbcommon.Keymap
import xkbcommon.XkbContext
import xkbcommon.XkbKey


class InputSystem(val compositor: Compositor) {
    val keyboards: MutableMap<Listener, Keyboard> = HashMap()
    val pointers: MutableMap<Listener, Pointer> = HashMap()


    fun processCursorMotion(timeMsec: Int) {
        when (compositor.cursorMode) {
            CursorMode.Move -> processCursorMove(timeMsec)
            CursorMode.Resize -> processCursorResize(timeMsec)
            CursorMode.Passthrough ->
                when (val tpl = compositor.windowSystem.toplevelAtCoordinates(
                    compositor.cursor.x(),
                    compositor.cursor.y()
                )) {
                    // Find the XdgToplevel under the pointer and forward the cursor events.
                    is UnderCursor -> {
                        compositor.seat.pointerNotifyEnter(tpl.surface, tpl.nx, tpl.ny)
                        compositor.seat.pointerNotifyMotion(timeMsec, tpl.nx, tpl.ny)
                    }
                    // Clear pointer focus so the future button evenets are not sent to the last client to
                    // have cursor over it
                    null -> {
                        compositor.seat.pointerClearFocus()
                        compositor.cursor.setXcursor(compositor.xcursorManager, "default")
                    }
                }
        }
    }


    fun processCursorMove(timeMsec: Int) {
        // TODO: Moving a window should be a method on the WindowSystem
        compositor.windowSystem.toplevelSceneTree[compositor.grabbedToplevel!!]!!.node().setPosition(
            (compositor.cursor.x() - compositor.grabX).toInt(),
            (compositor.cursor.y() - compositor.grabY).toInt()
        )
    }

    fun processCursorResize(timeMsec: Int) {
        TODO()
    }


    //
    // Listeners: Input devices appearing and disappearing
    //

    fun onNewInput(device: InputDevice) {
        when (val deviceType = device.type()) {
            InputDevice.Type.KEYBOARD -> onNewKeyboard(Keyboard.fromInputDevice(device))
            InputDevice.Type.POINTER -> onNewPointer(Pointer.fromInputDevice(device))
            else -> error("Unsupported wlr_input_device_type: $deviceType")
        }
    }


    fun onNewKeyboard(keyboard: Keyboard) {
        val context = XkbContext.of(XkbContext.Flags.NO_FLAGS) ?: error("Failed to create XKB context")
        val keymap = context.keymapNewFromNames(null, Keymap.CompileFlags.NO_FLAGS)
            ?: error("Failed to create XKB keymap")
        keyboard.setKeymap(keymap)
        keyboard.setRepeatInfo(25, 200)
        compositor.seat.setKeyboard(keyboard)

        with(keyboards) {
            put(keyboard.events.key.add(::onKeyboardKey), keyboard)
            put(keyboard.events.modifiers.add(::onKeyboardModifiers), keyboard)
            put(keyboard.base.events.destroy.add(::onKeyboardDestroy), keyboard)
        }

        keymap.unref()
        context.unref()

        compositor.seat.addCapability(SeatCapability.KEYBOARD)
    }


    fun onKeyboardDestroy(listener: Listener, device: InputDevice) {
        // For the given keyboard, find all of its listeners and destroy them
        val keyboard = keyboards[listener]!!
        val listeners = keyboards.filterValues(keyboard::equals).keys
        require(listeners.size == 3) // TODO: Remove later
        listeners.forEach { it.remove() } // TODO: Memory management: Close the confined arena here
    }


    fun onNewPointer(pointer: Pointer) {
        compositor.cursor.attachInputDevice(pointer.base())
        compositor.seat.addCapability(SeatCapability.POINTER)
    }


    //
    // Listeners: Keyboard input: key press/release, modifiers
    //

    fun onKeyboardKey(listener: Listener, event: KeyboardKeyEvent) {
        val keyboard = keyboards[listener]!!
        val keycode = event.keycode() + 8
        val keysym = keyboard.xkbState().keyGetOneSym(keycode)

        var handledInCompositor = false
        if (keyboard.modifiers.isAltDown && event.state() == KeyboardKeyState.PRESSED) {
            when (keysym) {
                XkbKey.F1 -> {
                    // TODO
//                    if (TOPLEVELS.isNotEmpty()) {
//                        val nextIdx = focusedToplevel + 1
//                        if (nextIdx < TOPLEVELS.size)
//                            focusToplevel(TOPLEVELS[nextIdx])
//                        else
//                            focusToplevel(TOPLEVELS[0])
//                    }
                    handledInCompositor = true
                }

                XkbKey.Escape -> {
                    compositor.stop()
                    handledInCompositor = true
                }
            }
        }

        if (!handledInCompositor) {
            compositor.seat.setKeyboard(keyboard)
            compositor.seat.keyboardNotifyKey(event.timeMsec(), event.keycode(), event.state())
        }
    }


    fun onKeyboardModifiers(keyboard: Keyboard) {
        compositor.seat.setKeyboard(keyboard)
        compositor.seat.keyboardNotifyModifiers(keyboard.modifiers())
    }

    //
    // Listeners: Mouse input: button clicks, scrolling, moving
    //

    fun onCursorMotion(event: PointerMotionEvent) {
        compositor.cursor.move(event.pointer.base(), event.deltaX, event.deltaY)
        processCursorMotion(event.timeMsec)
    }


    fun onCursorMotionAbsolute(event: PointerMotionAbsoluteEvent) {
        compositor.cursor.warpAbsolute(event.pointer.base(), event.x, event.y)
        processCursorMotion(event.timeMsec)
    }


    fun onCursorButton(event: PointerButtonEvent) {
        compositor.seat.pointerNotifyButton(event.timeMsec, event.button, event.state)

        when (event.state) {
            PointerButtonState.PRESSED -> {
                compositor.windowSystem.toplevelAtCoordinates(compositor.cursor.x(), compositor.cursor.y())
                    ?.let {
                        compositor.windowSystem.focusToplevel(it.toplevel)
                    }
            }

            PointerButtonState.RELEASED ->
                if (compositor.cursorMode == CursorMode.Move || compositor.cursorMode == CursorMode.Resize)
                    compositor.windowSystem.resetCursorMode()
        }
    }


    fun onCursorAxis(event: PointerAxisEvent) {
        compositor.seat.pointerNotifyAxis(
            event.timeMsec,
            event.orientation,
            event.delta,
            event.deltaDiscrete,
            event.source,
            event.relativeDirection
        )
    }


    fun onCursorFrame(cursor: Cursor) {
        compositor.seat.pointerNotifyFrame()
    }
}