package wlroots.wlr.render;

import jexwlroots.render.wlr_render_color;
import org.jspecify.annotations.NonNull;

import java.lang.foreign.MemorySegment;

public class Color {
    public final @NonNull MemorySegment colorPtr;


    public void setColor(float r, float g, float b, float a) {
        wlr_render_color.r(colorPtr, r);
        wlr_render_color.g(colorPtr, g);
        wlr_render_color.b(colorPtr, b);
        wlr_render_color.a(colorPtr, a);
    }


    public Color(@NonNull MemorySegment colorPtr) {
        this.colorPtr = colorPtr;
    }
}