package wlroots.types.seat;

import jextract.wlroots.wlr_seat_pointer_button;
import jextract.wlroots.wlr_seat_pointer_state;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.Signal;
import wayland.server.Signal.Signal1;
import wlroots.types.compositor.Surface;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.wlr.WLR_POINTER_BUTTONS_CAP;


@NullMarked
public class PointerState {
    public final MemorySegment pointerStatePtr;
    public final Events events;


    public PointerState(MemorySegment pointerStatePtr) {
        assert !pointerStatePtr.equals(NULL);
        this.pointerStatePtr = pointerStatePtr;
        this.events = new Events(wlr_seat_pointer_state.events(pointerStatePtr));
    }


    //
    // *** Field getters and setters ***
    //

    public @Nullable SeatClient getFocusedClient() {
        var ptr = wlr_seat_pointer_state.focused_client(pointerStatePtr);
        return !ptr.equals(NULL) ? new SeatClient(ptr) : null;
    }


    // TODO: Is this nullable?
    public @Nullable Surface getFocusedSurface() {
        return Surface.ofPtrOrNull(wlr_seat_pointer_state.focused_surface(pointerStatePtr));
    }


    /// @return `wlr_seat_pointer_state.buttons` field
    public PointerButton[] getButtons() {
        assert getButtonCount() <= WLR_POINTER_BUTTONS_CAP();

        // PERF: Maybe use regular for loop instead of streams
        return wlr_seat_pointer_state.buttons(pointerStatePtr)
            .elements(wlr_seat_pointer_button.layout())
            .limit(getButtonCount())
            .map(PointerButton::new)
            .toArray(PointerButton[]::new);
    }


    public long getButtonCount() {
        return wlr_seat_pointer_state.button_count(pointerStatePtr);
    }


    //
    // *** Events ***
    //

    public static class Events {
        /// Raised when the pointer focus is changed, including when the client is closed.
        public final Signal1<PointerFocusChangeEvent> focusChange;


        Events(MemorySegment ptr) {
            focusChange = Signal.of(wlr_seat_pointer_state.events.focus_change(ptr), PointerFocusChangeEvent::new);
        }
    }
}