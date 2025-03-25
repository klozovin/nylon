package wrap

import wayland.*
import wayland.server.Display
import wayland.wl_list
import wayland.wl_listener
import wayland.wl_notify_func_t
import wayland.wl_signal
import wlroots.*
import wlroots.types.*
import wlroots.types.wlr_box
import wlroots.types.wlr_render_color
import wlroots.types.wlr_render_rect_options
import wlroots.wlr.Backend
import wlroots.wlr.render.Allocator
import wlroots.wlr.render.Renderer
import wlroots.wlr_output
import wlroots.wlr_output_state
import xkb.xkbcommon_h
import xkb.xkbcommon_h_1
import xkb.xkbcommon_h_3
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.MemorySegment.NULL
import kotlin.system.exitProcess

val arena: Arena = Arena.global()


object State {
    /**
     * ```struct wl_display *display;```
     */
    @Deprecated("DEPRECATED")
    lateinit var display: MemorySegment
    lateinit var displayW: Display

    /**
     * ```struct wlr_renderer *renderer;```
     */
    @Deprecated("DEPRECATED")
    lateinit var renderer: MemorySegment
    lateinit var rendererW: Renderer

    /**
     * ```struct wlr_allocator *allocator;```
     */
    @Deprecated("DEPRECATED")
    lateinit var allocator: MemorySegment
    lateinit var allocatorW: Allocator

    /**
     * ```struct timespec last_frame;```
     */
    var lastFrame: Long = 0

    /**
     * ```float color[4];```
     */
    val color = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)

    /**
     * ```int dec;```
     */
    var dec: Int = 0
}

object Output {
    /**
     * ```struct wlr_output *output;```
     */
    lateinit var output: MemorySegment

    /**
     *  struct wl_listener frame;
     */
    lateinit var frame: MemorySegment

    /**
     * ```struct wl_listener destroy;```
     */
    lateinit var destroy: MemorySegment
}

object Keyboard {
    /**
     * ```struct wlr_keyboard *wlr_keyboard;```
     */
    lateinit var keyboard: MemorySegment

    /**
     * ```struct wl_listener key;```
     */
    lateinit var key: MemorySegment

    /**
     * ```struct wl_listener destroy;```
     */
    lateinit var destroy: MemorySegment
}


fun outputFrameNotify(listener: MemorySegment, data: MemorySegment) {
    // struct sample_output *sample_output = wl_container_of(listener, sample_output, frame);
    // struct sample_state *sample = sample_output->sample;
    // struct wlr_output *wlr_output = sample_output->output;

    // struct timespec now;
    // clock_gettime(CLOCK_MONOTONIC, &now);
    val now = System.currentTimeMillis()

    // long ms = (now.tv_sec - sample->last_frame.tv_sec) * 1000 + (now.tv_nsec - sample->last_frame.tv_nsec) / 1000000;
    // int inc = (sample->dec + 1) % 3;
    val ms = now - State.lastFrame
    val inc = (State.dec + 1) % 3

    // sample->color[inc] += ms / 2000.0f;
    // sample->color[sample->dec] -= ms / 2000.0f;
    State.color[inc] += ms / 2000.0f
    State.color[State.dec] -= ms / 2000.0f

    // if (sample->color[sample->dec] < 0.0f) {
    //     sample->color[inc] = 1.0f;
    //     sample->color[sample->dec] = 0.0f;
    //     sample->dec = inc;
    // }
    if (State.color[State.dec] < 0.0f) {
        State.color[inc] = 1.0f
        State.color[State.dec] = 0.0f
        State.dec = inc
    }

    // struct wlr_output_state state;
    // wlr_output_state_init(&state);
    val statePtr = wlr_output_state.allocate(arena)
    backend_h.wlr_output_state_init(statePtr)

    // struct wlr_render_pass *pass = wlr_output_begin_render_pass(wlr_output, &state, NULL);
    val pass = backend_h.wlr_output_begin_render_pass(Output.output, statePtr, NULL, NULL)

    // wlr_render_pass_add_rect(pass, &(struct wlr_render_rect_options){
    //     .box = { .width = wlr_output->width, .height = wlr_output->height },
    //     .color = {
    //          .r = sample->color[0],
    //          .g = sample->color[1],
    //          .b = sample->color[2],
    //          .a = sample->color[3],
    //      },
    // });
    backend_h.wlr_render_pass_add_rect(pass, wlr_render_rect_options.allocate(arena).also { rectPtr ->
        wlr_render_rect_options.box(rectPtr).let { boxPtr ->
            wlr_box.width(boxPtr, wlr_output.width(Output.output))
            wlr_box.height(boxPtr, wlr_output.height(Output.output))
        }
        wlroots.wlr_render_rect_options.color(rectPtr).let { colorPtr ->
            wlr_render_color.r(colorPtr, State.color[0])
            wlr_render_color.g(colorPtr, State.color[1])
            wlr_render_color.b(colorPtr, State.color[2])
            wlr_render_color.a(colorPtr, State.color[3])
        }
    })

    // wlr_render_pass_submit(pass);
    backend_h.wlr_render_pass_submit(pass)

    // wlr_output_commit_state(wlr_output, &state);
    backend_h.wlr_output_commit_state(Output.output, statePtr)

    // wlr_output_state_finish(&state);
    backend_h.wlr_output_state_finish(statePtr)

    // sample->last_frame = now;
    State.lastFrame = now
}


