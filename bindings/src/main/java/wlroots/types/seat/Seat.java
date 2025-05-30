package wlroots.types.seat;

import jextract.wlroots.types.wlr_seat;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.*;
import wayland.server.Display;
import wayland.server.Signal;
import wayland.server.Signal.Signal1;
import wlroots.types.DataSource;
import wlroots.types.input.Keyboard;
import wlroots.types.KeyboardModifiers;
import wlroots.types.compositor.Surface;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.EnumSet;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_data_device_h.wlr_seat_set_selection;
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

    // *** Fields ***************************************************************************************** //


    public EnumSet<SeatCapability> capabilities() {
        return SeatCapability.fromBitset(wlr_seat.capabilities(seatPtr));
    }


    public PointerState pointerState() {
        return new PointerState(wlr_seat.pointer_state(seatPtr));
    }


    public KeyboardState keyboardState() {
        return new KeyboardState(wlr_seat.keyboard_state(seatPtr));
    }


    // *** Keyboard methods ******************************************************************************* //


    /// Set this keyboard as the active keyboard for the seat.
    public void setKeyboard(Keyboard keyboard) {
        wlr_seat_set_keyboard(seatPtr, keyboard.keyboardPtr);
    }


    /// @return Active keyboard for the seat
    public @Nullable Keyboard getKeyboard() {
        var keyboardPtr = wlr_seat_get_keyboard(seatPtr);
        return !keyboardPtr.equals(NULL) ? new Keyboard(keyboardPtr) : null;
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


    /// Notify the seat that the keyboard focus has changed and request it to be the focused surface for this
    /// keyboard. Defers to any current grab of the seat's keyboard.
    public void keyboardNotifyEnter(Surface surface, MemorySegment keycodes, long numKeycodes, KeyboardModifiers modifiers) {
        wlr_seat_keyboard_notify_enter(
            seatPtr,
            surface.surfacePtr,
            keycodes,
            numKeycodes,
            modifiers.keyboardModifiersPtr
        );
    }


    // *** Pointer **************************************************************************************** //


    /// Notify the seat of an axis event. Defers to any grab of the pointer.
    public void pointerNotifyAxis(int timeMsec,
                                  PointerAxis orientation,
                                  double value,
                                  int valueDiscrete,
                                  PointerAxisSource source,
                                  PointerAxisRelativeDirection relativeDirection) {
        wlr_seat_pointer_notify_axis(
            seatPtr,
            timeMsec,
            orientation.value, value,
            valueDiscrete,
            source.value,
            relativeDirection.value);
    }


    /// Notify the seat that a button has been pressed. Returns the serial of the button press or zero if no
    /// button press was sent. Defers to any grab of the pointer.
    public int pointerNotifyButton(int timeMsec, int button, PointerButtonState state) {
        return wlr_seat_pointer_notify_button(seatPtr, timeMsec, button, state.value);
    }


    /// Clear the focused surface for the pointer and leave all entered surfaces. This function does not
    /// respect pointer grabs: you probably want wlr_seat_pointer_notify_clear_focus() instead.
    public void pointerClearFocus() {
        wlr_seat_pointer_clear_focus(seatPtr);
    }


    /// Notify the seat of a pointer enter event to the given surface and request it to be the focused surface
    /// for the pointer. Pass surface-local coordinates where the enter event occurred. This will send a leave
    /// event to the currently focused surface. Defers to any grab of the pointer.
    public void pointerNotifyEnter(Surface surface, double sx, double sy) {
        wlr_seat_pointer_notify_enter(seatPtr, surface.surfacePtr, sx, sy);
    }


    /// Notify the seat of a frame event. Frame events are sent to end a group of events that logically belong
    /// together. Motion, button and axis events should all be followed by a frame event. Defers to any grab
    /// of the pointer.
    public void pointerNotifyFrame() {
        wlr_seat_pointer_notify_frame(seatPtr);
    }


    /// Notify the seat of motion over the given surface. Pass surface-local coordinates where the pointer
    /// motion occurred. Defers to any grab of the pointer.
    public void pointerNotifyMotion(int timeMsec, double sx, double sy) {
        wlr_seat_pointer_notify_motion(seatPtr, timeMsec, sx, sy);
    }


    public boolean validatePointerGrabSerial(Surface origin, int serial) {
        return wlr_seat_validate_pointer_grab_serial(seatPtr, origin.surfacePtr, serial);
    }


    // *** Other ****************************************************************************************** //


    public void setCapabilities(EnumSet<SeatCapability> capabilities) {
        wlr_seat_set_capabilities(seatPtr, SeatCapability.setToBitfield(capabilities));
    }


    /// Add {@link SeatCapability} to this seat. Convenience function not present in C code.
    public void addCapability(SeatCapability capability) {
        var capabilities = this.capabilities();
        capabilities.add(capability);
        this.setCapabilities(capabilities);
    }


    /// Sets the current selection for the seat. NULL can be provided to clear it. This removes the previous
    /// one if there was any. In case the selection doesn't come from a client, wl_display_next_serial() can
    /// be used to generate a serial.
    public void setSelection(@Nullable DataSource source, int serial) {
        wlr_seat_set_selection(
            seatPtr,
            switch (source) {
                case DataSource s -> s.dataSourcePtr;
                case null -> NULL;
            },
            serial);
    }


    // *** Events ***************************************************************************************** //


    public static class Events {
        /// Raised when a client provides a cursor image.
        public final Signal1<PointerRequestSetCursorEvent> requestSetCursor;
        public final Signal1<RequestSetSelectionEvent> requestSetSelection;


        public Events(MemorySegment ptr) {
            this.requestSetCursor    = Signal.of(wlr_seat.events.request_set_cursor(ptr), PointerRequestSetCursorEvent::new);
            this.requestSetSelection = Signal.of(wlr_seat.events.request_set_selection(ptr), RequestSetSelectionEvent::new);
        }
    }
}