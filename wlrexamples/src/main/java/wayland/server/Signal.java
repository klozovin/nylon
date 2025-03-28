package wayland.server;

import jexwayland.wl_notify_func_t;
import jexwayland.wl_signal;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import wayland.util.IList;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;


/// A source of a type of observable event.
///
/// Signals are recognized points where significant events can be observed. Compositors as well as
/// the server can provide signals. Observers are wl_listener's that are added through
/// wl_signal_add. Signals are emitted using wl_signal_emit, which will invoke all listeners until
/// that listener is removed by wl_list_remove() (or whenever the signal is destroyed).
///
/// @param <T> type of parameters that gets passed to signal callback function
public class Signal<T> {
    public final @NonNull MemorySegment signalPtr;
    public final @NonNull Constructor<T> callbackParamCtor;
    private final IList<Listener> listenerList;


    public Signal(@NonNull MemorySegment signalPtr, @NotNull Constructor<T> callbackParamCtor) {
        this.signalPtr = signalPtr;
        this.callbackParamCtor = callbackParamCtor;
        this.listenerList = new IList<Listener>() {
            @Override
            public @NonNull MemorySegment getLink() {
                return wl_signal.listener_list(signalPtr);
            }
        };
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

        // Create wl_listener object and associate it with callback function
        // TODO: Memory ownership - can't be auto or confined/scope
        var arena = Arena.global();
        var listener = Listener.allocate(arena, notifyFunction);
        listenerList.append(listener);
    }
}