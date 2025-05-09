package wayland;

import static jextract.wayland.server.server_h.*;


public enum PointerAxisSource {
    WHEEL(WL_POINTER_AXIS_SOURCE_WHEEL()),
    FINGER(WL_POINTER_AXIS_SOURCE_FINGER()),
    CONTINOUS(WL_POINTER_AXIS_SOURCE_CONTINUOUS()),
    WHEEL_TILT(WL_POINTER_AXIS_SOURCE_WHEEL_TILT());


    public final int value;


    PointerAxisSource(int value) {
        this.value = value;
    }


    public static PointerAxisSource of(int value) {
        for (var e : values()) {
            if (e.value == value)
                return e;
        }
        throw new RuntimeException("Invalid enum value from C code for wl_pointer_axis_source");
    }
}