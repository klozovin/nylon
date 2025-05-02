package wlroots.types;

import jextract.wlroots.types.wlr_keyboard;
import org.jspecify.annotations.NullMarked;
import wayland.server.Signal;
import wayland.server.Signal.Signal1;
import xkbcommon.Keymap;
import xkbcommon.XkbState;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_keyboard_h.*;


@NullMarked
public class Keyboard {
    public final MemorySegment keyboardPtr;
    public final InputDevice base;
    public final Events events;


    public Keyboard(MemorySegment keyboardPtr) {
        assert !keyboardPtr.equals(NULL);
        this.keyboardPtr = keyboardPtr;
        this.base = new InputDevice(wlr_keyboard.base(keyboardPtr));
        this.events = new Events(wlr_keyboard.events(keyboardPtr));
    }


    public XkbState xkbState() {
        return new XkbState(wlr_keyboard.xkb_state(keyboardPtr));
    }


    public KeyboardModifiers modifiers() {
        return new KeyboardModifiers(wlr_keyboard.modifiers(keyboardPtr));
    }


    public boolean setKeymap(Keymap keymap) {
        return wlr_keyboard_set_keymap(keyboardPtr, keymap.keymapPtr);
    }


    /// Get the set of currently depressed or latched modifiers.
    public Modifiers getModifiers() {
        return new Modifiers(wlr_keyboard_get_modifiers(keyboardPtr));
    }


    /// Set the keyboard repeat info.
    ///
    /// @param rateHz  Key repeats per second
    /// @param delayMs Delay in milliseconds
    public void setRepeatInfo(int rateHz, int delayMs) {
        wlr_keyboard_set_repeat_info(keyboardPtr, rateHz, delayMs);
    }


    public final static class Events {
        public final MemorySegment eventsPtr;

        /// Raised when a key has been pressed or released on the keyboard. Emitted before the xkb state of
        /// the keyboard has been updated (including modifiers).
        public final Signal1<KeyboardKeyEvent> key;

        /// Raised when the modifier state of the {@link Keyboard} has been updated. At this time, you can
        /// read the modifier state of the struct wlr_keyboard and handle the updated state by sending it to
        /// clients.
        public final Signal.Signal0 modifiers;


        public Events(MemorySegment eventsPtr) {
            this.eventsPtr = eventsPtr;

            this.key       = Signal.of(wlr_keyboard.events.key(eventsPtr), KeyboardKeyEvent::new);
            this.modifiers = Signal.of(wlr_keyboard.events.modifiers(eventsPtr));
        }
    }

    public final static class Modifiers {
        private final int modifiers;


        public Modifiers(int modifiers) {
            this.modifiers = modifiers;
        }


        public boolean isAltDown() {
            return (modifiers & WLR_MODIFIER_ALT()) != 0;
        }
    }
}