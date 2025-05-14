package wlroots.types.xdgshell;

import jextract.wlroots.types.wlr_xdg_surface;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.Signal;
import wayland.server.Signal.Signal0;
import wlroots.types.compositor.Surface;
import wlroots.util.Box;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_xdg_shell_h.*;


/// An xdg-surface is a user interface element requiring management by the compositor. An xdg-surface alone
/// isn't useful, a role should be assigned to it in order to map it.
@NullMarked
public class XdgSurface {
    public final MemorySegment xdgSurfacePtr;
    public final Events events;


    public XdgSurface(MemorySegment xdgSurfacePtr) {
        assert !xdgSurfacePtr.equals(NULL);
        this.xdgSurfacePtr = xdgSurfacePtr;
        this.events = new Events(wlr_xdg_surface.events(xdgSurfacePtr));
    }


    /// @return NULL if the surface doesn't have the xdg_surface role or if the xdg_surface has been
    ///         destroyed.
    public static @Nullable XdgSurface tryFromSurface(Surface surface) {
        var ptr = wlr_xdg_surface_try_from_wlr_surface(surface.surfacePtr);
        return !ptr.equals(NULL) ? new XdgSurface(ptr) : null;
    }

    // *** Getters and setters **************************************************************************** //


    public Surface surface() {
        return new Surface(wlr_xdg_surface.surface(xdgSurfacePtr));
    }


    public boolean configured() {
        return wlr_xdg_surface.configured(xdgSurfacePtr);
    }


    public int scheduledSerial() {
        return wlr_xdg_surface.scheduled_serial(xdgSurfacePtr);
    }


    public boolean initialized() {
        return wlr_xdg_surface.initialized(xdgSurfacePtr);
    }


    /// Whether the latest commit is an initial commit.
    public boolean initialCommit() {
        return wlr_xdg_surface.initial_commit(xdgSurfacePtr);
    }


    // *** Methods **************************************************************************************** //


    /// Get the surface geometry.
    ///
    /// This is either the geometry as set by the client, or defaulted to the bounds of the surface + the
    /// subsurfaces (as specified by the protocol). The x and y value can be < 0.
    public Box getGeometry() {
        // TODO: Memory management: This could work as auto?
        var box = Box.allocate(Arena.global());
        wlr_xdg_surface_get_geometry(xdgSurfacePtr, box.boxPtr);
        return box;
    }


    /// Schedule a surface configuration. This should only be called by protocols extending the shell.
    public int scheduleConfigure() {
        return wlr_xdg_surface_schedule_configure(xdgSurfacePtr);
    }


    public static class Events {
        public final Signal0 destroy;


        public Events(MemorySegment eventsPtr) {
            this.destroy = Signal.of(wlr_xdg_surface.events.destroy(eventsPtr));
        }
    }
}