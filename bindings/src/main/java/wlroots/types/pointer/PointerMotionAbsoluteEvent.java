package wlroots.types.pointer;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class PointerMotionAbsoluteEvent {
    public final MemorySegment pointerMotionAbsoluteEventPtr;


    public PointerMotionAbsoluteEvent(MemorySegment pointerMotionAbsoluteEventPtr) {
        assert !pointerMotionAbsoluteEventPtr.equals(NULL);
        this.pointerMotionAbsoluteEventPtr = pointerMotionAbsoluteEventPtr;
    }
}