package wayland;

import static jextract.wayland.server.server_h.WL_POINTER_AXIS_RELATIVE_DIRECTION_IDENTICAL;
import static jextract.wayland.server.server_h.WL_POINTER_AXIS_RELATIVE_DIRECTION_INVERTED;


public enum PointerAxisRelativeDirection {
    IDENTICAL(WL_POINTER_AXIS_RELATIVE_DIRECTION_IDENTICAL()),
    INVERTED(WL_POINTER_AXIS_RELATIVE_DIRECTION_INVERTED());


    public final int value;


    PointerAxisRelativeDirection(int value) {
        this.value = value;
    }


    public static PointerAxisRelativeDirection of(int value) {
        for (var e : values()) {
            if (e.value == value)
                return e;
        }
        throw new RuntimeException("Invalid enum value from C code for wl_pointer_axis_relative_direction");
    }

}