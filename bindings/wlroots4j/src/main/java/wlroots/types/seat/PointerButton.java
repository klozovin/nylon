package wlroots.types.seat;

import jextract.wlroots.types.wlr_seat_pointer_button;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public record PointerButton(int button, long nPressed) {

    public PointerButton(MemorySegment pointerButtonPtr) {
        assert !pointerButtonPtr.equals(NULL);
        this(wlr_seat_pointer_button.button(pointerButtonPtr), wlr_seat_pointer_button.n_pressed(pointerButtonPtr));
    }
}