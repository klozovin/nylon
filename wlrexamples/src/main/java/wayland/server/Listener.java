package wayland.server;

import jexwayland.wl_listener;
import jexwayland.wl_notify_func_t;
import org.jspecify.annotations.NonNull;
import wayland.util.IList;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;


public class Listener implements IList<Listener> {
    public final @NonNull MemorySegment listenerPtr;


    private Listener(@NonNull MemorySegment listenerPtr) {
        this.listenerPtr = listenerPtr;
    }


    public static @NonNull Listener allocate(@NonNull Arena arena, wl_notify_func_t.@NonNull Function notify) {
        var listenerPtr = wl_listener.allocate(arena);
        var notifyFunctionPtr = wl_notify_func_t.allocate(notify, arena);
        wl_listener.notify(listenerPtr, notifyFunctionPtr);
        return new Listener(listenerPtr);
    }


    @Override
    public @NonNull MemorySegment getLink() {
        return wl_listener.link(listenerPtr);
    }
}