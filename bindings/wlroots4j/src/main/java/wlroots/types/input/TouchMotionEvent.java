package wlroots.types.input;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class TouchMotionEvent {
    public final MemorySegment touchMotionEventPtr;


    public TouchMotionEvent(MemorySegment touchMotionEventPtr) {
        assert !touchMotionEventPtr.equals(NULL);
        this.touchMotionEventPtr = touchMotionEventPtr;
    }
}