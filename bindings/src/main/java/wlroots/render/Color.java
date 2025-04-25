package wlroots.render;

import jextract.wlroots.render.wlr_render_color;
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


    public void rgba(double[] colors) {
        wlr_render_color.r(colorPtr, (float) colors[0]);
        wlr_render_color.g(colorPtr, (float) colors[1]);
        wlr_render_color.b(colorPtr, (float) colors[2]);
        wlr_render_color.a(colorPtr, (float) colors[3]);
    }


    public void rgba(double r, double g, double b, double a) {
        wlr_render_color.r(colorPtr, (float) r);
        wlr_render_color.g(colorPtr, (float) g);
        wlr_render_color.b(colorPtr, (float) b);
        wlr_render_color.a(colorPtr, (float) a);
    }
}