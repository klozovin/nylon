import wayland.KeyboardKeyState
import wayland.PointerButtonState
import wayland.server.Display
import wlroots.backend.Backend
import wlroots.render.Allocator
import wlroots.render.RectOptions
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
import java.lang.foreign.Arena
import kotlin.system.exitProcess


object Pointer {
    var currentX: Double = 0.0
    var currentY: Double = 0.0
    var clearColor = doubleArrayOf(0.25, 0.25, 0.25)
    var defaultColor = doubleArrayOf(0.25, 0.25, 0.25)

    val display = Display.create()

    val backend = Backend.autocreate(display.eventLoop, null)?.apply {
        events.newInput.add(::onNewInput)
        events.newOutput.add(::onNewOutput)
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
            motionAbsolute.add(::onCursorMotionAbsolute)
            button.add(::onCursorButton)
            axis.add(::onCursorAxis)

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
        if (!backend.start()) {
            Log.logError("Failed to start backend")
            backend.destroy()
            exitProcess(1)
        }
        display.run()
        display.destroy()
        xcursorManager.destroy()
        cursor.destroy()
    }

    //
    // Output: Signal listeners
    //

    fun onNewOutput(output: Output) {
        Pointer.output = output
        output.initRender(allocator, renderer)
        output.events.frame.add(::onOutputFrame)
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


    fun onOutputFrame() {
        Arena.ofConfined().use { arena ->
            val state = OutputState.allocate(arena)
            state.init()
            val pass = output.beginRenderPass(state, null, null) ?: error("Failed to create wlr_render_pass")
            pass.addRect(RectOptions.allocate(arena).apply {
                box.setWidth(output.width())
                box.setHeight(output.height())
                color.rgba(clearColor[0], clearColor[1], clearColor[2], 1.0)
            })
            output.addSoftwareCursorsToRenderPass(pass)
            pass.submit()
            output.commitState(state)
            state.finish()
        }
    }


    fun outputDestroyHandler() {

    }

    //
    // Keyboard: signal listeners
    //

    fun onNewInput(device: InputDevice) {
        when (device.type()) {
            POINTER, TOUCH, TABLET -> cursor.attachInputDevice(device)
            KEYBOARD -> {
                keyboard = device.keyboardFromInputDevice()
                keyboard.events.key.add(::onKeyboardKey)
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

    fun onKeyboardKey(key: KeyboardKeyEvent) {
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


    fun onCursorMotionAbsolute(event: PointerMotionAbsoluteEvent) {
        currentX = event.x
        currentY = event.y
        cursor.warpAbsolute(event.pointer.base(), currentX, currentY)
    }


    fun onCursorButton(event: PointerButtonEvent) {
        if (event.state == PointerButtonState.RELEASED) {
            val color = defaultColor.copyOf()
            clearColor = color.copyOf()
        } else {
            val color = doubleArrayOf(0.25, 0.25, 0.25, 1.0)
            color[event.button % 3] = 1.0
            clearColor = color.copyOf()
        }
    }


    fun onCursorAxis(event: PointerAxisEvent) {
        // Depending on the scroll direction, increase/decrease every channel by 0.05, clamping at 0.0 and 1.0
        val channel = (defaultColor[0] + if (event.delta > 0) -0.05 else 0.05).coerceIn(0.0, 1.0)

        defaultColor = DoubleArray(3) { channel }
        clearColor = defaultColor
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