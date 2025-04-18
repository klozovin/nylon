package wlroots.wlr.render;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


/// A Linux DMA-BUF pixel buffer.
@NullMarked
public class DmabufAttributes {
    MemorySegment dmabufAttributesPtr;


    public DmabufAttributes(MemorySegment dmabufAttributesPtr) {
        assert !dmabufAttributesPtr.equals(NULL);
        this.dmabufAttributesPtr = dmabufAttributesPtr;
    }
}