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
        for (var e : values()) {
            if (e.value == value)
                return e;
        }
        throw new RuntimeException("Invalid enum value from C code");
    }
}