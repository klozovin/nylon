package compositor

import wayland.PointerButtonState
import wayland.util.Edge
import wayland.util.Edge.BOTTOM
import wayland.util.Edge.LEFT
import wayland.util.Edge.RIGHT
import wayland.util.Edge.TOP
import wlroots.types.input.PointerButtonEvent
import wlroots.types.xdgshell.XdgToplevel
import wlroots.util.Box
import java.util.*


// Should go to input package (maybe compositor.input)
typealias Edges = EnumSet<Edge>


class InputCursorMode(val compositor: Compositor) {

    var state: CursorState = CursorState.Passthrough()


    fun transitionToPassthrough() {
        require(state is CursorState.WindowMove || state is CursorState.WindowResize)
        state = CursorState.Passthrough()
    }


    fun transitionToMove(toplevel: XdgToplevel) {
        require(state is CursorState.Passthrough)
        state = CursorState.WindowMove(compositor, toplevel)
    }


    fun transitionToResize(toplevel: XdgToplevel, edges: Edges) {
        require(state is CursorState.Passthrough)
        state = CursorState.WindowResize(compositor, toplevel, edges)
    }


    fun onCursorMotion(timeMsec: Int) {
        when (val state = this@InputCursorMode.state) {
            is CursorState.Passthrough -> {
                state.processCursorMotion(timeMsec)
            }

            is CursorState.WindowMove -> {
                state.processCursorMotion()
            }

            is CursorState.WindowResize -> {
                state.processCursorMotion()
            }
        }
    }


    fun onCursorButton(event: PointerButtonEvent) {
        // We only do two things in the compositor on mouse button clicks: focus the window under the cursor,
        // or exit the move/resize mode when appropriate
        when(val state = state) {
            is CursorState.Passthrough -> state.processCursorButton(event)
            is CursorState.WindowMove -> state.processCursorButton(event)
            is CursorState.WindowResize -> state.processCursorButton(event)
        }
    }


    fun isMoveOrResize(): Boolean =
        state is CursorState.WindowMove || state is CursorState.WindowResize
}


/**
 * State machine modeling the capture states of the cursor
 */
sealed class CursorState {

    /**
     * Normal cursor behaviour: pass all the pointer events to focused client
     */
    class Passthrough : CursorState() {

        fun processCursorMotion(timeMsec: Int) {
            val windowSystem = COMPOSITOR.windowSystem
            val cursor = COMPOSITOR.inputSystem.cursor
            val seat = COMPOSITOR.seat

            when (val tpl = windowSystem.toplevelAtCoordinates(cursor.x, cursor.y)) {
                // Find the XdgToplevel under the cursor and forward cursor events to it.
                is WindowSystem.UnderCursor -> {
                    seat.pointerNotifyEnter(tpl.surface, tpl.nx, tpl.ny)
                    seat.pointerNotifyMotion(timeMsec, tpl.nx, tpl.ny)

                }

                // Clear pointer focus so the future button events are not sent to the last client to
                // have cursor over it.
                null -> {
                    seat.pointerClearFocus()
                    cursor.setXcursor(COMPOSITOR.xcursorManager, "default")
                }
            }
        }


        fun processCursorButton(event: PointerButtonEvent) {
            val cursor = COMPOSITOR.inputSystem.cursor
            val seat = COMPOSITOR.seat
            val windowSystem = COMPOSITOR.windowSystem

            // TODO: Maybe swap the order, first change the focus, then send the pointer notify event?

            // Notify the client with the "pointer focus" that there's been a button press
            seat.pointerNotifyButton(event.timeMsec, event.button, event.state)

            // "Raise" the clicked window
            if (event.state == PointerButtonState.PRESSED) {
                windowSystem.toplevelAtCoordinates(cursor.x, cursor.y)?.let {
                    windowSystem.focusToplevel(it.toplevel)
                }
            }
        }
    }


    /**
     * Client window is being moved by the user, usually dragged by the title-bar while the LMB is being
     * pressed.
     */
    class WindowMove(val compositor: Compositor, val grabbedToplevel: XdgToplevel) : CursorState() {

        // TODO: pass this as param, no need to know where is the toplevel kept and how
        val grabbedSceneNode = compositor.windowSystem.toplevelSceneTree[grabbedToplevel]!!.node

        val cursor = COMPOSITOR.inputSystem.cursor
        val grabX = compositor.inputSystem.cursor.x - grabbedSceneNode.x
        val grabY = compositor.inputSystem.cursor.y - grabbedSceneNode.y


        fun processCursorMotion() {
            grabbedSceneNode.setPosition((cursor.x - grabX), (cursor.y - grabY))
        }


        fun processCursorButton(event: PointerButtonEvent) {
            if (event.state == PointerButtonState.RELEASED)
                COMPOSITOR.captureMode.transitionToPassthrough()
        }
    }


    /**
     * Client window is being resized by the user, initiated by dragging the border of the window.
     */
    class WindowResize(val compositor: Compositor, val grabbedToplevel: XdgToplevel, val edges: Edges) :
        CursorState() {

        val cursor = COMPOSITOR.inputSystem.cursor
        var grabbedGeometry: Box // TODO: var needed?
        val grabbedSceneTree = compositor.windowSystem.toplevelSceneTree[grabbedToplevel]!!
        val grabbedSceneNode = grabbedSceneTree.node
        var grabX: Double
        var grabY: Double

        init {
            val geometry = grabbedToplevel.getBase().geometry
            val borderX = (grabbedSceneNode.x + geometry.x) + if (RIGHT in edges) geometry.width else 0
            val borderY = (grabbedSceneNode.y + geometry.y) + if (BOTTOM in edges) geometry.height else 0
            grabX = cursor.x - borderX
            grabY = cursor.y - borderY
            grabbedGeometry = Box.allocateCopy(geometry).apply {
                x += grabbedSceneNode.x
                y += grabbedSceneNode.y
            }
        }

        // Do the window resize
        fun processCursorMotion() {
            val borderX = (cursor.x - grabX).toInt()
            val borderY = (cursor.y - grabY).toInt()

            // Coordinates for new (resized) vertices
            var left = grabbedGeometry.x
            var right = grabbedGeometry.x + grabbedGeometry.width
            var top = grabbedGeometry.y
            var bottom = grabbedGeometry.y + grabbedGeometry.height

            // TODO: Explain what does this do? (clamping)
            when {
                TOP in edges -> {
                    top = borderY
                    if (top >= bottom) top = bottom - 1
                }

                BOTTOM in edges -> {
                    bottom = borderY
                    if (bottom <= top) bottom = top + 1
                }
            }

            when {
                LEFT in edges -> {
                    left = borderX
                    if (left >= right) left = right - 1
                }

                RIGHT in edges -> {
                    right = borderX
                    if (right <= left) right = left + 1
                }
            }

            val geometry = grabbedToplevel.getBase().geometry
            grabbedSceneNode.setPosition(left - geometry.x, top - geometry.y)
            grabbedToplevel.setSize(right - left, bottom - top)
        }


        fun processCursorButton(event: PointerButtonEvent) {
            if (event.state == PointerButtonState.RELEASED)
                COMPOSITOR.captureMode.transitionToPassthrough()
        }
    }
}
