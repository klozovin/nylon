package wlroots.types.pointer;

import jextract.wlroots.types.wlr_pointer_motion_event;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class PointerMotionEvent {
    public final MemorySegment pointerMotionEventPtr;

    public final Pointer pointer;
    public final double deltaX;
    public final double deltaY;


    public PointerMotionEvent(MemorySegment pointerMotionEventPtr) {
        assert !pointerMotionEventPtr.equals(NULL);
        this.pointerMotionEventPtr = pointerMotionEventPtr;

        this.pointer = new Pointer(wlr_pointer_motion_event.pointer(pointerMotionEventPtr));
        this.deltaX = wlr_pointer_motion_event.delta_x(pointerMotionEventPtr);
        this.deltaY = wlr_pointer_motion_event.delta_y(pointerMotionEventPtr);
    }
}