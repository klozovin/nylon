package wayland.server;

import org.jspecify.annotations.Nullable;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wayland.wl.wl_event_source_timer_update;
import static jextract.wayland.wl_1.wl_event_source_remove;


public class EventSource {
    public final MemorySegment eventSourcePtr;


    public EventSource(MemorySegment eventSourcePtr) {
        assert !eventSourcePtr.equals(NULL);
        this.eventSourcePtr = eventSourcePtr;
    }


    @Override
    public boolean equals(@Nullable Object other) {
        return switch (other) {
            case null -> false;
            case EventSource otherEventSource -> eventSourcePtr.equals(otherEventSource.eventSourcePtr);
            default -> throw new RuntimeException("BUG: Trying to compare objects of different types");
        };
    }


    public int timerUpdate(int msDelay) {
        return wl_event_source_timer_update(eventSourcePtr, msDelay);
    }


    public int remove() {
        return wl_event_source_remove(eventSourcePtr);
    }
}