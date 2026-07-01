package wlroots.types.input;

import nylon.Utils;

import static jextract.wlroots.wlr.*;


public enum InputDeviceType {
    Keyboard(WLR_INPUT_DEVICE_KEYBOARD()),    // struct wlr_keyboard
    Pointer(WLR_INPUT_DEVICE_POINTER()),      // struct wlr_pointer
    Touch(WLR_INPUT_DEVICE_TOUCH()),          // struct wlr_touch
    Tablet(WLR_INPUT_DEVICE_TABLET()),        // struct wlr_tablet
    TabletPad(WLR_INPUT_DEVICE_TABLET_PAD()), // struct wlr_tablet_pad
    Switch(WLR_INPUT_DEVICE_SWITCH());        // struct wlr_switch

    public final int value;

    private static final InputDeviceType[] lookupTable = Utils.createLookupTableFromEnumClass(InputDeviceType.class);
    private static final InputDeviceType[] enumerations = values();


    InputDeviceType(int value) {
        this.value = value;
    }


    // TODO: Delete, don't need it anymore
    public static InputDeviceType ofIterate(int value) {
        for (var e : enumerations)
            if (e.value == value)
                return e;
        throw new RuntimeException("Invalid enum value from C code for wlr_input_device_type");
    }


    public static InputDeviceType of(int value) {
        var deviceType = lookupTable[value];
        assert deviceType != null : "Invalid enumeration value from C code for wlr_input_device_type: " + value;
        return deviceType;
    }
}