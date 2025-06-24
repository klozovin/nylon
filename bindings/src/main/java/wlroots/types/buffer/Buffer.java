package wlroots.types.buffer;

import jextract.wlroots.types.wlr_buffer_impl;
import jextract.wlroots.wlr_buffer;
import org.jspecify.annotations.NullMarked;
import wlroots.render.DmabufAttributes;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.EnumSet;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_buffer_h.*;


/// A buffer containing pixel data.
///
/// A buffer has a single producer (the party who created the buffer) and multiple consumers
/// (parties reading the buffer). When all consumers are done with the buffer, it gets
/// released and can be re-used by the producer. When the producer and all consumers are done
/// with the buffer, it gets destroyed.
///
/// ```struct wlr_buffer {};```
@NullMarked
public class Buffer {
    public MemorySegment bufferPtr;


    public Buffer(MemorySegment bufferPtr) {
        assert !bufferPtr.equals(NULL);
        this.bufferPtr = bufferPtr;
    }


    public void finish() {
        // TODO: This function appears wlroots 0.19 (finish v. drop?)
//        wlr_buffer_finish(bufferPtr)
        System.out.println("!!! WARNING !!! wlr_buffer.finish() not implemented in wlroots 0.18 !!!");
    }


    /// Unreference the buffer. This function should be called by producers when they are done with the buffer.
    public void drop() {
        wlr_buffer_drop(bufferPtr);
    }


    /// ```c
    ///
    /// struct wlr_buffer_impl {
    /// 	void (*destroy)(struct wlr_buffer *buffer);
    /// 	bool (*get_dmabuf)(struct wlr_buffer *buffer, struct wlr_dmabuf_attributes *attribs);
    /// 	bool (*get_shm)(struct wlr_buffer *buffer, struct wlr_shm_attributes *attribs);
    /// 	bool (*begin_data_ptr_access)(struct wlr_buffer *buffer, uint32_t flags, void **data, uint32_t *format, size_t *stride);
    /// 	void (*end_data_ptr_access)(struct wlr_buffer *buffer);
    ///};
    ///```
    @NullMarked
    public abstract static class Impl extends Buffer {
        MemorySegment implPtr;


        public Impl(Arena arena) {
            this(wlr_buffer.allocate(arena), wlr_buffer_impl.allocate(arena));
        }


        public Impl(MemorySegment bufferPtr, MemorySegment implPtr) {
            super(bufferPtr);
            assert !implPtr.equals(NULL);
            this.implPtr = implPtr;

            // TODO: Memory lifetime: Use confined here instead of global? -- nope??
            wlr_buffer_impl.destroy(implPtr, wlr_buffer_impl.destroy.allocate(
                (MemorySegment wlrBufferPtr) -> {
                    assert wlrBufferPtr.equals(this.bufferPtr);
                    this.destroy();
                },
                Arena.global()
            ));

            if (!(this instanceof DataSource dataSource))
                return;

            switch (dataSource) {

                // Buffer is in RAM memory
                case DataSource.Memory buf -> {
                    wlr_buffer_impl.begin_data_ptr_access(implPtr, wlr_buffer_impl.begin_data_ptr_access.allocate(
                        (MemorySegment wlrBufferPtr, int flags, MemorySegment data, MemorySegment format, MemorySegment stride) -> {
                            assert wlrBufferPtr.equals(this.bufferPtr);
                            var flagsSet = AccessFlag.setFromBitmask(flags);
                            var result = buf.beginDataAccess(flagsSet);
                            // TODO: When resul.result false, should we return without setting the out parameters?
                            data.set(C_POINTER, 0, result.data);
                            format.set(C_INT, 0, result.format);
                            stride.set(C_INT, 0, result.stride);
                            return result.result;
                        },
                        Arena.global()
                    ));

                    wlr_buffer_impl.end_data_ptr_access(implPtr, wlr_buffer_impl.end_data_ptr_access.allocate(
                        (MemorySegment wlrBufferPtr) -> {
                            assert wlrBufferPtr.equals(this.bufferPtr);
                            buf.endDataAccess();
                        },
                        Arena.global()
                    ));
                }

                // Buffer is Linux DMA-BUF pixel buffer
                case DataSource.Dmabuf buf -> {
                    wlr_buffer_impl.get_dmabuf(implPtr, wlr_buffer_impl.get_dmabuf.allocate(
                        (MemorySegment wlrBufferPtr, MemorySegment attribs) -> {
                            assert wlrBufferPtr.equals(bufferPtr);
                            return buf.getDmabuf(new DmabufAttributes(attribs));
                        },
                        Arena.global()
                    ));
                }

                // Buffer is in shared memory
                case DataSource.Shm buf -> {
                    wlr_buffer_impl.get_shm(implPtr, wlr_buffer_impl.get_shm.allocate(
                        (MemorySegment wlrBufferPtr, MemorySegment attribs) -> {
                            assert wlrBufferPtr.equals(bufferPtr);
                            return buf.getShm(new ShmAttributes(attribs));
                        },
                        Arena.global()
                    ));
                }
            }
        }


        public void init(int width, int height) {
            wlr_buffer_init(bufferPtr, implPtr, width, height);
        }


        public abstract void destroy();
    }

    /// In C, wlr_buffer_impl struct can actually implement three different interfaces (memory, shm, dmabuf). To pick
    /// an interface, implement those callbacks, leave others unassigned. To make it more idiomatic in Java, there are
    /// separeate interfaces for those.
    public sealed interface DataSource {

        non-sealed interface Memory extends DataSource {
            MemoryBufferFormat beginDataAccess(EnumSet<AccessFlag> flags);

            void endDataAccess();
        }

        non-sealed interface Shm extends DataSource {
            boolean getShm(ShmAttributes attributes);
        }

        non-sealed interface Dmabuf extends DataSource {
            boolean getDmabuf(DmabufAttributes attributes);
        }
    }


    /// Describe how the data is laid out for a buffer that keeps it's data in RAM memory. Used only in
    /// {@link DataSource.Memory} interface.
    public record MemoryBufferFormat(
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


        AccessFlag(int idx) {
            this.idx = idx;
        }


        public static AccessFlag of(int value) {
            if (value == WLR_BUFFER_DATA_PTR_ACCESS_READ())  return READ;
            if (value == WLR_BUFFER_DATA_PTR_ACCESS_WRITE()) return WRITE;

            throw new RuntimeException("Invalid enum value from C code for wlr_buffer_data_ptr_access_flag");
        }


        public static EnumSet<AccessFlag> setFromBitmask(int flags) {
            var flagsSet = EnumSet.noneOf(AccessFlag.class);
            for (var e: values()) {
                if ((flags & e.idx) != 0)
                    flagsSet.add(e);
            }
            return flagsSet;
        }
    }
}