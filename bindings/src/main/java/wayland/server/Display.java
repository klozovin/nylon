package wayland.server;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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


    public @Nullable String addSocketAuto() {
        var socketPtr = wl_display_add_socket_auto(displayPtr);
        return !socketPtr.equals(NULL) ? socketPtr.getString(0) : null;
    }


    public void terminate() {
        wl_display_terminate(displayPtr);
    }


    /// Destroy all clients connected to the display.
    ///
    /// This function should be called right before {@link #destroy()} to ensure all client
    /// resources are closed properly. Destroying a client from within is safe, but creating one
    ///  will leak resources and raise a warning.
    public void destroyClients() {
        wl_display_destroy_clients(displayPtr);
    }


    /// Destroy Wayland display object.
    ///
    /// This function emits the wl_display destroy signal, releases all the sockets added to this
    /// display, free's all the globals associated with this display, free's memory of additional
    /// shared memory formats and destroy the display object.
    public void destroy() {
        wl_display_destroy(displayPtr);
    }
}