package wlroots.types.xdgshell;

import jextract.wlroots.types.wlr_xdg_surface;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.Signal;
import wayland.server.Signal.Signal0;
import wlroots.types.compositor.Surface;
import wlroots.util.Box;

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


    @Override
    public boolean equals(Object obj) {
        return switch (obj) {
            case XdgSurface s -> xdgSurfacePtr.equals(s.xdgSurfacePtr);
            case null -> false;
            default -> throw new RuntimeException("BUG: Trying to compare objects of different types");
        };
    }


    // *** Getters and setters **************************************************************************** //


    public Surface getSurface() {
        return new Surface(wlr_xdg_surface.surface(xdgSurfacePtr));
    }


    public SurfaceRole role() {
        return SurfaceRole.of(wlr_xdg_surface.role(xdgSurfacePtr));
    }


    public boolean configured() {
        return wlr_xdg_surface.configured(xdgSurfacePtr);
    }


    public int scheduledSerial() {
        return wlr_xdg_surface.scheduled_serial(xdgSurfacePtr);
    }


    public boolean getInitialized() {
        return wlr_xdg_surface.initialized(xdgSurfacePtr);
    }


    /// Whether the latest commit is an initial commit.
    public boolean getInitialCommit() {
        return wlr_xdg_surface.initial_commit(xdgSurfacePtr);
    }


    ///  Surface geometry
    public Box getGeometry() {
        return new Box(wlr_xdg_surface.geometry(xdgSurfacePtr));
    }


    // *** Methods **************************************************************************************** //


    /// Schedule a surface configuration. This should only be called by protocols extending the shell.
    public int scheduleConfigure() {
        return wlr_xdg_surface_schedule_configure(xdgSurfacePtr);
    }


    // *** Associated *** //


    public enum SurfaceRole {
        NONE(WLR_XDG_SURFACE_ROLE_NONE()),
        TOPLEVEL(WLR_XDG_SURFACE_ROLE_TOPLEVEL()),
        POPUP(WLR_XDG_SURFACE_ROLE_POPUP());


        public final int value;


        SurfaceRole(int value) {
            this.value = value;
        }

        public static SurfaceRole of(int value) {
            if (value == WLR_XDG_SURFACE_ROLE_NONE())     return NONE;
            if (value == WLR_XDG_SURFACE_ROLE_TOPLEVEL()) return TOPLEVEL;
            if (value == WLR_XDG_SURFACE_ROLE_POPUP())    return POPUP;

            throw new RuntimeException("Invalid enum value from C code for wlr_xdg_surface_role");
        }
    }


    // *** Events ***************************************************************************************** //


    public static class Events {
        public final Signal0 destroy;


        public Events(MemorySegment eventsPtr) {
            this.destroy = Signal.of(wlr_xdg_surface.events.destroy(eventsPtr));
        }
    }
}