package wlroots.types.seat;

import jextract.wlroots.types.wlr_seat_pointer_request_set_cursor_event;
import org.jspecify.annotations.NullMarked;
import wlroots.types.compositor.Surface;

import java.lang.foreign.MemorySegment;


@NullMarked
public class PointerRequestSetCursorEvent {
    private final MemorySegment pointerRequestSetCursorEventPtr;


    public PointerRequestSetCursorEvent(MemorySegment pointerRequestSetCursorEventPtr) {
        this.pointerRequestSetCursorEventPtr = pointerRequestSetCursorEventPtr;
    }


    public SeatClient seatClient() {
        return new SeatClient(wlr_seat_pointer_request_set_cursor_event.seat_client(pointerRequestSetCursorEventPtr));
    }


    public Surface surface() {
        return new Surface(wlr_seat_pointer_request_set_cursor_event.surface(pointerRequestSetCursorEventPtr));
    }

    public int hotspotX() {
        return wlr_seat_pointer_request_set_cursor_event.hotspot_x(pointerRequestSetCursorEventPtr);
    }

    public int hotspotY() {
        return wlr_seat_pointer_request_set_cursor_event.hotspot_y(pointerRequestSetCursorEventPtr);
    }
}