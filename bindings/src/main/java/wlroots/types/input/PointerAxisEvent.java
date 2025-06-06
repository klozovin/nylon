package wlroots.types.input;

import jextract.wlroots.types.wlr_pointer_axis_event;
import org.jspecify.annotations.NullMarked;
import wayland.PointerAxis;
import wayland.PointerAxisRelativeDirection;
import wayland.PointerAxisSource;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class PointerAxisEvent {
    public Pointer pointer;
    public int     timeMsec;

    public PointerAxisSource            source;
    public PointerAxis                  orientation;
    public PointerAxisRelativeDirection relativeDirection;

    public double delta;
    public int    deltaDiscrete;


    public PointerAxisEvent(MemorySegment ptr) {
        assert !ptr.equals(NULL);

        pointer  = new Pointer(wlr_pointer_axis_event.pointer(ptr));
        timeMsec = wlr_pointer_axis_event.time_msec(ptr);

        source            = PointerAxisSource.of(wlr_pointer_axis_event.source(ptr));
        orientation       = PointerAxis.of(wlr_pointer_axis_event.orientation(ptr));
        relativeDirection = PointerAxisRelativeDirection.of(wlr_pointer_axis_event.relative_direction(ptr));

        delta         = wlr_pointer_axis_event.delta(ptr);
        deltaDiscrete = wlr_pointer_axis_event.delta_discrete(ptr);
    }
}