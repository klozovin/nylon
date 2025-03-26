package wlroots.wlr;

import wayland.server.EventLoop;
import wayland.server.Signal;
import wlroots.backend_h;
import wlroots.wlr.types.Output;
import wlroots.wlr_backend;

import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;


public final class Backend {

    public final MemorySegment backendPtr;
//    public final MemorySegment eventsPtr;
//    public final Events events;

    public Backend(MemorySegment backendPtr) {
        this.backendPtr = backendPtr;
//        this.eventsPtr = wlr_backend.events(backendPtr);
//        this.events = new Events(wlr_backend.events(backendPtr));

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
        assert backendPtr != MemorySegment.NULL;
        return new Backend(backendPtr);
    }

    //
    // Helper classes for nicer events API (wl_signal, wl_listener)
    //

    public final static class Events {
        public final MemorySegment eventsPtr;

//        public final Signal<InputDevice> newInput;
        public final Signal<Output> newOutput;
//        public final Signal<Void> destroy;

        Events(MemorySegment eventsPtr) {
            this.eventsPtr = eventsPtr;

//            this.newInput = new Signal<>(wlr_backend.events.new_input(eventsPtr), InputDevice::new) {
//                @Override
//                public void add(Consumer<InputDevice> callback) {
//                    super.add(callback);
//                }
//            };

            try {


                this.newOutput = new Signal<Output>(wlr_backend.events.new_output(eventsPtr), Output.class.getConstructor(MemorySegment.class)) {
                    @Override
                    public void add(Consumer<Output> callback) {
                        super.add(callback);
                    }
                };



            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

//            this.destroy = new Signal<>(wlr_backend.events.destroy(eventsPtr)) {
//                @Override
//                public void add(Consumer<Void> callback) {
//                    super.add(callback);
//                }
//            };
        }
    }

//    public static class NewOutputSignal extends Signal<examples.direct.Output> {
//
//        public NewOutputSignal(MemorySegment signalPtr) {
//            super(signalPtr);
//        }
//
//        @Override
//        public void add(Consumer<examples.direct.Output> callback) {
//            super.add(callback);
//            // examples.direct.wl_signal_add this.signalptr
//        }
//    }
}