package wlroots.types.xdg_shell;

import jextract.wlroots.wlr_xdg_popup;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.Signal;
import wayland.server.Signal.Signal0;
import wayland.server.Signal.Signal1;
import wlroots.types.compositor.Surface;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class XdgPopup {
    public final MemorySegment xdgPopupPtr;
    public final Events events;


    public XdgPopup(MemorySegment ptr) {
        assert !ptr.equals(NULL);
        xdgPopupPtr = ptr;
        events = new Events(wlr_xdg_popup.events(ptr));
    }


    @Override
    public boolean equals(@Nullable Object other) {
        return switch (other) {
            case null -> false;
            case XdgPopup otherPopup -> xdgPopupPtr.equals(otherPopup.xdgPopupPtr);
            default -> throw new RuntimeException("BUG: Trying to compare objects of different types");
        };
    }


    @Override
    public int hashCode() {
        return xdgPopupPtr.hashCode();
    }


    //
    // *** Getters and setters ***
    //


    public XdgSurface getBase() {
        return new XdgSurface(wlr_xdg_popup.base(xdgPopupPtr));
    }


    public Surface getParent() {
        return new Surface(wlr_xdg_popup.parent(xdgPopupPtr));
    }


    //
    // *** Events ***
    //

    public class Events {
        public final Signal0 destroy;

        /// Added for API safety and convenience, it does not exist in wlroots. Correctly passes the subclass
        /// of {@link XdgSurfaceConfigure} to handler. Use this instead of
        /// {@link XdgSurface.Events#ackConfigure}.
        public final Signal1<XdgSurfaceConfigure.Popup> ackConfigure;


        Events(MemorySegment ptr) {
            this.destroy = Signal.of(wlr_xdg_popup.events.destroy(ptr));

            var baseAckConfigureSignalPtr = getBase().events.ackConfigure.signalPtr;
            this.ackConfigure = Signal.of(baseAckConfigureSignalPtr, XdgSurfaceConfigure.Popup::new);
        }


        public Signal[] allSignals() {
            return new Signal[]{destroy};
        }
    }
}