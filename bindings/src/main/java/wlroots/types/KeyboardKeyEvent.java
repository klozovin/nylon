package wlroots.types;

import jextract.wlroots.types.wlr_keyboard_key_event;
import org.jspecify.annotations.NullMarked;
import wayland.KeyboardKeyState;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class KeyboardKeyEvent {
    public final MemorySegment keyboardKeyEventPtr;


    public KeyboardKeyEvent(MemorySegment keyboardKeyEventPtr) {
        assert !keyboardKeyEventPtr.equals(NULL);
        this.keyboardKeyEventPtr = keyboardKeyEventPtr;
    }


    public int timeMsec() {
        return wlr_keyboard_key_event.time_msec(keyboardKeyEventPtr);
    }


    public int keycode() {
        return wlr_keyboard_key_event.keycode(keyboardKeyEventPtr);
    }


    public KeyboardKeyState state() {
        return KeyboardKeyState.of(wlr_keyboard_key_event.state(keyboardKeyEventPtr));
    }
}