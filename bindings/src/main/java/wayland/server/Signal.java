package wayland.server;

import jextract.wayland.server.wl_signal;
import org.jspecify.annotations.NullMarked;
import wayland.util.List;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.foreign.MemorySegment.NULL;


/// A source of a type of observable event.
///
/// Signals are recognized points where significant events can be observed. Compositors as well as the server
/// can provide signals. Observers are wl_listener's that are added through wl_signal_add. Signals are emitted
/// using wl_signal_emit, which will invoke all listeners until that listener is removed by wl_list_remove()
/// (or whenever the signal is destroyed).
///
/// ```c
/// struct wl_signal {
///     struct wl_list listener_list;
///};
///```
@NullMarked
public abstract sealed class Signal {
    public final MemorySegment signalPtr;
    public final List<Listener> listenerList;


    public Signal(MemorySegment signalPtr) {
        this.signalPtr = signalPtr;
        this.listenerList = new List<>(wl_signal.listener_list(signalPtr), Listener.listElementMeta);
        assert !signalPtr.equals(NULL);
        assert signalPtr.equals(listenerList.listPtr);
    }


    public static Signal0 of(MemorySegment signalPtr) {
        return new Signal0(signalPtr);
    }


    public static <T> Signal1<T> of(MemorySegment signalPtr, Function<MemorySegment, T> function) {
        return new Signal1<>(signalPtr, function);
    }


    /// Remove a listener from the signal.
    ///
    /// Convenience function, does not exist in C code.
    public void remove(Listener listener) {
        // TODO: Assert that listener is present in listenerList
        listenerList.remove(listener);
    }


    /// Signal that doesn't emit any value.
    ///
    /// Listener function takes no parameters.
    public static final class Signal0 extends Signal {

        Signal0(MemorySegment signalPtr) {
            super(signalPtr);
        }


        /// Listener that takes no parameters
        public Listener add(Runnable callback) {
            // TODO: Memory ownership - can't be auto or confined/scope
            var listener = Listener.allocate(
                Arena.global(),
                (MemorySegment listenerPtr, MemorySegment dataPtr) -> {
                    assert !listenerPtr.equals(NULL);
                    assert dataPtr.equals(NULL) : "Parameter (void *data) to listener must be NULL";
                    callback.run();
                });
            listenerList.append(listener);
            return listener;
        }


        // Listener callback takes one parameter: wl_listener
        public Listener add(Consumer<Listener> callback) {
            // TODO: Memory ownership
            var listener = Listener.allocate(
                Arena.global(),
                (MemorySegment listenerPtr, MemorySegment dataPtr) -> {
                    assert !listenerPtr.equals(NULL);
                    assert dataPtr.equals(NULL) : "Parameter (void *data) to listener must be NULL";
                    callback.accept(new Listener(listenerPtr));
                });
            listenerList.append(listener);
            return listener;
        }


    }


    /// Signal that emits one value.
    ///
    /// Listener function takes one parameter.
    public static final class Signal1<T> extends Signal {
        public final Function<MemorySegment, T> observerParameterCtor;


        Signal1(MemorySegment signalPtr, Function<MemorySegment, T> function) {
            super(signalPtr);
            this.observerParameterCtor = function;
        }


        /// Listener callback takes one parameter: void *data
        public Listener add(Consumer<T> observer) {
            // TODO: Memory ownership - can't be auto or confined/scope
            var listener = Listener.allocate(
                Arena.global(),
                (MemorySegment listenerPtr, MemorySegment dataPtr) -> {
                    assert !listenerPtr.equals(NULL);
                    assert !dataPtr.equals(NULL) : "Parameter (void *data) to listener must not be NULL";
                    observer.accept(observerParameterCtor.apply(dataPtr));
                });
            listenerList.append(listener);
            return listener;
        }


        /// Listener callback takes two parameters: *wl_listener, void *data
        public Listener add(BiConsumer<Listener, T> callback) {
            // TODO: Memory ownership - can't be auto or confined/scope
            var listener = Listener.allocate(
                Arena.global(),
                (MemorySegment listenerPtr, MemorySegment dataPtr) -> {
                    assert !listenerPtr.equals(NULL);
                    assert !dataPtr.equals(NULL) : "Parameter (void *data) to listener must not be NULL";
                    callback.accept(new Listener(listenerPtr), observerParameterCtor.apply(dataPtr));
                });
            listenerList.append(listener);
            return listener;
        }
    }
}