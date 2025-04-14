package xkbcommon;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class RuleNames {
    public final MemorySegment ruleNamesPtr;


    public RuleNames(MemorySegment ruleNamesPtr) {
        assert !ruleNamesPtr.equals(NULL);
        this.ruleNamesPtr = ruleNamesPtr;
    }
}