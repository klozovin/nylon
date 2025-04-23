package wlroots.types.compositor;

import jextract.wlroots.types.wlr_surface;
import org.jspecify.annotations.NullMarked;
import wayland.server.Signal;
import wayland.server.Signal.Signal1;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


/// `struct wlr_surface {}`
@NullMarked
public class Surface {
    public final MemorySegment surfacePtr;
    public final Events events;


    public Surface(MemorySegment surfacePtr) {
        assert !surfacePtr.equals(NULL);
        this.surfacePtr = surfacePtr;
        this.events = new Events(wlr_surface.events(surfacePtr));
    }


    /// Contains the current, committed surface state.
    public SurfaceState current() {
        return new SurfaceState(wlr_surface.current(surfacePtr));
    }


    /// Accumulates state changes from the client between commits and shouldn't be accessed by the
    /// compositor directly.
    public SurfaceState pending() {
        return new SurfaceState(wlr_surface.pending(surfacePtr));
    }


    public static class Events {
        public final MemorySegment eventsPtr;

        /// Signals that a commit has been applied. The new state can be accessed in {@link #current()}.
        public final Signal1<Surface> commit;

        /// Signals that the surface is being destroyed.
        public final Signal1<Surface> destroy;


        public Events(MemorySegment eventsPtr) {
            this.eventsPtr = eventsPtr;
            this.commit = Signal.of(wlr_surface.events.commit(eventsPtr), Surface::new);
            this.destroy = Signal.of(wlr_surface.events.destroy(eventsPtr), Surface::new);
        }
    }
}