fun outputRemoveNotify(listener: MemorySegment, data: MemorySegment) {
    // struct sample_output *sample_output = wl_container_of(listener, sample_output, destroy);

    // wlr_log(WLR_DEBUG, "Output removed");
    Log.logDebug("Output removed")

    // wl_list_remove(&sample_output->frame.link);
    // wl_list_remove(&sample_output->destroy.link);
    backend_h_1.wl_list_remove(wl_listener.link(Output.frame))
    backend_h_1.wl_list_remove(wl_listener.link(Output.destroy))

    // free(sample_output);
}


/**
 * ```static void new_output_notify(struct wl_listener *listener, void *data)```
 */
fun newOutputNotify(listenerPtr: MemorySegment, outputPtr: MemorySegment) {
    // struct wlr_output *output = data;
    // struct sample_state *sample = wl_container_of(listener, sample, new_output);

    // wlr_output_init_render(output, sample->allocator, sample->renderer);
    backend_h.wlr_output_init_render(outputPtr, State.allocatorW.allocatorPtr, State.rendererW.rendererPtr)

    // struct sample_output *sample_output = calloc(1, sizeof(*sample_output));
    // sample_output->output = output;
    // sample_output->sample = sample;
    Output.output = outputPtr

    // wl_signal_add(&output->events.frame, &sample_output->frame);
    // sample_output->frame.notify = output_frame_notify;
    Output.frame = wl_listener.allocate(arena)
    wl_listener.notify(Output.frame, wl_notify_func_t.allocate(::outputFrameNotify, arena))
    wl_signal_add(wlr_output.events.frame(wlr_output.events(outputPtr)), Output.frame)

    // wl_signal_add(&output->events.destroy, &sample_output->destroy);
    // sample_output->destroy.notify = output_remove_notify;
    Output.destroy = wl_listener.allocate(arena)
    wl_listener.notify(Output.destroy, wl_notify_func_t.allocate(::outputRemoveNotify, arena))
    wl_signal_add(wlr_output.events.destroy(wlr_output.events(outputPtr)), Output.destroy)

    // struct wlr_output_state state;
    // wlr_output_state_init(&state);
    // wlr_output_state_set_enabled(&state, true);
    val statePtr = wlr_output_state.allocate(arena)
    backend_h.wlr_output_state_init(statePtr)
    backend_h.wlr_output_state_set_enabled(statePtr, true)

    // struct wlr_output_mode *mode = wlr_output_preferred_mode(output);
    val modePtr = backend_h.wlr_output_preferred_mode(outputPtr)

    // if (mode != NULL)
    //     wlr_output_state_set_mode(&state, mode);
    if (modePtr != NULL)
        backend_h.wlr_output_state_set_mode(statePtr, modePtr)

    // wlr_output_commit_state(output, &state);
    // wlr_output_state_finish(&state);
    backend_h.wlr_output_commit_state(outputPtr, statePtr)
    backend_h.wlr_output_state_finish(statePtr)
}


