package compositor.windows

import compositor.Compositor

class Focuser(val compositor: Compositor) {

    private var current: Window? = null


    fun getFocused(): Window? {
        return current
    }


    /// Focus the provided window, if anything was in focus before it loses it.
    fun focusWindow(window: Window) {
        require(!window.isDestroyed)

        // Don't refocus windows, just sends them duplicate events.
        if (this.current == window) return

        clearFocus()

        this.current = window
        window.sceneTree.raiseToTop()
        window.xdgToplevel.setActivated(true)
        compositor.seat.keyboard?.let { keyboard ->
            compositor.seat.keyboardNotifyEnter(
                window.xdgToplevel.base.surface,
                keyboard.keycodesPtr,
                keyboard.numKeycodes,
                keyboard.modifiers
            )
        }
    }


    /// Unfocus the window given. If the window given doesn't have focus, don't do anything. Just a
    /// convenience function so I don't have to check if window is focused, then clearing focus.
    fun unfocusWindow(window: Window) {
        if (this.current != window) return
        clearFocus()
    }


    /// Make sure that nothing is focused.
    fun clearFocus() {
        current?.let { focusedWindow ->
            focusedWindow.xdgToplevel.setActivated(false) // TODO Maybe do this only if !.isDestroyed?
            compositor.seat.keyboardNotifyClearFocus()
            current = null
        }
    }


    // TODO
    fun focusNextWindow() {
        // In order of last focus
//        if (toplevels.isNotEmpty()) {
//            val nextIdx = focusedToplevel + 1
//        }
    }
}