package wayland.server;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wayland.server.server_core_h.*;


/// Represents a connection to the compositor and acts as a proxy to the wl_display singleton object.
@NullMarked
public class Display {
    public final MemorySegment displayPtr;


    private Display(MemorySegment displayPtr) {
        assert !displayPtr.equals(NULL);
        this.displayPtr = displayPtr;
    }


    public static Display create() {
        return new Display(wl_display_create());
    }


    public void run() {
        wl_display_run(displayPtr);
    }


    public EventLoop getEventLoop() {
        return new EventLoop(wl_display_get_event_loop(displayPtr));
    }


    public void terminate() {
        wl_display_terminate(displayPtr);
    }


    public void destroy() {
        wl_display_destroy(displayPtr);
    }
}