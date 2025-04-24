package wlroots.types.pointer;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class PointerButtonEvent {
    public final MemorySegment pointerButtonEventPtr;


    public PointerButtonEvent(MemorySegment pointerButtonEventPtr) {
        assert !pointerButtonEventPtr.equals(NULL);
        this.pointerButtonEventPtr = pointerButtonEventPtr;
    }
}