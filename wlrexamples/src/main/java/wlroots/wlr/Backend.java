package wlroots.wlr;

import jexwlroots.wlr_backend;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import wayland.server.EventLoop;
import wayland.server.Signal;
import wlroots.wlr.backend.Session;
import wlroots.wlr.types.InputDevice;
import wlroots.wlr.types.Output;

import java.lang.foreign.MemorySegment;

import static jexwlroots.backend_h.*;


/// A backend provides a set of input and output devices.
///
/// Buffer capabilities and features can change over the lifetime of a backend, for instance when a
/// child backend is added to a multi-backend.
public final class Backend {
    public final @NonNull MemorySegment backendPtr;
    public final @NonNull Events events;


    public Backend(@NonNull MemorySegment backendPtr) {
        assert backendPtr != MemorySegment.NULL;
        this.backendPtr = backendPtr;
        this.events = new Events(wlr_backend.events(backendPtr));
    }


    public boolean start() {
        return wlr_backend_start(backendPtr);
    }


    public void destroy() {
        wlr_backend_destroy(backendPtr);
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
        var sessionPtr = switch (session) {
            case null -> MemorySegment.NULL;
            case Session s -> s.sessionPtr;
        };
        var backendPtr = wlr_backend_autocreate(eventLoop.eventLoopPtr, sessionPtr);
        assert backendPtr != null;

        if (backendPtr != MemorySegment.NULL)
            return new Backend(backendPtr);
        else
            return null;
    }


    public final static class Events {
        public final @NonNull MemorySegment eventsPtr;
        public final @NonNull Signal<Output> newOutput;
        public final @NonNull Signal<InputDevice> newInput;


        Events(@NotNull MemorySegment eventsPtr) {
            this.eventsPtr = eventsPtr;
            newOutput = new Signal<>(wlr_backend.events.new_output(eventsPtr), Output::new);
            newInput  = new Signal<>(wlr_backend.events.new_input(eventsPtr), InputDevice::new);
        }
    }
}