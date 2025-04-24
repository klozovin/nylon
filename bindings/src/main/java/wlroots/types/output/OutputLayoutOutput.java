package wlroots.types.output;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class OutputLayoutOutput {
    MemorySegment outputLayoutOutputPtr;


    public OutputLayoutOutput(MemorySegment outputLayoutOutputPtr) {
        assert !outputLayoutOutputPtr.equals(NULL);
        this.outputLayoutOutputPtr = outputLayoutOutputPtr;
    }
}