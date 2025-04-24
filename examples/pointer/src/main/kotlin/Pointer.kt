import wayland.KeyboardKeyState
import wayland.server.Display
import wlroots.backend.Backend
import wlroots.render.Allocator
import wlroots.render.Renderer
import wlroots.types.*
import wlroots.types.InputDevice.Type.*
import wlroots.types.output.Output
import wlroots.types.output.OutputLayout
import wlroots.types.output.OutputState
import wlroots.types.pointer.PointerAxisEvent
import wlroots.types.pointer.PointerButtonEvent
import wlroots.types.pointer.PointerMotionAbsoluteEvent
import wlroots.types.pointer.PointerMotionEvent
import wlroots.types.tablet.TabletToolAxisEvent
import wlroots.types.touch.TouchCancelEvent
import wlroots.types.touch.TouchDownEvent
import wlroots.types.touch.TouchMotionEvent
import wlroots.types.touch.TouchUpEvent
import wlroots.util.Log
import xkbcommon.Keymap
import xkbcommon.XkbContext
import xkbcommon.XkbKey
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.exitProcess


object Pointer {
    var lastFrame: Long = 0

    val display = Display.create()

    val backend = Backend.autocreate(display.eventLoop, null)?.apply {
        events.newInput.add(::newInputHandler)
        events.newOutput.add(::newOutputHandler)
    } ?: error("Failed to create wlr_backend")

    val renderer = Renderer.autocreate(backend) ?: error("Failed to create wlr_renderer")
    val allocator = Allocator.autocreate(backend, renderer)
    val outputLayout = OutputLayout.create(display)
    val xcursorManager = XcursorManager.create(null, 24) ?: error("Failed to load default cursor")

    lateinit var output: Output
    lateinit var keyboard: Keyboard


    val cursor = Cursor.create().apply {
        with(events) {
            motion.add(::cursorMotionHandler)
            motionAbsolute.add(::cursorMotionAbsoluteHandler)
            button.add(::cursorButtonHandler)
            axis.add(::cursorAxisHandler)

            touchUp.add(::cursorTouchUpHandler)
            touchDown.add(::cursorTouchDownHandler)
            touchMotion.add(::cursorTouchMotionHandler)
            touchCancel.add(::cursorTouchCancelHandler)

            tabletToolAxis.add(::cursorTabletToolAxisHandler)
        }

        setXcursor(xcursorManager, "default")
        attachOutputLayout(outputLayout)
    }


    fun main() {
        lastFrame = System.nanoTime()

        if (!backend.start()) {
            Log.logError("Failed to start backend")
            backend.destroy()
            exitProcess(1)
        }

        Timer(true).schedule(30_000) {
            display.terminate()
        }

        display.run()
        display.destroy()
        xcursorManager.destroy()
        cursor.destroy()
    }

    //
    // Output: Signal listeners
    //

    fun newOutputHandler(output: Output) {
        Pointer.output = output
        output.initRender(allocator, renderer)
        output.events.frame.add(::outputFrameHandler)
        output.events.destroy.add(::outputDestroyHandler)
        outputLayout.addAuto(output)

        OutputState.allocateConfined { state ->
            state.init()
            state.setEnabled(true)
            output.preferredMode()?.let {
                state.setMode(it)
            }
            output.commitState(state)
            state.finish()
        }
    }

    fun outputFrameHandler() {

    }


    fun outputDestroyHandler() {

    }

    //
    // Keyboard: signal listeners
    //

    fun newInputHandler(device: InputDevice) {
        when (device.type()) {
            POINTER, TOUCH, TABLET -> cursor.attachInputDevice(device)
            KEYBOARD -> {
                keyboard = device.keyboardFromInputDevice()
                keyboard.events.key.add(::keyboardKeyHandler)
                device.events.destroy.add(::keyboardDestroyHandler)

                val context = XkbContext.of(XkbContext.Flags.NO_FLAGS) ?: error {
                    Log.logError("Failed to create XKB keymap")
                    exitProcess(1)
                }

                val keymap = context.keymapNewFromNames(null, Keymap.CompileFlags.NO_FLAGS) ?: error {
                    Log.logError("Failed to create XKB keymap")
                    exitProcess(1)
                }

                keyboard.setKeymap(keymap)
                keymap.unref()
                context.unref()
            }

            else -> {}
        }
    }

    fun keyboardKeyHandler(key: KeyboardKeyEvent) {
        val keycode = key.keycode() + 8 // Convert from libinput/evdev raw hardware code to xkbcommon ones.
        val keysym = keyboard.xkbState().keyGetOneSym(keycode)
        check(keysym != XkbKey.NoSymbol)

        println(">> hwkeycode=${key.keycode()} keycode=$keycode, keysym=$keysym, state=${key.state()}")

        if (keysym == XkbKey.Escape && key.state() == KeyboardKeyState.RELEASED) {
            Log.logDebug(">> Terminating display...")
            display.terminate()
            Log.logDebug(">> ...terminated display!")
        }
    }


    fun keyboardDestroyHandler() {}

    //
    // Input handlers: mouse, touchscreen
    //

    fun cursorMotionHandler(event: PointerMotionEvent) {
        cursor.move(event.pointer.base(), event.deltaX, event.deltaY)
    }


    fun cursorMotionAbsoluteHandler(motion: PointerMotionAbsoluteEvent) {

    }


    fun cursorButtonHandler(button: PointerButtonEvent) {

    }


    fun cursorAxisHandler(axis: PointerAxisEvent) {


    }

    fun cursorTouchUpHandler(touch: TouchUpEvent) {}
    fun cursorTouchDownHandler(touch: TouchDownEvent) {}
    fun cursorTouchMotionHandler(touch: TouchMotionEvent) {}
    fun cursorTouchCancelHandler(touch: TouchCancelEvent) {}
    fun cursorTabletToolAxisHandler(tablet: TabletToolAxisEvent) {}
}


fun main() {
    Log.init(Log.Importance.DEBUG)
    Pointer.main()
}


inline fun error(block: () -> Any): Nothing = error(block())