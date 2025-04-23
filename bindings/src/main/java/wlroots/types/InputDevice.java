package wlroots.types;

import jextract.wlroots.types.wlr_input_device;
import org.jspecify.annotations.NonNull;
import wayland.server.Signal;
import wayland.server.Signal.Signal0;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_input_device_h.*;
import static jextract.wlroots.types.wlr_keyboard_h.wlr_keyboard_from_input_device;


public final class InputDevice {
    public final @NonNull MemorySegment inputDevicePtr;
    public final @NonNull Events events;


    public InputDevice(@NonNull MemorySegment inputDevicePtr) {
        assert !inputDevicePtr.equals(NULL);
        this.inputDevicePtr = inputDevicePtr;
        this.events = new Events(wlr_input_device.events(inputDevicePtr));
    }


    public @NonNull Keyboard keyboardFromInputDevice() {
        return new Keyboard(wlr_keyboard_from_input_device(inputDevicePtr));
    }


    public @NonNull Type type() {
        var inputDeviceType = wlr_input_device.type(inputDevicePtr);
        for (var e : Type.values())
            if (e.idx == inputDeviceType)
                return e;
        throw new RuntimeException("Unreachable");
    }


    public enum Type {
        KEYBOARD(WLR_INPUT_DEVICE_KEYBOARD()),      // struct wlr_keyboard
        POINTER(WLR_INPUT_DEVICE_POINTER()),        // struct wlr_pointer
        TOUCH(WLR_INPUT_DEVICE_TOUCH()),            // struct wlr_touch
        TABLET(WLR_INPUT_DEVICE_TABLET()),          // struct wlr_tablet
        TABLET_PAD(WLR_INPUT_DEVICE_TABLET_PAD()),  // struct wlr_tablet_pad
        SWITCH(WLR_INPUT_DEVICE_SWITCH());          // struct wlr_switch

        public final int idx;


        Type(int constant) {
            this.idx = constant;
        }
    }

    public final static class Events {
        public final @NonNull MemorySegment eventsPtr;
        public final @NonNull Signal0 destroy;


        public Events(@NonNull MemorySegment eventsPtr) {
            assert !eventsPtr.equals(NULL);
            this.eventsPtr = eventsPtr;
            this.destroy = Signal.of(wlr_input_device.events.destroy(eventsPtr));
        }
    }
}