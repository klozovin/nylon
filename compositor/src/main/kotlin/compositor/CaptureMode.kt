package compositor

import wayland.PointerButtonState
import wayland.util.Edge
import wlroots.types.xdgshell.XdgToplevel
import wlroots.util.Box
import java.util.*


// Should go to input package (maybe compositor.input)

typealias Edges = EnumSet<Edge>






sealed interface CursorState {
//    fun processCursorMove()
//    fun processCursorButton()
}


object Passthrough : CursorState


class ResizeWindow(
    val toplevel: XdgToplevel,
    val edges: Edges,
    var geometryBox: Box,
) : CursorState {

    fun process() {
        // windowSystem.resize()
    }

}


class MoveWindow(
    val targetToplevel: XdgToplevel,
    val grabX: Double,
    val grabY: Double,
) : CursorState {


    fun processMotion() {
        TODO("Not yet implemented")
    }
}


class CursorInputProcessor(
    val inputSystem: InputSystem,
    val windowSystem: WindowSystem,
) {
    var state: CursorState = Passthrough





    fun transitionToWindowMove(toplevel: XdgToplevel) {
        require(state is Passthrough)

        TODO()
//        val sceneNode = windowSystem.getSceneTreeForToplevel(toplevel).node()

        val grabX = inputSystem.cursor.x()
        val grabY = inputSystem.cursor.y()

        state = MoveWindow(toplevel, grabX, grabY)

    }


    fun transitionToResize(toplevel: XdgToplevel, edges: Edges) {
        require(state is Passthrough)
        state = ResizeWindow(toplevel, edges, TODO())

    }


    fun transitionToPassthrough() {
        require(state is MoveWindow || state is ResizeWindow)

        // back to passthrough
        state = Passthrough
    }




    fun processCursorMove() {
        when(val state = this.state) {
            Passthrough -> {
                TODO() // need timemsec here
            }

            is MoveWindow -> {
                state.processMotion()
                TODO() // dont need anythin
            }

            is ResizeWindow -> {
                TODO() // dont need anything
            }
        }

    }

    fun processCursorButton(buttonState: PointerButtonState) {

        TODO()
//        state.processCursorButton()
    }
}