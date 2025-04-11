package example.wrap

import jexwayland.wl_listener
import jexwlroots.backend_h
import jexwlroots.types.wlr_keyboard
import jexxkbcommon.xkbcommon_h
import wayland.server.Display
import wayland.server.Listener
import wlroots.Log
import wlroots.wlr.Backend
import wlroots.wlr.render.Allocator
import wlroots.wlr.render.RectOptions
import wlroots.wlr.render.Renderer
import wlroots.wlr.types.*
import xkbcommon.XkbContext
import xkbcommon.XkbKey
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
    lateinit var outputEventFrameListener: Listener         // struct wl_listener frame;
    lateinit var outputEventDestroyListener: Listener       // struct wl_listener destroy;

    // Keyboard
    lateinit var keyboard: Keyboard
    lateinit var keyboardEventKeyListener: Listener         // struct wl_listener key;
    lateinit var keyboardEventDestroyListener: Listener     // struct wl_listener destroy;
}


/**
 * ```
 * static void
 * new_output_notify(
 *      struct wl_listener *listener,
 *      void *data)
 * ```
 */
fun newOutputNotify(output: Output) {
    State.output = output

    output.initRender(State.allocator, State.renderer)
    State.outputEventFrameListener = output.events.frame.add(::outputFrameNotify)
    State.outputEventDestroyListener = output.events.destroy.add(::outputRemoveNotify)

    check(output.events.frame.listenerList.length() == 1)
    check(output.events.destroy.listenerList.length() == 1)

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

    if (inputDevice.type == InputDevice.Type.KEYBOARD) {
        State.keyboard = inputDevice.keyboard

        check(State.keyboard.base.events.destroy.listenerList.length() == 0)
        check(inputDevice.inputDevicePtr == wlr_keyboard.base(State.keyboard.keyboardPtr))
        check(inputDevice.inputDevicePtr == State.keyboard.base.inputDevicePtr)

        State.keyboardEventDestroyListener = inputDevice.events.destroy.add(::keyboardDestroyNotify)
        State.keyboardEventKeyListener = State.keyboard.events.key.add(::keyboardKeyNotify)

        // struct xkb_context *context = xkb_context_new(XKB_CONTEXT_NO_FLAGS);
        // if (!context) {
        //      wlr_log(WLR_ERROR, "Failed to create XKB context");
        //      exit(1);
        // }
        val xkbContext = XkbContext.newContext(XkbContext.Flags.NO_FLAGS)
        if (xkbContext == null) {
            Log.logError("Failed to create XKB context")
            exitProcess(1)
        }

        // struct xkb_keymap *keymap = xkb_keymap_new_from_names(context, NULL, XKB_KEYMAP_COMPILE_NO_FLAGS);
        // if (!keymap) {
        //     wlr_log(WLR_ERROR, "Failed to create XKB keymap");
        //     exit(1);
        // }
        val xkbKeymapPtr = xkbcommon_h.xkb_keymap_new_from_names(
            xkbContext.xkbContextPtr,
            MemorySegment.NULL,
            xkbcommon_h.XKB_KEYMAP_COMPILE_NO_FLAGS()
        )
        if (xkbKeymapPtr == MemorySegment.NULL) {
            Log.logError("Failed to create XKB keymap")
            exitProcess(1)
        }

        // wlr_keyboard_set_keymap(keyboard->wlr_keyboard, keymap);
//        wlr_keyboard_h.wlr_keyboard_set_keymap(State.keyboardPtr, xkbKeymapPtr)
        State.keyboard.setKeymap(xkbKeymapPtr)

        // xkb_keymap_unref(keymap);
        // xkb_context_unref(context);
        xkbcommon_h.xkb_keymap_unref(xkbKeymapPtr)
        xkbcommon_h.xkb_context_unref(xkbContext.xkbContextPtr)
    }
}


fun outputFrameNotify() {
    check(State.keyboard.base.events.destroy.listenerList.length() == 1)

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

    check(State.keyboard.base.events.destroy.listenerList.length() == 1)
}


fun keyboardKeyNotify(keyboardKeyEvent: KeyboardKeyEvent) {
    // uint32_t keycode = event->keycode + 8;

    val keycode = keyboardKeyEvent.keycode + 8
    val keySym = State.keyboard.xkbState.getOneSym(keycode)

    check(keySym != XkbKey.NoSymbol)

    println("> keycode=$keycode, sym=$keySym")
    if (keySym == XkbKey.Escape) {
        Log.logDebug("Terminating display...")
        State.display.terminate()
        Log.logDebug("...terminated display!")
    }
}


fun outputRemoveNotify() {
    Log.logDebug("Output removed")
    // TODO: Handle list removal somehow?
    backend_h.wl_list_remove(wl_listener.link(State.outputEventFrameListener.listenerPtr))
    backend_h.wl_list_remove(wl_listener.link(State.outputEventDestroyListener.listenerPtr))
    check(State.output.events.frame.listenerList.empty() == true)
    println(State.output.events.destroy.listenerList.empty())
    println(State.output.events.destroy.listenerList.length())
//    check(State.output.events.destroy.listenerList.empty() == true)
}


fun keyboardDestroyNotify() {
    Log.logDebug("Keyboard removed")
//    check(State.keyboard.base.events.destroy.listenerList.length() == 1)

    println("Keyboard.base.events.destroy listeners: ${State.keyboard.base.events.destroy.listenerList.length()}")

    // wl_list_remove(&keyboard->destroy.link);
    // wl_list_remove(&keyboard->key.link);
    // TODO: Handle list removal!
    backend_h.wl_list_remove(wl_listener.link(State.keyboardEventDestroyListener.listenerPtr))
    backend_h.wl_list_remove(wl_listener.link(State.keyboardEventKeyListener.listenerPtr))

    check(State.keyboard.events.key.listenerList.empty() == true)
    println("Keyboard.base.events.destroy listeners: ${State.keyboard.base.events.destroy.listenerList.length()}")
//    check(State.keyboard.events.destroy.listenerList.empty() == true)
}


fun main() {
    Log.init(Log.Importance.DEBUG)

    State.display = Display.create()
    val backend = Backend.autocreate(State.display.eventLoop, null) ?: exitProcess(1)
    State.renderer = Renderer.autocreate(backend)
    State.allocator = Allocator.autocreate(backend, State.renderer)

    backend.events.newOutput.add(::newOutputNotify)
    backend.events.newInput.add(::newInputNotify)

    check(backend.events.newOutput.listenerList.length() == 1)
    check(backend.events.newInput.listenerList.length() == 1)


    State.lastFrame = System.currentTimeMillis()

    if (!backend.start()) {
        Log.logError("Failed to start backend")
        backend.destroy()
        exitProcess(1)
    }

    State.display.run()

    check(State.keyboard.base.events.destroy.listenerList.length() == 1)


    State.display.destroy()
}