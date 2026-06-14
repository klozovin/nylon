package wlroots.types.data_device;

import org.jspecify.annotations.NullMarked;
import wayland.server.Display;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.wlr.wlr_data_device_manager_create;


@NullMarked
public class DataDeviceManager {
    public final MemorySegment dataDeviceManagerPtr;


    public DataDeviceManager(MemorySegment dataDeviceManagerPtr) {
        assert !dataDeviceManagerPtr.equals(NULL);
        this.dataDeviceManagerPtr = dataDeviceManagerPtr;
    }


    public static DataDeviceManager create(Display display) {
        return new DataDeviceManager(wlr_data_device_manager_create(display.displayPtr));
    }
}