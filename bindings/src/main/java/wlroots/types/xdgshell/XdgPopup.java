package wlroots.types.xdgshell;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class XdgPopup {
    public final MemorySegment xdgPopupPtr;


    public XdgPopup(MemorySegment xdgPopupPtr) {
        assert !xdgPopupPtr.equals(NULL);
        this.xdgPopupPtr = xdgPopupPtr;
    }
}