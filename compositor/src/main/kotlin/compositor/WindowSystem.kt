package compositor

import wayland.server.Listener
import wayland.util.Edge
import wlroots.types.compositor.Surface
import wlroots.types.scene.SceneBuffer
import wlroots.types.scene.SceneNode
import wlroots.types.scene.SceneSurface
import wlroots.types.scene.SceneTree
import wlroots.types.xdgshell.XdgPopup
import wlroots.types.xdgshell.XdgSurface
import wlroots.types.xdgshell.XdgToplevel
import java.util.*


class WindowSystem(val compositor: Compositor) {
    val toplevels: MutableMap<Listener, XdgToplevel> = HashMap()
    val toplevelSceneTree: MutableMap<XdgToplevel, SceneTree> = HashMap() // TODO: Use BidiMap here?

    val popups: MutableMap<Listener, XdgPopup> = HashMap()
    val popupSceneTree: MutableMap<XdgPopup, SceneTree> = HashMap()

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

        // TODO: Extract into method
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
        // TODO: Is this the way to do it?
        // Deny move/resize requests from unfocused clients
        if (toplevel.base().surface() != compositor.seat.pointerState().focusedSurface().rootSurface)
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
                require(edges != null)

                val geometryBox = toplevel.base().getGeometry()

                val borderX =
                    (sceneNode.x() + geometryBox.x()) + if (Edge.RIGHT in edges) geometryBox.width() else 0
                val borderY =
                    (sceneNode.y() + geometryBox.y()) + if (Edge.BOTTOM in edges) geometryBox.height() else 0

                compositor.grabX = compositor.cursor.x() - borderX
                compositor.grabY = compositor.cursor.y() - borderY

                compositor.grabGeobox = geometryBox
                with(compositor.grabGeobox) {
                    x(x() + sceneNode.x())
                    y(y() + sceneNode.y())
                }

                compositor.resizeEdges = edges
            }

            CursorMode.Passthrough -> {
                unreachable()
            }
        }
    }


    // **************************************************************************************************** //
    // * Listeners: XDG toplevel windows                                                                  * //
    // **************************************************************************************************** //


    fun onNewToplevel(toplevel: XdgToplevel) {
        // Create the SceneTree for this XdgToplevel and add all signal handlers we have to deal with.
        toplevelSceneTree[toplevel] = compositor.scene.tree().xdgSurfaceCreate(toplevel.base())
        arrayOf(
            *with(toplevel.base().surface().events) {
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
        if (!isPointerGrabValid(event.serial))
            return
        // TODO: Cleaner API, without having to pass null for edges
        beginInteractive(event.toplevel, CursorMode.Move, null)
    }


    fun onToplevelRequestResize(event: XdgToplevel.ResizeEvent) {
        if (!isPointerGrabValid(event.serial))
            return
        beginInteractive(event.toplevel, CursorMode.Resize, event.edges)
    }


    fun onToplevelRequestMaximize(listener: Listener) {
        // Maximize request not supported, but protocol demands we send a configure back to client.
        toplevels[listener]!!.apply {
            if (base().initialized())
                base().scheduleConfigure()
        }
    }


    fun onToplevelRequestFullscreen(listener: Listener) {
        // Fullscreen request not supported, behave the same as maximize request.
        toplevels[listener]!!.apply {
            if (base().initialized())
                base().scheduleConfigure()
        }
    }


    // **************************************************************************************************** //
    // * Listeners: XDG popups                                                                            * //
    // **************************************************************************************************** //


    fun onNewPopup(popup: XdgPopup) {
        val parentSurface = XdgSurface.tryFromSurface(popup.parent())
            ?: error("Popup's parent can't be null")

        // Search for the parent of this new popup: first among toplevels, then other popups (they are nestable)
        val parentSceneTree =
            toplevelSceneTree.asSequence().find { (toplevel, _) -> toplevel.base() == parentSurface }?.value
                ?: popupSceneTree.asSequence().find { (popup, _) -> popup.base() == parentSurface }?.value
                ?: error("BUG: Can't have a XdgPopup without a parent")

        popupSceneTree[popup] = parentSceneTree.xdgSurfaceCreate(popup.base())
        popups[popup.base().surface().events.commit.add(::onPopupCommit)] = popup
        popups[popup.events.destroy.add(::onPopupDestroy)] = popup
    }


    fun onPopupCommit(listener: Listener, surface: Surface) {
        popups[listener]!!.apply {
            if (base().initialCommit())
                base().scheduleConfigure()
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
        val surfaceNum = this.base().surface().events.allSignals().sumOf { it.listenerList.length() }
        val popupNum = this.events.allSignals().sumOf { it.listenerList.length() }
        return surfaceNum + popupNum
    }


    private fun XdgPopup.removeSceneTree() {
        popupSceneTree.remove(this)!!
    }


    /**
     * Check whether the pointer grab for move or resize operation is valid.
     *
     * Fixes bad behaviour for clients built with Rust's "winit" library: they try to initiate the move/resize
     * after the mouse button has already been released, leading to state where the window is "stuck" to the
     * cursor even though no mouse button are being held down.
     */
    private fun isPointerGrabValid(serial: Int): Boolean =
        compositor.seat.validatePointerGrabSerial(compositor.seat.pointerState().focusedSurface(), serial)


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
        with(toplevel.base().surface().events) {
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