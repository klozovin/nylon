package compositor.input

import compositor.COMPOSITOR
import compositor.Compositor
import compositor.input.CursorInputState.InitiatedWith
import compositor.windows.Window
import wayland.KeyboardKeyState
import wayland.util.Edge
import wayland.util.Edge.Left
import wayland.util.Edge.Top
import wlroots.types.pointer.PointerButtonEvent
import java.util.*


// Should go to input package (maybe compositor.input)
typealias Edges = EnumSet<Edge>


class CursorInputMode(val compositor: Compositor) {

    var state: CursorInputState = CursorInputState.Passthrough(compositor)


    //
    // State machine transitions
    //

    fun transitionToPassthrough() {
        require(state is CursorInputState.WindowMove || state is CursorInputState.WindowResize)
        state = CursorInputState.Passthrough(compositor)
    }

    /**
     * @param pressedButton Mouse button that was held down while the user initiated the move request
     */
    fun transitionToMove(
        targetWindow: Window,
        pressedButton: Int?,
        initiatedWith: InitiatedWith = InitiatedWith.Mouse
    ) {
        require(state is CursorInputState.Passthrough)

        println("____")
        println("Currently number of held buttons: ${COMPOSITOR.seat.pointerState.buttonCount}")
        println("Buttons:")
        for (button in COMPOSITOR.seat.pointerState.buttons) {
            println("\t*) Button: btnid: ${button.button}, npressed: ${button.nPressed}")
        }

        // TODO: Use button_count for this
        // TODO: Remove this
        val pressedButtons = COMPOSITOR.seat.pointerState.buttons.filter { it.button != 0 }.map { it.button }

        // Change the cursor here? Right? Right??
        compositor.inputSystem.cursor.setIcon("grabbing")

        state = CursorInputState.WindowMove(compositor, targetWindow, pressedButton, initiatedWith)
    }


    fun transitionToResize(targetWindow: Window, edges: Edges) {
        require(state is CursorInputState.Passthrough)
//        setCursorIcon("grabbing")
        // Icons: nw-, ne-, sw-, se-resize

        val cursorIconName = "${if (Top in edges) "n" else "s"}${if (Left in edges) "w" else "e"}-resize"
        compositor.inputSystem.cursor.setIcon(cursorIconName)
        state = CursorInputState.WindowResize(compositor, targetWindow, edges)
    }

    // --------------------- ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


    fun onKeyboardKey(keysym: Int, keyState: KeyboardKeyState): Boolean {
        return state.onKeyboardKey(keysym, keyState)
    }


    fun onCursorMotion(timeMsec: Int) {
        state.onCursorMotion(timeMsec)
    }


    fun onCursorButton(event: PointerButtonEvent) {
        state.onCursorButton(event)
    }


    //
    // Helpers
    //

    fun isWindowGrabbed(window: Window): Boolean {
        return when (val state = state) {
            is CursorInputState.Passthrough -> false
            is CursorInputState.WindowMove -> state.targetWindow == window
            is CursorInputState.WindowResize -> state.targetWindow == window
        }

    }
}