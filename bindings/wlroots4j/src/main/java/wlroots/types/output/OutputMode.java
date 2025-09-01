package wlroots.types.output;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public final class OutputMode {
    public final MemorySegment outputModePtr;


    public OutputMode(MemorySegment outputModePtr) {
        assert !outputModePtr.equals(NULL);
        this.outputModePtr = outputModePtr;
    }
}
