package example.wrap

import wayland.server.Display
import wayland.server.Listener
import wlroots.Log
import wlroots.Version
import wlroots.wlr.Backend
import wlroots.wlr.render.Allocator
import wlroots.wlr.render.RectOptions
import wlroots.wlr.render.Renderer
import wlroots.wlr.types.*
import xkbcommon.Keymap
import xkbcommon.XkbContext
import xkbcommon.XkbKey
import java.lang.foreign.Arena
import kotlin.system.exitProcess


val arena: Arena = Arena.global()


object State {
    lateinit var display: Display
    lateinit var renderer: Renderer
    lateinit var allocator: Allocator

    var dec: Int = 0                                    // int dec;
    var lastFrame: Long = 0                             // struct timespec last_frame;
    val color = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)    // float color[4];

    // Output
    lateinit var output: Output                     // struct wlr_output *output;
    lateinit var outputFrameListener: Listener      // struct wl_listener frame;
    lateinit var outputDestroyListener: Listener    // struct wl_listener destroy;

    // Keyboard
    lateinit var keyboard: Keyboard                 // struct wlr_keyboard *wlr_keyboard;
    lateinit var keyboardKeyListener: Listener      // struct wl_listener key;
    lateinit var keyboardDestroyListener: Listener  // struct wl_listener destroy;
}


/**
 * ```
 * static void
 * new_output_notify(
 *      struct wl_listener *listener,
 *      void *data
 * )
 * ```
 */
fun newOutputNotify(output: Output) {
    State.output = output

    output.initRender(State.allocator, State.renderer)
    State.outputFrameListener = output.events.frame.add(::outputFrameNotify)
    State.outputDestroyListener = output.events.destroy.add(::outputRemoveNotify)

    Arena.ofConfined().use {
        OutputState.allocate(it).apply {
            init()
            setEnabled(true)
            output.preferredMode()?.let { mode -> setMode(mode) }
            output.commitState(this)
            finish()
        }
    }
}


/**
 * ```
 * static void
 * new_input_notify(
 *      struct wl_listener *listener,
 *      void *data
 * )
 * ```
 */
fun newInputNotify(inputDevice: InputDevice) {
    if (inputDevice.type() == InputDevice.Type.KEYBOARD) {
        State.keyboard = inputDevice.keyboardFromInputDevice()
        State.keyboardKeyListener = State.keyboard.events.key.add(::keyboardKeyNotify)
        State.keyboardDestroyListener = inputDevice.events.destroy.add(::keyboardDestroyNotify)

        val xkbContext = XkbContext.of(XkbContext.Flags.NO_FLAGS) ?: error {
            Log.logError("Failed to create XKB context")
            exitProcess(1)
        }
        val keymap = xkbContext.keymapNewFromNames(null, Keymap.CompileFlags.NO_FLAGS) ?: error {
            Log.logError("Failed to create XKB keymap")
            exitProcess(1)
        }

        State.keyboard.setKeymap(keymap)

        keymap.unref()
        xkbContext.unref()
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

        val renderPass = State.output.beginRenderPass(outputState, 0, null) ?: error("Render pass should not be null")
        renderPass.addRect(RectOptions.allocate(arena).apply {
            box.setWidth(State.output.width())
            box.setHeight(State.output.height())
            color.rgba(State.color)
        })
        renderPass.submit()

        State.output.commitState(outputState)
        outputState.finish()
    }
    State.lastFrame = now
}


fun keyboardKeyNotify(keyboardKeyEvent: KeyboardKeyEvent) {
    val keycode = keyboardKeyEvent.keycode() + 8
    val keySym = State.keyboard.xkbState().keyGetOneSym(keycode)

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
    State.output.events.frame.remove(State.outputFrameListener)
    State.output.events.destroy.remove(State.outputDestroyListener)
}


fun keyboardDestroyNotify() {
    Log.logDebug("Keyboard removed")
    State.keyboardKeyListener.remove()
    State.keyboardDestroyListener.remove()
}


fun main() {
    Log.init(Log.Importance.DEBUG)
    Log.log(Log.Importance.INFO, "This is Java version: ${System.getProperty("java.version")}")
    Log.log(Log.Importance.INFO, "Running on wlroots version: ${Version.STR}")


    State.display = Display.create()
    val backend = Backend.autocreate(State.display.eventLoop, null) ?: exitProcess(1)
    State.renderer = Renderer.autocreate(backend) ?: exitProcess(1)
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


inline fun error(block: () -> Any): Nothing = error(block())