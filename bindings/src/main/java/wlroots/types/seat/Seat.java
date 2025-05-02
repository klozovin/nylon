package wlroots.types.seat;

import jextract.wlroots.types.wlr_seat;
import org.jspecify.annotations.NullMarked;
import wayland.KeyboardKeyState;
import wayland.SeatCapability;
import wayland.server.Display;
import wayland.server.Signal;
import wayland.server.Signal.Signal1;
import wlroots.types.Keyboard;
import wlroots.types.KeyboardModifiers;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.EnumSet;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_xdg_shell_h.*;


@NullMarked
public class Seat {
    public final MemorySegment seatPtr;
    public final Events events;


    public Seat(MemorySegment seatPtr) {
        assert !seatPtr.equals(NULL);
        this.seatPtr = seatPtr;
        this.events = new Events(wlr_seat.events(seatPtr));
    }


    /// Allocates a new struct wlr_seat and adds a wl_seat global to the display.
    public static Seat create(Display display, String name) {
        try (var arena = Arena.ofConfined()) {
            return new Seat(wlr_seat_create(display.displayPtr, arena.allocateFrom(name)));
        }
    }


    /// Set this keyboard as the active keyboard for the seat.
    public void setKeyboard(Keyboard keyboard) {
        wlr_seat_set_keyboard(seatPtr, keyboard.keyboardPtr);
    }


    /// Notify the seat that a key has been pressed on the keyboard. Defers to any keyboard grabs.
    ///
    /// @param time in milliseconds
    public void keyboardNotifyKey(int time, int key, KeyboardKeyState state) {
        wlr_seat_keyboard_notify_key(seatPtr, time, key, state.value);
    }


    /// Notify the seat that the modifiers for the keyboard have changed. Defers to any keyboard grabs.
    public void keyboardNotifyModifiers(KeyboardModifiers modifiers) {
        wlr_seat_keyboard_notify_modifiers(seatPtr, modifiers.keyboardModifiersPtr);
    }


    public void setCapabilities(EnumSet<SeatCapability> capabilities) {
        wlr_seat_set_capabilities(seatPtr, SeatCapability.setToBitfield(capabilities));
    }


    public static class Events {
        private final MemorySegment eventsPtr;

        /// Raised when a client provides a cursor image.
        public final Signal1<PointerRequestSetCursorEvent> requestSetCursor;

        public final Signal1<RequestSetSelectionEvent> requestSetSelection;


        public Events(MemorySegment eventsPtr) {
            this.eventsPtr = eventsPtr;
            this.requestSetCursor    = Signal.of(wlr_seat.events.request_set_cursor(eventsPtr),    PointerRequestSetCursorEvent::new);
            this.requestSetSelection = Signal.of(wlr_seat.events.request_set_selection(eventsPtr), RequestSetSelectionEvent::new);
        }
    }
}