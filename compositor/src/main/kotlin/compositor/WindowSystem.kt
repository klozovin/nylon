package compositor

import wayland.server.Listener
import wlroots.types.compositor.Surface
import wlroots.types.scene.SceneBuffer
import wlroots.types.scene.SceneNode
import wlroots.types.scene.SceneSurface
import wlroots.types.scene.SceneTree
import wlroots.types.xdgshell.XdgPopup
import wlroots.types.xdgshell.XdgShell
import wlroots.types.xdgshell.XdgSurface
import wlroots.types.xdgshell.XdgToplevel
import java.util.*


class WindowSystem(val compositor: Compositor) {
    val xdgShell: XdgShell

    val toplevels: MutableMap<Listener, XdgToplevel> = HashMap()
    val toplevelSceneTree: MutableMap<XdgToplevel, SceneTree> = HashMap() // TODO: Use BidiMap here?

    val popups: MutableMap<Listener, XdgPopup> = HashMap()
    val popupSceneTree: MutableMap<XdgPopup, SceneTree> = HashMap()

    var focusedToplevel: XdgToplevel? = null // TODO: Should go to 'cycle' class


    init {
        xdgShell = XdgShell.create(compositor.display, 3).apply {
            events.newToplevel.add(::onNewToplevel)
            events.newPopup.add(::onNewPopup)
        }
    }


    fun focusToplevel(toplevel: XdgToplevel) {
        val prevFocusedSurface = compositor.seat.keyboardState.getFocusedSurface()
        val prevFocusedToplevel = focusedToplevel

        focusedToplevel = toplevel

        // Don't focus what's already focused
        if (prevFocusedSurface == toplevel.base.surface)
            return

        // Deactivate the previously focused surface, let the client know it lost focus so it can repaint
        // accordingly (e.g. stop drawing a caret)
        prevFocusedSurface?.let {
            // TODO: Why do it like this, we already have the focused toplevel?
            val prevToplevelFromSurface = XdgToplevel.tryFromSurface(prevFocusedSurface)
            require(prevFocusedToplevel == prevToplevelFromSurface)
            prevToplevelFromSurface?.setActivated(false)
        }

        toplevelSceneTree[toplevel]!!.node.raiseToTop()
        toplevel.setActivated(true)

        compositor.seat.getKeyboard()?.let { keyboard ->
            compositor.seat.keyboardNotifyEnter(
                toplevel.getBase().getSurface(),
                keyboard.keycodesPtr(),
                keyboard.keycodesNum(),
                keyboard.modifiers()
            )
        }
    }

    // TODO
    fun focusNextToplevel() {
        // In order of last focus
        if (toplevels.isNotEmpty()) {
//            val nextIdx = focusedToplevel + 1
        }
    }


    fun toplevelAtCoordinates(x: Double, y: Double): UnderCursor? {
        val (sceneNode, nx, ny) = compositor.scene.tree().node.nodeAt(x, y)
            ?: return null

        if (sceneNode.type != SceneNode.Type.BUFFER)
            return null

        val sceneBuffer = SceneBuffer.fromNode(sceneNode)
        val sceneSurface = SceneSurface.tryFromBuffer(sceneBuffer) ?: return null

        // TODO: Extract into method
        for (sceneTree in sceneNode.parentIterator)
            for ((toplevel, toplevelSceneTree) in toplevelSceneTree)
                if (sceneTree == toplevelSceneTree)
                    return UnderCursor(toplevel, sceneSurface.surface(), nx, ny)
        unreachable()
    }


    // **************************************************************************************************** //
    // * Listeners: XDG toplevel windows                                                                  * //
    // **************************************************************************************************** //


    fun onNewToplevel(toplevel: XdgToplevel) {
        // Create the SceneTree for this XdgToplevel and add all signal handlers we have to deal with.
        toplevelSceneTree[toplevel] = compositor.scene.tree().xdgSurfaceCreate(toplevel.getBase())
        arrayOf(
            *with(toplevel.base.surface.events) {
                // Listeners for the base surface
                arrayOf(
                    map.add(::onToplevelMap),
                    unmap.add(::onToplevelUnmap),
                    commit.add(::onToplevelCommit)
                )
            },
            *with(toplevel.events) {
                // Listeners for the XDG toplevel
                arrayOf(
                    destroy.add(::onToplevelDestroy),
                    requestMove.add(::onToplevelRequestMove),
                    requestResize.add(::onToplevelRequestResize),
                    requestMaximize.add(::onToplevelRequestMaximize),
                    requestFullscreen.add(::onToplevelRequestFullscreen),
                )
            }
        ).forEach { toplevels.put(it, toplevel) }
    }


    fun onToplevelDestroy(listener: Listener) {
        val toplevel = toplevels[listener]!!

        // Sanity checks
        val listenersNumBefore = _countSignalHandlers(toplevel)

        // All listeners added to this XdgToplevel
        val listeners = toplevels.entries.filter { it.value == toplevel }

        // Remove listeners from this XdgToplevel's signals
        // TODO: Memory management: close the Arena for every Listener
        listeners.forEach { (listener, _) -> listener.remove() }

        // Remove all listener->toplevel entries in the hash map
        toplevels.entries.removeAll(listeners)

        // Remove the SceneTree belonging to this XdgToplevel window
        toplevelSceneTree.remove(toplevel)!!

        // Sanity checks
        require(listeners.isNotEmpty())
        require(toplevels.values.none(toplevel::equals))
        require(listenersNumBefore - _countSignalHandlers(toplevel) == listeners.size)
    }


