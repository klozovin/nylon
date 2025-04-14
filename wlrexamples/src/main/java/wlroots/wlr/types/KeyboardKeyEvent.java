package wlroots.wlr.types;

import jextract.wlroots.types.wlr_keyboard_key_event;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class KeyboardKeyEvent {
    public final MemorySegment keyboardKeyEventPtr;


    public KeyboardKeyEvent(MemorySegment keyboardKeyEventPtr) {
        assert !keyboardKeyEventPtr.equals(NULL);
        this.keyboardKeyEventPtr = keyboardKeyEventPtr;
    }


    public int keycode() {
        return wlr_keyboard_key_event.keycode(keyboardKeyEventPtr);
    }
}