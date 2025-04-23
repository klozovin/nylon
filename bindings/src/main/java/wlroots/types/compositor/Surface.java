package wlroots.types.compositor;

import jextract.wlroots.types.wlr_surface;
import org.jspecify.annotations.NullMarked;
import wayland.server.Signal;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


/// `struct wlr_surface {};`
@NullMarked
public class Surface {
    public final MemorySegment surfacePtr;
    public final Events events;


    public Surface(MemorySegment surfacePtr) {
        assert !surfacePtr.equals(NULL);
        this.surfacePtr = surfacePtr;
        this.events = new Events(wlr_surface.events(surfacePtr));
    }


    /// Cntains the current, committed surface state.
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
        public final Signal<Void> commit;

        public final Signal<Void> destroy;


        public Events(MemorySegment eventsPtr) {
            this.eventsPtr = eventsPtr;
            this.commit = new Signal<>(wlr_surface.events.commit(eventsPtr));
            this.destroy = new Signal<>(wlr_surface.events.destroy(eventsPtr));
        }
    }
}