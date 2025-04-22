package wlroots.wlr.types;

import jextract.wlroots.types.wlr_output_state;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_output_h.*;


/// Holds the double-buffered output state.
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


    /// Enables or disables an output. A disabled output is turned off and doesn't
    /// emit `frame` events.
    ///
    /// This state will be applied once [#commitState] is called.
    public void setEnabled(boolean enabled) {
        wlr_output_state_set_enabled(outputStatePtr, enabled);
    }


    /// Sets the output mode of an output. An output mode will specify the resolution and refresh
    /// rate, among other things.
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