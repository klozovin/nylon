package wlroots.wlr.render;

import wlroots.wlr.Backend;

import java.lang.foreign.MemorySegment;

import static jexwlroots.render.allocator_h.wlr_allocator_autocreate;


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