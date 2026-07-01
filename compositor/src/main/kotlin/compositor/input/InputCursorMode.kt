package compositor.input

import compositor.COMPOSITOR
import compositor.Compositor
import compositor.WindowSystem
import compositor.enumSetOf
import linux.MouseButton
import wayland.PointerButtonState
import wayland.util.Edge
import wayland.util.Edge.*
import wlroots.types.keyboard.KeyboardModifier
import wlroots.types.pointer.PointerButtonEvent
import wlroots.types.xdgshell.XdgToplevel
import wlroots.util.Box
import java.util.*


// Should go to input package (maybe compositor.input)
typealias Edges = EnumSet<Edge>


class InputCursorMode(val compositor: Compositor) {

    val inputSystem = compositor.inputSystem
    var state: CursorState = CursorState.Passthrough()
    var pointerPressedButtons = mutableSetOf<Int>()


    //
    // State machine transitions
    //

    fun transitionToPassthrough() {
        require(state is CursorState.WindowMove || state is CursorState.WindowResize)
        setCursorIcon("default")
        state = CursorState.Passthrough()
    }

    /**
     * @param pressedButton Mouse button that was held down while the user initiated the move request
     */
    fun transitionToMove(toplevel: XdgToplevel, pressedButton: Int) {
        require(state is CursorState.Passthrough)

        println("____")
        println("Currently held down buttons: $pointerPressedButtons")
        println("Currently number of held buttons: ${COMPOSITOR.seat.pointerState.buttonCount}")
        println("Buttons:")
        for (button in COMPOSITOR.seat.pointerState.buttons) {
            println("\t*) Button: btnid: ${button.button}, npressed: ${button.nPressed}")
        }

        // TODO: Use button_count for this
        // TODO: Remove this
        val pressedButtons = COMPOSITOR.seat.pointerState.buttons.filter { it.button != 0 }.map { it.button }

        // Change the cursor here? Right? Right??
        setCursorIcon("grabbing")

        state = CursorState.WindowMove(compositor, toplevel, pressedButton)
    }


    fun transitionToResize(toplevel: XdgToplevel, edges: Edges) {
        require(state is CursorState.Passthrough)
//        setCursorIcon("grabbing")
        // Icons: nw-, ne-, sw-, se-resize

        val cursorIconName = "${if (Top in edges) "n" else "s"}${if (Left in edges) "w" else "e"}-resize"
        setCursorIcon(cursorIconName)
        state = CursorState.WindowResize(compositor, toplevel, edges)
    }

    //
    // Event handling
    //

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
        // Remember pointer button held down (used for canceling the move/resize request)
        // TODO: Probably remove this later, only used for debugging tracking
        when (event.state) {
            PointerButtonState.Pressed -> pointerPressedButtons.add(event.button)
            PointerButtonState.Released -> pointerPressedButtons.remove(event.button)
        }


        // We only do two things in the compositor on mouse button clicks: focus the window under the cursor,
        // or exit the move/resize mode when appropriate
        when (val state = state) {
            is CursorState.Passthrough -> state.processCursorButton(event)
            is CursorState.WindowMove -> state.processCursorButton(event)
            is CursorState.WindowResize -> state.processCursorButton(event)
        }
    }

    //
    // Helpers
    //

    fun isToplevelGrabbed(toplevel: XdgToplevel): Boolean {
        return when (val state = state) {
            is CursorState.Passthrough -> false
            is CursorState.WindowMove -> state.grabbedToplevel == toplevel
            is CursorState.WindowResize -> state.grabbedToplevel == toplevel
        }
    }


    fun setCursorIcon(name: String) {
        compositor.inputSystem.cursor.wlrCursor.setXcursor(compositor.xcursorManager, name)
    }
}


/**
 * State machine modeling the capture states of the cursor
 */
sealed class CursorState {

    /**
     * Normal cursor behavior: pass all the pointer events to focused client
     */
    class Passthrough : CursorState() {

