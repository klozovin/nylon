package wlroots.render;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wlroots.backend.Backend;

import java.lang.foreign.MemorySegment;

import static jextract.wlroots.render.allocator_h.wlr_allocator_autocreate;
import static jextract.wlroots.render.allocator_h.wlr_allocator_destroy;


/// An allocator is responsible for allocating memory for pixel buffers.
@NullMarked
public class Allocator {
    public final MemorySegment allocatorPtr;


    private Allocator(MemorySegment allocatorPtr) {
        this.allocatorPtr = allocatorPtr;
    }


    public static @Nullable Allocator autocreate(Backend backend, Renderer renderer) {
        var allocatorPtr = wlr_allocator_autocreate(backend.backendPtr, renderer.rendererPtr);
        return !allocatorPtr.equals(MemorySegment.NULL) ? new Allocator(allocatorPtr) : null;
    }


    // Destroy the allocator.
    public void destroy() {
        wlr_allocator_destroy(allocatorPtr);
    }
}