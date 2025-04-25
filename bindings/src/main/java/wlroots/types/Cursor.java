package wlroots.types;

import jextract.wlroots.types.wlr_cursor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.Signal;
import wayland.server.Signal.Signal1;
import wlroots.types.output.OutputLayout;
import wlroots.types.pointer.PointerAxisEvent;
import wlroots.types.pointer.PointerButtonEvent;
import wlroots.types.pointer.PointerMotionAbsoluteEvent;
import wlroots.types.pointer.PointerMotionEvent;
import wlroots.types.tablet.TabletToolAxisEvent;
import wlroots.types.touch.TouchCancelEvent;
import wlroots.types.touch.TouchDownEvent;
import wlroots.types.touch.TouchMotionEvent;
import wlroots.types.touch.TouchUpEvent;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_cursor_h.*;


@NullMarked
public class Cursor {
    public final MemorySegment cursorPtr;
    public final Events events;


    public Cursor(MemorySegment cursorPtr) {
        this.cursorPtr = cursorPtr;
        this.events = new Events(wlr_cursor.events(cursorPtr));
    }


    public static Cursor create() {
        return new Cursor(wlr_cursor_create());
    }


    /// Uses the given layout to establish the boundaries and movement semantics of this cursor.
    /// Cursors without an output layout allow infinite movement in any direction and do not
    /// support absolute input events.
    public void attachOutputLayout(OutputLayout layout) {
        wlr_cursor_attach_output_layout(cursorPtr, layout.outputLayoutPtr);
    }


    /// Attaches this input device to this cursor. The input device must be one of:{@link InputDevice.Type#POINTER},
    ///  {@link InputDevice.Type#TOUCH}, {@link InputDevice.Type#TABLET}
    public void attachInputDevice(InputDevice device) {
        wlr_cursor_attach_input_device(cursorPtr, device.inputDevicePtr);
    }


    /// Move the cursor in the direction of the given x and y layout coordinates. If one
    /// coordinate is NAN, it will be ignored.
    ///
    /// @param device May be passed to respect device mapping constraints. If NULL, device mapping constraints will be ignored.
    public void move(@Nullable InputDevice device, double deltaX, double deltaY) {
        wlr_cursor_move(
            cursorPtr,
            switch (device) {
                case InputDevice d -> d.inputDevicePtr;
                case null -> NULL;
            },
            deltaX,
            deltaY
        );
    }


    /// Warp the cursor to the given x and y in absolute 0..1 coordinates. If the given point is
    /// out of the layout boundaries or constraints, the closest point will be used. If one
    /// coordinate is NAN, it will be ignored.
    ///
    /// @param device May be passed to respect device mapping constraints, if NULL, device mapping constraints will be ignored.
    public void warpAbsolute(InputDevice device,  double x, double y) {
        wlr_cursor_warp_absolute(cursorPtr, device.inputDevicePtr, x, y);
    }


    /// Set the cursor image from an XCursor theme.
    ///
    /// The image will be loaded from the {@link XcursorManager}.
    public void setXcursor(XcursorManager manager, String name) {
        try (var arena = Arena.ofConfined()) {
            wlr_cursor_set_xcursor(cursorPtr, manager.xcursorManagerPtr, arena.allocateFrom(name));
        }
    }


    public void destroy() {
        wlr_cursor_destroy(cursorPtr);
    }


    public static class Events {
        final MemorySegment eventsPtr;

        public final Signal1<PointerMotionEvent> motion;
        public final Signal1<PointerMotionAbsoluteEvent> motionAbsolute;
        public final Signal1<PointerButtonEvent> button;
        public final Signal1<PointerAxisEvent> axis;

        public final Signal1<TouchUpEvent> touchUp;
        public final Signal1<TouchDownEvent> touchDown;
        public final Signal1<TouchMotionEvent> touchMotion;
        public final Signal1<TouchCancelEvent> touchCancel;

        public final Signal1<TabletToolAxisEvent> tabletToolAxis;


        Events(MemorySegment eventsPtr) {
            this.eventsPtr = eventsPtr;

            this.motion         = Signal.of(wlr_cursor.events.motion(eventsPtr),            PointerMotionEvent::new);
            this.motionAbsolute = Signal.of(wlr_cursor.events.motion_absolute(eventsPtr),   PointerMotionAbsoluteEvent::new);
            this.button         = Signal.of(wlr_cursor.events.button(eventsPtr),            PointerButtonEvent::new);
            this.axis           = Signal.of(wlr_cursor.events.axis(eventsPtr),              PointerAxisEvent::new);

            this.touchUp        = Signal.of(wlr_cursor.events.touch_up(eventsPtr),          TouchUpEvent::new);
            this.touchDown      = Signal.of(wlr_cursor.events.touch_down(eventsPtr),        TouchDownEvent::new);
            this.touchMotion    = Signal.of(wlr_cursor.events.touch_motion(eventsPtr),      TouchMotionEvent::new);
            this.touchCancel    = Signal.of(wlr_cursor.events.touch_cancel(eventsPtr),      TouchCancelEvent::new);

            this.tabletToolAxis = Signal.of(wlr_cursor.events.tablet_tool_axis(eventsPtr),  TabletToolAxisEvent::new);
        }
    }
}