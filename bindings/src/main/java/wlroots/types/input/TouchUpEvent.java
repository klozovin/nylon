package wlroots.types.input;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;


@NullMarked
public class TouchUpEvent {
    public final MemorySegment touchUpEventPtr;


    public TouchUpEvent(MemorySegment touchUpEventPtr) {
        assert !touchUpEventPtr.equals(MemorySegment.NULL);
        this.touchUpEventPtr = touchUpEventPtr;
    }
}