package wlroots.types.pointer;

import jextract.wlroots.types.wlr_pointer;
import org.jspecify.annotations.NullMarked;
import wlroots.types.InputDevice;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class Pointer {
    public final MemorySegment pointerPtr;


    public Pointer(MemorySegment pointerPtr) {
        assert !pointerPtr.equals(NULL);
        this.pointerPtr = pointerPtr;
    }


    public InputDevice base() {
        return new InputDevice(wlr_pointer.base(pointerPtr));
    }
}