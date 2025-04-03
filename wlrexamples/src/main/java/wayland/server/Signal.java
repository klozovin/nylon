package wayland.server;

import jexwayland.wl_signal;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import wayland.util.IList;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;
import java.util.function.Function;


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
    public final @NonNull IList<Listener> listenerList;
    public final @NonNull Function<MemorySegment, T> callbackArgumentCtor;


    public Signal(@NotNull MemorySegment signalPtr, @NotNull Function<MemorySegment, T> callbackArgumentCtor) {
        this.signalPtr = signalPtr;
        this.callbackArgumentCtor = callbackArgumentCtor;
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
    /// @param callback Callback function to call when the signal is emitted
    public void add(@NonNull Consumer<T> callback) {
        // TODO: Memory ownership - can't be auto or confined/scope
        listenerList.append(Listener.allocate(Arena.global(), (MemorySegment _, MemorySegment data) -> {
            assert data != null;
            assert data != MemorySegment.NULL;
            callback.accept(callbackArgumentCtor.apply(data));
        }));
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
    public void add(@NonNull Runnable callback) {
        // TODO: Memory ownership - can't be auto or confined/scope
        listenerList.append(Listener.allocate(Arena.global(), (MemorySegment _, MemorySegment data) -> {
            assert data != null;
            assert data != MemorySegment.NULL;
            callback.run();
        }));
    }
}