package wlroots.types.xdgshell;

import jextract.wlroots.wlr_xdg_surface_configure;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static wlroots.types.xdgshell.XdgSurface.SurfaceRole.Popup;
import static wlroots.types.xdgshell.XdgSurface.SurfaceRole.Toplevel;


@NullMarked
public sealed class XdgSurfaceConfigure permits XdgSurfaceConfigure.Popup, XdgSurfaceConfigure.Toplevel {
    public final MemorySegment xdgSurfaceConfigurePtr;


    public XdgSurfaceConfigure(MemorySegment xdgSurfaceConfigurePtr) {
        assert !xdgSurfaceConfigurePtr.equals(NULL);
        this.xdgSurfaceConfigurePtr = xdgSurfaceConfigurePtr;
    }


    //
    // Fields
    //

    public XdgSurface getSurface() {
        return new XdgSurface(wlr_xdg_surface_configure.surface(xdgSurfaceConfigurePtr));
    }


    public int getSerial() {
        return wlr_xdg_surface_configure.serial(xdgSurfaceConfigurePtr);
    }


    /// When an event fires on a XdgToplevel
    public static final class Toplevel extends XdgSurfaceConfigure {
        public Toplevel(MemorySegment xdgSurfaceConfigurePtr) {
            super(xdgSurfaceConfigurePtr);
        }


        public XdgToplevelConfigure getToplevelConfigure() {
            assert this.getSurface().role() == Toplevel;
            return new XdgToplevelConfigure(wlr_xdg_surface_configure.toplevel_configure(xdgSurfaceConfigurePtr));
        }
    }


    /// When event fires on a XdgToplevel
    public static final class Popup extends XdgSurfaceConfigure {

        public Popup(MemorySegment xdgSurfaceConfigurePtr) {
            super(xdgSurfaceConfigurePtr);
        }


        public XdgPopupConfigure getPopupConfigure() {
            assert getSurface().role() == Popup;
            return new XdgPopupConfigure(wlr_xdg_surface_configure.popup_configure(xdgSurfaceConfigurePtr));

        }
    }
}