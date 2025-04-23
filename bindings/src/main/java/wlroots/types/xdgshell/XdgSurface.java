package wlroots.types.xdgshell;

import jextract.wlroots.types.wlr_xdg_surface;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


/// An xdg-surface is a user interface element requiring management by the compositor.
/// An xdg-surface alone isn't useful, a role should be assigned to it in order to map it.
@NullMarked
public class XdgSurface {
    public final MemorySegment xdgSurfacePtr;


    public XdgSurface(MemorySegment xdgSurfacePtr) {
        assert !xdgSurfacePtr.equals(NULL);
        this.xdgSurfacePtr = xdgSurfacePtr;
    }


    /// Whether the latest commit is an initial commit.
    public boolean initialCommit() {
        return wlr_xdg_surface.initial_commit(xdgSurfacePtr);
    }
}