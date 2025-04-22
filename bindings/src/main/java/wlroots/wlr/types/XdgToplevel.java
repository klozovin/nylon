package wlroots.wlr.types;

import jextract.wlroots.types.wlr_xdg_toplevel;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_xdg_shell_h.wlr_xdg_toplevel_set_size;
import static jextract.wlroots.types.wlr_xdg_shell_h.wlr_xdg_toplevel_try_from_wlr_surface;


@NullMarked
public class XdgToplevel {
    public final MemorySegment xdgToplevelPtr;


    public XdgToplevel(MemorySegment xdgToplevelPtr) {
        assert !xdgToplevelPtr.equals(NULL);
        this.xdgToplevelPtr = xdgToplevelPtr;
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


    public XdgSurface base() {
        return new XdgSurface(wlr_xdg_toplevel.base(xdgToplevelPtr));
    }
}