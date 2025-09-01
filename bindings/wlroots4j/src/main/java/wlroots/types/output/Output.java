package wlroots.types.output;

import jextract.wlroots.types.wlr_output;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.Display;
import wayland.server.Signal;
import wayland.server.Signal.Signal1;
import wlroots.render.Allocator;
import wlroots.render.BufferPassOptions;
import wlroots.render.RenderPass;
import wlroots.render.Renderer;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static jextract.wlroots.types.wlr_output_h.*;


/// A compositor output region. This typically corresponds to a monitor that displays part of the compositor
/// space.
///
/// The {@link Events#frame} event will be emitted when it is a good time for the compositor to submit a new
/// frame.
///
/// `struct wlr_output {};`
@NullMarked
public final class Output {
    public final MemorySegment outputPtr;
    public final Events events;


    public Output(MemorySegment outputPtr) {
        assert !outputPtr.equals(NULL);
        this.outputPtr = outputPtr;
        this.events = new Events(wlr_output.events(outputPtr));
    }


    @Override
    public boolean equals(Object other) {
        return switch (other) {
            case Output otherOutput -> outputPtr.equals(otherOutput.outputPtr);
            case null -> false;
            default -> throw new RuntimeException("BUG: Trying to compare objects of different types");
        };
    }


    @Override
    public int hashCode() {
        return outputPtr.hashCode();
    }


    // *** Fields ***************************************************************************************** //


    public int width() {
        return wlr_output.width(outputPtr);
    }


    public int height() {
        return wlr_output.height(outputPtr);
    }


    // *** Methods *** //


    /// Returns the preferred mode for this output. If the output doesn't support modes, returns NULL.
    public @Nullable OutputMode preferredMode() {
        var modePtr = wlr_output_preferred_mode(outputPtr);
        return !modePtr.equals(NULL) ? new OutputMode(modePtr) : null;
    }


    /// Configures the output to use the allocator and renderer, must be done once, before commiting the
    /// output.
    public boolean initRender(Allocator allocator, Renderer renderer) {
        return wlr_output_init_render(outputPtr, allocator.allocatorPtr, renderer.rendererPtr);
    }


    /// Begin a render pass on this output.
    ///
    /// Compositors can call this function to begin rendering. After the render pass has been submitted, they
    /// should call {@link #commitState(OutputState)} to submit the new frame.
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
    /// @param state     describes the output changes the rendered frame will be committed with. A NULL state
    ///                  indicates no change.
    /// @param bufferAge If non-NULL, buffer_age is set to the drawing buffer age in number of frames or -1 if
    ///                  unknown. This is useful for damage tracking.
    /// @return On error, NULL is returned. Creating a render pass on a disabled output is an error.
    public @Nullable RenderPass beginRenderPass(OutputState state, @Nullable Integer bufferAge, @Nullable BufferPassOptions renderOptions) {
        // TODO: overload with renderoptions=null for cleaner API
        var renderPassPtr = wlr_output_begin_render_pass(
            outputPtr,
            state.outputStatePtr,
            switch (bufferAge) {
                // TODO: Use something platform independent instead of JAVA_INT
                // TODO: Memory lifetime, why not confined?
                case Integer ba -> Arena.global().allocateFrom(JAVA_INT, ba);
                case null -> NULL;
            },
            //Arena.global().allocateFrom(JAVA_INT, bufferAge), // TODO: Memory lifetime, why not confined?
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


    /// Render software cursors. The damage is in buffer-local coordinate space.
    ///
    /// This is a utility function that can be called when compositors render.
    public void addSoftwareCursorsToRenderPass(RenderPass pass) {
        // TODO: Add overload for damage parameter
        wlr_output_add_software_cursors_to_render_pass(outputPtr, pass.renderPassPtr, NULL);
    }


    public void createGlobal(Display display) {
        wlr_output_create_global(outputPtr, display.displayPtr);
    }


    // *** Events ***************************************************************************************** //


    public final static class Events {
        /// Raised every time an output is ready to display a frame, generally at the output's refresh rate
        public final Signal1<Output> frame;

        public final Signal1<Output> destroy;

        /// Raised when the backend requests a new state for the output (i.e. output window resized)
        public final Signal1<EventRequestState> requestState;


        Events(MemorySegment ptr) {
            frame        = Signal.of(wlr_output.events.frame(ptr), Output::new);
            destroy      = Signal.of(wlr_output.events.destroy(ptr), Output::new);
            requestState = Signal.of(wlr_output.events.request_state(ptr), EventRequestState::new);
        }
    }
}