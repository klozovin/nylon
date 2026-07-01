package wlroots.types.buffer;

import java.util.EnumSet;

import static jextract.wlroots.wlr.WLR_BUFFER_DATA_PTR_ACCESS_READ;
import static jextract.wlroots.wlr.WLR_BUFFER_DATA_PTR_ACCESS_WRITE;


/// `enum wlr_buffer_data_ptr_access_flag`
public enum DataAccessFlag {
    /// The buffer contents can be read back.
    Read(WLR_BUFFER_DATA_PTR_ACCESS_READ()),

    /// The buffer contents can be written to.
    Write(WLR_BUFFER_DATA_PTR_ACCESS_WRITE());

    public final int value;


    DataAccessFlag(int value) {
        this.value = value;
    }


    public static DataAccessFlag of(int value) {
        if (value == WLR_BUFFER_DATA_PTR_ACCESS_READ()) return Read;
        if (value == WLR_BUFFER_DATA_PTR_ACCESS_WRITE()) return Write;
        throw new RuntimeException("Invalid enum value from C code for wlr_buffer_data_ptr_access_flag");
    }


    public static EnumSet<DataAccessFlag> setFromBitmask(int flags) {
        var flagsSet = EnumSet.noneOf(DataAccessFlag.class);
        for (var e : values()) {
            if ((flags & e.value) != 0)
                flagsSet.add(e);
        }
        return flagsSet;
    }
}