package wlroots.types.xdgshell;

import jextract.wlroots.types.wlr_xdg_surface;
import org.jspecify.annotations.NullMarked;
import wlroots.types.compositor.Surface;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


/// An xdg-surface is a user interface element requiring management by the compositor. An xdg-surface alone
/// isn't useful, a role should be assigned to it in order to map it.
@NullMarked
public class XdgSurface {
    public final MemorySegment xdgSurfacePtr;


    public XdgSurface(MemorySegment xdgSurfacePtr) {
        assert !xdgSurfacePtr.equals(NULL);
        this.xdgSurfacePtr = xdgSurfacePtr;
    }


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
}