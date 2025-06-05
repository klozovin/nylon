package compositor

import wayland.server.Listener
import wayland.util.Edge
import wlroots.types.compositor.Surface
import wlroots.types.scene.SceneBuffer
import wlroots.types.scene.SceneNode
import wlroots.types.scene.SceneSurface
import wlroots.types.scene.SceneTree
import wlroots.types.xdgshell.XdgToplevel
import java.util.*


class WindowSystem(val compositor: Compositor) {
    val toplevels: MutableMap<Listener, XdgToplevel> = HashMap()
    val toplevelSceneTree: MutableMap<XdgToplevel, SceneTree> = HashMap() // TODO: Use BidiMap here

    var focusedToplevel: XdgToplevel? = null // TODO: Should go to 'cycle' class


    fun focusToplevel(toplevel: XdgToplevel) {
        val prevFocusedSurface = compositor.seat.keyboardState().focusedSurface()
        val prevFocusedToplevel = focusedToplevel
        focusedToplevel = toplevel

        // Don't focus what's alread focused
        if (prevFocusedSurface == toplevel.base().surface())
            return

        // Deactivate the previously focused surface, let the client know it lost focus so it can repaint
        // accordingly (e.g. stop drawing a caret)
        prevFocusedSurface?.let {
            // TODO: Why do it like this, we already have the focused toplevel?
            val prevToplevelFromSurface = XdgToplevel.tryFromSurface(prevFocusedSurface)
            require(prevFocusedToplevel == prevToplevelFromSurface)
            prevToplevelFromSurface?.setActivated(false)
        }

        toplevelSceneTree[toplevel]!!.node().raiseToTop()
        toplevel.setActivated(true)

        compositor.seat.getKeyboard()?.let { keyboard ->
            compositor.seat.keyboardNotifyEnter(
                toplevel.base().surface(),
                keyboard.keycodesPtr(),
                keyboard.keycodesNum(),
                keyboard.modifiers()
            )
        }
    }


    fun toplevelAtCoordinates(x: Double, y: Double): UnderCursor? {
        val (sceneNode, nx, ny) = compositor.scene.tree().node().nodeAt(x, y)
            ?: return null

        if (sceneNode.type() != SceneNode.Type.BUFFER)
            return null

        val sceneBuffer = SceneBuffer.fromNode(sceneNode)
        val sceneSurface = SceneSurface.tryFromBuffer(sceneBuffer) ?: return null

        for (sceneTree in sceneNode.parentIterator)
            for ((toplevel, toplevelSceneTree) in toplevelSceneTree)
                if (sceneTree == toplevelSceneTree)
                    return UnderCursor(toplevel, sceneSurface.surface(), nx, ny)

        unreachable()
    }


    // TODO: Shouldn't this go to input system? - No, it goes into the grab mode. Reset the state machine to base state
    fun resetCursorMode() {
        compositor.cursorMode = CursorMode.Passthrough
        compositor.grabbedToplevel = null
    }


    fun beginInteractive(toplevel: XdgToplevel, mode: CursorMode, edges: EnumSet<Edge>?) {
        val focusedSurface = compositor.seat.pointerState().focusedSurface()

        if (toplevel.base().surface() != focusedSurface.rootSurface)
            return

        compositor.grabbedToplevel = toplevel
        compositor.cursorMode = mode

        val sceneNode = toplevelSceneTree[toplevel]!!.node()
        when (mode) {
            CursorMode.Move -> {
                compositor.grabX = compositor.cursor.x() - sceneNode.x()
                compositor.grabY = compositor.cursor.y() - sceneNode.y()
            }

            CursorMode.Resize -> {
                TODO()
            }

            CursorMode.Passthrough -> {
                unreachable()
            }
        }
    }


    //
    // *** Listeners: XDG toplevel windows ***
    //

    fun onNewToplevel(toplevel: XdgToplevel) {
        val sceneTree = compositor.scene.tree().xdgSurfaceCreate(toplevel.base())
        toplevelSceneTree.put(toplevel, sceneTree)

        // Signal listeners for this toplevel's base surface
        with(toplevel.base().surface().events) {
            toplevels.put(map.add(::onToplevelMap), toplevel)
            toplevels.put(unmap.add(::onToplevelUnmap), toplevel)
            toplevels.put(commit.add(::onToplevelCommit), toplevel)
        }

        // Signal listeners for the XDG toplevel
        with(toplevel.events) {
            toplevels.put(destroy.add(::onToplevelDestroy), toplevel)
            toplevels.put(requestMove.add(::onToplevelRequestMove), toplevel)
            toplevels.put(requestResize.add(::onToplevelRequestResize), toplevel)
            toplevels.put(requestMaximize.add(::onToplevelRequestMaximize), toplevel)
            toplevels.put(requestFullscreen.add(::onToplevelRequestFullscreen), toplevel)
        }
    }


    fun onToplevelDestroy(listener: Listener) {
        val toplevel = toplevels[listener]!!
        val listeners = toplevels.filterValues(toplevel::equals).keys

        require(listeners.size == 8)

        // Remove all the Listeners associated with this XdgToplevel window
        // TODO: Memory management: close the Arena for every Listener
        listeners.forEach { it.remove() }

        // Remove its SceneTree
        toplevelSceneTree.remove(toplevel)!!
    }


    // TODO: Maybe remove surface argument?
    fun onToplevelCommit(listener: Listener, surface: Surface) {
        toplevels[listener]!!.apply {
            if (base().initialCommit())
                setSize(0, 0)
        }
    }


    fun onToplevelMap(listener: Listener) {
        focusToplevel(toplevels[listener]!!)
    }


    fun onToplevelUnmap(listener: Listener) {
        // Reset the cursor mode if we have to unmap (hide) the grabbed toplevel
        if (compositor.grabbedToplevel == toplevels[listener]!!)
            resetCursorMode()
    }


    fun onToplevelRequestMove(event: XdgToplevel.MoveEvent) {
        // Fix: Clients (using "winit" Rust library) trying to initiate drag after mouse button has been released
        if (!compositor.seat.validatePointerGrabSerial(
                compositor.seat.pointerState().focusedSurface(),
                event.serial
            )
        )
            return
        beginInteractive(event.toplevel, CursorMode.Move, null)
    }


    fun onToplevelRequestResize(event: XdgToplevel.ResizeEvent) {
        // Fix: Same as for the move event
        if (!compositor.seat.validatePointerGrabSerial(
                compositor.seat.pointerState().focusedSurface(),
                event.serial
            )
        )
            return
        beginInteractive(event.toplevel, CursorMode.Resize, event.edges)
    }


    fun onToplevelRequestMaximize(listener: Listener) {
        // Maximize request not supported, but protocol demands we send a configure back to client.
        val toplevel = toplevels[listener]!!
        if (toplevel.base().initialized())
            toplevel.base().scheduleConfigure()
    }


    fun onToplevelRequestFullscreen(listener: Listener) {
        // Fullscreen request not supported, behave the same as maximize request.
        val toplevel = toplevels[listener]!!
        if (toplevel.base().initialized())
            toplevel.base().scheduleConfigure()
    }


    //
    // Listeners: XDG popups
    //

    fun onNewPopup(x: Any) {}

    fun onPopupCommit() {

    }


    fun onPopupDestroy() {


    }

    //
    // Helper classes
    //

    data class UnderCursor(
        val toplevel: XdgToplevel,
        val surface: Surface,
        val nx: Double,
        val ny: Double,
    )
}