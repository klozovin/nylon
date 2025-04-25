package wayland;

import static jextract.wayland.server.server_h.WL_KEYBOARD_KEY_STATE_PRESSED;
import static jextract.wayland.server.server_h.WL_KEYBOARD_KEY_STATE_RELEASED;


public enum KeyboardKeyState {
    PRESSED(WL_KEYBOARD_KEY_STATE_PRESSED()),
    RELEASED(WL_KEYBOARD_KEY_STATE_RELEASED());

    public final int value;


    KeyboardKeyState(int value) {
        this.value = value;
    }


    public static KeyboardKeyState of(int value) {
        for (var e : values()) {
            if (e.value == value)
                return e;
        }
        throw new RuntimeException("Invalid enum value from C code");
    }
}