package wayland.server;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import wayland.wl_list;
import wayland.wl_listener;
import wayland.wl_notify_func_t;
import wayland.wl_signal;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

import static wayland.server_h.wl_list_insert;

/// A source of a type of observable event.
///
/// Signals are recognized points where significant events can be observed. Compositors as well as
/// the server can provide signals. Observers are wl_listener's that are added through
/// wl_signal_add. Signals are emitted using wl_signal_emit, which will invoke all listeners until
/// that listener is removed by wl_list_remove() (or whenever the signal is destroyed).
public class Signal<T> {
    public final @NonNull MemorySegment signalPtr;
    public final @NonNull Constructor<T> callbackParamCtor;

    public Signal(@NonNull MemorySegment signalPtr, @NotNull Constructor<T> callbackParamCtor) {
        this.signalPtr = signalPtr;
        this.callbackParamCtor = callbackParamCtor;
    }

    /// Add the specified listener to this signal.
    ///
    /// ```
    /// static inline void
    /// wl_signal_add(struct wl_signal *signal,
    ///               struct wl_listener *listener)
    ///```
    ///
    /// @param callback Callback function
    public void add(Consumer<T> callback) {
        var notifyFunction = new wl_notify_func_t.Function() {
            @Override
            public void apply(MemorySegment listener, MemorySegment data) {
                try {
                    T callbackArgument = callbackParamCtor.newInstance(data);
                    callback.accept(callbackArgument);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        // TODO: Memory ownership - can't be auto or confined/scope
        var arena = Arena.global();

        // Create wl_listener object and associate it with callback function
        var notifyFuncPtr = wl_notify_func_t.allocate(notifyFunction, arena);
        var listenerPtr = wl_listener.allocate(arena);
        wl_listener.notify(listenerPtr, notifyFuncPtr);

        // TODO: Move to wl_list implementation, keep inline for now
        // Add listener to signal (append at the end)
        var listLastElementPtr = wl_list.prev(wl_signal.listener_list(signalPtr)); // `prev` is last element in list
        var listenerLinkPtr = wl_listener.link(listenerPtr);
        wl_list_insert(listLastElementPtr, listenerLinkPtr);
    }
}
