package wayland.server;

import jexwayland.wl_signal;
import org.jspecify.annotations.NonNull;
import wayland.util.List;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.foreign.MemorySegment.NULL;


/// A source of a type of observable event.
///
/// Signals are recognized points where significant events can be observed. Compositors as well as
/// the server can provide signals. Observers are wl_listener's that are added through
/// wl_signal_add. Signals are emitted using wl_signal_emit, which will invoke all listeners until
/// that listener is removed by wl_list_remove() (or whenever the signal is destroyed).
///
/// ```c
/// struct wl_signal {
///     struct wl_list listener_list;
///};
///```
///
/// @param <T> type of parameters that gets passed to signal callback function
public class Signal<T> {
    public final @NonNull MemorySegment signalPtr;
    public final @NonNull List<Listener> listenerList;
    public final @NonNull Function<MemorySegment, T> callbackArgumentCtor;


    public Signal(@NonNull MemorySegment signalPtr, @NonNull Function<MemorySegment, T> callbackArgumentCtor) {
        this.signalPtr = signalPtr;
        this.callbackArgumentCtor = callbackArgumentCtor;
        this.listenerList = new List<>(wl_signal.listener_list(signalPtr), Listener.listElementMeta);
    }


    public Signal(@NonNull MemorySegment signalPtr) {
        this.signalPtr = signalPtr;
        this.callbackArgumentCtor = (MemorySegment _) -> null;
        this.listenerList = new List<>(wl_signal.listener_list(signalPtr), Listener.listElementMeta);
    }


    /// Add the specified listener to this signal.
    ///
    /// ```
    /// static inline void
    /// wl_signal_add(struct wl_signal *signal,
    ///               struct wl_listener *listener)
    ///```
    ///
    /// @param callback Callback function to call when the signal is emitted
    /// @return
    public @NonNull Listener add(@NonNull Consumer<T> callback) {
        // TODO: Memory ownership - can't be auto or confined/scope
        var listener = Listener.allocate(Arena.global(), (MemorySegment listenerPtr, MemorySegment dataPtr) -> {
            assert listenerPtr != null;
            assert !listenerPtr.equals(NULL);
            assert dataPtr != null;
            assert !dataPtr.equals(NULL);
            callback.accept(callbackArgumentCtor.apply(dataPtr));
        });
        listenerList.append(listener);
        return listener;
    }


    /// Add the specified listener to this signal. Callback with no passed argument.
    ///
    /// ```
    /// static inline void
    /// wl_signal_add(struct wl_signal *signal,
    ///               struct wl_listener *listener)
    ///```
    ///
    /// @param callback Callback function to call when the signal is emitted
    /// @return
    public @NonNull Listener add(@NonNull Runnable callback) {
        // TODO: Memory ownership - can't be auto or confined/scope
        var listener = Listener.allocate(Arena.global(), (MemorySegment listenerPtr, MemorySegment dataPtr) -> {
            assert listenerPtr != null;
            assert !listenerPtr.equals(NULL);
            assert dataPtr != null;
            assert !dataPtr.equals(NULL);
            callback.run();
        });
        listenerList.append(listener);
        return listener;
    }


    /// Convenience function to remove a listener from the signal. Does not existst in C code.
    public void remove(@NonNull Listener listener) {
        listenerList.remove(listener);
    }
}