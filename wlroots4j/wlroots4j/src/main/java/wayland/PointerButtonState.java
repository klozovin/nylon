package wayland;

import org.jspecify.annotations.NullMarked;

import static jextract.wayland.wl.WL_POINTER_BUTTON_STATE_PRESSED;
import static jextract.wayland.wl.WL_POINTER_BUTTON_STATE_RELEASED;


@NullMarked
public enum PointerButtonState {
    Pressed(WL_POINTER_BUTTON_STATE_PRESSED()),
    Released(WL_POINTER_BUTTON_STATE_RELEASED());

    public final int value;


    PointerButtonState(int value) {
        this.value = value;
    }


    public static PointerButtonState of(int value) {
        if (value == WL_POINTER_BUTTON_STATE_PRESSED()) return Pressed;
        if (value == WL_POINTER_BUTTON_STATE_RELEASED()) return Released;

        throw new RuntimeException("Invalid enum value from C code for wl_pointer_button_state");
    }


    public boolean isPressed() {
        return this == Pressed;
    }


    public boolean isReleased() {
        return this == Released;
    }
}