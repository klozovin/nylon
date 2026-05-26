package wlroots.types.input;

import jextract.wlroots.types.wlr_pointer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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


    @Override
    public boolean equals(@Nullable Object obj) {
        return switch (obj) {
            case null -> false;
            case Pointer pt -> pointerPtr.equals(pt.pointerPtr);
            default -> throw new RuntimeException("BUG: Trying to compare objects of different types");
        };
    }


    /// Get a {@link Pointer} from an {@link InputDevice}, asserting that the input device is a pointer.
    public static Pointer fromInputDevice(InputDevice inputDevice) {
        return new Pointer(wlr_pointer_from_input_device(inputDevice.inputDevicePtr));
    }


    // *** Getters and setters **************************************************************************** //


    /**
     * @return wlr_pointer.base
     */
    public InputDevice getBase() {
        return new InputDevice(wlr_pointer.base(pointerPtr));
    }


    /*
        Events to implement

        struct {
            struct wl_signal motion; // struct wlr_pointer_motion_event
            struct wl_signal motion_absolute; // struct wlr_pointer_motion_absolute_event
            struct wl_signal button; // struct wlr_pointer_button_event
            struct wl_signal axis; // struct wlr_pointer_axis_event
            struct wl_signal frame;

            struct wl_signal swipe_begin; // struct wlr_pointer_swipe_begin_event
            struct wl_signal swipe_update; // struct wlr_pointer_swipe_update_event
            struct wl_signal swipe_end; // struct wlr_pointer_swipe_end_event

            struct wl_signal pinch_begin; // struct wlr_pointer_pinch_begin_eve                                                                                                                                                                                                          hhhlhlhlhnt
            struct wl_signal pinch_update; // struct wlr_pointer_pinch_update_event
            struct wl_signal pinch_end; // struct wlr_pointer_pinch_end_event

            struct wl_signal hold_begin; // struct wlr_pointer_hold_begin_event
            struct wl_signal hold_end; // struct wlr_pointer_hold_end_event
	    } events;
     */
}