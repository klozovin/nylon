package wlroots.types;

import jextract.wlroots.types.wlr_keyboard;
import org.jspecify.annotations.NullMarked;
import wayland.server.Signal;
import xkbcommon.Keymap;
import xkbcommon.XkbState;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_keyboard_h.wlr_keyboard_set_keymap;


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


    public boolean setKeymap(Keymap keymap) {
        return wlr_keyboard_set_keymap(keyboardPtr, keymap.keymapPtr);
    }


    public XkbState xkbState() {
        return new XkbState(wlr_keyboard.xkb_state(keyboardPtr));
    }


    @NullMarked
    public final static class Events {
        public final MemorySegment eventsPtr;
        public final Signal<KeyboardKeyEvent> key;


        public Events(MemorySegment eventsPtr) {
            this.eventsPtr = eventsPtr;
            this.key = new Signal<>(wlr_keyboard.events.key(eventsPtr), KeyboardKeyEvent::new);
        }
    }
}