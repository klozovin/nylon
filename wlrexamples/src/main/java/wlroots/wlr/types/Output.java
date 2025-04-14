package wlroots.wlr.types;

import jextract.wlroots.types.wlr_output;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.Signal;
import wlroots.wlr.render.Allocator;
import wlroots.wlr.render.BufferPassOptions;
import wlroots.wlr.render.RenderPass;
import wlroots.wlr.render.Renderer;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static jextract.wlroots.types.wlr_output_h.*;


public final class Output {
    public final @NonNull MemorySegment outputPtr;
    public final @NonNull Events events;


    public Output(@NonNull MemorySegment outputPtr) {
        assert !outputPtr.equals(NULL);
        this.outputPtr = outputPtr;
        this.events = new Events(wlr_output.events(outputPtr));
    }


    public int width() {
        return wlr_output.width(outputPtr);
    }


    public int height() {
        return wlr_output.height(outputPtr);
    }


    /// Returns the preferred mode for this output. If the output doesn't support modes, returns NULL.
    public @Nullable OutputMode preferredMode() {
        var modePtr = wlr_output_preferred_mode(outputPtr);
        return !modePtr.equals(NULL) ? new OutputMode(modePtr) : null;
    }


    public boolean initRender(@NonNull Allocator allocator, @NonNull Renderer renderer) {
        return wlr_output_init_render(outputPtr, allocator.allocatorPtr, renderer.rendererPtr);
    }


    /// Begin a render pass on this output.
    ///
    /// Compositors can call this function to begin rendering. After the render pass has been
    /// submitted, they should call {@link #commitState(OutputState)} to submit the new frame.
    ///
    /// ```c
    /// struct wlr_render_pass *
    /// wlr_output_begin_render_pass(
    ///     struct wlr_output *output,
    ///     struct wlr_output_state *state,
    ///     int *buffer_age,
    ///     struct wlr_buffer_pass_options *render_options
    ///)
    ///```
    ///
    /// @param state     describes the output changes the rendered frame will be committed with. A NULL state indicates no change.
    /// @param bufferAge If non-NULL, buffer_age is set to the drawing buffer age in number of frames or -1 if unknown. This is useful for damage tracking.
    /// @return On error, NULL is returned. Creating a render pass on a disabled output is an error.
    public @Nullable RenderPass beginRenderPass(OutputState state, int bufferAge, @Nullable BufferPassOptions renderOptions) {
        var renderPassPtr = wlr_output_begin_render_pass(
            outputPtr,
            state.outputStatePtr,
            Arena.global().allocateFrom(JAVA_INT, bufferAge), // TODO: Memory lifetime
            switch (renderOptions) {
                case BufferPassOptions bpo -> bpo.bufferPassOptionsPtr;
                case null -> NULL;
            }
        );
        return !renderPassPtr.equals(NULL) ? new RenderPass(renderPassPtr) : null;
    }


    public boolean commitState(OutputState state) {
        return wlr_output_commit_state(outputPtr, state.outputStatePtr);
    }


    @NullMarked
    public final static class Events {
        public final MemorySegment eventsPtr;
        public final Signal<Void> frame;
        public final Signal<Void> destroy;


        Events(MemorySegment eventsPtr) {
            this.eventsPtr = eventsPtr;
            this.frame = new Signal<>(wlr_output.events.frame(eventsPtr));
            this.destroy = new Signal<>(wlr_output.events.destroy(eventsPtr));
        }
    }
}