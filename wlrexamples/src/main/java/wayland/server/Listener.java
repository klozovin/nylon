package wayland.server;

import wayland.wl_listener;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

// TODO: Maybe don't make end user use this class?
public class Listener {

    public final MemorySegment listenerPtr;

    private Listener(MemorySegment listenerPtr) {
        this.listenerPtr = listenerPtr;
    }

    public void setNotify() {

        // TODO: new_output event signature

    }

    public static Listener create(Arena arena) {
        var listenerPtr = wl_listener.allocate(arena);
        return new Listener(listenerPtr);
    }
}
