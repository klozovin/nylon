package compositor

import wlroots.types.Cursor
import wlroots.types.input.PointerAxisEvent
import wlroots.types.input.PointerButtonEvent
import wlroots.types.input.PointerMotionAbsoluteEvent
import wlroots.types.input.PointerMotionEvent


class InputSystem(val compositor: Compositor) {

    // *** Handle devices appearing and destroying *** //

    // TODO: arguments,
    fun onNewInput(x: Any) {

    }

    fun onNewKeyboard() {}

    fun onKeyboardDestroy() {}


    fun onNewPointer() {

    }


    // *** Keyboard input *** //

    fun onKeyboardKey() {

    }


    // *** Mouse input *** //

    fun onCursorMotion(event: PointerMotionEvent) {
        TODO()
    }


    fun onCursorMotionAbsoulute(event: PointerMotionAbsoluteEvent) {
        TODO()
    }


    fun onCursorButton(event: PointerButtonEvent) {
        TODO()
    }


    fun onCursorAxis(event: PointerAxisEvent) {
        TODO()
    }

    fun onCursorFrame(cursor: Cursor) {
        TODO()
    }

}