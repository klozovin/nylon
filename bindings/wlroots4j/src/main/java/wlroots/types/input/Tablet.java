package wlroots.types.input;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class Tablet {
    final MemorySegment tabletPtr;


    public Tablet(MemorySegment tabletPtr) {
        assert !tabletPtr.equals(NULL);
        this.tabletPtr = tabletPtr;
    }
}