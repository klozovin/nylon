package wlroots.wlr.types;

import jextract.wlroots.types.wlr_buffer_impl;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_buffer_impl.*;


/// ```c
/// struct wlr_buffer_impl {
/// 	void (*destroy)(struct wlr_buffer *buffer);
/// 	bool (*get_dmabuf)(struct wlr_buffer *buffer, struct wlr_dmabuf_attributes *attribs);
/// 	bool (*get_shm)(struct wlr_buffer *buffer, struct wlr_shm_attributes *attribs);
/// 	bool (*begin_data_ptr_access)(struct wlr_buffer *buffer, uint32_t flags, void **data, uint32_t *format, size_t *stride);
/// 	void (*end_data_ptr_access)(struct wlr_buffer *buffer);
///};
///```
@NullMarked
public class BufferImpl {
    public final MemorySegment bufferImplPtr;


    public BufferImpl(MemorySegment bufferImplPtr) {
        assert !bufferImplPtr.equals(NULL);
        this.bufferImplPtr = bufferImplPtr;
    }


    static public BufferImpl allocate(Arena arena) {
        return new BufferImpl(wlr_buffer_impl.allocate(arena));
    }


    /*** Struct setters ***/

    public void destroy(destroy.Function method) {
        // TODO: Memory lifetime
        wlr_buffer_impl.destroy(bufferImplPtr, destroy.allocate(method, Arena.global()));
    }


    public void get_dmabuf(get_dmabuf.Function method) {
        // TODO: Memory lifetime
        wlr_buffer_impl.get_dmabuf(bufferImplPtr, get_dmabuf.allocate(method, Arena.global()));
    }


    public void get_shm(get_shm.Function method) {
        // TODO: Memory lifetime
        wlr_buffer_impl.get_shm(bufferImplPtr, get_shm.allocate(method, Arena.global()));
    }


    public void begin_data_ptr_access(begin_data_ptr_access.Function method) {
        // TODO: Memory lifetime
        wlr_buffer_impl.begin_data_ptr_access(bufferImplPtr, begin_data_ptr_access.allocate(method, Arena.global()));
    }


    public void end_data_ptr_access(end_data_ptr_access.Function method) {
        // TODO: Memory lifetime
        wlr_buffer_impl.end_data_ptr_access(bufferImplPtr, end_data_ptr_access.allocate(method, Arena.global()));
    }


    @NullMarked
    class BufferDataAccess {
        int format;
        MemorySegment data;
        int stride;


        public BufferDataAccess(int format, MemorySegment data, int stride) {
            this.format = format;
            this.data = data;
            this.stride = stride;
        }
    }

}