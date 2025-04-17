package wlroots.wlr.render;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.render.pass_h.wlr_render_pass_add_rect;
import static jextract.wlroots.render.pass_h.wlr_render_pass_submit;


@NullMarked
public class RenderPass {
    private final MemorySegment renderPassPtr;


    public RenderPass(MemorySegment renderPassPtr) {
        assert !renderPassPtr.equals(NULL);
        this.renderPassPtr = renderPassPtr;
    }


    public void addRect(RectOptions options) {
        wlr_render_pass_add_rect(renderPassPtr, options.rectOptionsPtr);
    }


    public void submit() {
        wlr_render_pass_submit(renderPassPtr);
    }
}