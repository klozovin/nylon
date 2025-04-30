package wlroots.types.output;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.Display;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_output_layout_h.*;


/// Helper to arrange outputs in a 2D coordinate space. The output effective resolution is used, see
/// wlr_output_effective_resolution().
///
/// `struct wlr_output_layout {}`
@NullMarked
public class OutputLayout {
    public final MemorySegment outputLayoutPtr;


    public OutputLayout(MemorySegment outputLayoutPtr) {
        assert !outputLayoutPtr.equals(NULL);
        this.outputLayoutPtr = outputLayoutPtr;
    }


    public static OutputLayout create(Display display) {
        return new OutputLayout(wlr_output_layout_create(display.displayPtr));
    }


    /// Add the output to the layout as automatically configured. This will place the output in a sensible
    /// location in the layout. The coordinates of the output in the layout will be adjusted dynamically when
    /// the layout changes. If the output is already a part of the layout, it will become automatically
    /// configured.
    ///
    /// @return Output's output layout or NULL on error
    public @Nullable OutputLayoutOutput addAuto(Output output) {
        var outputLayoutOutputPtr = wlr_output_layout_add_auto(outputLayoutPtr, output.outputPtr);
        return outputLayoutOutputPtr.equals(NULL) ? new OutputLayoutOutput(outputLayoutOutputPtr) : null;
    }


    /// Remove the output from the layout. If the output is already not a part of the layout, this function is
    /// a no-op.
    public void remove(Output output) {
        wlr_output_layout_remove(outputLayoutPtr, output.outputPtr);
    }
}