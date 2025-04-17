package wayland.server;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class EventLoop {
    public final MemorySegment eventLoopPtr;


    public EventLoop(MemorySegment eventLoopPtr) {
        assert !eventLoopPtr.equals(NULL);
        this.eventLoopPtr = eventLoopPtr;
    }
}