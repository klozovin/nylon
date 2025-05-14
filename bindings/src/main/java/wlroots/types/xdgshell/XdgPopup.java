package wlroots.types.xdgshell;

import jextract.wlroots.types.wlr_xdg_popup;
import org.jspecify.annotations.NullMarked;
import wayland.server.Signal;
import wayland.server.Signal.Signal0;
import wlroots.types.compositor.Surface;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class XdgPopup {
    public final MemorySegment xdgPopupPtr;
    public final Events events;


    public XdgPopup(MemorySegment xdgPopupPtr) {
        assert !xdgPopupPtr.equals(NULL);
        this.xdgPopupPtr = xdgPopupPtr;
        this.events = new Events(wlr_xdg_popup.events(xdgPopupPtr));
    }


    // *** Getters and setters *** //

    public XdgSurface base() {
        return new XdgSurface(wlr_xdg_popup.base(xdgPopupPtr));
    }


    public Surface parent() {
        return new Surface(wlr_xdg_popup.parent(xdgPopupPtr));
    }


    // *** Events *** //


    public static class Events {
        public final Signal0 destroy;

        public Events(MemorySegment eventsPtr) {
            this.destroy = Signal.of(wlr_xdg_popup.events.destroy(eventsPtr));
        }
    }
}