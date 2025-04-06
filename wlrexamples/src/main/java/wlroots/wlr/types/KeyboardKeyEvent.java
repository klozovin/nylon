package wlroots.wlr.types;

import jexwlroots.types.wlr_keyboard_key_event;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.lang.foreign.MemorySegment;


public class KeyboardKeyEvent {
    public final @NonNull MemorySegment keyboardKeyEventPtr;


    public int getKeycode() {
        return wlr_keyboard_key_event.keycode(keyboardKeyEventPtr);
    }


    public KeyboardKeyEvent(@NotNull MemorySegment keyboardKeyEventPtr) {
        this.keyboardKeyEventPtr = keyboardKeyEventPtr;
    }
}