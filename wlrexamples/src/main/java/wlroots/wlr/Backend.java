package wlroots.wlr;

import jextract.wlroots.wlr_backend;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.EventLoop;
import wayland.server.Signal;
import wlroots.wlr.backend.Session;
import wlroots.wlr.types.InputDevice;
import wlroots.wlr.types.Output;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.backend_h.*;


/// A backend provides a set of input and output devices.
///
/// Buffer capabilities and features can change over the lifetime of a backend, for instance when a
/// child backend is added to a multi-backend.
public final class Backend {
    public final @NonNull MemorySegment backendPtr;
    public final @NonNull Events events;


    public Backend(@NonNull MemorySegment backendPtr) {
        assert !backendPtr.equals(NULL);
        this.backendPtr = backendPtr;
        this.events = new Events(wlr_backend.events(backendPtr));
    }


    /// Automatically initializes the most suitable backend given the environment. Will always
    /// return a multi-backend. The backend is created but not started. Returns NULL on failure.
    ///
    /// If session_ptr is not NULL, it's populated with the session which has been created with the
    /// backend, if any.
    ///
    /// The multi-backend will be destroyed if one of the primary underlying backends is destroyed
    /// (e.g. if the primary DRM device is unplugged).
    ///
    /// `struct wlr_backend *wlr_backend_autocreate(​struct wl_event_loop *loop, struct wlr_session **session_ptr);`
    public static @Nullable Backend autocreate(@NonNull EventLoop eventLoop, @Nullable Session session) {
        var backendPtr = wlr_backend_autocreate(
            eventLoop.eventLoopPtr,
            switch (session) {
                case Session s -> s.sessionPtr;
                case null -> NULL;
            });
        return !backendPtr.equals(NULL) ? new Backend(backendPtr) : null;
    }


    public boolean start() {
        return wlr_backend_start(backendPtr);
    }


    public void destroy() {
        wlr_backend_destroy(backendPtr);
    }


    @NullMarked
    public final static class Events {
        public final MemorySegment eventsPtr;
        public final Signal<Output> newOutput;
        public final Signal<InputDevice> newInput;


        Events(MemorySegment eventsPtr) {
            assert !eventsPtr.equals(NULL);
            this.eventsPtr = eventsPtr;
            newOutput = new Signal<>(wlr_backend.events.new_output(eventsPtr), Output::new);
            newInput = new Signal<>(wlr_backend.events.new_input(eventsPtr), InputDevice::new);
        }
    }
}