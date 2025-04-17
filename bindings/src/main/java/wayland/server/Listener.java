package wayland.server;

import jextract.wayland.server.wl_listener;
import jextract.wayland.server.wl_notify_func_t;
import org.jspecify.annotations.NonNull;
import wayland.util.List;
import wayland.util.List.ElementMetadata;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;

/// ```
/// struct wl_listener {
///     struct wl_list link;
///     wl_notify_func_t notify;
///};
///```
public class Listener implements List.Element<Listener> {
    public static ElementMetadata<Listener> listElementMeta = new ElementMetadata<>(Listener.class, wl_listener.layout(), "link");
    public final @NonNull MemorySegment listenerPtr;


    private Listener(@NonNull MemorySegment listenerPtr) {
        assert !listenerPtr.equals(NULL);
        this.listenerPtr = listenerPtr;
    }


    public static @NonNull Listener allocate(@NonNull Arena arena, wl_notify_func_t.@NonNull Function notify) {
        var listenerPtr = wl_listener.allocate(arena);
        var notifyFunctionPtr = wl_notify_func_t.allocate(notify, arena);
        wl_listener.notify(listenerPtr, notifyFunctionPtr);
        return new Listener(listenerPtr);
    }


    @Override
    public MemorySegment getLinkMemberPtr() {
        var linkPtr = wl_listener.link(listenerPtr);
        assert linkPtr.equals(listenerPtr);
        return linkPtr;
    }
}