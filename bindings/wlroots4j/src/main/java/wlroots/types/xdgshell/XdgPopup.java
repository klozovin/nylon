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
    public final Events        events;


    public XdgPopup(MemorySegment ptr) {
        assert !ptr.equals(NULL);
        xdgPopupPtr = ptr;
        events      = new Events(wlr_xdg_popup.events(ptr));
    }


    @Override
    public boolean equals(Object other) {
        return switch (other) {
            case XdgPopup otherPopup -> xdgPopupPtr.equals(otherPopup.xdgPopupPtr);
            case null -> false;
            default -> throw new RuntimeException("BUG: Trying to compare objects of different types");
        };
    }


    @Override
    public int hashCode() {
        return xdgPopupPtr.hashCode();
    }


    // *** Getters and setters **************************************************************************** //


    public XdgSurface getBase() {
        return new XdgSurface(wlr_xdg_popup.base(xdgPopupPtr));
    }


    public Surface getParent() {
        return new Surface(wlr_xdg_popup.parent(xdgPopupPtr));
    }


    // *** Events ***************************************************************************************** //


    public static class Events {
        public final Signal0 destroy;


        public Events(MemorySegment ptr) {
            destroy = Signal.of(wlr_xdg_popup.events.destroy(ptr));
        }


        public Signal[] allSignals() {
            return new Signal[]{destroy};
        }
    }
}