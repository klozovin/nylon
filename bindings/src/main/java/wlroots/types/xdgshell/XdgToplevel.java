package wlroots.types.xdgshell;

import jdk.jfr.Event;
import jextract.wlroots.types.wlr_xdg_toplevel;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.Signal;
import wayland.server.Signal.Signal0;
import wlroots.types.compositor.Surface;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_xdg_shell_h.wlr_xdg_toplevel_set_size;
import static jextract.wlroots.types.wlr_xdg_shell_h.wlr_xdg_toplevel_try_from_wlr_surface;


@NullMarked
public class XdgToplevel {
    public final MemorySegment xdgToplevelPtr;
    public final Events events;


    public XdgToplevel(MemorySegment xdgToplevelPtr) {
        assert !xdgToplevelPtr.equals(NULL);
        this.xdgToplevelPtr = xdgToplevelPtr;
        this.events = new Events(wlr_xdg_toplevel.events(xdgToplevelPtr));
    }


    public XdgSurface base() {
        return new XdgSurface(wlr_xdg_toplevel.base(xdgToplevelPtr));
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


    /// Get a {@link XdgToplevel} from a {@link Surface}.
    ///
    /// Returns NULL if the surface doesn't have the xdg_surface role, the xdg_surface is not a
    /// toplevel, or the xdg_surface/xdg_toplevel objects have been destroyed.
    public static @Nullable XdgToplevel tryFromSurface(Surface surface) {
        var xdgToplevelPtr = wlr_xdg_toplevel_try_from_wlr_surface(surface.surfacePtr);
        return !xdgToplevelPtr.equals(NULL) ? new XdgToplevel(xdgToplevelPtr) : null;
    }


    public int setSize(int width, int height) {
        return wlr_xdg_toplevel_set_size(xdgToplevelPtr, width, height);
    }


    public static class Events {
        public final Signal0 requestMove;
        public final Signal0 requestResize;
        public final Signal0 requestMaximize;
        public final Signal0 requestFullscreen;


        public Events(MemorySegment eventsPtr) {
            this.requestMove = Signal.of(wlr_xdg_toplevel.events.request_move(eventsPtr));
            this.requestResize = Signal.of(wlr_xdg_toplevel.events.request_resize(eventsPtr));
            this.requestMaximize = Signal.of(wlr_xdg_toplevel.events.request_maximize(eventsPtr));
            this.requestFullscreen = Signal.of(wlr_xdg_toplevel.events.request_fullscreen(eventsPtr));
        }
    }
}