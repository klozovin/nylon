package compositor.input

import compositor.Compositor
import wayland.SeatCapability
import wlroots.types.input.InputDevice
import wlroots.types.input.InputDeviceType
import wlroots.types.pointer.Pointer
import wlroots.types.keyboard.Keyboard as WlrKeyboard

class InputSystem(val compositor: Compositor) {

    val cursor = Cursor(compositor)
    val keyboards: MutableList<compositor.input.Keyboard> = mutableListOf()


    init {
        compositor.backend.events.newInput.add(::onNewInput)
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
        compositor.seat.setKeyboard(wlrKeyboard)
        compositor.seat.addCapability(SeatCapability.Keyboard)
    }


    fun onNewPointer(pointer: Pointer) {
        cursor.wlrCursor.attachInputDevice(pointer.base)
        compositor.seat.addCapability(SeatCapability.Pointer)
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