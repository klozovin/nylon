package compositor.windows

import compositor.unreachable
import compositor.windows.WindowSystem.BaseWindow
import wayland.server.Listener
import wlroots.types.compositor.Surface
import wlroots.types.scene.SceneTree
import wlroots.types.xdg_shell.XdgPopup


class Popup(val windows: WindowSystem, val parent: BaseWindow, val xdgPopup: XdgPopup) : BaseWindow {

    override val sceneTree: SceneTree
    val listeners: MutableList<Listener>
    val childPopups: MutableList<Popup> = mutableListOf()

    // Debugging
    var isDestroyed = false


    init {
        sceneTree = SceneTree.createFromParent(parent.sceneTree, xdgPopup.base)

        listeners = mutableListOf(
            xdgPopup.base.surface.events.commit.add(::onCommit),
            xdgPopup.events.destroy.add(::onDestroy)
        )
    }


    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Popup -> xdgPopup == other.xdgPopup
            else -> false
        }
    }


    override fun hashCode(): Int {
        TODO("Don't use yet")
        return xdgPopup.hashCode()
    }


    override fun addChild(child: BaseWindow) {
        when(child) {
            is Window -> unreachable()
            is Popup -> {
                require(!childPopups.contains(child))
                require(!child.isDestroyed)
                childPopups.add(child)
            }
        }
    }

    override fun removeChild(child: BaseWindow) {
        when(child) {
            is Window -> unreachable()
            is Popup -> {
                val removed = childPopups.remove(child)
                check(removed)
            }
        }
    }


    //
    // *** Signal handlers
    //

    fun onCommit(surface: Surface) {
        if (xdgPopup.base.initialCommit) {
            xdgPopup.base.scheduleConfigure()
        }
    }

    fun onDestroy() {
        listeners.forEach { it.remove() }
        parent.removeChild(this)
        windows.removePopup(this)
        isDestroyed = true

        // TODO Free listeners arena here
    }
}