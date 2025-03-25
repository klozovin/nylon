package wlroots.wlr.types;

import java.lang.foreign.MemorySegment;

public final class Output {

    public final MemorySegment outputPtr;

    public Output(MemorySegment outputPtr) {
        this.outputPtr = outputPtr;
    }
}