fun keyboardKeyNotify(listener: MemorySegment, keyboardKeyEventPtr: MemorySegment) {
    // struct sample_keyboard *keyboard = wl_container_of(listener, keyboard, key);
    // struct sample_state *sample = keyboard->sample;
    // struct wlr_keyboard_key_event *event = data;

    // uint32_t keycode = event->keycode + 8;
    val keycode = wlr_keyboard_key_event.keycode(keyboardKeyEventPtr) + 8

    // const xkb_keysym_t *syms;
    val symsPtr = arena.allocate(xkbcommon_h.C_POINTER)

    // int nsyms = xkb_state_key_get_syms(keyboard->wlr_keyboard->xkb_state, keycode, &syms);
    val nsyms = xkbcommon_h_1.xkb_state_key_get_syms(wlr_keyboard.xkb_state(Keyboard.keyboard), keycode, symsPtr)

    // for (int i = 0; i < nsyms; i++) {
    //     xkb_keysym_t sym = syms[i];
    //     if (sym == XKB_KEY_Escape) {
    //         wl_display_terminate(sample->display);
    //     }
    // }
    for (i in 0..<nsyms) {
        val sym = symsPtr.get(xkbcommon_h.C_POINTER, i.toLong()).get(xkbcommon_h_1.xkb_keysym_t, 0)
        if (sym == xkbcommon_h_3.XKB_KEY_Escape()) {
            backend_h_1.wl_display_terminate(State.display)
        }
    }
}


fun keyboardDestroyNotify(listener: MemorySegment, data: MemorySegment) {
    // struct sample_keyboard *keyboard = wl_container_of(listener, keyboard, destroy);

    // wl_list_remove(&keyboard->destroy.link);
    // wl_list_remove(&keyboard->key.link);
    backend_h_1.wl_list_remove(wl_listener.link(Keyboard.destroy))
    backend_h_1.wl_list_remove(wl_listener.link(Keyboard.key))

    // free(keyboard);
}


/**
 * ```
 * static void new_input_notify(struct wl_listener *listener, void *data)
 * ```
 */
