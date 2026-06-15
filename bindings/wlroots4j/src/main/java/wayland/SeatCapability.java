package wayland;

import java.util.EnumSet;

import static jextract.wayland.wl.*;


public enum SeatCapability {
    Pointer(WL_SEAT_CAPABILITY_POINTER()),
    Keyboard(WL_SEAT_CAPABILITY_KEYBOARD()),
    Touch(WL_SEAT_CAPABILITY_TOUCH());

    public final int value;


    SeatCapability(int value) {
        this.value = value;
    }


    public static int toBitset(EnumSet<SeatCapability> capabilities) {
        // TODO: Don't use streaming ^)
        //        int bitfield = 0;
//        for (var cap : capabilities)
//            bitfield |= cap.value;
        return capabilities
            .stream()
            .mapToInt(x -> x.value)
            .reduce(0, (x, y) -> x | y);
    }


    /// Create an `EnumSet<SeatCapabilities>` from the C bitmask
    public static EnumSet<SeatCapability> fromBitset(int bitset) {
        assert bitset >= 0;
        var capabilities = EnumSet.noneOf(SeatCapability.class);
        for (var capability : values())
            if ((capability.value & bitset) != 0)
                capabilities.add(capability);
        return capabilities;
    }
}