package wlroots.wlr.types;

import jexwlroots.types.wlr_keyboard;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import wayland.server.Signal;

import java.lang.foreign.MemorySegment;

import static jexwlroots.types.wlr_keyboard_h_1.wlr_keyboard_set_keymap;


public class Keyboard {
    public final @NonNull MemorySegment keyboardPtr;
    public final @NonNull Events events;


    public Keyboard(@NotNull MemorySegment keyboardPtr) {
        this.keyboardPtr = keyboardPtr;
        this.events = new Events(wlr_keyboard.events(keyboardPtr));
    }


    @Deprecated
    public boolean setKeymap(MemorySegment keymap) {
        return wlr_keyboard_set_keymap(keyboardPtr, keymap);
    }


    @Deprecated
    public MemorySegment getXkbState() {
        return wlr_keyboard.xkb_state(keyboardPtr);
    }


    public final static class Events {
        public final @NonNull MemorySegment eventsPtr;
        public final @NonNull Signal<KeyboardKeyEvent> key;


        public Events(@NotNull MemorySegment eventsPtr) {
            this.eventsPtr = eventsPtr;
            this.key = new Signal<>(wlr_keyboard.events.key(eventsPtr), KeyboardKeyEvent::new);
        }
    }
}