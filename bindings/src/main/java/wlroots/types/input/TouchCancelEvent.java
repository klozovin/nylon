package wlroots.types.input;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class TouchCancelEvent {
    public final MemorySegment touchCancelEventPtr;


    public TouchCancelEvent(MemorySegment touchCancelEventPtr) {
        assert !touchCancelEventPtr.equals(NULL);
        this.touchCancelEventPtr = touchCancelEventPtr;
    }
}