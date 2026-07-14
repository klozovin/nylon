package wayland.server;

import jextract.wayland.wl_event_loop_timer_func_t;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wayland.wl.wl_event_loop_add_timer;


@NullMarked
public class EventLoop {
    public final MemorySegment eventLoopPtr;


    public EventLoop(MemorySegment eventLoopPtr) {
        assert !eventLoopPtr.equals(NULL);
        this.eventLoopPtr = eventLoopPtr;
    }


    @Override
    public boolean equals(@Nullable Object other) {
        return switch (other) {
            case null -> false;
            case EventLoop otherEventLoop -> eventLoopPtr.equals(otherEventLoop.eventLoopPtr);
            default -> throw new RuntimeException("BUG: Trying to compare objects of different types");
        };
    }


    /// Convenience function for development, allocates using a global arena. Probably best not to use in
    /// production.
    public EventSource addTimer(IntSupplier func) {
        return addTimer(Arena.global(), func);
    }


    public EventSource addTimer(Arena arena, IntSupplier func) {
        var timerFuncPtr = wl_event_loop_timer_func_t.allocate(
            (MemorySegment _) -> func.getAsInt(),
            arena
        );
        var eventSourcePtr = wl_event_loop_add_timer(eventLoopPtr, timerFuncPtr, NULL);
        return new EventSource(eventSourcePtr);
    }
}