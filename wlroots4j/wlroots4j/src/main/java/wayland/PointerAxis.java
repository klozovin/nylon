package wayland;

import static jextract.wayland.wl.WL_POINTER_AXIS_HORIZONTAL_SCROLL;
import static jextract.wayland.wl.WL_POINTER_AXIS_VERTICAL_SCROLL;


public enum PointerAxis {
    VerticalScroll(WL_POINTER_AXIS_VERTICAL_SCROLL()),
    HorizontalScroll(WL_POINTER_AXIS_HORIZONTAL_SCROLL());


    public final int value;


    PointerAxis(int value) {
        this.value = value;
    }


    public static PointerAxis of(int value) {
        if (value == WL_POINTER_AXIS_VERTICAL_SCROLL())   return VerticalScroll;
        if (value == WL_POINTER_AXIS_HORIZONTAL_SCROLL()) return HorizontalScroll;

        throw new RuntimeException("Invalid enum value from C code for wl_pointer_axis");
    }
}