    fun onToplevelCommit(listener: Listener, surface: Surface) {
        toplevels[listener]!!.apply {
            if (base.initialCommit)
                setSize(0, 0)
        }
    }


    fun onToplevelMap(listener: Listener) {
        focusToplevel(toplevels[listener]!!)
    }


    fun onToplevelUnmap(listener: Listener) {
        // Reset the cursor mode if we have to unmap (hide) the grabbed toplevel, otherwise the compositor
        // crashes when trying to move/resize a non-existing window.
        val unmappedToplevel = toplevels[listener]!!
        if (compositor.captureMode.isToplevelGrabbed(unmappedToplevel))
            compositor.captureMode.transitionToPassthrough()
    }


    fun onToplevelRequestMove(event: XdgToplevel.MoveEvent) {
        if (!isPointerGrabValid(event.serial))
            return
        compositor.captureMode.transitionToMove(event.toplevel)
    }


    fun onToplevelRequestResize(event: XdgToplevel.ResizeEvent) {
        if (!isPointerGrabValid(event.serial))
            return
        compositor.captureMode.transitionToResize(event.toplevel, event.edges)
    }


    fun onToplevelRequestMaximize(listener: Listener) {
        // Maximize request not supported, but protocol demands we send a configure back to client.
        toplevels[listener]!!.apply {
            if (base.initialized)
                base.scheduleConfigure()
        }
    }


    fun onToplevelRequestFullscreen(listener: Listener) {
        // Fullscreen request not supported, behave the same as maximize request.
        toplevels[listener]!!.apply {
            if (base.initialized)
                base.scheduleConfigure()
        }
    }


    // **************************************************************************************************** //
    // * Listeners: XDG popups                                                                            * //
    // **************************************************************************************************** //


    fun onNewPopup(popup: XdgPopup) {
        val parentSurface = XdgSurface.tryFromSurface(popup.parent)
            ?: error("Popup's parent can't be null")

        // Search for the parent of this new popup: first among toplevels, then other popups (they are nestable)
        val parentSceneTree =
            toplevelSceneTree.asSequence().find { (toplevel, _) -> toplevel.base == parentSurface }?.value
                ?: popupSceneTree.asSequence().find { (popup, _) -> popup.base == parentSurface }?.value
                ?: error("BUG: Can't have a XdgPopup without a parent")

        popupSceneTree[popup] = parentSceneTree.xdgSurfaceCreate(popup.base)
        popups[popup.base.surface.events.commit.add(::onPopupCommit)] = popup
        popups[popup.events.destroy.add(::onPopupDestroy)] = popup
    }


    fun onPopupCommit(listener: Listener, surface: Surface) {
        popups[listener]!!.apply {
            if (base.initialCommit)
                base.scheduleConfigure()
        }
    }


    fun onPopupDestroy(listener: Listener) {
        val popup = popups[listener]!!

        // Get rid of the listeners for this popup's signals
        popup.cleanupListeners()

        // Remove the scene tree
        popup.removeSceneTree()
    }


    /**
     * Unregister listeners from their signals (wlroots side), remove the cached listener entries in the
     * hash map.
     */
    private fun XdgPopup.cleanupListeners() {
        val listeners = popups.entries.filter { it.value == this }

        // Sanity check
        require(listeners.isNotEmpty())
        val numListenersBefore = this.numOfListeners()

        // Unregister all listeners from their XdgPopup signals
        listeners.forEach { (listener, _) -> listener.remove() }

        // Remove the listener entries from the hash map
        popups.entries.removeAll(listeners)

        // TODO: Memory management: Close the Arena for this popup

        // Sanity check
        require(popups.values.none(this::equals))
        require(numListenersBefore - numOfListeners() == listeners.size)
    }


    /**
     * Get the number of registered listeners for all the signals of the XdgPopup object
     */
    private fun XdgPopup.numOfListeners(): Int {
        val surfaceNum = this.base.surface.events.allSignals().sumOf { it.listenerList.length() }
        val popupNum = this.events.allSignals().sumOf { it.listenerList.length() }
        return surfaceNum + popupNum
    }


    private fun XdgPopup.removeSceneTree() {
        popupSceneTree.remove(this)!!
    }


    /**
     * Check whether the pointer grab for move or resize operation is valid.
     *
     * Also fixes bad behaviour for clients built with Rust's "winit" library: they try to initiate the
     * move/resize after the mouse button has already been released, leading to state where the window is
     * "stuck" to the cursor even though no mouse buttons are held down.
     */
    private fun isPointerGrabValid(serial: Int): Boolean {
        // TODO: Add some kind of logging when this fails
        val focusedSurface = compositor.seat.pointerState.focusedSurface ?: error("No surface focused")
        return compositor.seat.validatePointerGrabSerial(focusedSurface, serial)
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

// TODO: Move to extension function and enumerate signals in bindings code
private fun _countSignalHandlers(toplevel: XdgToplevel): Int =
    arrayOf(
        with(toplevel.getBase().getSurface().events) {
            arrayOf(map, unmap, commit)
        },
        with(toplevel.events) {
            arrayOf(
                destroy,
                requestMove,
                requestResize,
                requestMaximize,
                requestFullscreen
            )
        }
    ).flatten().sumOf { it.listenerList.length() }