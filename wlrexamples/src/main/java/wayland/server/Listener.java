package wayland.server;

import wayland.wl_listener;
import wayland.wl_notify_func_t;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

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
        // :fix:memory: who and when deallocates this?
        var listenerPtr = wl_listener.allocate(arena);
        return new Listener(listenerPtr);
    }

    public static <T> Listener create(Arena arena, Consumer<T> callback) {
        var listenerPtr = wl_listener.allocate(arena);
        var notifyFuncTPtr = wl_notify_func_t.allocate((wl_notify_func_t.Function) callback, arena);
        wl_listener.notify(listenerPtr, notifyFuncTPtr);
        var listener = new Listener(listenerPtr);
        listener.setNotify();
        return null;
    }
}
