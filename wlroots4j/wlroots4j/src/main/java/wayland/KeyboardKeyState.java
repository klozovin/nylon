package wayland;

import static jextract.wayland.wl.WL_KEYBOARD_KEY_STATE_RELEASED;
import static jextract.wayland.wl.WL_KEYBOARD_KEY_STATE_PRESSED;


public enum KeyboardKeyState {
    Pressed(WL_KEYBOARD_KEY_STATE_PRESSED()),
    Released(WL_KEYBOARD_KEY_STATE_RELEASED());

    public final int value;


    KeyboardKeyState(int value) {
        this.value = value;
    }


    public static KeyboardKeyState of(int value) {
        if (value == WL_KEYBOARD_KEY_STATE_PRESSED())  return Pressed;
        if (value == WL_KEYBOARD_KEY_STATE_RELEASED()) return Released;

        throw new RuntimeException("Invalid enum value from C code for wl_keyboard_key_state");
    }
}