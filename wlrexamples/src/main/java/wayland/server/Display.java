package wayland.server;

import wayland.server_h;

import java.lang.foreign.MemorySegment;

public class Display {

    public final MemorySegment displayPtr;

    private Display(MemorySegment displayPtr) {
        this.displayPtr = displayPtr;
    }

    public void run() {
        server_h.wl_display_run(displayPtr);
    }

    public void destroy() {
        server_h.wl_client_destroy(displayPtr);
    }

    public EventLoop getEventLoop() {
        return new EventLoop(server_h.wl_display_get_event_loop(displayPtr));
    }

    public static Display create() {
        return new Display(server_h.wl_display_create());
    }
}