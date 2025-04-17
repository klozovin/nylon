package wlroots.wlr.render;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class BufferPassOptions {
    public final MemorySegment bufferPassOptionsPtr;


    public BufferPassOptions(MemorySegment bufferPassOptionsPtr) {
        assert !bufferPassOptionsPtr.equals(NULL);
        this.bufferPassOptionsPtr = bufferPassOptionsPtr;
    }
}