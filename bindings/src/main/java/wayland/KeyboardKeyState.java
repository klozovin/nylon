package wayland;

import jextract.wayland.server.server_h;


public enum KeyboardKeyState {
    PRESSED(server_h.WL_KEYBOARD_KEY_STATE_PRESSED()),
    RELEASED(server_h.WL_KEYBOARD_KEY_STATE_RELEASED());

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