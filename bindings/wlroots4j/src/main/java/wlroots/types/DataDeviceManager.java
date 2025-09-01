package wlroots.types;

import org.jspecify.annotations.NullMarked;
import wayland.server.Display;

import java.lang.foreign.MemorySegment;

import static jextract.wlroots.types.wlr_data_device_h.wlr_data_device_manager_create;


@NullMarked
public class DataDeviceManager {
    public final MemorySegment dataDeviceManagerPtr;


    public DataDeviceManager(MemorySegment dataDeviceManagerPtr) {
        this.dataDeviceManagerPtr = dataDeviceManagerPtr;
    }

    public static DataDeviceManager create(Display display) {
        return new DataDeviceManager(wlr_data_device_manager_create(display.displayPtr));
    }
}