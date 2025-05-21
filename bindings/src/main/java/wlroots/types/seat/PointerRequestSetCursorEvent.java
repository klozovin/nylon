package wlroots.types.seat;

import jextract.wlroots.types.wlr_seat_pointer_request_set_cursor_event;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wlroots.types.compositor.Surface;

import java.lang.foreign.MemorySegment;


@NullMarked
public class PointerRequestSetCursorEvent {
    public final SeatClient seatClient;
    public final @Nullable Surface surface;
    public final int hotspotX;
    public final int hotspotY;


    public PointerRequestSetCursorEvent(MemorySegment ptr) {
        this.seatClient = new SeatClient(wlr_seat_pointer_request_set_cursor_event.seat_client(ptr));
        this.surface = Surface.ofPtrOrNull(wlr_seat_pointer_request_set_cursor_event.surface(ptr));
        this.hotspotX = wlr_seat_pointer_request_set_cursor_event.hotspot_x(ptr);
        this.hotspotY = wlr_seat_pointer_request_set_cursor_event.hotspot_y(ptr);
    }
}