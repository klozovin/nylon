package wlroots.types.pointer;

import jextract.wlroots.types.wlr_pointer_axis_event;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class PointerAxisEvent {
    public final MemorySegment pointerAxisEventPtr;

    public double delta;


    public PointerAxisEvent(MemorySegment pointerAxisEventPtr) {
        assert !pointerAxisEventPtr.equals(NULL);
        this.pointerAxisEventPtr = pointerAxisEventPtr;

        this.delta = wlr_pointer_axis_event.delta(pointerAxisEventPtr);
    }
}