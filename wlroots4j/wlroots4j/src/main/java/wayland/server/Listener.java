package wayland.server;

import jextract.wayland.wl_listener;
import jextract.wayland.wl_notify_func_t;
import org.jspecify.annotations.NullMarked;
import wayland.util.List;
import wayland.util.List.ElementMetadata;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


/// ```
/// struct wl_listener {
///     struct wl_list      link;
///     wl_notify_func_t    notify;
/// };
/// ```
///
/// # Memory management
///
/// [Listener] can't use Arena's of type:
///
/// * Global - because it leaks memory. It may be acceptable in a short demo, but a long running compositor
/// can't do that.
/// * Automatic - because Listener objects may be GC collected before while the data structures on the native
/// wlroots side still live. One would have to make sure to store references to the Java objects to prevent
/// this from happening, making it error-prone and non-deterministic.
/// * Confined with resources - lifetimes are not scope based, but dynamic.
///
/// That means we have to use some sort of manual disposal of Arenas. First we have to call .remove() on the
/// list side, then close the Arena.
@NullMarked
public class Listener implements List.Element<Listener> {
    public final MemorySegment listenerPtr;
    public static ElementMetadata<Listener> listElementMeta = new ElementMetadata<>(Listener.class, wl_listener.layout(), "link");


    public Listener(MemorySegment listenerPtr) {
        assert !listenerPtr.equals(NULL);
        this.listenerPtr = listenerPtr;
    }


    @Override
    public boolean equals(Object obj) {
        return switch (obj) {
            case Listener l -> listenerPtr.equals(l.listenerPtr);
            default -> false;
        };
    }


    @Override
    public int hashCode() {
        return listenerPtr.hashCode();
    }


    public static Listener allocate(Arena arena, wl_notify_func_t.Function notify) {
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