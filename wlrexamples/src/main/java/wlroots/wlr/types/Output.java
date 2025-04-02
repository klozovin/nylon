package wlroots.wlr.types;

import jexwlroots.types.wlr_output;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import wayland.server.Signal;
import wlroots.wlr.render.Allocator;
import wlroots.wlr.render.Renderer;

import java.lang.foreign.MemorySegment;

import static jexwlroots.types.wlr_output_h.*;


public final class Output {
    public final @NonNull MemorySegment outputPtr;
    public final @NonNull Events events;


    public Output(@NotNull MemorySegment outputPtr) {
        this.outputPtr = outputPtr;
        this.events = new Events(wlr_output.events(outputPtr));
    }


    public @Nullable OutputMode getPreferredMode() {
        var ptr = wlr_output_preferred_mode(outputPtr);

        assert ptr != null;

        if (ptr == MemorySegment.NULL) return null;
        return new OutputMode(ptr);
    }


    public boolean commitState(OutputState state) {
        return wlr_output_commit_state(outputPtr, state.outputStatePtr);
    }


    public boolean initRender(Allocator allocator, Renderer renderer) {
        return wlr_output_init_render(outputPtr, allocator.allocatorPtr, renderer.rendererPtr);
    }


    public final static class Events {
        public final @NonNull MemorySegment eventsPtr;
        public final @NonNull Signal<Void> frame;
        public final @NonNull Signal<Void> destroy;


        Events(@NonNull MemorySegment eventsPtr) {
            this.eventsPtr = eventsPtr;
            frame = new Signal<>(wlr_output.events.frame(eventsPtr), (_) -> null);
            destroy = new Signal<>(wlr_output.events.destroy(eventsPtr), (_) -> null);
        }
    }
}