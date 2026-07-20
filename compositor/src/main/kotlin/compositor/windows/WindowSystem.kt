package compositor.windows

import compositor.Compositor
import compositor.unreachable
import wayland.server.Listener
import wlroots.types.compositor.Surface
import wlroots.types.scene.Scene
import wlroots.types.scene.SceneBuffer
import wlroots.types.scene.SceneNode
import wlroots.types.scene.SceneSurface
import wlroots.types.scene.SceneTree
import wlroots.types.xdg_shell.*


class WindowSystem(val compositor: Compositor) {

    val scene: Scene

    val xdgShell: XdgShell

    val newToplevelListener: Listener
    val newPopupListener: Listener
    val xdgShellDestroyListener: Listener

    val windows: MutableList<Window> = mutableListOf()
    val popups: MutableList<Popup> = mutableListOf()

    val focuser = Focuser(compositor)
    var moveAndResize: MoveAndResize? = null


    init {
        scene = Scene.create()

        xdgShell = XdgShell.create(compositor.display, 3).apply {
            newToplevelListener = events.newToplevel.add(::onNewToplevel)
            newPopupListener = events.newPopup.add(::onNewPopup)
            xdgShellDestroyListener = events.destroy.add(::onXdgShellDestroy)
        }
    }


    fun onNewToplevel(xdgToplevel: XdgToplevel) {
        require(xdgToplevel.parent == null) // For the time being, remove later when it first fails
        val window = Window(this, xdgToplevel)
        addWindow(window)
    }


    fun onNewPopup(xdgPopup: XdgPopup) {
        // Try to get parent's XdgSurface. I guess this is the only way to locate a popup.
        val parentSurface = XdgSurface.tryFromSurface(xdgPopup.parent)
            ?: error("Popup's parent can't be null")

        // Find a parent (either a Window or Popup)
        val parent: BaseWindow = findWindowByXdgSurface(parentSurface)
            ?: findPopupByXdgSurface(parentSurface) ?: error("Popup must have a parent Window/Popup")

        val popup = Popup(this, parent, xdgPopup)
        parent.addChild(popup)
        popups.add(popup)
    }


    fun onXdgShellDestroy(xdgShell: XdgShell) {
        newToplevelListener.remove()
        newPopupListener.remove()
        xdgShellDestroyListener.remove()
    }


    fun addWindow(window: Window) {
        require(!windows.contains(window))
        windows.add(window)
    }


    fun removeWindow(window: Window) {
        windows.remove(window)
        focuser.unfocusWindow(window)
    }


    fun removePopup(popup: Popup) {
        val removed = this.popups.remove(popup)
        check(removed)
    }

    fun findWindowAtCoordinates(x: Double, y: Double): UnderCursor? {
        // First, find a scene node under the cursor, then walk its parents upwards until you reach a
        // top level window.
        val (node, nx, ny) = scene.nodeAt(x, y) ?: return null
        if (node !is SceneBuffer) return null

        val buffer = node // SceneBuffer.fromNode(sceneNode)
        val surface = SceneSurface.tryFromBuffer(buffer) ?: return null

        for (sceneTree in node.parentIterator) {
            findWindowBySceneTree(sceneTree)?.let { window ->
                return UnderCursor(window, window.xdgToplevel, surface.getSurface(), nx, ny)
            }
        }

        unreachable()
    }


    fun findWindowBySceneTree(sceneTree: SceneTree): Window?{
        return windows.find {
            it.sceneTree == sceneTree
        }
    }


    fun findWindowByXdgSurface(xdgSurface: XdgSurface): Window? {
        return windows.find {
            it.xdgToplevel.base == xdgSurface
        }
    }

    fun findPopupByXdgSurface(xdgSurface: XdgSurface): Popup? {
        return popups.find { popup ->
            popup.xdgPopup.base == xdgSurface
        }
    }


    /**
     * Check whether the pointer grab for move or resize operation is valid.
     *
     * Also fixes bad behavior for clients built with Rust's "winit" library: they try to initiate the
     * move/resize after the mouse button has already been released, leading to state where the window is
     * "stuck" to the cursor even though no mouse buttons are held down.
     */
    fun isPointerGrabValid(serial: Int): Boolean {
        // TODO: Add some kind of logging when this fails
        val focusedSurface = compositor.seat.pointerState.focusedSurface ?: error("No surface focused")
        return compositor.seat.validatePointerGrabSerial(focusedSurface, serial)
    }


    fun moveAndResizeAtomic(window: Window, x: Int, y: Int, w: Int, h: Int) {
        assert(window.xdgToplevel.base.initialized)

        // Set size, commit transaction
        val setSizeSerial = window.xdgToplevel.setSize(w, h)
        val configureSerial = window.xdgToplevel.base.scheduleConfigure()
        check(setSizeSerial == configureSerial) // Don't need this for real, just to check an assumption

        // Setup waiting for ack
        val op = MoveAndResize(window.xdgToplevel, window.sceneTree, configureSerial, x, y, w, h)

        moveAndResize = op
        // Then do a move ... but NOT here :D


    }


    //
    // Helper classes
    //

    data class MoveAndResize(
        val toplevel: XdgToplevel,
        val sceneNode: SceneNode,
        val configureSerial: Int,
        val x: Int,
        val y: Int,
        val width: Int, // TODO: Maybe don't need it here?
        val height: Int
    )


    data class UnderCursor(
        val window: Window,
        val toplevel: XdgToplevel,
        val surface: Surface,
        val nx: Double,
        val ny: Double,
    ) {
        init {
            window.xdgToplevel.base.surface == surface
        }
    }


    sealed interface BaseWindow {
        val sceneTree: SceneTree
        fun addChild(child: BaseWindow)
        fun removeChild(child: BaseWindow)
    }
}