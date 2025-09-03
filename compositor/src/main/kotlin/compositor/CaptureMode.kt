package compositor

import wayland.util.Edge
import wayland.util.Edge.BOTTOM
import wayland.util.Edge.LEFT
import wayland.util.Edge.RIGHT
import wayland.util.Edge.TOP
import wlroots.types.xdgshell.XdgToplevel
import wlroots.util.Box
import java.util.*


// Should go to input package (maybe compositor.input)
typealias Edges = EnumSet<Edge>


class CaptureMode(val compositor: Compositor) {

    var state: CursorState = CursorState.Passthrough


    fun transitionToPassthrough() {
        require(state is CursorState.WindowMove || state is CursorState.WindowResize)
        state = CursorState.Passthrough
    }


    fun transitionToMove(toplevel: XdgToplevel) {
        require(state is CursorState.Passthrough)
        state = CursorState.WindowMove(compositor, toplevel)
    }


    fun transitionToResize() {
        require(state is CursorState.Passthrough)

    }


    fun onCursorMotion() {
        when (state) {
            CursorState.Passthrough -> TODO()
            is CursorState.WindowMove -> TODO()
            is CursorState.WindowResize -> TODO()
        }
    }


    fun onCursorButton() {

    }
}


// State machine modeling the capture state
sealed class CursorState {

    object Passthrough : CursorState()


    // Handle moving windows initiated by the client
    class WindowMove(val compositor: Compositor, val grabbedToplevel: XdgToplevel) : CursorState() {

        // TODO: pass this as param, no need to know where is the toplevel kept and how
        val grabbedSceneNode = compositor.windowSystem.toplevelSceneTree[grabbedToplevel]!!.node

        // TODO: Maybe pass cursor as param?
        val cursor = COMPOSITOR.inputSystem.cursor
        var grabX = compositor.inputSystem.cursor.x - grabbedSceneNode.x
        var grabY = compositor.inputSystem.cursor.y - grabbedSceneNode.y


        // Do the window move
        fun process() {
            grabbedSceneNode.setPosition((cursor.x - grabX), (cursor.y - grabY))
        }
    }


    // Handle window resize, initiated by the client

    class WindowResize(val compositor: Compositor, val grabbedToplevel: XdgToplevel, val edges: Edges) : CursorState() {

        val cursor = COMPOSITOR.inputSystem.cursor
        var grabbedGeometry: Box // TODO: var needed?
        val grabbedSceneTree = compositor.windowSystem.toplevelSceneTree[grabbedToplevel]!!
        val grabbedSceneNode = grabbedSceneTree.node
        var grabX: Double
        var grabY: Double

        init {
            val geometry = grabbedToplevel.base().geometry
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
        fun process() {
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

            val geometry = grabbedToplevel.base().geometry
            grabbedSceneNode.setPosition(left - geometry.x, top -geometry.y)
            grabbedToplevel.setSize(right - left, bottom - top)
        }
    }
}