fun newInputNotify(listenerPtr: MemorySegment, inputDevicePtr: MemorySegment) {
    // struct wlr_input_device *device = data;
    // struct sample_state *sample = wl_container_of(listener, sample, new_input);

    // switch (device->type) {
    // case WLR_INPUT_DEVICE_KEYBOARD:;
    if (wlr_input_device.type(inputDevicePtr) == wlr_input_device_h.WLR_INPUT_DEVICE_KEYBOARD()) {
        // struct sample_keyboard *keyboard = calloc(1, sizeof(*keyboard));

        // keyboard->wlr_keyboard = wlr_keyboard_from_input_device(device);
        Keyboard.keyboard = wlr_keyboard_h_1.wlr_keyboard_from_input_device(inputDevicePtr)

        // keyboard->sample = sample;

        // wl_signal_add(&device->events.destroy, &keyboard->destroy);
        // keyboard->destroy.notify = keyboard_destroy_notify;
        Keyboard.destroy = wl_listener.allocate(arena)
        wl_listener.notify(Keyboard.destroy, wl_notify_func_t.allocate(::keyboardDestroyNotify, arena))
        wl_signal_add(wlr_input_device.events.destroy(wlr_input_device.events(inputDevicePtr)), Keyboard.destroy)

        // wl_signal_add(&keyboard->wlr_keyboard->events.key, &keyboard->key);
        // keyboard->key.notify = keyboard_key_notify;
        Keyboard.key = wl_listener.allocate(arena)
        wl_listener.notify(Keyboard.key, wl_notify_func_t.allocate(::keyboardKeyNotify, arena))
        wl_signal_add(wlr_keyboard.events.key(wlr_keyboard.events(Keyboard.keyboard)), Keyboard.key)

        // struct xkb_context *context = xkb_context_new(XKB_CONTEXT_NO_FLAGS);
        // if (!context) {
        //      wlr_log(WLR_ERROR, "Failed to create XKB context");
        //      exit(1);
        // }
        val xkbContextPtr = xkbcommon_h_1.xkb_context_new(xkbcommon_h_1.XKB_CONTEXT_NO_FLAGS())
        if (xkbContextPtr == NULL) {
            Log.logError("Failed to create XKB context")
            exitProcess(1)
        }

        // struct xkb_keymap *keymap = xkb_keymap_new_from_names(context, NULL, XKB_KEYMAP_COMPILE_NO_FLAGS);
        // if (!keymap) {
        //     wlr_log(WLR_ERROR, "Failed to create XKB keymap");
        //     exit(1);
        // }
        val xkbKeymapPtr = xkbcommon_h_1.xkb_keymap_new_from_names(
            xkbContextPtr,
            NULL,
            xkbcommon_h_1.XKB_KEYMAP_COMPILE_NO_FLAGS()
        )
        if (xkbKeymapPtr == NULL) {
            Log.logError("Failed to create XKB keymap")
            exitProcess(1)
        }

        // wlr_keyboard_set_keymap(keyboard->wlr_keyboard, keymap);
        wlr_keyboard_h_1.wlr_keyboard_set_keymap(Keyboard.keyboard, xkbKeymapPtr)

        // xkb_keymap_unref(keymap);
        // xkb_context_unref(context);
        xkbcommon_h_1.xkb_keymap_unref(xkbKeymapPtr)
        xkbcommon_h_1.xkb_context_unref(xkbContextPtr)
    }
}


fun main() {
    Log.init(Log.Importance.DEBUG)

    val display = Display.create()
    val backend = Backend.autocreate(display.eventLoop, NULL)

    State.displayW = display
    State.display = display.displayPtr

    State.rendererW = Renderer.autocreate(backend)
    State.allocatorW = Allocator.autocreate(backend, State.rendererW)

    // wl_signal_add(&backend->events.new_output, &state.new_output);
    // state.new_output.notify = new_output_notify;
    val newOutputListenerPtr = wl_listener.allocate(arena)
    wl_listener.notify(newOutputListenerPtr, wl_notify_func_t.allocate(::newOutputNotify, arena))
    wl_signal_add(wlr_backend.events.new_output(wlr_backend.events(backend.backendPtr)), newOutputListenerPtr)

    // wl_signal_add(&backend->events.new_input, &state.new_input);
    // state.new_input.notify = new_input_notify;
    val newInputListenerPtr = wl_listener.allocate(arena)
    wl_listener.notify(newInputListenerPtr, wl_notify_func_t.allocate(::newInputNotify, arena))
    wl_signal_add(wlr_backend.events.new_input(wlr_backend.events(backend.backendPtr)), newInputListenerPtr)

    // clock_gettime(CLOCK_MONOTONIC, &state.last_frame);
    State.lastFrame = System.currentTimeMillis()

    if (!backend.start()) {
        Log.logError("Failed to start backend")
        backend.destroy()
        exitProcess(1)
    }

    display.run()
    display.destroy()
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
 *  @param signalPtr: The signal that will emit events to the listener
 *  @param listenerPtr: The listener to add
 */
fun wl_signal_add(signalPtr: MemorySegment, listenerPtr: MemorySegment) {
    val signal_listenerList_prev = wl_list.prev(wl_signal.listener_list(signalPtr))
    val listener_link = wl_listener.link(listenerPtr)
    server_h.wl_list_insert(signal_listenerList_prev, listener_link)
}