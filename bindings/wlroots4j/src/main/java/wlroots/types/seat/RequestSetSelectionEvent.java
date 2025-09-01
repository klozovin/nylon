package wlroots.types.seat;

import jextract.wlroots.types.wlr_seat_request_set_selection_event;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wlroots.types.DataSource;

import java.lang.foreign.MemorySegment;


@NullMarked
public class RequestSetSelectionEvent {

    public final @Nullable DataSource source;
    public final int serial;


    public RequestSetSelectionEvent(MemorySegment ptr) {
        this.source = DataSource.ofPtrOrNull(wlr_seat_request_set_selection_event.source(ptr));
        this.serial = wlr_seat_request_set_selection_event.serial(ptr);
    }
}