        fun processCursorMotion(timeMsec: Int) {
            val windowSystem = COMPOSITOR.windowSystem
            val cursor = COMPOSITOR.inputSystem.cursor
            val seat = COMPOSITOR.seat

            when (val tpl = windowSystem.toplevelAtCoordinates(cursor.wlrCursor.x, cursor.wlrCursor.y)) {
                // Find the XdgToplevel under the cursor and forward cursor events to it.
                is WindowSystem.UnderCursor -> {
                    seat.pointerNotifyEnter(tpl.surface, tpl.nx, tpl.ny)
                    seat.pointerNotifyMotion(timeMsec, tpl.nx, tpl.ny)

                }

                // Clear pointer focus so the future button events are not sent to the last client to
                // have cursor over it.
                null -> {
                    seat.pointerClearFocus()
                    cursor.wlrCursor.setXcursor(COMPOSITOR.xcursorManager, "default")
                }
            }
        }


        /**
         * Possible actions to take on mouse click:
         *
         * - defocus, when clicking on desktop background
         * - focus/raise window when clicking on a window
         * - move when clicking while holding mod key
         * - resize when rmb clicking while holding mod key
         */
        fun processCursorButton(event: PointerButtonEvent) {
            val cursor = COMPOSITOR.inputSystem.cursor
            val seat = COMPOSITOR.seat
            val windowSystem = COMPOSITOR.windowSystem

            // TODO: Maybe swap the order, first change the focus, then send the pointer notify event?


            // Check for Alt key modifier to start dragging
            val altPressed = COMPOSITOR.seat.keyboard!!.keyboardModifiers.contains(KeyboardModifier.Alt)
            val lmbPressed = event.button == MouseButton.Left && event.state == PointerButtonState.Pressed
            val rmbPressed = event.button == MouseButton.Right && event.state.isPressed
            when {
                altPressed && lmbPressed -> {
                    // begin window move
                    windowSystem.toplevelAtCoordinates(cursor.wlrCursor.x, cursor.wlrCursor.y)?.let {
                        COMPOSITOR.captureMode.transitionToMove(it.toplevel, event.button)
                        return
                    }
                }

                altPressed && rmbPressed -> {
                    // TODO: Maybe defocus when clicking on desktop?
                    val cursorTarget =
                        windowSystem.toplevelAtCoordinates(cursor.wlrCursor.x, cursor.wlrCursor.y) ?: return
                    val toplevel = cursorTarget.toplevel


                    val geometry = toplevel.base.geometry
                    val sceneTree = windowSystem.toplevelSceneTree[toplevel]!!
                    val coordinates = sceneTree.node.coords()


                    println("Window geometry: $geometry")
                    println("Window coordinates: $coordinates")
                    println("Mouse clicked at: ${cursor.wlrCursor.x}, ${cursor.wlrCursor.y}")


                    val cursorX = cursor.wlrCursor.x
                    val cursorY = cursor.wlrCursor.y
                    val width = geometry.width
                    val height = geometry.height
                    val coordX = coordinates.x
                    val coordY = coordinates.y

                    val halfX = coordX + (width / 2)
                    val halfY = coordY + (height / 2)

                    val edges = enumSetOf(
                        if (cursorX <= halfX) Left else Right,
                        if (cursorY <= halfY) Top else Bottom
                    )

                    if (cursorX <= halfX) {
                        println("we're in the left half")
                    } else {
                        println("were in the right half")
                    }

                    if (cursorY <= halfY)
                        println("We're in the top half")
                    else
                        println("we in the bottom half")

                    // TODO: Detect grabbed window edge

                    COMPOSITOR.captureMode.transitionToResize(cursorTarget.toplevel, edges)
                    return

                }

                // Raise and focus the clicked window
                event.state == PointerButtonState.Pressed -> {
                    windowSystem.toplevelAtCoordinates(cursor.wlrCursor.x, cursor.wlrCursor.y)?.let {
                        windowSystem.focusToplevel(it.toplevel)
                    }
                }
            }

            // Notify the client with the "pointer focus" that there's been a button press
            // TODO: Move this to upper branch?
            seat.pointerNotifyButton(event.timeMsec, event.button, event.state)

//            if (event.state == PointerButtonState.Pressed) {
//                windowSystem.toplevelAtCoordinates(cursor.wlrCursor.x, cursor.wlrCursor.y)?.let {
//                    windowSystem.focusToplevel(it.toplevel)
//                }
//            }
        }
    }


