package wlroots.wlr.types;

import jexwlroots.types.wlr_input_device;
import org.jspecify.annotations.NonNull;
import wayland.server.Signal;

import java.lang.foreign.MemorySegment;

import static jexwlroots.types.wlr_input_device_h.*;
import static jexwlroots.types.wlr_keyboard_h_1.wlr_keyboard_from_input_device;


public final class InputDevice {
    public final @NonNull MemorySegment inputDevicePtr;
    public final @NonNull Events events;


    public InputDevice(@NonNull MemorySegment inputDevicePtr) {
        this.inputDevicePtr = inputDevicePtr;
        this.events = new Events(wlr_input_device.events(inputDevicePtr));
    }


    public @NonNull Keyboard getKeyboard() {
        return new Keyboard(wlr_keyboard_from_input_device(inputDevicePtr));
    }


    public @NonNull Type getType() {
        var inputDeviceType = wlr_input_device.type(inputDevicePtr);
        for (var enm : Type.values())
            if (enm.constant == inputDeviceType)
                return enm;
        throw new RuntimeException("Unreachable");
    }


    public enum Type {
        KEYBOARD(WLR_INPUT_DEVICE_KEYBOARD()),      // struct wlr_keyboard
        POINTER(WLR_INPUT_DEVICE_POINTER()),        // struct wlr_pointer
        TOUCH(WLR_INPUT_DEVICE_TOUCH()),            // struct wlr_touch
        TABLET(WLR_INPUT_DEVICE_TABLET()),          // struct wlr_tablet
        TABLET_PAD(WLR_INPUT_DEVICE_TABLET_PAD()),  // struct wlr_tablet_pad
        SWITCH(WLR_INPUT_DEVICE_SWITCH());          // struct wlr_switch

        private final int constant;


        Type(int i) {
            this.constant = i;
        }
    }

    public final static class Events {
        public final @NonNull MemorySegment eventsPtr;
        public final @NonNull Signal<Void> destroy;


        public Events(@NonNull MemorySegment eventsPtr) {
            this.eventsPtr = eventsPtr;
            this.destroy = new Signal<>(wlr_input_device.events.destroy(eventsPtr));
        }
    }
}