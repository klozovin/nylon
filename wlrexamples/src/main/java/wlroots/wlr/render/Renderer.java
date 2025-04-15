package wlroots.wlr.render;

import org.jspecify.annotations.NullMarked;
import wayland.server.Display;
import wlroots.wlr.Backend;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.backend_h.wlr_renderer_autocreate;
import static jextract.wlroots.backend_h.wlr_renderer_init_wl_display;


@NullMarked
public class Renderer {
    public final MemorySegment rendererPtr;


    private Renderer(MemorySegment rendererPtr) {
        assert !rendererPtr.equals(NULL);
        this.rendererPtr = rendererPtr;
    }


    public static Renderer autocreate(Backend backend) {
        return new Renderer(wlr_renderer_autocreate(backend.backendPtr));
    }


    /// Initializes wl_shm, linux-dmabuf and other buffer factory protocols.
    ///
    /// @return false on failure
    public boolean initWlDisplay(Display display) {
        return wlr_renderer_init_wl_display(rendererPtr, display.displayPtr);
    }
}