    /**
     * Client window is being moved by the user, usually dragged by the title-bar while the LMB is being
     * pressed.
     */
    class WindowMove(
        val compositor: Compositor,
        val grabbedToplevel: XdgToplevel,
        val initiatingButton: Int
    ) : CursorState() {

        // TODO: pass this as param, no need to know where is the toplevel kept and how
        val grabbedSceneNode = compositor.windowSystem.toplevelSceneTree[grabbedToplevel]!!.node

        val cursor = COMPOSITOR.inputSystem.cursor
        val grabX = compositor.inputSystem.cursor.wlrCursor.x - grabbedSceneNode.x
        val grabY = compositor.inputSystem.cursor.wlrCursor.y - grabbedSceneNode.y

        init {
            println("WindowMove: $initiatingButton")
        }


        fun processCursorMotion() {
            // HACK: this kind of check should be automatic and centralized somewhere
            check(compositor.windowSystem.toplevels.containsValue(grabbedToplevel)) {
                "BUG: Trying to move a non existent window"
            }
            grabbedSceneNode.setPosition((cursor.wlrCursor.x - grabX), (cursor.wlrCursor.y - grabY))
        }


        /**
         * Gets called AFTER the window moving process has begun, that means it won't see the button press that
         * initiated the move, only the release. It can still see other mouse button events, best probably to
         * ignore them.
         */
        fun processCursorButton(event: PointerButtonEvent) {
            if (event.button == initiatingButton) {
                assert(event.state != PointerButtonState.Pressed) { "This can't happen, button pressed twice in row" }

                // Bugfix: It's not enough to just transition to `passthrough` mode, MUST pass "button released"
                // event to client window, so it can know the users stopped the drag procedure. Otherwise, the
                // compositor stops the drag, but the client window still thinks it's going on.
                compositor.seat.pointerNotifyButton(event.timeMsec, event.button, event.state)

                // Now it's okay to stop the move, and go back to pass through.
                compositor.captureMode.transitionToPassthrough()

            } else {
                // Exit the move mode only when the released button is the one that started the move request
                // (kind of). Don't do anything on any other mouse button, just swallow the event.
                println("Mouse button pressed while dragging: ${event.button}")
            }

            // TOOD: Why do I have to comment this out???
//            if (event.state == PointerButtonState.RELEASED /*&& event.button in pressedButtons*/) {
//                println(">>> ${pressedButtons}")
//            }

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
            val borderX = (grabbedSceneNode.x + geometry.x) + if (Right in edges) geometry.width else 0
            val borderY = (grabbedSceneNode.y + geometry.y) + if (Bottom in edges) geometry.height else 0
            grabX = cursor.wlrCursor.x - borderX
            grabY = cursor.wlrCursor.y - borderY
            grabbedGeometry = Box.allocateCopy(geometry).apply {
                x += grabbedSceneNode.x
                y += grabbedSceneNode.y
            }
        }

        // Do the window resize
        fun processCursorMotion() {
            val borderX = (cursor.wlrCursor.x - grabX).toInt()
            val borderY = (cursor.wlrCursor.y - grabY).toInt()

            // Coordinates for new (resized) vertices
            var left = grabbedGeometry.x
            var right = grabbedGeometry.x + grabbedGeometry.width
            var top = grabbedGeometry.y
            var bottom = grabbedGeometry.y + grabbedGeometry.height

            // TODO: Explain what does this do? (clamping)
            when {
                Top in edges -> {
                    top = borderY
                    if (top >= bottom) top = bottom - 1
                }

                Bottom in edges -> {
                    bottom = borderY
                    if (bottom <= top) bottom = top + 1
                }
            }

            when {
                Left in edges -> {
                    left = borderX
                    if (left >= right) left = right - 1
                }

                Right in edges -> {
                    right = borderX
                    if (right <= left) right = left + 1
                }
            }

            val geometry = grabbedToplevel.getBase().geometry
            grabbedSceneNode.setPosition(left - geometry.x, top - geometry.y)
            grabbedToplevel.setSize(right - left, bottom - top)
        }


        fun processCursorButton(event: PointerButtonEvent) {
            if (event.state == PointerButtonState.Released)
                COMPOSITOR.captureMode.transitionToPassthrough()
        }
    }
}
