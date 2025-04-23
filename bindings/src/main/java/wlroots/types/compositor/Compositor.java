package wlroots.types.compositor;

import jextract.wlroots.types.wlr_compositor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.Display;
import wayland.server.Signal;
import wayland.server.Signal.Signal0;
import wayland.server.Signal.Signal1;
import wlroots.render.Renderer;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_compositor_h.wlr_compositor_create;


@NullMarked
public class Compositor {
    public final MemorySegment compositorPtr;
    public final Events events;


    public Compositor(MemorySegment compositorPtr) {
        assert !compositorPtr.equals(NULL);
        this.compositorPtr = compositorPtr;
        this.events = new Events(wlr_compositor.events(compositorPtr));
    }


    /// Create the wl_compositor global, which can be used by clients to create surfaces and
    /// regions.
    ///
    /// If a renderer is supplied, the compositor will create struct wlr_texture objects from
    /// client buffers on surface commit.
    public static Compositor create(Display display, int version, @Nullable Renderer renderer) {
        return new Compositor(wlr_compositor_create(
            display.displayPtr,
            version,
            switch (renderer) {
                case Renderer r -> r.rendererPtr;
                case null -> NULL;
            }
        ));
    }


    public final static class Events {
        public final MemorySegment eventsPtr;
        public final Signal1<Surface> newSurface;
        public final Signal0 destroy;


        public Events(MemorySegment eventsPtr) {
            assert !eventsPtr.equals(NULL);
            this.eventsPtr = eventsPtr;
            this.newSurface = Signal.of(wlr_compositor.events.new_surface(eventsPtr), Surface::new);
            this.destroy = Signal.of(wlr_compositor.events.destroy(eventsPtr));
        }
    }
}