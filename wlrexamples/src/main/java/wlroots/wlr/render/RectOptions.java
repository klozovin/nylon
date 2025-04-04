package wlroots.wlr.render;

import jexwlroots.render.wlr_render_rect_options;
import org.jspecify.annotations.NonNull;
import wlroots.wlr.util.Box;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;


public class RectOptions {
    public final @NonNull MemorySegment rectOptionsPtr;
    public final @NonNull Box box;
    public final @NonNull Color color;


    public RectOptions(@NonNull MemorySegment rectOptionsPtr) {
        this.rectOptionsPtr = rectOptionsPtr;
        this.box = new Box(wlr_render_rect_options.box(rectOptionsPtr));
        this.color = new Color(wlr_render_rect_options.color(rectOptionsPtr));
    }


    public static @NonNull RectOptions allocate(Arena arena) {
        return new RectOptions(wlr_render_rect_options.allocate(arena));
    }
}
