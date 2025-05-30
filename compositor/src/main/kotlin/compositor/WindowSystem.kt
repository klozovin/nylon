package compositor

import wayland.server.Listener
import wayland.util.Edge
import wlroots.types.compositor.Surface
import wlroots.types.scene.SceneTree
import wlroots.types.xdgshell.XdgToplevel
import java.util.EnumSet


class WindowSystem(val compositor: Compositor) {
    val toplevels: MutableMap<Listener, XdgToplevel> = HashMap()
    val toplevelSceneTree: MutableMap<XdgToplevel, SceneTree> = HashMap()


    fun focusToplevel(toplevel: XdgToplevel) {
        TODO()
    }

    fun resetCursorMode() {
        TODO()
    }

    fun beginInteractive(toplevel: XdgToplevel, mode: CursorMode, edges: EnumSet<Edge>?) {
        TODO()
    }


    //
    // Listeners: XDG toplevel windows
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
        if (!compositor.seat.validatePointerGrabSerial(compositor.seat.pointerState().focusedSurface(), event.serial))
            return
        beginInteractive(event.toplevel, CursorMode.Move, null)
    }


    fun onToplevelRequestResize(event: XdgToplevel.ResizeEvent) {
        // Fix: Same as for the move event
        if (!compositor.seat.validatePointerGrabSerial(compositor.seat.pointerState().focusedSurface(), event.serial))
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

}