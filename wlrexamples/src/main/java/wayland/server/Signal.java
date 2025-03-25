package wayland.server;

import wayland.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.util.function.Consumer;

public class Signal<T> {
    // TODO: Interface instead of an abstract class?

    public Consumer<T> callback;
    public final MemorySegment signalPtr;
    public final Constructor<T> callbackParamCtor;

    public Signal(MemorySegment signalPtr, Constructor<T> callbackParamCtor) {
        this.signalPtr = signalPtr;
        this.callbackParamCtor = callbackParamCtor;
    }

    /**
     * wayland-server-core.h/wl_signal_add
     *
     * @param callback Callback function
     */
    public void add(Consumer<T> callback) {
        // TODO: Memory ownership

        this.callback = callback;

        var listener = wl_listener.allocate(Arena.global());
        var notifyFunc = wl_notify_func_t.allocate(this::funfun, Arena.global());
        wl_listener.notify(listener, notifyFunc);

        // TODO: Move to wl_list implementation, keep inline for now
        var previous = wl_list.prev(wl_signal.listener_list(signalPtr));
        var link = wl_listener.link(listener);
        server_h.wl_list_insert(previous, link);
    }

    public void funfun(MemorySegment listener, MemorySegment data) {
        try {
            T xyz = this.callbackParamCtor.newInstance(data);
            this.callback.accept(xyz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Not a direct C bindings, used to release memory allocated for a signal. Just a wrapper for wl_list_remove.
     */
    public void remove() {
        // memory cleanup here?
    }
}
