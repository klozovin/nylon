package wlroots.wlr.types;

import jextract.wlroots.wlr_buffer;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_buffer_h.wlr_buffer_init;
import static jextract.wlroots.types.wlr_buffer_h_1.wlr_buffer_drop;


/// A buffer containing pixel data.
///
/// A buffer has a single producer (the party who created the buffer) and multiple consumers
/// (parties reading the buffer). When all consumers are done with the buffer, it gets
/// released and can be re-used by the producer. When the producer and all consumers are done
/// with the buffer, it gets destroyed.
@NullMarked
public class Buffer {
    MemorySegment bufferPtr;


    public Buffer(MemorySegment bufferPtr) {
        assert !bufferPtr.equals(NULL);
        this.bufferPtr = bufferPtr;
    }


    static public Buffer allocate(Arena arena) {
        return new Buffer(wlr_buffer.allocate(arena));
    }


    /// Initialize a buffer. This function should be called by producers. The initialized buffer
    /// is referenced: once the producer is done with the buffer they should call {@link #drop()}.
    public void init(BufferImpl impl, int width, int height) {
        wlr_buffer_init(bufferPtr, impl.bufferImplPtr, width, height);
    }


    public void finish() {
        // TODO: This function appears wlroots 0.19
//        wlr_buffer_finish(bufferPtr)
    }


    /// Unreference the buffer. This function should be called by producers when they are done with the buffer.
    public void drop() {
        wlr_buffer_drop(bufferPtr);
    }
}