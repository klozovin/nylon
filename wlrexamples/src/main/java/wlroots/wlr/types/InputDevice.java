package wlroots.wlr.types;

import org.jspecify.annotations.NonNull;

import java.lang.foreign.MemorySegment;


public final class InputDevice {
    public final @NonNull MemorySegment inputDevicePtr;


    public InputDevice(@NonNull MemorySegment inputDevicePtr) {
        this.inputDevicePtr = inputDevicePtr;
    }
}