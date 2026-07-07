package wlroots.types.xdg_shell;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;


@NullMarked
public class XdgPopupConfigure {
    public final MemorySegment xdgPopupConfigurePtr;


    public XdgPopupConfigure(MemorySegment xdgPopupConfigurePtr) {
       assert !xdgPopupConfigurePtr.equals(MemorySegment.NULL);
       this.xdgPopupConfigurePtr = xdgPopupConfigurePtr;
    }
}