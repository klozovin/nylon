package wlroots.types.output;

import jextract.wlroots.wlr_output_event_request_state;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


// TODO: Rename OutputEventRequestState? Move into output.Event class?
/// `struct wlr_output_event_request_state {}`
@NullMarked
public class EventRequestState {
//    final MemorySegment eventRequestStatePtr;
    public final Output output;
    public final OutputState state;


    public EventRequestState(MemorySegment eventRequestStatePtr) {
        assert !eventRequestStatePtr.equals(NULL);
//        this.eventRequestStatePtr = eventRequestStatePtr;
        this.output = new Output(wlr_output_event_request_state.output(eventRequestStatePtr));
        this.state = new OutputState(wlr_output_event_request_state.state(eventRequestStatePtr));
    }
}