package wlroots.types.input;

import static jextract.wlroots.wlr.WLR_BUTTON_RELEASED;
import static jextract.wlroots.wlr.WLR_BUTTON_PRESSED;


public enum ButtonState {
    Released(WLR_BUTTON_RELEASED()),
    Pressed(WLR_BUTTON_PRESSED());

    final int value;


    ButtonState(int value) {
        this.value = value;
    }


    public static ButtonState of(int value) {
        for (var e : values())
            if (e.value == value)
                return e;
        throw new RuntimeException("Invalid enum value from C code for wlr_button_state");
    }
}