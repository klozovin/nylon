package wlroots.wlr.render;

import jextract.wlroots.render.wlr_render_rect_options;
import org.jspecify.annotations.NullMarked;
import wlroots.wlr.util.Box;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;

@NullMarked
public final class RectOptions {
    public final MemorySegment rectOptionsPtr;
    public final Box box;
    public final Color color;


    public RectOptions(MemorySegment rectOptionsPtr) {
        assert !rectOptionsPtr.equals(NULL);
        this.rectOptionsPtr = rectOptionsPtr;
        this.box = new Box(wlr_render_rect_options.box(rectOptionsPtr));
        this.color = new Color(wlr_render_rect_options.color(rectOptionsPtr));
    }


    public static RectOptions allocate(Arena arena) {
        return new RectOptions(wlr_render_rect_options.allocate(arena));
    }
}
