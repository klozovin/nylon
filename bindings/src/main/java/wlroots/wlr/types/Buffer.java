package wlroots.wlr.types;

import jextract.wlroots.types.wlr_buffer_impl;
import jextract.wlroots.wlr_buffer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.EnumSet;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_buffer_h.*;

/*
use one class to model the base wlr_buffer, and possible wlr_buffer_impl classes
there's no need to have separate bufferimpl java class

expected use is to derive from buffer, not use it directly (in java code)
 */


/// A buffer containing pixel data.
///
/// A buffer has a single producer (the party who created the buffer) and multiple consumers
/// (parties reading the buffer). When all consumers are done with the buffer, it gets
/// released and can be re-used by the producer. When the producer and all consumers are done
/// with the buffer, it gets destroyed.
@NullMarked
public class Buffer {
    public MemorySegment bufferPtr;
    public @Nullable MemorySegment bufferImplPtr = null;


    public Buffer(MemorySegment bufferPtr) {
        assert !bufferPtr.equals(NULL);
        this.bufferPtr = bufferPtr;
    }


    /// Allocate wlr_buffer, wlr_buffer_impl
    public Buffer(Arena arena) {
        // TODO: Memory management - maybe everything can be confined?
        this(wlr_buffer.allocate(arena), wlr_buffer_impl.allocate(arena));

        wlr_buffer_impl.destroy(
            bufferImplPtr,
            wlr_buffer_impl.destroy.allocate(
                (MemorySegment wlrBufferPtr) -> {
                    assert wlrBufferPtr.equals(this.bufferPtr);
                    this.implDestroy();
                }, arena
            ));


        wlr_buffer_impl.begin_data_ptr_access(
            bufferImplPtr,
            wlr_buffer_impl.begin_data_ptr_access.allocate(
                (MemorySegment wlrBufferPtr, int flags, MemorySegment data, MemorySegment format, MemorySegment stride) -> {
                    assert wlrBufferPtr.equals(this.bufferPtr);
                    // TODO: BUG: Flags are enum, bug passed around as bitfields!!! use enumset
                    // TODO: Move this to the access flags enum
                    var flagsSet = EnumSet.noneOf(AccessFlag.class);
                    if ((flags & AccessFlag.READ.idx) != 0) flagsSet.add(AccessFlag.READ);
                    if ((flags & AccessFlag.WRITE.idx) != 0) flagsSet.add(AccessFlag.WRITE);

                    var result = this.implBeginDataAccess(flagsSet);
                    data.set(C_POINTER, 0, result.data);
                    format.set(C_INT, 0, result.format);
                    stride.set(C_INT, 0, result.stride);
                    return result.result;
                },
                arena
            ));

        wlr_buffer_impl.end_data_ptr_access(
            bufferImplPtr,
            wlr_buffer_impl.end_data_ptr_access.allocate(
                (MemorySegment wlrBufferPtr) -> {
                    assert wlrBufferPtr.equals(this.bufferPtr);
                    this.implEndDataAccess();
                },
                arena
            )
        );

        // TODO: Implement get_dmabuf, get_shm functions
    }


    public Buffer(MemorySegment bufferPtr, MemorySegment bufferImplPtr) {
        assert !bufferPtr.equals(NULL);
        assert !bufferImplPtr.equals(NULL);
        this.bufferPtr = bufferPtr;
        this.bufferImplPtr = bufferImplPtr;
    }


    static public Buffer allocate(Arena arena) {
        return new Buffer(wlr_buffer.allocate(arena));
    }


    /// Initialize a buffer. This function should be called by producers. The initialized buffer
    /// is referenced: once the producer is done with the buffer they should call {@link #drop()}.
    @Deprecated
    public void init(BufferImpl impl, int width, int height) {
        wlr_buffer_init(bufferPtr, impl.bufferImplPtr, width, height);
    }


    ///  Only makes sense to call when deriving this class
    public void init(int width, int height) {
        assert bufferImplPtr != null;
        wlr_buffer_init(bufferPtr, bufferImplPtr, width, height);
    }


    public void finish() {
        // TODO: This function appears wlroots 0.19
//        wlr_buffer_finish(bufferPtr)
        System.out.println("!!! WARNING !!! wlr_buffer.finish() not implemented in wlroots 0.18 !!!");
    }


    /// Unreference the buffer. This function should be called by producers when they are done with the buffer.
    public void drop() {
        wlr_buffer_drop(bufferPtr);
    }


    /*** BufferImpl: Methods to override ***/


    public BufferDataFormat implBeginDataAccess(EnumSet<AccessFlag> flags) {
        throw new RuntimeException("Override me");
    }


    public boolean implGetDmaBuf() {
        throw new RuntimeException("Override me");
    }


    public boolean implGetShm() {
        throw new RuntimeException("Override me");
    }


    public void implEndDataAccess() {
        throw new RuntimeException("Override me");
    }


    public void implDestroy() {
        throw new RuntimeException("Override me");
    }


    public record BufferDataFormat(
        boolean result,
        int format,
        MemorySegment data,
        int stride
    ) {
    }


    public enum AccessFlag {
        READ(WLR_BUFFER_DATA_PTR_ACCESS_READ()),
        WRITE(WLR_BUFFER_DATA_PTR_ACCESS_WRITE());

        public final int idx;


        public static AccessFlag of(int idx) {
            for (var e : values()) {
                if (e.idx == idx)
                    return e;
            }
            throw new RuntimeException("Invalid enum value from C code");
        }


        AccessFlag(int idx) {
            this.idx = idx;
        }
    }
}