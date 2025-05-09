package wlroots.types.seat;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


// TODO: Rename to just Client?
@NullMarked
public class SeatClient {
    public final MemorySegment seatClientPtr;


    public SeatClient(MemorySegment seatClientPtr) {
        assert !seatClientPtr.equals(NULL);
        this.seatClientPtr = seatClientPtr;
    }
}