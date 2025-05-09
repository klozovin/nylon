package wlroots.types.seat;


import jextract.wlroots.types.wlr_seat_keyboard_state;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wlroots.types.compositor.Surface;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class KeyboardState {
    public final MemorySegment keyboardStatePtr;


    public KeyboardState(MemorySegment keyboardStatePtr) {
        assert !keyboardStatePtr.equals(NULL);
        this.keyboardStatePtr = keyboardStatePtr;
    }


    public @Nullable Surface focusedSurface() {
        var surfacePtr = wlr_seat_keyboard_state.focused_surface(keyboardStatePtr);
        return !surfacePtr.equals(NULL) ? new Surface(surfacePtr) : null;
    }
}