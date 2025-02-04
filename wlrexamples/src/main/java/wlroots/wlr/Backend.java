package wlroots.wlr;

import wayland.server.EventLoop;
import wlroots.backend_h;
import wlroots.wlr_backend;
import wrap.Output;

import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

public class Backend {

    public final MemorySegment backendPtr;
    public final Events events;

    public Backend(MemorySegment backendPtr) {
        this.backendPtr = backendPtr;
        this.events = new Events(wlr_backend.events(backendPtr), null, null, null);
    }

    public boolean start() {
        return backend_h.wlr_backend_start(backendPtr);
    }

    public void destroy() {
        backend_h.wlr_backend_destroy(backendPtr);
    }

    public static Backend autocreate(EventLoop eventLoop, MemorySegment sessionPtr) {
        // TODO: wrap sessionPtr in object
        var backendPtr = backend_h.wlr_backend_autocreate(eventLoop.eventLoopPtr, sessionPtr);
        assert backendPtr != null;
        return new Backend(backendPtr);
    }

    abstract class Signal<T> {
        // maybe as interface?
        // wl_signal wrapper
        // put wl_signal_add here

        public final MemorySegment signalPtr;

        public Signal(MemorySegment signalPtr) {
            this.signalPtr = signalPtr;
        }

        abstract void add(Consumer<T> callback);
    }

    class NewOutputSignal extends Signal<Output> {

        public NewOutputSignal(MemorySegment signalPtr) {
            super(signalPtr);
        }

        @Override
        public void add(Consumer<Output> callback) {
//            super.add(callback);
            // wl_signal_add this.signalptr
        }
    }

    class Events {
        public final MemorySegment eventsPtr;
        public final Signal destroy;
        public final Signal newInput;
        public final Signal newOutput;

        Events(MemorySegment eventsPtr, Signal destroy, Signal newInput, Signal newOutput) {
            this.eventsPtr = eventsPtr;
            this.destroy = destroy;
            this.newInput = newInput;
            this.newOutput = newOutput;
        }
    }
}