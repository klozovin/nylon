package example.wrap

import jexwayland.*
import jexwayland.wl_list
import jexwayland.wl_listener
import jexwayland.wl_notify_func_t
import jexwayland.wl_signal
import jexwlroots.backend_h
import jexwlroots.types.*
import jexxkb.xkbcommon_h
import wayland.server.Display
import wlroots.Log
import wlroots.wlr.Backend
import wlroots.wlr.render.Allocator
import wlroots.wlr.render.RectOptions
import wlroots.wlr.render.Renderer
import wlroots.wlr.types.InputDevice
import wlroots.wlr.types.Output
import wlroots.wlr.types.OutputState
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import kotlin.system.exitProcess


val arena: Arena = Arena.global()
val autoArena: Arena = Arena.ofAuto()


object State {
    lateinit var display: Display
    lateinit var renderer: Renderer
    lateinit var allocator: Allocator

    var lastFrame: Long = 0                         // struct timespec last_frame;

    /**
     * ``````
     */
    val color = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)    // float color[4];

    var dec: Int = 0                                    // int dec;

    // Output
    lateinit var output: Output
    lateinit var outputFramePtr: MemorySegment          // struct wl_listener frame;
    lateinit var outputDestroyPtr: MemorySegment        // struct wl_listener destroy;

    // Keyboard
//    lateinit var keyboard: Keyboard
    lateinit var keyboardPtr: MemorySegment             // struct wlr_keyboard *wlr_keyboard;
    lateinit var keyboardKeyPtr: MemorySegment          // struct wl_listener key;
    lateinit var keyboardDestroyPtr: MemorySegment      // struct wl_listener destroy;
}


/**
 * ```static void new_output_notify(struct wl_listener *listener, void *data)```
 */
fun newOutputNotify(output: Output) {
    State.output = output

    output.initRender(State.allocator, State.renderer)
    output.events.frame.add(::outputFrameNotify)
    output.events.destroy.add(::outputRemoveNotify)

    // TODO: Memory lifetime - maybe use confined Arena?
    OutputState.allocate(autoArena).apply {
        init()
        setEnabled(true)
        State.output.preferredMode?.let { mode -> setMode(mode) }
        State.output.commitState(this)
        finish()
    }
}


/**
 * ```
 * static void new_input_notify(struct wl_listener *listener, void *data)
 * ```
 */
//fun newInputNotify(listenerPtr: MemorySegment, inputDevicePtr: MemorySegment) {
fun newInputNotify(inputDevice: InputDevice) {
    // struct wlr_input_device *device = data;
    // struct sample_state *sample = wl_container_of(listener, sample, new_input);

    // switch (device->type) {
    // case WLR_INPUT_DEVICE_KEYBOARD:;
    if (wlr_input_device.type(inputDevice.inputDevicePtr) == wlr_input_device_h.WLR_INPUT_DEVICE_KEYBOARD()) {
        // struct sample_keyboard *keyboard = calloc(1, sizeof(*keyboard));

        // keyboard->wlr_keyboard = wlr_keyboard_from_input_device(device);
        State.keyboardPtr = wlr_keyboard_h.wlr_keyboard_from_input_device(inputDevice.inputDevicePtr)

        // keyboard->sample = sample;

        // wl_signal_add(&device->events.destroy, &keyboard->destroy);
        // keyboard->destroy.notify = keyboard_destroy_notify;
        State.keyboardDestroyPtr = wl_listener.allocate(arena)
        wl_listener.notify(State.keyboardDestroyPtr, wl_notify_func_t.allocate(::keyboardDestroyNotify, arena))
        wl_signal_add(
            wlr_input_device.events.destroy(wlr_input_device.events(inputDevice.inputDevicePtr)),
            State.keyboardDestroyPtr
        )

        // wl_signal_add(&keyboard->wlr_keyboard->events.key, &keyboard->key);
        // keyboard->key.notify = keyboard_key_notify;
        State.keyboardKeyPtr = wl_listener.allocate(arena)
        wl_listener.notify(State.keyboardKeyPtr, wl_notify_func_t.allocate(::keyboardKeyNotify, arena))
        wl_signal_add(wlr_keyboard.events.key(wlr_keyboard.events(State.keyboardPtr)), State.keyboardKeyPtr)

        // struct xkb_context *context = xkb_context_new(XKB_CONTEXT_NO_FLAGS);
        // if (!context) {
        //      wlr_log(WLR_ERROR, "Failed to create XKB context");
        //      exit(1);
        // }
        val xkbContextPtr = xkbcommon_h.xkb_context_new(xkbcommon_h.XKB_CONTEXT_NO_FLAGS())
        if (xkbContextPtr == MemorySegment.NULL) {
            Log.logError("Failed to create XKB context")
            exitProcess(1)
        }

        // struct xkb_keymap *keymap = xkb_keymap_new_from_names(context, NULL, XKB_KEYMAP_COMPILE_NO_FLAGS);
        // if (!keymap) {
        //     wlr_log(WLR_ERROR, "Failed to create XKB keymap");
        //     exit(1);
        // }
        val xkbKeymapPtr = xkbcommon_h.xkb_keymap_new_from_names(
            xkbContextPtr,
            MemorySegment.NULL,
            xkbcommon_h.XKB_KEYMAP_COMPILE_NO_FLAGS()
        )
        if (xkbKeymapPtr == MemorySegment.NULL) {
            Log.logError("Failed to create XKB keymap")
            exitProcess(1)
        }

        // wlr_keyboard_set_keymap(keyboard->wlr_keyboard, keymap);
        wlr_keyboard_h.wlr_keyboard_set_keymap(State.keyboardPtr, xkbKeymapPtr)

        // xkb_keymap_unref(keymap);
        // xkb_context_unref(context);
        xkbcommon_h.xkb_keymap_unref(xkbKeymapPtr)
        xkbcommon_h.xkb_context_unref(xkbContextPtr)
    }
}


