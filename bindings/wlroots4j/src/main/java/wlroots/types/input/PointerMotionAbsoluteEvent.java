package wlroots.types.input;

import jextract.wlroots.types.wlr_pointer_motion_absolute_event;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class PointerMotionAbsoluteEvent {
    public final Pointer pointer;
    public final int timeMsec;
    public final double x;
    public final double y;


    public PointerMotionAbsoluteEvent(MemorySegment pointerMotionAbsoluteEventPtr) {
        assert !pointerMotionAbsoluteEventPtr.equals(NULL);
        pointer  = new Pointer(wlr_pointer_motion_absolute_event.pointer(pointerMotionAbsoluteEventPtr));
        timeMsec = wlr_pointer_motion_absolute_event.time_msec(pointerMotionAbsoluteEventPtr);
        x        = wlr_pointer_motion_absolute_event.x(pointerMotionAbsoluteEventPtr);
        y        = wlr_pointer_motion_absolute_event.y(pointerMotionAbsoluteEventPtr);
    }
}