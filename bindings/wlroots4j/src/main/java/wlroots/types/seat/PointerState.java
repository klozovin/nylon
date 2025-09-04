package wlroots.types.seat;

import jextract.wlroots.types.wlr_seat_pointer_state;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wlroots.types.compositor.Surface;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class PointerState {
    public final MemorySegment pointerStatePtr;


    public PointerState(MemorySegment pointerStatePtr) {
        assert !pointerStatePtr.equals(NULL);
        this.pointerStatePtr = pointerStatePtr;
    }


    // *** Getters and setters **************************************************************************** //


    public @Nullable SeatClient getFocusedClient() {
        var ptr = wlr_seat_pointer_state.focused_client(pointerStatePtr);
        return !ptr.equals(NULL) ? new SeatClient(ptr) : null;
    }


    // TODO: Is this nullable?
    public @Nullable Surface getFocusedSurface() {
        return Surface.ofPtrOrNull(wlr_seat_pointer_state.focused_surface(pointerStatePtr));
    }
}