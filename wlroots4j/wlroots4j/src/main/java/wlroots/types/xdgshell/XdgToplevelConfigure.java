package wlroots.types.xdgshell;

import jextract.wlroots.wlr;
import jextract.wlroots.wlr_xdg_toplevel_configure;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;
import java.util.EnumSet;

import static jextract.wlroots.wlr.WLR_XDG_TOPLEVEL_CONFIGURE_BOUNDS;
import static jextract.wlroots.wlr.WLR_XDG_TOPLEVEL_CONFIGURE_WM_CAPABILITIES;


/// `struct wlr_xdg_toplevel_configure {}`
@NullMarked
public class XdgToplevelConfigure {
    public final MemorySegment xdgToplevelConfigurePtr;


    public XdgToplevelConfigure(MemorySegment xdgToplevelConfigurePtr) {
        assert !xdgToplevelConfigurePtr.equals(MemorySegment.NULL);
        this.xdgToplevelConfigurePtr = xdgToplevelConfigurePtr;
    }


    public EnumSet<Field> getFields() {
        return Field.fromBitset(wlr_xdg_toplevel_configure.fields(xdgToplevelConfigurePtr));
    }


    //
    // *** Fields ***
    //

    /// Convenience function, not present in wlroots. Only valid when {@link #getFields()} has {@link Field#Bounds}
    ///
    /// @return Value of `wlr_xdg_toplevel_configure.bounds.width`
    public int getBoundsWidth() {
        assert getFields().contains(Field.Bounds);
        return wlr_xdg_toplevel_configure.bounds.width(wlr_xdg_toplevel_configure.bounds(xdgToplevelConfigurePtr));
    }


    /// Convenience function, not present in wlroots. Only valid when {@link #getFields()} has {@link Field#Bounds}
    ///
    /// @return Value of `wlr_xdg_toplevel_configure.bounds.height`
    public int getBoundsHeight() {
        assert getFields().contains(Field.Bounds);
        return wlr_xdg_toplevel_configure.bounds.height(wlr_xdg_toplevel_configure.bounds(xdgToplevelConfigurePtr));
    }


    /// ` enum wlr_xdg_toplevel_configure_field {}`
    public enum Field {
        Bounds(WLR_XDG_TOPLEVEL_CONFIGURE_BOUNDS()),
        WmCapabilities(WLR_XDG_TOPLEVEL_CONFIGURE_WM_CAPABILITIES());


        public final int value;


        Field(int value) {
            this.value = value;
        }


        public static Field of(int value) {
            if (value == WLR_XDG_TOPLEVEL_CONFIGURE_BOUNDS())          return Bounds;
            if (value == WLR_XDG_TOPLEVEL_CONFIGURE_WM_CAPABILITIES()) return WmCapabilities;

            throw new RuntimeException("Invalid enum value from C code for wlr_xdg_toplevel_configure_field");
        }


        public static EnumSet<Field> fromBitset(int bitset) {
            var set = EnumSet.noneOf(Field.class);

            if ((Bounds.value         & bitset) != 0) set.add(Bounds);
            if ((WmCapabilities.value & bitset) != 0) set.add(WmCapabilities);

            return set;
        }
    }
}