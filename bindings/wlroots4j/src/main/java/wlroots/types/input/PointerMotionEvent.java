package wlroots.types.input;

import jextract.wlroots.types.wlr_pointer_motion_event;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class PointerMotionEvent {
    public final Pointer pointer;
    public final double deltaX;
    public final double deltaY;
    public final int timeMsec;


    public PointerMotionEvent(MemorySegment ptr) {
        assert !ptr.equals(NULL);
        pointer  = new Pointer(wlr_pointer_motion_event.pointer(ptr));
        deltaX   = wlr_pointer_motion_event.delta_x(ptr);
        deltaY   = wlr_pointer_motion_event.delta_y(ptr);
        timeMsec = wlr_pointer_motion_event.time_msec(ptr);
    }
}