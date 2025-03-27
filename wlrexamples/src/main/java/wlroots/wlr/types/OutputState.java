package wlroots.wlr.types;

import jexwlroots.types.wlr_output_state;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static jexwlroots.types.wlr_output_h.*;


/// Holds the double-buffered output state.
public final class OutputState {

    public final MemorySegment outputStatePtr;

    public OutputState(MemorySegment outputStatePtr) {
        this.outputStatePtr = outputStatePtr;
    }

    public static OutputState allocate(Arena arena) {
        return new OutputState(wlr_output_state.allocate(arena));
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

    @Deprecated
    public void setMode(MemorySegment mode) {
        wlr_output_state_set_mode(outputStatePtr, mode);
    }

    /**
     * Sets the output mode of an output. An output mode will specify the resolution and refresh
     * rate, among other things.
     * <p>
     * This state will be applied once {@link Output#commitState} is called.
     */
    public void setMode(OutputMode mode) {
        wlr_output_state_set_mode(outputStatePtr, mode.outputModePtr);
    }

    /**
     * Releases all resources associated with an output state.
     */
    public void finish() {
        wlr_output_state_finish(outputStatePtr);
    }
}
