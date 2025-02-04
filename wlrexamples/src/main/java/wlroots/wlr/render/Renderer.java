package wlroots.wlr.render;

import wlroots.backend_h;
import wlroots.wlr.Backend;

import java.lang.foreign.MemorySegment;

public class Renderer {

    public final MemorySegment rendererPtr;

    private Renderer(MemorySegment rendererPtr) {
        this.rendererPtr = rendererPtr;
    }

    public static Renderer autocreate(Backend backend) {
        return new Renderer(backend_h.wlr_renderer_autocreate(backend.backendPtr));
    }
}