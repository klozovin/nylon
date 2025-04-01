package wayland.util;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;

public class Utils {
    /// Retrieves a pointer to a containing struct, given a member name. (intrusive lists)
    public static MemorySegment containerOf(MemorySegment memberPtr, StructLayout layout, String name) {

    }
}
