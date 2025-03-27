package wlroots.wlr.render;

import wlroots.wlr.Backend;

import java.lang.foreign.MemorySegment;

import static jexwlroots.backend_h.wlr_renderer_autocreate;


public class Renderer {

    public final MemorySegment rendererPtr;

    private Renderer(MemorySegment rendererPtr) {
        this.rendererPtr = rendererPtr;
    }

    public static Renderer autocreate(Backend backend) {
        return new Renderer(wlr_renderer_autocreate(backend.backendPtr));
    }
}