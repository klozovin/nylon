package compositor.input

import compositor.Compositor
import wayland.SeatCapability
import wayland.server.Listener
import wlroots.types.input.InputDevice
import wlroots.types.input.InputDeviceType
import wlroots.types.pointer.Pointer
import wlroots.types.seat.PointerRequestSetCursorEvent
import wlroots.types.seat.RequestSetSelectionEvent
import wlroots.types.seat.Seat
import wlroots.types.keyboard.Keyboard as WlrKeyboard

class InputSystem(val compositor: Compositor) {

    val seat: Seat
    val cursor = Cursor(compositor)
    val keyboards: MutableList<Keyboard> = mutableListOf()

    val seatRequestSetCursorListener: Listener
    val seatRequestSetSelectionListener: Listener
    val seatDestroyListener: Listener


    init {
        seat = Seat.create(compositor.display, "seat0").apply {
            seatRequestSetCursorListener= events.requestSetCursor.add(::onSeatRequestSetCursor)
            seatRequestSetSelectionListener = events.requestSetSelection.add(::onSeatRequestSetSelection)
            seatDestroyListener = events.destroy.add(::onSeatDestroy)
        }
    }


    fun onNewInput(device: InputDevice) {
        when (device.type) {
            InputDeviceType.Keyboard -> onNewKeyboard(WlrKeyboard.fromInputDevice(device))
            InputDeviceType.Pointer -> onNewPointer(Pointer.fromInputDevice(device))
            else -> error("Unsupported wlr_input_device_type: ${device.type}")
        }
    }


    fun onNewKeyboard(wlrKeyboard: WlrKeyboard) {
        assert(keyboards.none { it.wlrKeyboard == wlrKeyboard }) { "We should never see the same keyboard twice" }

        val keyboard = Keyboard(compositor, wlrKeyboard)
        keyboards.add(keyboard)
        seat.setKeyboard(wlrKeyboard)
        seat.addCapability(SeatCapability.Keyboard)
    }


    fun onNewPointer(pointer: Pointer) {
        cursor.wlrCursor.attachInputDevice(pointer.base)
        seat.addCapability(SeatCapability.Pointer)
    }


    fun onSeatRequestSetCursor(event: PointerRequestSetCursorEvent) {
        if (seat.pointerState.getFocusedClient() == event.seatClient)
            cursor.wlrCursor.setSurface(event.surface, event.hotspotX, event.hotspotY)
    }


    fun onSeatRequestSetSelection(event: RequestSetSelectionEvent) {
        // TODO: This should go to some future 'clipboard' system
        seat.setSelection(event.source, event.serial)
    }


    fun onSeatDestroy(seat: Seat) {
        seatRequestSetCursorListener.remove()
        seatRequestSetSelectionListener.remove()
        seatDestroyListener.remove()
    }


    /*
    fun onNewKeyboardOld(keyboard: WlrKeyboard) {
        val context = XkbContext.of(XkbContext.Flags.NO_FLAGS)
            ?: error("Failed to create XKB context")
        val keymap = context.keymapNewFromNames(null, Keymap.CompileFlags.NO_FLAGS)
            ?: error("Failed to create XKB keymap")
        keyboard.setKeymap(keymap)
        keyboard.setRepeatInfo(25, 200)

        compositor.seat.setKeyboard(keyboard)

        with(keyboard) {
            keyboards_old[events.key.add(::onKeyboardKey)] = keyboard
            keyboards_old[events.modifiers.add(::onKeyboardModifiers)] = keyboard
            keyboards_old[base.events.destroy.add(::onKeyboardDestroy)] = keyboard
        }

        keymap.unref()
        context.unref()

        compositor.seat.addCapability(SeatCapability.Keyboard)
    }
     */
}