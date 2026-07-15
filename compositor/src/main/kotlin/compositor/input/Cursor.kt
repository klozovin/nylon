package compositor.input

import compositor.Compositor
import wayland.server.Listener
import wlroots.types.pointer.PointerAxisEvent
import wlroots.types.pointer.PointerButtonEvent
import wlroots.types.pointer.PointerMotionAbsoluteEvent
import wlroots.types.pointer.PointerMotionEvent
import wlroots.types.cursor.Cursor as WlrCursor


class Cursor(val compositor: Compositor) {

    val listeners: MutableList<Listener> = mutableListOf()

    val wlrCursor = WlrCursor.create().apply {
        attachOutputLayout(compositor.outputSystem.outputLayout)
        with(events) {
            listeners.addAll(
                arrayOf(
                    motion.add(::onCursorMotion),
                    motionAbsolute.add(::onCursorMotionAbsolute),
                    button.add(::onCursorButton),
                    axis.add(::onCursorAxis),
                    frame.add(::onCursorFrame)
                )
            )
        }
    }


    fun destroy() {
        listeners.forEach { it.remove() }
        wlrCursor.destroy()
    }


    fun onCursorMotion(event: PointerMotionEvent) {
        wlrCursor.move(event.pointer.base, event.deltaX, event.deltaY)
        compositor.captureMode.onCursorMotion(event.timeMsec)
    }


    fun onCursorMotionAbsolute(event: PointerMotionAbsoluteEvent) {
        wlrCursor.warpAbsolute(event.pointer.base, event.x, event.y)
        compositor.captureMode.onCursorMotion(event.timeMsec)
    }


    fun onCursorButton(event: PointerButtonEvent) {
        compositor.captureMode.onCursorButton(event)
    }


    fun onCursorAxis(event: PointerAxisEvent) {
        compositor.seat.pointerNotifyAxis(event)
    }

    fun onCursorFrame(cursor: WlrCursor) {
        // TODO: Swallow this when moving windows
        compositor.seat.pointerNotifyFrame()
    }

    //
    // Helpers
    //

    fun setIcon(name: String) {
        compositor.inputSystem.cursor.wlrCursor.setXcursor(compositor.xcursorManager, name)
    }
}
