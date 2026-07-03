package wayland;

import static jextract.wayland.wl.*;


public enum PointerAxisSource {
    Wheel(WL_POINTER_AXIS_SOURCE_WHEEL()),
    Finger(WL_POINTER_AXIS_SOURCE_FINGER()),
    Continuous(WL_POINTER_AXIS_SOURCE_CONTINUOUS()),
    WheelTilt(WL_POINTER_AXIS_SOURCE_WHEEL_TILT());

    public final int value;


    PointerAxisSource(int value) {
        this.value = value;
    }


    public static PointerAxisSource of(int value) {
        if (value == WL_POINTER_AXIS_SOURCE_WHEEL())      return Wheel;
        if (value == WL_POINTER_AXIS_SOURCE_FINGER())     return Finger;
        if (value == WL_POINTER_AXIS_SOURCE_CONTINUOUS()) return Continuous;
        if (value == WL_POINTER_AXIS_SOURCE_WHEEL_TILT()) return WheelTilt;

        throw new RuntimeException("Invalid enum value from C code for wl_pointer_axis_source");
    }
}