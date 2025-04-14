package wlroots.wlr.render;

import jexwlroots.render.wlr_render_color;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class Color {
    public final MemorySegment colorPtr;


    public Color(MemorySegment colorPtr) {
        assert !colorPtr.equals(NULL);
        this.colorPtr = colorPtr;
    }


    public void rgba(float[] colors) {
        wlr_render_color.r(colorPtr, colors[0]);
        wlr_render_color.g(colorPtr, colors[1]);
        wlr_render_color.b(colorPtr, colors[2]);
        wlr_render_color.a(colorPtr, colors[3]);
    }


    public void rgba(float r, float g, float b, float a) {
        wlr_render_color.r(colorPtr, r);
        wlr_render_color.g(colorPtr, g);
        wlr_render_color.b(colorPtr, b);
        wlr_render_color.a(colorPtr, a);
    }
}