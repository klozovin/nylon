import wayland.*
import wlroots.Log
import wlroots.backend_h
import wlroots.render.allocator_h
import wlroots.render.wlr_renderer_h
import wlroots.types.wlr_output_h
import wlroots.wlr_backend
import wlroots.wlr_output
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

object State {
    lateinit var renderer: MemorySegment
    lateinit var allocator: MemorySegment
}

fun main() {
    val arena = Arena.global()

    Log.init(Log.Importance.DEBUG)

    val displayPtr = server_h.wl_display_create()

    val eventLoopPtr = server_h.wl_display_get_event_loop(displayPtr)
    val backendPtr = backend_h.wlr_backend_autocreate(eventLoopPtr, MemorySegment.NULL)

    State.renderer = wlr_renderer_h.wlr_renderer_autocreate(backendPtr)
    State.allocator = allocator_h.wlr_allocator_autocreate(backendPtr, State.renderer)

    check(backendPtr != MemorySegment.NULL)

    // Signal #1 handler: wlr_backend.events.new_output -> wlr_output
    //      wl_signal_add(&backend->events.new_output, &state.new_output);
    //      state.new_output.notify = new_output_notify;
    val backend_events_newOutput_Ptr = wlr_backend.events.new_output(wlr_backend.events(backendPtr))

    val newOutputListenerPtr = wl_listener.allocate(arena)
    val newOutputNotifyCallbackPtr = wl_notify_func_t.allocate(::newOutputNotify, arena)
    wl_listener.notify(newOutputListenerPtr, newOutputNotifyCallbackPtr)

    wl_signal_add(backend_events_newOutput_Ptr, newOutputListenerPtr)


    /*

    // Signal #2 handler: wlr_backend.events.new_input -> wlr_input_device
    //      wl_signal_add(&backend->events.new_input, &state.new_input);
    //      state.new_input.notify = new_input_notify;
    val backend_events_newInput_Ptr = wlr_backend.events.new_input(backendPtr)

    val newInputListenerPtr = wlroots.wl_listener.allocate(arena)
    val newInputNotifyCallbackPtr = wl_notify_func_t.allocate(::newInputNotify, arena)
    wl_listener.notify(newInputListenerPtr, newInputNotifyCallbackPtr)

    wl_signal_add(backend_events_newInput_Ptr, newInputListenerPtr)
     */


    // Let's get this party started
    val backendStartResult = backend_h.wlr_backend_start(backendPtr)
    if (!backendStartResult)
        error("Whoop-de-do")
    else {
        server_h.wl_display_run(displayPtr)
        server_h.wl_display_destroy(displayPtr)
    }
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
    val backendPtr = wlr_output.backend(outputPtr)
    val backendImplPtr = wlr_backend.impl(backendPtr)

    // wlr_output_init_render(output, sample->allocator, sample->renderer);
    wlr_output_h.wlr_output_init_render(outputPtr, State.allocator, State.renderer)
    TODO()
}

fun newInputNotify(listnerPtr: MemorySegment, dataPtr: MemorySegment) {
    // dataPtr -> wlr_input_device*
    TODO()
}




