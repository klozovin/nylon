package wayland.server;

import java.lang.foreign.MemorySegment;

public class EventLoop {

    public final MemorySegment eventLoopPtr;

    public EventLoop(MemorySegment eventLoopPtr) {
        this.eventLoopPtr = eventLoopPtr;
    }
}