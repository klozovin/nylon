package wlroots.wlr.types;


import org.jspecify.annotations.Nullable;
import wlroots.wlr.render.Allocator;
import wlroots.wlr.render.Renderer;

import java.lang.foreign.MemorySegment;

import static wlroots.types.wlr_output_h.*;

public final class Output {

    public final MemorySegment outputPtr;

    public Output(MemorySegment outputPtr) {
        this.outputPtr = outputPtr;
    }

    public @Nullable OutputMode preferredMode() {
        var ptr = wlr_output_preferred_mode(outputPtr);

        assert ptr != null;

        if (ptr == MemorySegment.NULL)
            return null;

        return new OutputMode(ptr);
    }

    public boolean commitState(OutputState state) {
        return wlr_output_commit_state(outputPtr, state.outputStatePtr);
    }

    public boolean initRender(Allocator allocator, Renderer renderer) {
        return wlr_output_init_render(outputPtr, allocator.allocatorPtr, renderer.rendererPtr);
    }
}