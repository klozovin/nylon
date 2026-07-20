package wlroots.types.input;

import jextract.wlroots.wlr_input_device;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.Signal;
import wayland.server.Signal.Signal1;
import wlroots.types.keyboard.Keyboard;
import wlroots.types.pointer.Pointer;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public sealed class InputDevice permits Keyboard, Pointer {
    public final MemorySegment inputDevicePtr;
    public final Events events;


    public InputDevice(MemorySegment inputDevicePtr) {
        assert !inputDevicePtr.equals(NULL);
        this.inputDevicePtr = inputDevicePtr;
        this.events = new Events(wlr_input_device.events(inputDevicePtr));
    }


    /// Convenience method, not present in wlroots. Converts base InputDevice to a concrete implementation.
    public InputDevice toConcreteInputDevice() {
        return switch (getType()) {
            case Keyboard -> Keyboard.fromInputDevice(this);
            case Pointer -> Pointer.fromInputDevice(this);
            default -> throw new AssertionError("Not yet implemented");
        };
    }


    //
    // *** Getters and setters ***
    //

    public InputDeviceType getType() {
        return InputDeviceType.of(wlr_input_device.type(inputDevicePtr));
    }


    public @Nullable String getName() {
        var namePtr = wlr_input_device.name(inputDevicePtr);
        return !namePtr.equals(NULL) ? namePtr.getString(0) : null;
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