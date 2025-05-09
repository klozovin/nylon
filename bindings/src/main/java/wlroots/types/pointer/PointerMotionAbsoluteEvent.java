package wlroots.types.pointer;

import jextract.wlroots.types.wlr_pointer_motion_absolute_event;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class PointerMotionAbsoluteEvent {
    public final MemorySegment pointerMotionAbsoluteEventPtr;

    public Pointer pointer;
    public int timeMsec;
    public double x;
    public double y;


    public PointerMotionAbsoluteEvent(MemorySegment pointerMotionAbsoluteEventPtr) {
        assert !pointerMotionAbsoluteEventPtr.equals(NULL);
        this.pointerMotionAbsoluteEventPtr = pointerMotionAbsoluteEventPtr;

        this.pointer = new Pointer(wlr_pointer_motion_absolute_event.pointer(pointerMotionAbsoluteEventPtr));
        this.timeMsec = wlr_pointer_motion_absolute_event.time_msec(pointerMotionAbsoluteEventPtr);
        this.x = wlr_pointer_motion_absolute_event.x(pointerMotionAbsoluteEventPtr);
        this.y = wlr_pointer_motion_absolute_event.y(pointerMotionAbsoluteEventPtr);
    }
}