package wlroots.wlr.render;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.lang.foreign.MemorySegment;

import static jexwlroots.render.pass_h.wlr_render_pass_add_rect;
import static jexwlroots.render.pass_h.wlr_render_pass_submit;


public class RenderPass {
    private final @NonNull MemorySegment renderPassPtr;


    public RenderPass(@NotNull MemorySegment renderPassPtr) {
        this.renderPassPtr = renderPassPtr;
    }


    public void addRect(RectOptions rectOptions) {
        wlr_render_pass_add_rect(renderPassPtr, rectOptions.rectOptionsPtr);
    }


    public void submit() {
        wlr_render_pass_submit(renderPassPtr);
    }
}