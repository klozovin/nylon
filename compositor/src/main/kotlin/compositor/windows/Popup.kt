package compositor.windows

import wayland.server.Listener
import wlroots.types.compositor.Surface
import wlroots.types.scene.SceneTree
import wlroots.types.xdg_shell.XdgPopup


class Popup(val xdgPopup: XdgPopup, val sceneTree: SceneTree) {

    val listeners: MutableList<Listener>
    val childPopups: MutableList<Popup> = mutableListOf()


    init {
        listeners = mutableListOf(
            xdgPopup.base.surface.events.commit.add(::onCommit),
            xdgPopup.events.destroy.add(::onDestroy)
        )
    }


    /**
     * Two Popups are equal when their XdgPopups are equal
     */
    override fun equals(other: Any?): Boolean {
        return when(other) {
            is Popup -> xdgPopup == other.xdgPopup
            else -> false
        }
    }


    override fun hashCode(): Int {
        TODO()
        var result = xdgPopup.hashCode()
        result = 31 * result + sceneTree.hashCode()
        result = 31 * result + listeners.hashCode()
        result = 31 * result + childPopups.hashCode()
        return result
    }


    fun addChild(child: Popup) {
        require(!childPopups.contains(child))
        childPopups.add(child)
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
        // Remove listeners
        listeners.forEach { it.remove() }
        // TODO

        // TODO Free listeners arena here
    }
}