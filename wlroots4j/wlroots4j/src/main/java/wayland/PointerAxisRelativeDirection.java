package wayland;

import static jextract.wayland.wl.WL_POINTER_AXIS_RELATIVE_DIRECTION_IDENTICAL;
import static jextract.wayland.wl.WL_POINTER_AXIS_RELATIVE_DIRECTION_INVERTED;


public enum PointerAxisRelativeDirection {
    Identical(WL_POINTER_AXIS_RELATIVE_DIRECTION_IDENTICAL()),
    Inverted(WL_POINTER_AXIS_RELATIVE_DIRECTION_INVERTED());

    public final int value;


    PointerAxisRelativeDirection(int value) {
        this.value = value;
    }


    public static PointerAxisRelativeDirection of(int value) {
        if (value == WL_POINTER_AXIS_RELATIVE_DIRECTION_IDENTICAL()) return Identical;
        if (value == WL_POINTER_AXIS_RELATIVE_DIRECTION_INVERTED()) return Inverted;

        throw new RuntimeException("Invalid enum value from C code for wl_pointer_axis_relative_direction");
    }
}