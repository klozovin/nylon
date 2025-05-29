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
    public final MemorySegment pointerAxisEventPtr;

    public Pointer pointer;
    public int timeMsec;

    public PointerAxisSource source;
    public PointerAxis orientation;
    public PointerAxisRelativeDirection relativeDirection;

    public double delta;
    public int deltaDiscrete;


    public PointerAxisEvent(MemorySegment pointerAxisEventPtr) {
        assert !pointerAxisEventPtr.equals(NULL);
        this.pointerAxisEventPtr = pointerAxisEventPtr;

        this.pointer = new Pointer(wlr_pointer_axis_event.pointer(pointerAxisEventPtr));
        this.timeMsec = wlr_pointer_axis_event.time_msec(pointerAxisEventPtr);

        this.source = PointerAxisSource.of(wlr_pointer_axis_event.source(pointerAxisEventPtr));
        this.orientation = PointerAxis.of(wlr_pointer_axis_event.orientation(pointerAxisEventPtr));
        this.relativeDirection = PointerAxisRelativeDirection.of(wlr_pointer_axis_event.relative_direction(pointerAxisEventPtr));

        this.delta = wlr_pointer_axis_event.delta(pointerAxisEventPtr);
        this.deltaDiscrete = wlr_pointer_axis_event.delta_discrete(pointerAxisEventPtr);
    }
}