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


    fun processCursorMotion(timeMsec: Int) {
        when (compositor.cursorMode) {
            CursorMode.Move -> processCursorMoveWindow()

            CursorMode.Resize -> processCursorResizeWindow()

            CursorMode.Passthrough ->
                when (val tpl = compositor.windowSystem.toplevelAtCoordinates(
                    cursor.getX(),
                    cursor.getY()
                )) {
                    // Find the XdgToplevel under the pointer and forward the cursor events.
                    is UnderCursor -> {
                        compositor.seat.pointerNotifyEnter(tpl.surface, tpl.nx, tpl.ny)
                        compositor.seat.pointerNotifyMotion(timeMsec, tpl.nx, tpl.ny)
                    }
                    // Clear pointer focus so the future button events are not sent to the last client to
                    // have cursor over it.
                    null -> {
                        compositor.seat.pointerClearFocus()
                        cursor.setXcursor(compositor.xcursorManager, "default")
                    }
                }
        }
    }


    fun processCursorMoveWindow() {
        // TODO: Moving a window should be a method on the WindowSystem
        compositor.windowSystem.toplevelSceneTree[compositor.grabbedToplevel!!]!!.getNode().setPosition(
            (cursor.getX() - compositor.grabX).toInt(),
            (cursor.getY() - compositor.grabY).toInt()
        )
    }


    fun processCursorResizeWindow() {
        val grabbedToplevel = compositor.grabbedToplevel!!
        val grabbedSceneTree = compositor.windowSystem.toplevelSceneTree[grabbedToplevel]!!

        val borderX = cursor.getX() - compositor.grabX
        val borderY = cursor.getY() - compositor.grabY

        var newLeft = compositor.grabGeobox.x
        var newRight = compositor.grabGeobox.x + compositor.grabGeobox.width

        var newTop = compositor.grabGeobox.y
        var newBottom = compositor.grabGeobox.y + compositor.grabGeobox.height

        if (Edge.TOP in compositor.resizeEdges) {
            newTop = borderY.toInt()
            if (newTop >= newBottom)
                newTop = newBottom - 1
        } else if (Edge.BOTTOM in compositor.resizeEdges) {
            newBottom = borderY.toInt()
            if (newBottom <= newTop)
                newBottom = newTop + 1
        }

        if (Edge.LEFT in compositor.resizeEdges) {
            newLeft = borderX.toInt()
            if (newLeft >= newRight)
                newLeft = newRight - 1
        } else if (Edge.RIGHT in compositor.resizeEdges) {
            newRight = borderX.toInt()
            if (newRight <= newLeft)
                newRight = newLeft + 1
        }

        val geoBox = grabbedToplevel.base().getGeometry()
        grabbedSceneTree.getNode().setPosition(newLeft - geoBox.x, newTop - geoBox.y)
        grabbedToplevel.setSize(newRight - newLeft, newBottom - newTop)
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
        processCursorMotion(event.timeMsec)
    }


    fun onCursorMotionAbsolute(event: PointerMotionAbsoluteEvent) {
        cursor.warpAbsolute(event.pointer.base(), event.x, event.y)
        processCursorMotion(event.timeMsec)
    }


    fun onCursorButton(event: PointerButtonEvent) {
        // Notify the client with the "pointer focus" that there's been a button press
        compositor.seat.pointerNotifyButton(event.timeMsec, event.button, event.state)

        when (event.state) {
            PointerButtonState.PRESSED -> {
                val x = cursor.getX()
                val y = cursor.getY()
                compositor.windowSystem.toplevelAtCoordinates(x, y)?.let {
                    compositor.windowSystem.focusToplevel(it.toplevel)
                }
            }

            PointerButtonState.RELEASED -> {
                val isMove = compositor.cursorMode == CursorMode.Move
                val isResize = compositor.cursorMode == CursorMode.Resize
                if (isMove || isResize)
                    compositor.windowSystem.resetCursorMode()
            }
        }
    }


    fun onCursorAxis(event: PointerAxisEvent) {
        compositor.seat.pointerNotifyAxis(event)
    }


    fun onCursorFrame(cursor: Cursor) {
        compositor.seat.pointerNotifyFrame()
    }
}


enum class CursorMode {
    Move,
    Resize,
    Passthrough
}