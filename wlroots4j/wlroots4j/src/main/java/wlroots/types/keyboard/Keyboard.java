package wlroots.types.keyboard;

import jextract.wlroots.wlr_input_device;
import jextract.wlroots.wlr_keyboard;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.Signal;
import wayland.server.Signal.Signal1;
import wlroots.types.input.InputDevice;
import xkbcommon.Keymap;
import xkbcommon.XkbState;

import java.lang.foreign.MemorySegment;
import java.util.EnumSet;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.wlr.*;


@NullMarked
public final class Keyboard extends InputDevice {
    public final MemorySegment keyboardPtr;
    public final Events events;


    public Keyboard(MemorySegment keyboardPtr) {
        assert !keyboardPtr.equals(NULL);

        super(wlr_keyboard.base(keyboardPtr));
        this.keyboardPtr = keyboardPtr;
        this.events = new Events(wlr_keyboard.events(keyboardPtr));
    }


    @Override
    public boolean equals(@Nullable Object other) {
        return switch (other) {
            case null -> false;
            case Keyboard otherKeyboard -> keyboardPtr.equals(otherKeyboard.keyboardPtr);
            default -> throw new RuntimeException("BUG: Trying to compare objects of different types");
        };
    }


    @Override
    public int hashCode() {
        return keyboardPtr.hashCode();
    }


    /// Get a {@link Keyboard} from {@link InputDevice}, asserting that the input device is a keyboard.
    public static Keyboard fromInputDevice(InputDevice inputDevice) {
        return new Keyboard(wlr_keyboard_from_input_device(inputDevice.inputDevicePtr));
    }


    //
    // *** Fields ***
    //

    public XkbState getXkbState() {
        return new XkbState(wlr_keyboard.xkb_state(keyboardPtr));
    }


    /// `wlr_keyboard.modifiers: wlr_keyboard_modifiers`
    public KeyboardModifiers getModifiers() {
        return new KeyboardModifiers(wlr_keyboard.modifiers(keyboardPtr));
    }


    public int[] getKeycodes() {
        var numKeycodes = Math.toIntExact(getNumKeycodes());
        if (numKeycodes == 0) return new int[0];

        /*
        return switch (numKeycodes) {
            case 0 -> new int[0];
            case 1 -> {
                // hardcode
            }
            default -> {
             // loop, or stream
            }
        }
        */

        return wlr_keyboard.keycodes(keyboardPtr)
            .elements(wlr_keyboard.keycodes$layout().elementLayout())
            .limit(numKeycodes)
            .mapToInt(x -> x.get(C_INT, 0))
            .toArray();


        /*
        Left for benchmarking later
        var keycodesPtr = wlr_keyboard.keycodes(keyboardPtr);
        var keycodesElementLayout = wlr_keyboard.keycodes$layout().elementLayout();
        var keycodesSlice = keycodesPtr.asSlice(0, keycodesElementLayout.byteSize() * numKeycodes);
        var keycodes = new int[numKeycodes];
        for (int i = 0; i < numKeycodes; i++) {
            keycodes[i] = keycodesSlice.getAtIndex(C_INT, i);
        }
         */
    }


    // TODO: Delete
    public MemorySegment getKeycodesPtr() {
        return wlr_keyboard.keycodes(keyboardPtr);
    }


    /// `wlr_keyboard.num_keycodes: size_t`
    public long getNumKeycodes() {
        return wlr_keyboard.num_keycodes(keyboardPtr);
    }


    //
    // *** Methods ***
    //

    public boolean setKeymap(Keymap keymap) {
        return wlr_keyboard_set_keymap(keyboardPtr, keymap.keymapPtr);
    }


    /// Get the set of currently depressed or latched modifiers.
    ///
    /// `uint32_t wlr_keyboard_get_modifiers(struct wlr_keyboard *keyboard)`
    public EnumSet<KeyboardModifier> getKeyboardModifiers() {
        return KeyboardModifier.fromBitset(wlr_keyboard_get_modifiers(keyboardPtr));
    }


    /// Set the keyboard repeat info.
    ///
    /// @param rateHz  Key repeats per second
    /// @param delayMs Delay in milliseconds
    public void setRepeatInfo(int rateHz, int delayMs) {
        wlr_keyboard_set_repeat_info(keyboardPtr, rateHz, delayMs);
    }


    //
    // *** Modifiers ***
    //

    // TODO: Delete, maybe move into benchmark
    public final static class Modifiers {
        private final int modifiers;


        public Modifiers(int modifiers) {
            this.modifiers = modifiers;
        }


        public boolean containsCtrlAltShift() {
            var mask = WLR_MODIFIER_CTRL() | WLR_MODIFIER_ALT() | WLR_MODIFIER_SHIFT();
            return (modifiers & mask) != 0;
        }


        public boolean containsCtrl() {
            return (modifiers & WLR_MODIFIER_CTRL()) != 0;
        }


        public boolean containsAlt() {
            return (modifiers & WLR_MODIFIER_ALT()) != 0;
        }


        public boolean containsShift() {
            return (modifiers & WLR_MODIFIER_SHIFT()) != 0;
        }
    }


    //
    // *** Events ***
    //

    public final class Events extends InputDevice.Events {
        /// Raised when a key has been pressed or released on the keyboard. Emitted before the xkb state of
        /// the keyboard has been updated (including modifiers).
        public final Signal1<KeyEvent> key;

        /// Raised when the modifier state of the {@link Keyboard} has been updated. At this time, you can
        /// read the modifier state of the struct wlr_keyboard and handle the updated state by sending it to
        /// clients.
        public final Signal1<Keyboard> modifiers;


        Events(MemorySegment ptr) {
            assert !ptr.equals(NULL);

            super(wlr_input_device.events(inputDevicePtr));
            key       = Signal.of(wlr_keyboard.events.key(ptr), KeyEvent::new);
            modifiers = Signal.of(wlr_keyboard.events.modifiers(ptr), Keyboard::new);
        }
    }
}