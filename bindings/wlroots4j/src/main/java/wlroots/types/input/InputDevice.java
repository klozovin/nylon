package wlroots.types.input;

import jextract.wlroots.types.wlr_input_device;
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


    public enum Type {
        KEYBOARD(WLR_INPUT_DEVICE_KEYBOARD()),      // struct wlr_keyboard
        POINTER(WLR_INPUT_DEVICE_POINTER()),        // struct wlr_pointer
        TOUCH(WLR_INPUT_DEVICE_TOUCH()),            // struct wlr_touch
        TABLET(WLR_INPUT_DEVICE_TABLET()),          // struct wlr_tablet
        TABLET_PAD(WLR_INPUT_DEVICE_TABLET_PAD()),  // struct wlr_tablet_pad
        SWITCH(WLR_INPUT_DEVICE_SWITCH());          // struct wlr_switch

        final int value;


        Type(int value) {
            this.value = value;
        }


        public static Type of(int value) {
            if (value == WLR_INPUT_DEVICE_KEYBOARD())   return KEYBOARD;
            if (value == WLR_INPUT_DEVICE_POINTER())    return POINTER;
            if (value == WLR_INPUT_DEVICE_TOUCH())      return TOUCH;
            if (value == WLR_INPUT_DEVICE_TABLET())     return TABLET;
            if (value == WLR_INPUT_DEVICE_TABLET_PAD()) return TABLET_PAD;
            if (value == WLR_INPUT_DEVICE_SWITCH())     return SWITCH;

            throw new RuntimeException("Invalid enum value from C code for wlr_input_device_type");
        }
    }


    // *** Events ***************************************************************************************** //


    public final static class Events {
        public final  Signal1<InputDevice> destroy;


        public Events(MemorySegment eventsPtr) {
            assert !eventsPtr.equals(NULL);
            this.destroy = Signal.of(wlr_input_device.events.destroy(eventsPtr), InputDevice::new);
        }
    }
}