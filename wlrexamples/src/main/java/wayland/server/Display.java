package wayland.server;

import org.jspecify.annotations.NonNull;

import java.lang.foreign.MemorySegment;

import static jexwayland.server_h.*;


public class Display {
    public final @NonNull MemorySegment displayPtr;

    private Display(@NonNull MemorySegment displayPtr) {
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