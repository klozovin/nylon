package wlroots.types.input;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class TabletToolAxisEvent {
    public final MemorySegment tabletToolAxisEventPtr;


    public TabletToolAxisEvent(MemorySegment tabletToolAxisEventPtr) {
        assert !tabletToolAxisEventPtr.equals(NULL);
        this.tabletToolAxisEventPtr = tabletToolAxisEventPtr;
    }
}