package wlroots.types.input;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


/// `struct wlr_touch {}`
@NullMarked
public final class Touch {
    final MemorySegment touchPtr;


    public Touch(MemorySegment touchPtr) {
        assert !touchPtr.equals(NULL);
        this.touchPtr = touchPtr;
    }


    // TODO: Events
    /*
    	struct {
		struct wl_signal up; // struct wlr_touch_up_event
		struct wl_signal down; // struct wlr_touch_down_event
		struct wl_signal motion; // struct wlr_touch_motion_event
		struct wl_signal cancel; // struct wlr_touch_cancel_event
		struct wl_signal frame;
	} events;
     */
}