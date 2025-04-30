package wlroots.types.seat;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;


@NullMarked
public class PointerRequestSetCursorEvent {
    private final MemorySegment pointerRequestSetCursorEventPtr;


    public PointerRequestSetCursorEvent(MemorySegment pointerRequestSetCursorEventPtr) {
        this.pointerRequestSetCursorEventPtr = pointerRequestSetCursorEventPtr;
    }
}