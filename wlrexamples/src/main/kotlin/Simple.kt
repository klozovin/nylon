import wayland.*
import wlroots.Log
import wlroots.backend_h.*
import wlroots.render.allocator_h
import wlroots.render.wlr_renderer_h
import wlroots.types.wlr_output_h
import wlroots.util.log_h.WLR_DEBUG
import wlroots.util.log_h.wlr_log_init
import wlroots.wlr_backend
import wlroots.wlr_output
import wlroots.wlr_output_state
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import kotlin.system.exitProcess

val arena: Arena = Arena.global()

object State {
    lateinit var renderer: MemorySegment
    lateinit var allocator: MemorySegment
    var lastFrame: Long = 0
}

object Output {
    // struct wlr_output *output;
    lateinit var output: MemorySegment

    // struct wl_listener frame;
    lateinit var frame: MemorySegment

    // struct wl_listener destroy;
    lateinit var destroy: MemorySegment
}

object Keyboard


fun main() {

    // wlr_log_init(WLR_DEBUG, NULL);
    wlr_log_init(WLR_DEBUG(), MemorySegment.NULL)

    // struct wl_display *display = wl_display_create();
    val displayPtr = server_h.wl_display_create()

    // 	struct wlr_backend *backend = wlr_backend_autocreate(wl_display_get_event_loop(display), NULL);
    val backendPtr = wlr_backend_autocreate(
        server_h.wl_display_get_event_loop(displayPtr),
        MemorySegment.NULL
    )

    // state.renderer = wlr_renderer_autocreate(backend);
    // state.allocator = wlr_allocator_autocreate(backend, state.renderer);
    State.renderer = wlr_renderer_h.wlr_renderer_autocreate(backendPtr)
    State.allocator = allocator_h.wlr_allocator_autocreate(backendPtr, State.renderer)

    // wl_signal_add(&backend->events.new_output, &state.new_output);
    // state.new_output.notify = new_output_notify;
    val newOutputListenerPtr = wl_listener.allocate(arena)
    wl_listener.notify(newOutputListenerPtr, wl_notify_func_t.allocate(::newOutputNotify, arena))
    wl_signal_add(
        wlr_backend.events.new_output(wlr_backend.events(backendPtr)),
        newOutputListenerPtr
    )

    TODO()
    // wl_signal_add(&backend->events.new_input, &state.new_input);
    // state.new_input.notify = new_input_notify;

    TODO()
    // clock_gettime(CLOCK_MONOTONIC, &state.last_frame);


    /*

    // Signal #2 handler: wlr_backend.events.new_input -> wlr_input_device
    val backend_events_newInput_Ptr = wlr_backend.events.new_input(backendPtr)

    val newInputListenerPtr = wlroots.wl_listener.allocate(arena)
    val newInputNotifyCallbackPtr = wl_notify_func_t.allocate(::newInputNotify, arena)
    wl_listener.notify(newInputListenerPtr, newInputNotifyCallbackPtr)

    wl_signal_add(backend_events_newInput_Ptr, newInputListenerPtr)
     */

    // if (!wlr_backend_start(backend)) {
    //     wlr_log(WLR_ERROR, "Failed to start backend");
    //     wlr_backend_destroy(backend);
    //     exit(1);
    // }
    if (!wlr_backend_start(backendPtr)) {
        Log.logError("Failed to start backend")
        wlr_backend_destroy(backendPtr)
        exitProcess(1)
    }

    // wl_display_run(display);
    wl_display_run(displayPtr)

    // wl_display_destroy(display);
    wl_display_destroy(displayPtr)
}


/**
 * Add the specified listener to this signal.
 *
 * ```
 * static inline void wl_signal_add(struct wl_signal *signal, struct wl_listener *listener) {
 *     wl_list_insert(signal->listener_list.prev, &listener->link);
 * }
 * ```
 *
 *  @param [signalPtr]: The signal that will emit events to the listener
 *  @param [listenerPtr]: The listener to add
 */
fun wl_signal_add(signalPtr: MemorySegment, listenerPtr: MemorySegment) {
    val signal_listenerList_prev = wl_list.prev(wl_signal.listener_list(signalPtr))
    val listener_link = wl_listener.link(listenerPtr)
    server_h.wl_list_insert(signal_listenerList_prev, listener_link)
}

/**
 * ```
 * static void new_output_notify(struct wl_listener *listener, void *data) {
 * ```
 *
 * @param [outputPtr]: wlr_output*
 */
fun newOutputNotify(listenerPtr: MemorySegment, outputPtr: MemorySegment) {
    // wlr_output_init_render(output, sample->allocator, sample->renderer);
    wlr_output_h.wlr_output_init_render(outputPtr, State.allocator, State.renderer)

    // struct sample_output *sample_output = calloc(1, sizeof(*sample_output));
    // sample_output->output = output;
    // sample_output->sample = sample;
    Output.output = outputPtr

    // wl_signal_add(&output->events.frame, &sample_output->frame);
    // sample_output->frame.notify = output_frame_notify;
    Output.frame = wl_listener.allocate(arena)
    wl_listener.notify(Output.frame, wl_notify_func_t.allocate(::outputFrameNotify, arena))
    wl_signal_add(
        wlr_output.events.destroy(wlr_output.events(outputPtr)),
        Output.frame
    )

    // wl_signal_add(&output->events.destroy, &sample_output->destroy);
    // sample_output->destroy.notify = output_remove_notify;
    Output.destroy = wl_listener.allocate(arena)
    wl_listener.notify(Output.destroy, wl_notify_func_t.allocate(::outputRemoveNotify, arena))
    wl_signal_add(
        wlr_output.events.destroy(wlr_output.events(outputPtr)),
        Output.destroy
    )


    // struct wlr_output_state state;
    val statePtr = wlr_output_state.allocate(arena)
    // wlr_output_state_init(&state);
    wlr_output_state_init(statePtr)
    // wlr_output_state_set_enabled(&state, true);
    wlr_output_state_set_enabled(statePtr, true)
    // struct wlr_output_mode *mode = wlr_output_preferred_mode(output);
    val modePtr = wlr_output_preferred_mode(outputPtr)

    // if (mode != NULL)
    //     wlr_output_state_set_mode(&state, mode);
    if (modePtr != MemorySegment.NULL)
        wlr_output_state_set_mode(statePtr, modePtr)

    // wlr_output_commit_state(output, &state);
    wlr_output_commit_state(outputPtr, statePtr)

    // wlr_output_state_finish(&state);
    wlr_output_state_finish(statePtr)
}


fun newInputNotify(listnerPtr: MemorySegment, dataPtr: MemorySegment) {
    // dataPtr -> wlr_input_device*
    TODO()
}


fun outputFrameNotify(listener: MemorySegment, data: MemorySegment) {
    TODO()

}

fun outputRemoveNotify(listener: MemorySegment, data: MemorySegment) {
    TODO()
}