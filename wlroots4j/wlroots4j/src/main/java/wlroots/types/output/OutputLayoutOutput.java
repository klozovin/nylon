package wlroots.types.output;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


/// Info about the specific {@link Output} and it's position withing an {@link OutputLayout}.
///
/// Will return this from the {@link OutputLayout#addAuto(Output)} marking which Output was added, and to what OutputLayout.
@NullMarked
public class OutputLayoutOutput {
    public final MemorySegment outputLayoutOutputPtr;


    public OutputLayoutOutput(MemorySegment outputLayoutOutputPtr) {
        assert !outputLayoutOutputPtr.equals(NULL);
        this.outputLayoutOutputPtr = outputLayoutOutputPtr;
    }

    public static @Nullable OutputLayoutOutput ofPtrOrNull(MemorySegment ptr) {
        return !ptr.equals(NULL) ? new OutputLayoutOutput(ptr) : null;
    }
}