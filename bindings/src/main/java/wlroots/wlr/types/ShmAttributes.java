package wlroots.wlr.types;


import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;


@NullMarked
public class ShmAttributes {
    MemorySegment shmAttributesPtr;


    public ShmAttributes(MemorySegment shmAttributesPtr) {
        this.shmAttributesPtr = shmAttributesPtr;
    }
}