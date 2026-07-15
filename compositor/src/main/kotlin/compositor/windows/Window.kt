package compositor.windows

import compositor.windows.WindowSystem.BaseWindow
import wayland.server.Listener
import wlroots.types.compositor.Surface
import wlroots.types.scene.SceneTree
import wlroots.types.xdg_shell.XdgSurfaceConfigure
import wlroots.types.xdg_shell.XdgToplevel


/**
 * Represents one visible window on the screen, combines in one place:
 *
 * - XdgSurface
 * - XdgToplevel
 * - SceneTree
 */
class Window(val windows: WindowSystem, val xdgToplevel: XdgToplevel) : BaseWindow {

    override val sceneTree: SceneTree
    val listeners: MutableList<Listener> = mutableListOf()

    // Children
    val childWindows: MutableList<Window> = mutableListOf()
    val childPopups: MutableList<Popup> = mutableListOf()

    // For debugging
    var isDestroyed = false


    init {
        sceneTree = windows.scene.tree.xdgSurfaceCreate(xdgToplevel.base)
        with(xdgToplevel.base.surface.events) {
            listeners.addAll(
                arrayOf(
                    // TODO: Use array literals
                    map.add(::onMap),
                    unmap.add(::onUnmap),
                    commit.add(::onCommit),
                )
            )
        }
        with(xdgToplevel.events) {
            listeners.addAll(
                arrayOf(
                    destroy.add(::onDestroy),
                    requestMove.add(::onRequestMove),
                    requestResize.add(::onRequestResize),
                    requestMaximize.add(::onRequestMaximize),
                    requestFullscreen.add(::onRequestFullscreen),
                    ackConfigure.add(::onAckConfigure)
                )
            )
        }
    }


    override fun equals(other: Any?): Boolean {
        // Two windows are the same if they reference the same XdgToplevel
        return when (other) {
            is Window -> this.xdgToplevel == other.xdgToplevel
            else -> false
        }
    }


    override fun hashCode(): Int {
        TODO("Dont use yet")
        return xdgToplevel.hashCode()
    }


    //
    // *** Helper functions
    //

    override fun addChild(child: BaseWindow) {
        when (child) {
            is Window -> {
                require(!childWindows.contains(child))
                require(!child.isDestroyed)
                childWindows.add(child)
            }

            is Popup -> {
                require(!childPopups.contains(child))
                require(!child.isDestroyed)
                childPopups.add(child)
            }
        }
    }

    override fun removeChild(child: BaseWindow) {
        when (child) {
            is Window -> childWindows.remove(child)
            is Popup -> childPopups.remove(child)
        }
    }


    //
    // *** Signal handlers
    //

    fun onMap() {
        windows.focuser.focusWindow(this)
    }


    fun onUnmap(listener: Listener) {
        // TODO: Grab mode should use Window abstraction
        if (windows.compositor.captureMode.isWindowGrabbed(this)) {
            windows.compositor.captureMode.transitionToPassthrough()
        }

        // TODO: Clear focus or switch to previously focused?
        windows.focuser.unfocusWindow(this)

    }


    fun onCommit(listener: Listener, surface: Surface) {
        if (xdgToplevel.base.initialCommit)
            xdgToplevel.setSize(0, 0)

    }


    fun onDestroy() {
        require(childWindows.isEmpty())
        require(childPopups.isEmpty())

        // Unregister listeners, ask the window system to remove this window
        listeners.forEach { it.remove() }
        windows.removeWindow(this)

        // Used for debugging and much needed sanity checking
        isDestroyed = true
    }


    fun onRequestMove(event: XdgToplevel.MoveEvent) {
        // Have to check if the event is valid, ie user initiated
        if (!windows.isPointerGrabValid(event.serial))
            return

        val pressedButton = windows.compositor.seat.pointerState.buttons.first()
        require(pressedButton.nPressed == 1L) { "Can't handle multiple pointing devies at the same time" }

        windows.compositor.captureMode.transitionToMove(this, pressedButton.button)
    }


    fun onRequestResize(event: XdgToplevel.ResizeEvent) {
        if (!windows.isPointerGrabValid(event.serial))
            return

        windows.compositor.captureMode.transitionToResize(this, event.edges)
    }


    fun onRequestMaximize() {
        // Maximize request not supported, but wayland protocol demands we send a configure back to client.
        if (xdgToplevel.base.initialized)
            xdgToplevel.base.scheduleConfigure()

    }


    fun onRequestFullscreen() {
        // Fullscreen not supported, Wayland protocol demands we send a configure back to client.
        if (xdgToplevel.base.initialized)
            xdgToplevel.base.scheduleConfigure()
    }


    fun onAckConfigure(configure: XdgSurfaceConfigure.Toplevel) {
        // TODO: Should this remain here?
        windows.moveAndResize?.let {
            require(it.toplevel == this.xdgToplevel)
            this.sceneTree.node.setPosition(it.x, it.y)
            windows.moveAndResize = null
        }
    }


    fun moveDiagonallyDown() {
        val x = sceneTree.node.x
        val y = sceneTree.node.y
        sceneTree.node.setPosition(x + 10, y + 10)
    }
}