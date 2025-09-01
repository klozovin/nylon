package wlroots.types.output;

import jextract.wlroots.types.wlr_output_state;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_output_h.*;


/// Represent the changes (staging area) to be applied to an {@link Output} when committed.
///
/// Some of the changes that can be applied:
///
/// * resolution
/// * refresh rate
/// * enabling, disabling
/// * damage regions
///
/// Supports atomic updates, you can change multiple properties and apply them at once. Only the fields
/// explicitly set will be applied (necessary to call {@link #init()} first).
///
/// Holds the double-buffered output state.
///
/// You do not set properties directly on the {@link Output}, instead use this class and then commit.
@NullMarked
public final class OutputState {
    public final MemorySegment outputStatePtr;


    public OutputState(MemorySegment outputStatePtr) {
        assert !outputStatePtr.equals(NULL);
        this.outputStatePtr = outputStatePtr;
    }


    public static OutputState allocate(Arena arena) {
        return new OutputState(wlr_output_state.allocate(arena));
    }


    public static void allocateConfined(Consumer<OutputState> block) {
        try (var arena = Arena.ofConfined()) {
            var outputState = allocate(arena);
            block.accept(outputState);
        }
    }


    public void init() {
        wlr_output_state_init(outputStatePtr);
    }


    /// Enables or disables an output. A disabled output is turned off and doesn't emit `frame` events.
    ///
    /// This state will be applied once {@link Output#commitState(OutputState)} is called.
    public void setEnabled(boolean enabled) {
        wlr_output_state_set_enabled(outputStatePtr, enabled);
    }


    /// Sets the output mode of an output. An output mode will specify the resolution and refresh rate, among
    /// other things.
    ///
    /// This state will be applied once {@link Output#commitState(OutputState)} is called.
    public void setMode(OutputMode mode) {
        wlr_output_state_set_mode(outputStatePtr, mode.outputModePtr);
    }


    /// Releases all resources associated with an output state.
    public void finish() {
        wlr_output_state_finish(outputStatePtr);
    }
}