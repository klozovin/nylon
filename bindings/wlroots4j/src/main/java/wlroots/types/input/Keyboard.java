package wlroots.types.input;

import jextract.wlroots.types.wlr_keyboard;
import org.jspecify.annotations.NullMarked;
import wayland.server.Signal;
import wayland.server.Signal.Signal1;
import wlroots.types.KeyboardModifiers;
import xkbcommon.Keymap;
import xkbcommon.XkbState;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

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


    @Override
    public boolean equals(Object other) {
        return switch (other) {
            case Keyboard otherKeyboard -> keyboardPtr.equals(otherKeyboard.keyboardPtr);
            case null -> false;
            default -> throw new RuntimeException("BUG: Trying to compare objects of different types");
        };
    }


    @Override
    public int hashCode() {
        return keyboardPtr.hashCode();
    }


    ///  Get a {@link Keyboard} from {@link InputDevice}, asserting that the input device is a keyboard.
    public static Keyboard fromInputDevice(InputDevice inputDevice) {
        return new Keyboard(wlr_keyboard_from_input_device(inputDevice.inputDevicePtr));
    }


    // *** Fields ***************************************************************************************** //


    public XkbState xkbState() {
        return new XkbState(wlr_keyboard.xkb_state(keyboardPtr));
    }


    public KeyboardModifiers modifiers() {
        return new KeyboardModifiers(wlr_keyboard.modifiers(keyboardPtr));
    }


    public void keycodes() {
        // TODO: Test this pointer shenanigans
        var ptr = keycodesPtr();
        var keycodesElementLayout = wlr_keyboard.keycodes$layout().elementLayout();
        var slice = ptr.asSlice(0, keycodesElementLayout.byteSize() * keycodesNum());

        slice.getAtIndex(ValueLayout.JAVA_INT, 0);
//        slice.getAtIndex(wlr_keyboard.keycodes$layout().elementLayout(), );

    }


    public MemorySegment keycodesPtr() {
        return wlr_keyboard.keycodes(keyboardPtr);
    }


    public long keycodesNum() {
        return wlr_keyboard.num_keycodes(keyboardPtr);
    }


    // *** Methods **************************************************************************************** //


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


    // *** Modifiers *** //


    // TODO: Move to enum, and enumset (benchmark), move out from Keyboard class
    public final static class Modifiers {
        private final int modifiers;


        public Modifiers(int modifiers) {
            this.modifiers = modifiers;
        }


        public boolean containsAlt() {
            return (modifiers & WLR_MODIFIER_ALT()) != 0;
        }


        public boolean containsMod2() {
            return (modifiers & WLR_MODIFIER_MOD2()) != 0;
        }


        public boolean containsMod3() {
            return (modifiers & WLR_MODIFIER_MOD3()) != 0;
        }


        public boolean containsLogo() {
            return (modifiers & WLR_MODIFIER_LOGO()) != 0;
        }


        public boolean containsMod5() {
            return (modifiers & WLR_MODIFIER_MOD5()) != 0;
        }
    }


    // *** Events ***************************************************************************************** //


    public final static class Events {
        /// Raised when a key has been pressed or released on the keyboard. Emitted before the xkb state of
        /// the keyboard has been updated (including modifiers).
        public final Signal1<KeyboardKeyEvent> key;

        /// Raised when the modifier state of the {@link Keyboard} has been updated. At this time, you can
        /// read the modifier state of the struct wlr_keyboard and handle the updated state by sending it to
        /// clients.
        public final Signal1<Keyboard> modifiers;


        public Events(MemorySegment ptr) {
            assert !ptr.equals(NULL);
            key       = Signal.of(wlr_keyboard.events.key(ptr), KeyboardKeyEvent::new);
            modifiers = Signal.of(wlr_keyboard.events.modifiers(ptr), Keyboard::new);
        }
    }
}