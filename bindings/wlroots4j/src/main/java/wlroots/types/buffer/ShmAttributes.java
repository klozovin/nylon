package wlroots.types.buffer;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


/// Shared-memory attributes for a buffer.
@NullMarked
public class ShmAttributes {
    MemorySegment shmAttributesPtr;


    public ShmAttributes(MemorySegment shmAttributesPtr) {
        assert !shmAttributesPtr.equals(NULL);
        this.shmAttributesPtr = shmAttributesPtr;
    }
}