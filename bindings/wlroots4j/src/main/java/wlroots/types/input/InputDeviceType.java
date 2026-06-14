package wlroots.types.input;

import static jextract.wlroots.wlr.*;


public enum InputDeviceType {
    Keyboard(WLR_INPUT_DEVICE_KEYBOARD()),    // struct wlr_keyboard
    Pointer(WLR_INPUT_DEVICE_POINTER()),      // struct wlr_pointer
    Touch(WLR_INPUT_DEVICE_TOUCH()),          // struct wlr_touch
    Tablet(WLR_INPUT_DEVICE_TABLET()),        // struct wlr_tablet
    TabletPad(WLR_INPUT_DEVICE_TABLET_PAD()), // struct wlr_tablet_pad
    Switch(WLR_INPUT_DEVICE_SWITCH());        // struct wlr_switch

    final int value;


    InputDeviceType(int value) {
        this.value = value;
    }


    public static InputDeviceType of(int value) {
        for (var e : values())
            if (e.value == value)
                return e;
        throw new RuntimeException("Invalid enum value from C code for wlr_input_device_type");
    }
}