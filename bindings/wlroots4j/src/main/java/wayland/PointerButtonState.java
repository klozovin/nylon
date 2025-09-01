package wayland;

import static jextract.wayland.server.server_h.WL_POINTER_BUTTON_STATE_PRESSED;
import static jextract.wayland.server.server_h.WL_POINTER_BUTTON_STATE_RELEASED;


public enum PointerButtonState {
    PRESSED(WL_POINTER_BUTTON_STATE_PRESSED()),
    RELEASED(WL_POINTER_BUTTON_STATE_RELEASED());

    public final int value;


    PointerButtonState(int value) {
        this.value = value;
    }


    public static PointerButtonState of(int value) {
        if (value == WL_POINTER_BUTTON_STATE_PRESSED())  return PRESSED;
        if (value == WL_POINTER_BUTTON_STATE_RELEASED()) return RELEASED;

        throw new RuntimeException("Invalid enum value from C code for wl_pointer_button_state");
    }
}