fun outputFrameNotify() {
    val now = System.currentTimeMillis()
    val ms = now - State.lastFrame
    val inc = (State.dec + 1) % 3
    State.color[inc] += ms / 2000.0f
    State.color[State.dec] -= ms / 2000.0f

    if (State.color[State.dec] < 0.0f) {
        State.color[inc] = 1.0f
        State.color[State.dec] = 0.0f
        State.dec = inc
    }

    Arena.ofConfined().use { arena ->
        val outputState = OutputState.allocate(arena)
        outputState.init()

        val renderPass = State.output.beginRenderPass(outputState) ?: error("Render pass should not be null")

        renderPass.addRect(RectOptions.allocate(arena).apply {
            box.setWidth(State.output.width)
            box.setHeight(State.output.height)
            color.setColor(State.color[0], State.color[1], State.color[2], State.color[3])
        })
        renderPass.submit()
        State.output.commitState(outputState)
        outputState.finish()
    }
    State.lastFrame = now
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
    val nsyms = xkbcommon_h.xkb_state_key_get_syms(wlr_keyboard.xkb_state(State.keyboardPtr), keycode, symsPtr)

    // for (int i = 0; i < nsyms; i++) {
    //     xkb_keysym_t sym = syms[i];
    //     if (sym == XKB_KEY_Escape) {
    //         wl_display_terminate(sample->display);
    //     }
    // }
    println("xkb: nsyms = $nsyms")
    for (i in 0..<nsyms) {
        val sym = symsPtr.get(xkbcommon_h.C_POINTER, i.toLong()).get(xkbcommon_h.xkb_keysym_t, 0)
        if (sym == xkbcommon_h.XKB_KEY_Escape()) {
            backend_h.wl_display_terminate(State.display.displayPtr)
        } else {
            println("Key: $sym")
        }
    }
}


fun outputRemoveNotify() {
    Log.logDebug("Output removed")
    // TODO: Handle list removal somehow?
//    backend_h.wl_list_remove(wl_listener.link(State.outputFramePtr))
//    backend_h.wl_list_remove(wl_listener.link(State.outputDestroyPtr))
}


fun keyboardDestroyNotify(listener: MemorySegment, data: MemorySegment) {
    // struct sample_keyboard *keyboard = wl_container_of(listener, keyboard, destroy);

    // wl_list_remove(&keyboard->destroy.link);
    // wl_list_remove(&keyboard->key.link);
    backend_h.wl_list_remove(wl_listener.link(State.keyboardDestroyPtr))
    backend_h.wl_list_remove(wl_listener.link(State.keyboardKeyPtr))
}


fun main() {
    Log.init(Log.Importance.DEBUG)

    State.display = Display.create()
    val backend = Backend.autocreate(State.display.eventLoop, null) ?: exitProcess(1)
    State.renderer = Renderer.autocreate(backend)
    State.allocator = Allocator.autocreate(backend, State.renderer)

    backend.events.newOutput.add(::newOutputNotify)
    backend.events.newInput.add(::newInputNotify)

    State.lastFrame = System.currentTimeMillis()

    if (!backend.start()) {
        Log.logError("Failed to start backend")
        backend.destroy()
        exitProcess(1)
    }

    State.display.run()
    State.display.destroy()
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
