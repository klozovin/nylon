package wlroots.types;

import jextract.wlroots.types.wlr_input_device;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import wayland.server.Signal;
import wayland.server.Signal.Signal1;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_input_device_h.*;
import static jextract.wlroots.types.wlr_keyboard_h.wlr_keyboard_from_input_device;


@NullMarked
public final class InputDevice {
    public final MemorySegment inputDevicePtr;
    public final Events events;


    public InputDevice(MemorySegment inputDevicePtr) {
        assert !inputDevicePtr.equals(NULL);
        this.inputDevicePtr = inputDevicePtr;
        this.events = new Events(wlr_input_device.events(inputDevicePtr));
    }


    // *** Fields ***************************************************************************************** //


    public Type type() {
        return Type.of(wlr_input_device.type(inputDevicePtr));
    }


    // *** Methods *** //


    public  Keyboard keyboardFromInputDevice() {
        return new Keyboard(wlr_keyboard_from_input_device(inputDevicePtr));
    }


    public enum Type {
        KEYBOARD(WLR_INPUT_DEVICE_KEYBOARD()),      // struct wlr_keyboard
        POINTER(WLR_INPUT_DEVICE_POINTER()),        // struct wlr_pointer
        TOUCH(WLR_INPUT_DEVICE_TOUCH()),            // struct wlr_touch
        TABLET(WLR_INPUT_DEVICE_TABLET()),          // struct wlr_tablet
        TABLET_PAD(WLR_INPUT_DEVICE_TABLET_PAD()),  // struct wlr_tablet_pad
        SWITCH(WLR_INPUT_DEVICE_SWITCH());          // struct wlr_switch

        public final int value;


        Type(int constant) {
            this.value = constant;
        }


        public static Type of(int value) {
            for (var e : values())
                if (e.value == value)
                    return e;
            throw new RuntimeException("Invalid enum value from C code");
        }
    }


    // *** Events *** //

    public final static class Events {
        public final @NonNull Signal1<InputDevice> destroy;


        public Events(@NonNull MemorySegment eventsPtr) {
            assert !eventsPtr.equals(NULL);
            this.destroy = Signal.of(wlr_input_device.events.destroy(eventsPtr), InputDevice::new);
        }
    }
}