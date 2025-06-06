package wlroots.types.input;

import jextract.wlroots.types.wlr_keyboard_key_event;
import org.jspecify.annotations.NullMarked;
import wayland.KeyboardKeyState;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


/// `struct wlr_keyboard_key_event {}`
@NullMarked
public class KeyboardKeyEvent {
    public final int timeMsec;
    public final int keycode;
    public final boolean updateState;
    public final KeyboardKeyState state;


    public KeyboardKeyEvent(MemorySegment ptr) {
        assert !ptr.equals(NULL);
        timeMsec    = wlr_keyboard_key_event.time_msec(ptr);
        keycode     = wlr_keyboard_key_event.keycode(ptr);
        updateState = wlr_keyboard_key_event.update_state(ptr);
        state       = KeyboardKeyState.of(wlr_keyboard_key_event.state(ptr));
    }
}