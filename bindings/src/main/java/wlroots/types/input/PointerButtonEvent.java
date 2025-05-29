package wlroots.types.input;

import jextract.wlroots.types.wlr_pointer_button_event;
import org.jspecify.annotations.NullMarked;
import wayland.PointerButtonState;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class PointerButtonEvent {
    public final MemorySegment pointerButtonEventPtr;


    public int timeMsec;
    public int button;
    public PointerButtonState state;


    public PointerButtonEvent(MemorySegment pointerButtonEventPtr) {
        assert !pointerButtonEventPtr.equals(NULL);
        this.pointerButtonEventPtr = pointerButtonEventPtr;

        this.timeMsec = wlr_pointer_button_event.time_msec(pointerButtonEventPtr);
        this.button = wlr_pointer_button_event.button(pointerButtonEventPtr);
        this.state = PointerButtonState.of(wlr_pointer_button_event.state(pointerButtonEventPtr));
    }
}