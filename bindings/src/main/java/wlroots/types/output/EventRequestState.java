package wlroots.types.output;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class EventRequestState {
    public final MemorySegment eventRequestStatePtr;


    public EventRequestState(MemorySegment eventRequestStatePtr) {
        assert !eventRequestStatePtr.equals(NULL);
        this.eventRequestStatePtr = eventRequestStatePtr;
    }
}