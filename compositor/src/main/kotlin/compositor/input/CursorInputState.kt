package compositor.input

import compositor.*
import compositor.windows.Window
import compositor.windows.WindowSystem
import linux.MouseButton
import wayland.KeyboardKeyState
import wayland.PointerButtonState
import wayland.util.Edge
import wlroots.types.keyboard.KeyboardModifier
import wlroots.types.pointer.PointerButtonEvent
import wlroots.util.Box
import xkbcommon.XkbKey


/**
 * State machine modeling the cursor input modes (passthrough, move, resize).
 */
sealed class CursorInputState(val compositor: Compositor) {
    // TODO: add compositor as param

    abstract fun onKeyboardKey(keysym: Int, state: KeyboardKeyState): Boolean
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


        override fun onKeyboardKey(keysym: Int, state: KeyboardKeyState): Boolean {
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

            when (val tpl = windowSystem.findWindowAtCoordinates(cursor.wlrCursor.x, cursor.wlrCursor.y)) {
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
                    windowSystem.findWindowAtCoordinates(cursor.wlrCursor.x, cursor.wlrCursor.y)?.let {
                        COMPOSITOR.captureMode.transitionToMove(it.window, event.button)
                        return
                    }
                }

                altPressed && rmbPressed -> {
                    // TODO: Maybe defocus when clicking on desktop?
                    val cursorTarget =
                        windowSystem.findWindowAtCoordinates(cursor.wlrCursor.x, cursor.wlrCursor.y) ?: return
                    val toplevel = cursorTarget.toplevel


                    val geometry = toplevel.base.geometry
                    val sceneTree =
                        cursorTarget.window.sceneTree // windowSystem.toplevelSceneTree[toplevel]!!
                    val coordinates = sceneTree.coords()


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

                    compositor.captureMode.transitionToResize(cursorTarget.window, edges)
                    return

                }

                // Raise and focus the clicked window
                event.state == PointerButtonState.Pressed -> {
                    windowSystem.findWindowAtCoordinates(cursor.wlrCursor.x, cursor.wlrCursor.y)?.let {
                        val toplevel = it.toplevel
                        // TODO: Don't do it like this, create new function windowAtCoords
//                        val window = compositor.windowSystem.windows.find { it.xdgToplevel == toplevel }!!


                        windowSystem.focuser.focusWindow(it.window)
//                        windowSystem.focusWindow(it.window)
//                        windowSystem.focusToplevel(it.toplevel)
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
        compositor: Compositor,
        val targetWindow: Window, // TODO: Rename to target
//        val grabbedToplevel: XdgToplevel,
        val initiatingButton: Int?,
        val initiatedWith: InitiatedWith = InitiatedWith.Mouse
    ) : CursorInputState(compositor) {

        // TODO: pass this as param, no need to know where is the toplevel kept and how
//        val grabbedSceneNode = compositor.windowSystem.toplevelSceneTree[grabbedToplevel]!!.node

        val grabbedSceneTree = targetWindow.sceneTree

//        val startingX = grabbedSceneNode.x
//        val startingY = grabbedSceneNode.y

        val startingX = targetWindow.sceneTree.x
        val startingY = targetWindow.sceneTree.y


        val cursor = compositor.inputSystem.cursor
        val grabX = compositor.inputSystem.cursor.wlrCursor.x - targetWindow.sceneTree.x
        val grabY = compositor.inputSystem.cursor.wlrCursor.y - targetWindow.sceneTree.y


        init {
            println("WindowMove: $initiatingButton")
        }


        override fun onKeyboardKey(keysym: Int, state: KeyboardKeyState): Boolean {
            if (keysym == XkbKey.Escape) {
                // Stop the move, restore starting window position
                grabbedSceneTree.setPosition(startingX, startingY)
                compositor.captureMode.transitionToPassthrough()
                return true
            }

            // Ignore all keypresses in mouse initiated move except above ESC
            if (initiatedWith == InitiatedWith.Mouse) return false

            // Do nothing on key release, only move window on key press
            if (state == KeyboardKeyState.Released) return false

            val nudge = 10
            var nudged= true
            when (keysym) {
                XkbKey.i -> grabbedSceneTree.setPosition(grabbedSceneTree.x, grabbedSceneTree.y -10)
                XkbKey.j -> grabbedSceneTree.setPosition(grabbedSceneTree.x - nudge, grabbedSceneTree.y)
                XkbKey.k -> grabbedSceneTree.setPosition(grabbedSceneTree.x, grabbedSceneTree.y + 10)
                XkbKey.l -> grabbedSceneTree.setPosition(grabbedSceneTree.x + nudge, grabbedSceneTree.y)
                XkbKey.o -> grabbedSceneTree.setPosition(grabbedSceneTree.x + nudge, grabbedSceneTree.y - nudge)
                else -> nudged = false
            }
            return nudged
        }


        override fun onCursorMotion(timeMsec: Int) {
            if (initiatedWith == InitiatedWith.Keyboard) return

            // HACK: this kind of check should be automatic and centralized somewhere
//            check(compositor.windowSystem.toplevels.containsValue(grabbedToplevel)) {
            check(compositor.windowSystem.windows.contains(targetWindow)) {

                "BUG: Trying to move a non existent window"
            }
            check(!targetWindow.isDestroyed)
            grabbedSceneTree.setPosition((cursor.wlrCursor.x - grabX), (cursor.wlrCursor.y - grabY))
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
    class WindowResize(
        compositor: Compositor,
        val targetWindow: Window,
//        val grabbedToplevel: XdgToplevel,
        val edges: Edges
    ) :
        CursorInputState(compositor) {

        val cursor = COMPOSITOR.inputSystem.cursor
        var grabbedGeometry: Box // TODO: var needed?

        //        val grabbedSceneTree = compositor.windowSystem.toplevelSceneTree[grabbedToplevel]!!
        val grabbedSceneTree = targetWindow.sceneTree

        // Starting size and position, used for restoring.
        val startingX = grabbedSceneTree.x
        val startingY = grabbedSceneTree.y
        val startingWidth = targetWindow.xdgToplevel.base.geometry.width
        val startingHeight = targetWindow.xdgToplevel.base.geometry.height

        var grabX: Double
        var grabY: Double

        init {
            val geometry = targetWindow.xdgToplevel.base.geometry
            val borderX = (grabbedSceneTree.x + geometry.x) + if (Edge.Right in edges) geometry.width else 0
            val borderY = (grabbedSceneTree.y + geometry.y) + if (Edge.Bottom in edges) geometry.height else 0
            grabX = cursor.wlrCursor.x - borderX
            grabY = cursor.wlrCursor.y - borderY
            grabbedGeometry = Box.allocateCopy(geometry).apply {
                x += grabbedSceneTree.x
                y += grabbedSceneTree.y
            }
        }


        override fun onKeyboardKey(keysym: Int, state: KeyboardKeyState): Boolean {
            when (keysym) {
                XkbKey.Escape -> {

                    compositor.windowSystem.moveAndResizeAtomic(
                        targetWindow,
                        startingX, startingY,
                        startingWidth, startingHeight
                    )


//                    grabbedSceneNode.setPosition(startingX, startingY)
//                    grabbedToplevel.setSize(startingWidth, startingHeight)

                    // Where to transition? Here or in the atomic move handler? Leave it for now here, it easier.
                    compositor.captureMode.transitionToPassthrough()
                    return true
                }

                else -> {
                    println("$this -> don't know what to do on that key")
                    return false
                }
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

            val geometry = targetWindow.xdgToplevel.base.geometry
//            grabbedSceneNode.setPosition(left - geometry.x, top - geometry.y)
//            grabbedToplevel.setSize(right - left, bottom - top)

            val positionX = left - geometry.x
            val positionY = top - geometry.y
            val width = right - left
            val height = bottom - top
            compositor.windowSystem.moveAndResizeAtomic(targetWindow, positionX, positionY, width, height)
        }


        override fun onCursorButton(event: PointerButtonEvent) {
            if (event.state == PointerButtonState.Released) {
                COMPOSITOR.captureMode.transitionToPassthrough()
            }
        }
    }


    enum class InitiatedWith {
        Mouse,
        Keyboard
    }
}