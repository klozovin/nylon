package wlroots.types.seat;

import jextract.wlroots.types.wlr_seat_pointer_button;
import jextract.wlroots.types.wlr_seat_pointer_state;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wlroots.types.compositor.Surface;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_pointer_h.WLR_POINTER_BUTTONS_CAP;


@NullMarked
public class PointerState {
    public final MemorySegment pointerStatePtr;


    public PointerState(MemorySegment pointerStatePtr) {
        assert !pointerStatePtr.equals(NULL);
        this.pointerStatePtr = pointerStatePtr;
    }


    //
    // *** Getters and setters ***
    //

    public @Nullable SeatClient getFocusedClient() {
        var ptr = wlr_seat_pointer_state.focused_client(pointerStatePtr);
        return !ptr.equals(NULL) ? new SeatClient(ptr) : null;
    }


    // TODO: Is this nullable?
    public @Nullable Surface getFocusedSurface() {
        return Surface.ofPtrOrNull(wlr_seat_pointer_state.focused_surface(pointerStatePtr));
    }


    public PointerButton[] getButtons() {
        var buttons = new PointerButton[WLR_POINTER_BUTTONS_CAP()];
        var buttonsArrayPtr = wlr_seat_pointer_state.buttons(pointerStatePtr);
        var buttonByteSize = wlr_seat_pointer_button.layout().byteSize();
        for (var i = 0; i < buttons.length; i++) {

            buttons[i] = new PointerButton(buttonsArrayPtr.asSlice(i * buttonByteSize));
        }
        return buttons;
    }


    public long getButtonCount() {
        return wlr_seat_pointer_state.button_count(pointerStatePtr);
    }
}