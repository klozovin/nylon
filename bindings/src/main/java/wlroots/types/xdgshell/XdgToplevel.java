package wlroots.types.xdgshell;

import jextract.wlroots.types.wlr_xdg_toplevel;
import jextract.wlroots.types.wlr_xdg_toplevel_move_event;
import jextract.wlroots.types.wlr_xdg_toplevel_resize_event;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.Signal;
import wayland.server.Signal.Signal0;
import wayland.server.Signal.Signal1;
import wayland.util.Edge;
import wlroots.types.compositor.Surface;
import wlroots.types.seat.SeatClient;

import java.lang.foreign.MemorySegment;
import java.util.EnumSet;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_xdg_shell_h.*;


@NullMarked
public class XdgToplevel {
    public final MemorySegment xdgToplevelPtr;
    public final Events        events;


    public XdgToplevel(MemorySegment ptr) {
        assert !ptr.equals(NULL);

        xdgToplevelPtr = ptr;
        events         = new Events(wlr_xdg_toplevel.events(ptr));
    }


    @Override
    public boolean equals(Object other) {
        return switch (other) {
            case XdgToplevel otherToplevel -> xdgToplevelPtr.equals(otherToplevel.xdgToplevelPtr);
            case null -> false;
            default -> throw new RuntimeException("BUG: Trying to compare objects of different types");
        };
    }


    @Override
    public int hashCode() {
        return xdgToplevelPtr.hashCode();
    }


    public static @Nullable XdgToplevel ofPtrOrNull(MemorySegment ptr) {
        return !ptr.equals(NULL) ? new XdgToplevel(ptr) : null;
    }


    /// Get a {@link XdgToplevel} from a {@link Surface}.
    ///
    /// Returns NULL if the surface doesn't have the xdg_surface role, the xdg_surface is not a toplevel, or
    /// the xdg_surface/xdg_toplevel objects have been destroyed.
    public static @Nullable XdgToplevel tryFromSurface(Surface surface) {
        var xdgToplevelPtr = wlr_xdg_toplevel_try_from_wlr_surface(surface.surfacePtr);
        return !xdgToplevelPtr.equals(NULL) ? new XdgToplevel(xdgToplevelPtr) : null;
    }

    // *** Getters and setters **************************************************************************** //


    public XdgSurface base() {
        return new XdgSurface(wlr_xdg_toplevel.base(xdgToplevelPtr));
    }


    public @Nullable XdgToplevel parent() {
        return XdgToplevel.ofPtrOrNull(wlr_xdg_toplevel.parent(xdgToplevelPtr));
    }


    public String title() {
        // TODO: Is this the best way to handle it? Maybe return null?
        var titlePtr = wlr_xdg_toplevel.title(xdgToplevelPtr);
        return !titlePtr.equals(NULL) ? titlePtr.getString(0) : "";
    }


    public String appId() {
        // TODO: Is this the best way to handle it?
        var appIdPtr = wlr_xdg_toplevel.app_id(xdgToplevelPtr);
        return !appIdPtr.equals(NULL) ? appIdPtr.getString(0) : "";
    }

    // *** Methods **************************************************************************************** //


    public int setSize(int width, int height) {
        return wlr_xdg_toplevel_set_size(xdgToplevelPtr, width, height);
    }


    /// Request that this toplevel show itself in an activated or deactivated state.
    ///
    /// @return Associated configure serial
    public int setActivated(boolean activated) {
        return wlr_xdg_toplevel_set_activated(xdgToplevelPtr, activated);
    }


    // *** Events ***************************************************************************************** //


    public static class Events {
        public final Signal0 destroy;
        public final Signal1<MoveEvent> requestMove;
        public final Signal1<ResizeEvent> requestResize;
        public final Signal0 requestMaximize;
        public final Signal0 requestFullscreen;


        public Events(MemorySegment eventsPtr) {
            this.destroy           = Signal.of(wlr_xdg_toplevel.events.destroy(eventsPtr));
            this.requestMove       = Signal.of(wlr_xdg_toplevel.events.request_move(eventsPtr), MoveEvent::new);
            this.requestResize     = Signal.of(wlr_xdg_toplevel.events.request_resize(eventsPtr), ResizeEvent::new);
            this.requestMaximize   = Signal.of(wlr_xdg_toplevel.events.request_maximize(eventsPtr));
            this.requestFullscreen = Signal.of(wlr_xdg_toplevel.events.request_fullscreen(eventsPtr));
        }
    }


    /// `struct wlr_xdg_toplevel_move_event {}`
    public static class MoveEvent {
        public final XdgToplevel toplevel;
        public final SeatClient  seat;
        public final int         serial;


        public MoveEvent(MemorySegment ptr) {
            toplevel = new XdgToplevel(wlr_xdg_toplevel_move_event.toplevel(ptr));
            seat     = new SeatClient(wlr_xdg_toplevel_move_event.seat(ptr));
            serial   = wlr_xdg_toplevel_move_event.serial(ptr);
        }
    }


    /// `struct wlr_xdg_toplevel_resize_event {}`
    public static class ResizeEvent {
        public final XdgToplevel   toplevel;
        public final SeatClient    seat;
        public final int           serial;
        public final EnumSet<Edge> edges;


        public ResizeEvent(MemorySegment ptr) {
            toplevel = new XdgToplevel(wlr_xdg_toplevel_resize_event.toplevel(ptr));
            seat     = new SeatClient(wlr_xdg_toplevel_resize_event.seat(ptr));
            serial   = wlr_xdg_toplevel_resize_event.serial(ptr);
            edges    = Edge.fromBitset(wlr_xdg_toplevel_resize_event.edges(ptr));
        }
    }
}