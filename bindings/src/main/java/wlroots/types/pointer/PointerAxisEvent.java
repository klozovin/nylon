package wlroots.types.pointer;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class PointerAxisEvent {
    public final MemorySegment pointerAxisEventPtr;


    public PointerAxisEvent(MemorySegment pointerAxisEventPtr) {
        assert !pointerAxisEventPtr.equals(NULL);
        this.pointerAxisEventPtr = pointerAxisEventPtr;
    }
}