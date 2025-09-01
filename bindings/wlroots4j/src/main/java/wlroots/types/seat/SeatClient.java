package wlroots.types.seat;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


/// Contains state for a single client's bound wl_seat resource and can be used to issue input events to that
/// client. The lifetime of these objects is managed by struct wlr_seat; some may be NULL.
@NullMarked
public class SeatClient {
    public final MemorySegment seatClientPtr;


    public SeatClient(MemorySegment ptr) {
        assert !ptr.equals(NULL);
        seatClientPtr = ptr;
    }


    @Override
    public boolean equals(Object other) {
        return switch (other) {
            case SeatClient otherSeatClient -> seatClientPtr.equals(otherSeatClient.seatClientPtr);
            case null -> false;
            default -> throw new RuntimeException("BUG: Trying to compare objects of different types");
        };
    }
}