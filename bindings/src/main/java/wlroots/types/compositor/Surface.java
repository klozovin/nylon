package wlroots.types.compositor;

import jextract.wlroots.types.wlr_surface;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.Signal;
import wayland.server.Signal.Signal0;
import wayland.server.Signal.Signal1;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_compositor_h.wlr_surface_get_root_surface;


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


    @Override
    public boolean equals(Object other) {
        return switch (other) {
            case Surface otherSurface -> surfacePtr.equals(otherSurface.surfacePtr);
            case null -> false;
            default -> throw new RuntimeException("BUG: Trying to compare objects of different types");
        };
    }


    public static @Nullable Surface ofPtrOrNull(MemorySegment ptr) {
        return !ptr.equals(NULL) ? new Surface(ptr) : null;
    }


    // *** Fields ***************************************************************************************** //


    /// Contains the current, committed surface state.
    public SurfaceState current() {
        return new SurfaceState(wlr_surface.current(surfacePtr));
    }


    /// Is the surface ready to be displayed?
    public boolean mapped() {
        return wlr_surface.mapped(surfacePtr);
    }


    /// Accumulates state changes from the client between commits and shouldn't be accessed by the compositor
    /// directly.
    public SurfaceState pending() {
        return new SurfaceState(wlr_surface.pending(surfacePtr));
    }


    public void lockPending() {
        throw new RuntimeException("Not implemented");
    }


    // *** Methods **************************************************************************************** //


    /// Get the root of the subsurface tree for this surface. May return the same surface passed if that
    /// surface is the root. Never returns NULL.
    public Surface getRootSurface() {
        return new Surface(wlr_surface_get_root_surface(surfacePtr));
    }


    // *** Events ***************************************************************************************** //


    public static class Events {
        /// Raised when the client has sent a wl_surface.commit request. The state to committed can be
        /// accessed in {@link Surface#pending()}.
        ///
        /// The commit may not be applied immediately, in which case it's marked as "cached" and put into a
        /// queue. See {@link Surface#lockPending()}
        public final Signal0 clientCommit;

        /// Signals that a commit has been applied. The new state can be accessed in {@link #current()}.
        public final Signal1<Surface> commit;

        ///  Raised when the surface has a non-null buffer committed and is ready to be displayed.
        public final Signal0 map;

        /// Raised when the surface shouldn't be displayed anymore. This can happen when a null buffer is
        /// committed, the associated role object is destroyed, or when the role-specific conditions for the
        /// surface to be mapped no longer apply.
        public final Signal0 unmap;

//        public final Signal1<Subsurface> newSubsurface;

        /// Raised when the surface is being destroyed.
        public final Signal1<Surface> destroy;


        public Events(MemorySegment ptr) {
            clientCommit = Signal.of(wlr_surface.events.client_commit(ptr));
            commit       = Signal.of(wlr_surface.events.commit(ptr), Surface::new);
            map          = Signal.of(wlr_surface.events.map(ptr));
            unmap        = Signal.of(wlr_surface.events.unmap(ptr));
            destroy      = Signal.of(wlr_surface.events.destroy(ptr), Surface::new);
        }


        public Signal[] allSignals() {
            return new Signal[]{clientCommit, commit, map, unmap, destroy};
        }
    }
}