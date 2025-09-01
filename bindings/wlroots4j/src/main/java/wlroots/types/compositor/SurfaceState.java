package wlroots.types.compositor;

import jextract.wlroots.types.wlr_surface_state;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


/// `struct wlr_surface_state {};`
@NullMarked
public class SurfaceState {
    public final MemorySegment surfaceStatePtr;


    public SurfaceState(MemorySegment surfaceStatePtr) {
        assert !surfaceStatePtr.equals(NULL);
        this.surfaceStatePtr = surfaceStatePtr;
    }


    /// In surface-local coordinates
    public int width() {
        return wlr_surface_state.width(surfaceStatePtr);
    }


    /// In surface-local coordinates
    public int height() {
        return wlr_surface_state.height(surfaceStatePtr);
    }
}