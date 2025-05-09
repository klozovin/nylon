package wayland;

import static jextract.wayland.server.server_h.WL_POINTER_AXIS_HORIZONTAL_SCROLL;
import static jextract.wayland.server.server_h.WL_POINTER_AXIS_VERTICAL_SCROLL;


public enum PointerAxis {
    VERTICAL_SCROLL(WL_POINTER_AXIS_VERTICAL_SCROLL()),
    HORIZONTAL_SCROLL(WL_POINTER_AXIS_HORIZONTAL_SCROLL());


    public final int value;


    PointerAxis(int value) {
        this.value = value;
    }


    public static PointerAxis of(int value) {
        for (var e : values()) {
            if (e.value == value)
                return e;
        }
        throw new RuntimeException("Invalid enum value from C code for wl_pointer_axis");
    }
}