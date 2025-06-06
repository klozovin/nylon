package wlroots.backend;

import jextract.wlroots.wlr_backend;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wayland.server.EventLoop;
import wayland.server.Signal;
import wayland.server.Signal.Signal1;
import wlroots.types.input.InputDevice;
import wlroots.types.output.Output;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.backend_h.*;


/// A backend provides a set of input and output devices.
///
/// Buffer capabilities and features can change over the lifetime of a backend, for instance when a
/// child backend is added to a multi-backend.
@NullMarked
public final class Backend {
    public final MemorySegment backendPtr;
    public final Events events;


    public Backend(MemorySegment backendPtr) {
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
    /// ```c
    /// struct wlr_backend *wlr_backend_autocreate(
    ///     struct wl_event_loop *loop,
    ///     struct wlr_session **session_ptr
    /// );
    ///```
    public static @Nullable Backend autocreate(EventLoop eventLoop, @Nullable Session session) {
        var backendPtr = wlr_backend_autocreate(
            eventLoop.eventLoopPtr,
            switch (session) {
                case Session s -> s.sessionPtr;
                case null -> NULL;
            });
        return !backendPtr.equals(NULL) ? new Backend(backendPtr) : null;
    }

    // *** Methods **************************************************************************************** //


    public boolean start() {
        return wlr_backend_start(backendPtr);
    }


    public void destroy() {
        wlr_backend_destroy(backendPtr);
    }


    // *** Events ***************************************************************************************** //


    public final static class Events {
        /// Raised when a new output (display, monitor, VR) becomes available
        public final Signal1<Output> newOutput;

        /// Raised when a new input device becomes available
        public final Signal1<InputDevice> newInput;

        ///  Raised when backend destroyed
        public final Signal1<Backend> destroy;


        Events(MemorySegment eventsPtr) {
            assert !eventsPtr.equals(NULL);
            newOutput = Signal.of(wlr_backend.events.new_output(eventsPtr), Output::new);
            newInput  = Signal.of(wlr_backend.events.new_input(eventsPtr), InputDevice::new);
            destroy   = Signal.of(wlr_backend.events.destroy(eventsPtr), Backend::new);
        }
    }
}