package wlroots.types.input;

import jextract.wlroots.types.wlr_keyboard_key_event;
import org.jspecify.annotations.NullMarked;
import wayland.KeyboardKeyState;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


/// `struct wlr_keyboard_key_event {}`
@NullMarked
public class KeyboardKeyEvent {
    public final MemorySegment keyboardKeyEventPtr;


    public KeyboardKeyEvent(MemorySegment keyboardKeyEventPtr) {
        assert !keyboardKeyEventPtr.equals(NULL);
        this.keyboardKeyEventPtr = keyboardKeyEventPtr;
    }


    // *** Fields ***************************************************************************************** //


    public int timeMsec() {
        return wlr_keyboard_key_event.time_msec(keyboardKeyEventPtr);
    }


    public int keycode() {
        return wlr_keyboard_key_event.keycode(keyboardKeyEventPtr);
    }


    /// If backend doesn't update modifiers on its own
    public boolean updateState() {
        return wlr_keyboard_key_event.update_state(keyboardKeyEventPtr);
    }


    public KeyboardKeyState state() {
        return KeyboardKeyState.of(wlr_keyboard_key_event.state(keyboardKeyEventPtr));
    }
}