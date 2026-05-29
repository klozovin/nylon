package wlroots.types.seat;

import jextract.wlroots.wlr_seat_pointer_button;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


/// `struct wlr_seat_pointer_button`
@NullMarked
public record PointerButton(int button, long nPressed) {

    public PointerButton(MemorySegment pointerButtonPtr) {
        assert !pointerButtonPtr.equals(NULL);
        this(
            wlr_seat_pointer_button.button(pointerButtonPtr),
            wlr_seat_pointer_button.n_pressed(pointerButtonPtr)
        );
    }
}