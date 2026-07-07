package compositor

import compositor.windows.Focuser
import compositor.windows.Popup
import compositor.windows.Window
import wlroots.types.compositor.Surface
import wlroots.types.scene.SceneBuffer
import wlroots.types.scene.SceneNode
import wlroots.types.scene.SceneSurface
import wlroots.types.scene.SceneTree
import wlroots.types.xdg_shell.*
import java.util.*


class WindowSystem(val compositor: Compositor) {
    val xdgShell: XdgShell

    val windows: MutableList<Window> = mutableListOf()
    val popups: MutableList<Popup> = mutableListOf()

    val focuser = Focuser(compositor)
    var moveAndResize: MoveAndResize? = null


    init {
        xdgShell = XdgShell.create(compositor.display, 3).apply {
            events.newToplevel.add(::onNewToplevel)
            events.newPopup.add(::onNewPopup)
        }
    }


    fun onNewToplevel(xdgToplevel: XdgToplevel) {
        require(xdgToplevel.parent == null) // For the time being, remove later when it first fails
        val sceneTree = compositor.scene.tree.xdgSurfaceCreate(xdgToplevel.base)
        val window = Window(this, xdgToplevel, sceneTree)
        check(!windows.contains(window))
        windows.add(window)
    }



    fun onNewPopup(xdgPopup: XdgPopup) {
        // Try to get parent's XdgSurface. I guess this is the only way to locate a popup.
        val parentSurface =
            XdgSurface.tryFromSurface(xdgPopup.parent) ?: error("Popup's parent can't be null")

        // Parent is a Window
        findWindowByXdgSurface(parentSurface)?.let { parent ->
            val sceneTree = SceneTree.createFromParent(parent.sceneTree, xdgPopup.base)
            val popup = Popup(xdgPopup, sceneTree)
            parent.addChild(popup)
            popups.add(popup)
            return

        }

        // Parent is a Popup
        findPopupByXdgSurface(parentSurface)?.let { parent ->
            val sceneTree = SceneTree.createFromParent(parent.sceneTree, xdgPopup.base)
            val popup = Popup(xdgPopup, sceneTree)
            parent.addChild(popup)
            popups.add(popup)
            return
        }

        error("BUG: Can't have a XdgPopup without a parent")

        // Find the parent's SceneTree. In wlroots parent of an XdgPopup can be either XdgToplevel or another
        // XdgPopup.
//        val parentsSceneTree =
//            findWindowByXdgSurface(parentSurface)?.sceneTree
//                ?: findPopupByXdgSurface(parentSurface)?.sceneTree
//                ?: error("BUG: Can't have a XdgPopup without a parent")

        // Create a SceneTree for this popup in order for the wlroots to display it
//        val sceneTree = SceneTree.createFromParent(parentsSceneTree, xdgPopup.base)

//        val popup: Popup
//        if (val blabal = findWindowByXdgSurface(parentSurface)) {
//            pare
//        }
//        popup = Popup(xdgPopup, sceneTree)
//        popups.add(popup)
    }


    fun removeWindow(window: Window) {
        windows.remove(window)
        focuser.unfocusWindow(window)
    }


    fun findWindowAtCoordinates(x: Double, y: Double): UnderCursor? {
        // First, find a scene node under the cursor, then walk its parents upwards until you reach a
        // top level window.
        val (sceneNode, nx, ny) = compositor.scene.tree.node.nodeAt(x, y)
            ?: return null

        if (sceneNode.type != SceneNode.Type.Buffer)
            return null

        val sceneBuffer = SceneBuffer.fromNode(sceneNode)
        val sceneSurface = SceneSurface.tryFromBuffer(sceneBuffer) ?: return null

        // TODO: Extract into method
//        for (sceneTree in sceneNode.parentIterator)
//            for ((toplevel, toplevelSceneTree) in toplevelSceneTree)
//                if (sceneTree == toplevelSceneTree)
//                    return UnderCursor(toplevel, sceneSurface.surface(), nx, ny)

        for (sceneTree in sceneNode.parentIterator) {
           for (window in windows) {
               if (sceneTree == window.sceneTree)
                   return UnderCursor(window, window.xdgToplevel, sceneSurface.surface(), nx, ny)
           }
        }

        unreachable()
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
        val op = MoveAndResize(window.xdgToplevel, window.sceneTree.node, configureSerial, x, y, w, h)

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
}