package wlroots.types.input;

import jextract.wlroots.types.wlr_pointer;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_pointer_h.wlr_pointer_from_input_device;


@NullMarked
public class Pointer {
    public final MemorySegment pointerPtr;


    public Pointer(MemorySegment pointerPtr) {
        assert !pointerPtr.equals(NULL);
        this.pointerPtr = pointerPtr;
    }


    ///  Get a {@link Pointer} from an {@link InputDevice}, asserting that the input device is a pointer.
    public static Pointer fromInputDevice(InputDevice inputDevice) {
        return new Pointer(wlr_pointer_from_input_device(inputDevice.inputDevicePtr));
    }


    // *** Fields ***************************************************************************************** //


    public InputDevice base() {
        return new InputDevice(wlr_pointer.base(pointerPtr));
    }
}