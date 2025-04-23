package wlroots.render;

import wlroots.backend.Backend;

import java.lang.foreign.MemorySegment;

import static jextract.wlroots.render.allocator_h.wlr_allocator_autocreate;


/// An allocator is responsible for allocating memory for pixel buffers.
public class Allocator {
    public final MemorySegment allocatorPtr;

    private Allocator(MemorySegment allocatorPtr) {
        this.allocatorPtr = allocatorPtr;
    }

    public static Allocator autocreate(Backend backend, Renderer renderer) {
        var allocatorPtr = wlr_allocator_autocreate(backend.backendPtr, renderer.rendererPtr);
        return new Allocator(allocatorPtr);
    }
}