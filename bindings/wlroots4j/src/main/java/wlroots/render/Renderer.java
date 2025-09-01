package wlroots.render;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.Display;
import wlroots.backend.Backend;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.backend_h.*;


/// A renderer for basic 2D operations.
@NullMarked
public class Renderer {
    public final MemorySegment rendererPtr;


    private Renderer(MemorySegment rendererPtr) {
        assert !rendererPtr.equals(NULL);
        this.rendererPtr = rendererPtr;
    }


    /// Automatically initializes the most suitable backend given the environment. Will always return a
    /// multi-backend. The backend is created but not started.
    ///
    /// @return null on failure.
    public static @Nullable Renderer autocreate(Backend backend) {
        var rendererPtr = wlr_renderer_autocreate(backend.backendPtr);
        return !rendererPtr.equals(NULL) ? new Renderer(rendererPtr) : null;
    }


    /// Initializes wl_shm, linux-dmabuf and other buffer factory protocols.
    ///
    /// @return false on failure
    public boolean initWlDisplay(Display display) {
        return wlr_renderer_init_wl_display(rendererPtr, display.displayPtr);
    }


    /// Destroys the renderer.
    ///
    /// Textures must be destroyed separately.
    public void destroy() {
        wlr_renderer_destroy(rendererPtr);
    }
}