package wlroots.types.seat;

import jextract.wlroots.wlr_seat_pointer_focus_change_event;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wlroots.types.compositor.Surface;

import java.lang.foreign.MemorySegment;


@NullMarked
public record PointerFocusChangeEvent(
    Seat seat,
    Surface oldSurface,
    @Nullable Surface newSurface,
    double sx,
    double sy
) {
    public PointerFocusChangeEvent(MemorySegment ptr) {
        this(
            new Seat(wlr_seat_pointer_focus_change_event.seat(ptr)),

            Surface.ofPtr(wlr_seat_pointer_focus_change_event.old_surface(ptr)),
            Surface.ofPtrOrNull(wlr_seat_pointer_focus_change_event.new_surface(ptr)),

            wlr_seat_pointer_focus_change_event.sx(ptr),
            wlr_seat_pointer_focus_change_event.sy(ptr)
        );
    }
}