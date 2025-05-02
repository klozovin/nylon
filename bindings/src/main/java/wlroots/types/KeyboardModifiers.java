package wlroots.types;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class KeyboardModifiers {
    public final MemorySegment keyboardModifiersPtr;


    public KeyboardModifiers(MemorySegment keyboardModifiersPtr) {
        assert !keyboardModifiersPtr.equals(NULL);
        this.keyboardModifiersPtr = keyboardModifiersPtr;
    }
}