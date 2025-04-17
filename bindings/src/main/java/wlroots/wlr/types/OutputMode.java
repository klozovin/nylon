package wlroots.wlr.types;

import org.jspecify.annotations.NonNull;

import java.lang.foreign.MemorySegment;

public final class OutputMode {

    public final @NonNull MemorySegment outputModePtr;

    public OutputMode(@NonNull MemorySegment outputModePtr) {
        this.outputModePtr = outputModePtr;
    }
}
