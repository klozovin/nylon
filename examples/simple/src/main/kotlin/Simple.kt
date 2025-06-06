import wayland.KeyboardKeyState
import wayland.server.Display
import wayland.server.Listener
import wlroots.util.Log
import wlroots.Version
import wlroots.backend.Backend
import wlroots.render.Allocator
import wlroots.render.RectOptions
import wlroots.render.Renderer
import wlroots.types.input.InputDevice
import wlroots.types.input.Keyboard
import wlroots.types.input.KeyboardKeyEvent
import wlroots.types.output.Output
import wlroots.types.output.OutputState
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

    OutputState.allocateConfined { outputState ->
        outputState.apply {
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
        State.keyboard = Keyboard.fromInputDevice(inputDevice)
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


fun outputFrameNotify(output: Output) {
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
    val keycode = keyboardKeyEvent.keycode + 8
    val keySym = State.keyboard.xkbState().keyGetOneSym(keycode)

    check(keySym != XkbKey.NoSymbol)
    println("> keycode=$keycode, sym=$keySym")

    // BUGFIX: Have to check for Escape release, because on some keyboard setups both PRESSED and RELEASED events come
    //         together, when the key is depressed.
    if (keySym == XkbKey.Escape && keyboardKeyEvent.state == KeyboardKeyState.RELEASED) {
        Log.logDebug("Terminating display...")
        State.display.terminate()
        Log.logDebug("...terminated display!")
    }
}


fun outputRemoveNotify(output: Output) {
    Log.logDebug("Output removed")
    State.output.events.frame.remove(State.outputFrameListener)
    State.output.events.destroy.remove(State.outputDestroyListener)
}


fun keyboardDestroyNotify(device: InputDevice) {
    Log.logDebug("Keyboard removed")
    State.keyboardKeyListener.remove()
    State.keyboardDestroyListener.remove()
}


fun main() {
    Log.init(Log.Importance.DEBUG)
    Log.logInfo("This is Java version: ${System.getProperty("java.version")}")
    Log.logDebug("Running on wlroots version: ${Version.STR}")

    State.display = Display.create()
    val backend = Backend.autocreate(State.display.eventLoop, null) ?: exitProcess(1)
    State.renderer = Renderer.autocreate(backend) ?: exitProcess(1)
    State.allocator = Allocator.autocreate(backend, State.renderer) ?: error("Failed to create wlr_allocator")

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