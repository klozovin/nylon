package compositor.input

import compositor.*
import linux.MouseButton
import wayland.PointerButtonState
import wayland.util.Edge
import wlroots.types.keyboard.KeyEvent
import wlroots.types.keyboard.KeyboardModifier
import wlroots.types.pointer.PointerButtonEvent
import wlroots.types.xdgshell.XdgToplevel
import wlroots.util.Box
import xkbcommon.XkbKey


/**
 * State machine modeling the cursor input modes (passthrough, move, resize).
 */
sealed class CursorInputState(val compositor: Compositor) {
    // TODO: add compositor as param

    abstract fun onKeyboardKey(event: KeyEvent, keysym: Int)
    abstract fun onCursorMotion(timeMsec: Int)
    abstract fun onCursorButton(event: PointerButtonEvent)


    /**
     * Normal cursor behavior: pass all the pointer events to focused client
     *
     */
    class Passthrough(compositor: Compositor) : CursorInputState(compositor) {

        init {
            compositor.inputSystem.cursor.setIcon("default")
        }


        override fun onKeyboardKey(event: KeyEvent, keysym: Int) {
            unreachable()
        }


        /**
         * Possible action to take:
         *
         * - Raise window when cursor passing over
         * - Give focus to window when cursor passing over (but don't raise)
         */
        override fun onCursorMotion(timeMsec: Int) {
            val windowSystem = compositor.windowSystem
            val cursor = compositor.inputSystem.cursor
            val seat = compositor.seat

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
         * - Defocus, when clicking on desktop background
         * - Focus/raise window when clicking on a window
         * - Move when clicking while holding mod key
         * - Resize when RMB clicking while holding mod key
         */
        override fun onCursorButton(event: PointerButtonEvent) {
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
                        if (cursorX <= halfX) Edge.Left else Edge.Right,
                        if (cursorY <= halfY) Edge.Top else Edge.Bottom
                    )

                    if (cursorX <= halfX) {
                        println("we're in the left half")
                    } else {
                        println("were in the right half")
                    }

                    if (cursorY <= halfY) println("We're in the top half")
                    else println("we in the bottom half")

                    // TODO: Detect grabbed window edge

                    compositor.captureMode.transitionToResize(cursorTarget.toplevel, edges)
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
        compositor: Compositor, val grabbedToplevel: XdgToplevel, val initiatingButton: Int
    ) : CursorInputState(compositor) {

        // TODO: pass this as param, no need to know where is the toplevel kept and how
        val grabbedSceneNode = compositor.windowSystem.toplevelSceneTree[grabbedToplevel]!!.node
        val startingX = grabbedSceneNode.x
        val startingY = grabbedSceneNode.y

        val cursor = compositor.inputSystem.cursor
        val grabX = compositor.inputSystem.cursor.wlrCursor.x - grabbedSceneNode.x
        val grabY = compositor.inputSystem.cursor.wlrCursor.y - grabbedSceneNode.y


        init {
            println("WindowMove: $initiatingButton")
        }


        override fun onKeyboardKey(event: KeyEvent, keysym: Int) {
            when (keysym) {
                // Stop the move operation, restore starting window position
                XkbKey.Escape -> {
                    grabbedSceneNode.setPosition(startingX, startingY)
                    compositor.captureMode.transitionToPassthrough()
                }
            }
        }


        override fun onCursorMotion(timeMsec: Int) {
            // HACK: this kind of check should be automatic and centralized somewhere
            check(compositor.windowSystem.toplevels.containsValue(grabbedToplevel)) {
                "BUG: Trying to move a non existent window"
            }
            grabbedSceneNode.setPosition((cursor.wlrCursor.x - grabX), (cursor.wlrCursor.y - grabY))
        }


        override fun onCursorButton(event: PointerButtonEvent) {
            // Gets called AFTER the window moving process has begun, that means it won't see the button press that
            // initiated the move, only the release. It can still see other mouse button events, best probably to
            // ignore them.

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
    class WindowResize(compositor: Compositor, val grabbedToplevel: XdgToplevel, val edges: Edges) :
        CursorInputState(compositor) {

        val cursor = COMPOSITOR.inputSystem.cursor
        var grabbedGeometry: Box // TODO: var needed?
        val grabbedSceneTree = compositor.windowSystem.toplevelSceneTree[grabbedToplevel]!!
        val grabbedSceneNode = grabbedSceneTree.node

        // Starting size and position, used for restoring.
        val startingX = grabbedSceneNode.x
        val startingY = grabbedSceneNode.y
        val startingWidth = grabbedToplevel.base.geometry.width
        val startingHeight = grabbedToplevel.base.geometry.height

        var grabX: Double
        var grabY: Double

        init {
            val geometry = grabbedToplevel.base.geometry
            val borderX = (grabbedSceneNode.x + geometry.x) + if (Edge.Right in edges) geometry.width else 0
            val borderY = (grabbedSceneNode.y + geometry.y) + if (Edge.Bottom in edges) geometry.height else 0
            grabX = cursor.wlrCursor.x - borderX
            grabY = cursor.wlrCursor.y - borderY
            grabbedGeometry = Box.allocateCopy(geometry).apply {
                x += grabbedSceneNode.x
                y += grabbedSceneNode.y
            }
        }


        override fun onKeyboardKey(event: KeyEvent, keysym: Int) {
            when (keysym) {
                XkbKey.Escape -> {
                    grabbedSceneNode.setPosition(startingX, startingY)
                    grabbedToplevel.setSize(startingWidth, startingHeight)
                    compositor.captureMode.transitionToPassthrough()
                }
                else -> println("$this -> don't know what to do on that key")
            }
        }


        override fun onCursorMotion(timeMsec: Int) {
            val borderX = (cursor.wlrCursor.x - grabX).toInt()
            val borderY = (cursor.wlrCursor.y - grabY).toInt()

            // Coordinates for new (resized) vertices
            var left = grabbedGeometry.x
            var right = grabbedGeometry.x + grabbedGeometry.width
            var top = grabbedGeometry.y
            var bottom = grabbedGeometry.y + grabbedGeometry.height

            // TODO: Explain what does this do? (clamping)
            when {
                Edge.Top in edges -> {
                    top = borderY
                    if (top >= bottom) top = bottom - 1
                }

                Edge.Bottom in edges -> {
                    bottom = borderY
                    if (bottom <= top) bottom = top + 1
                }
            }

            when {
                Edge.Left in edges -> {
                    left = borderX
                    if (left >= right) left = right - 1
                }

                Edge.Right in edges -> {
                    right = borderX
                    if (right <= left) right = left + 1
                }
            }

            val geometry = grabbedToplevel.getBase().geometry
            grabbedSceneNode.setPosition(left - geometry.x, top - geometry.y)
            grabbedToplevel.setSize(right - left, bottom - top)
        }


        override fun onCursorButton(event: PointerButtonEvent) {
            if (event.state == PointerButtonState.Released) COMPOSITOR.captureMode.transitionToPassthrough()
        }
    }
}