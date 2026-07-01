package wlroots.types.input;

import jextract.wlroots.wlr_input_device;
import org.jspecify.annotations.NullMarked;
import wayland.server.Signal;
import wayland.server.Signal.Signal1;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public final class InputDevice {
    public final MemorySegment inputDevicePtr;
    public final Events events;


    public InputDevice(MemorySegment inputDevicePtr) {
        assert !inputDevicePtr.equals(NULL);
        this.inputDevicePtr = inputDevicePtr;
        this.events = new Events(wlr_input_device.events(inputDevicePtr));
    }


    //
    // *** Getters and setters ***
    //

    public InputDeviceType getType() {
        return InputDeviceType.of(wlr_input_device.type(inputDevicePtr));
    }


    //
    // *** Events ***
    //

    public static class Events {
        public final Signal1<InputDevice> destroy;


        public Events(MemorySegment eventsPtr) {
            assert !eventsPtr.equals(NULL);
            this.destroy = Signal.of(wlr_input_device.events.destroy(eventsPtr), InputDevice::new);
        }
    }
}