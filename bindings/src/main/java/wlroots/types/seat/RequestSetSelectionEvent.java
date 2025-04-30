package wlroots.types.seat;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;


@NullMarked
public class RequestSetSelectionEvent {
    private final MemorySegment requestSetSelectionEventPtr;


    public RequestSetSelectionEvent(MemorySegment requestSetSelectionEventPtr) {
        this.requestSetSelectionEventPtr = requestSetSelectionEventPtr;
    }
}