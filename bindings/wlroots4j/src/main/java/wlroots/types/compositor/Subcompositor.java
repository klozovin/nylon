package wlroots.types.compositor;

import org.jspecify.annotations.NullMarked;
import wayland.server.Display;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_subcompositor_h.wlr_subcompositor_create;


@NullMarked
public class Subcompositor {
    public final MemorySegment subcompositorPtr;


    public Subcompositor(MemorySegment subcompositorPtr) {
        assert !subcompositorPtr.equals(NULL);
        this.subcompositorPtr = subcompositorPtr;
    }


    public static Subcompositor create(Display display){
        return new Subcompositor(wlr_subcompositor_create(display.displayPtr));
    }
}