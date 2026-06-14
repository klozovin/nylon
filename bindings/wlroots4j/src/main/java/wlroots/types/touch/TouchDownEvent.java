package wlroots.types.touch;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;


@NullMarked
public class TouchDownEvent {
    public final MemorySegment touchDownEvent;


    public TouchDownEvent(MemorySegment touchDownEvent) {
        assert !touchDownEvent.equals(MemorySegment.NULL);
        this.touchDownEvent = touchDownEvent;
    }
}