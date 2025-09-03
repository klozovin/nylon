import wayland.KeyboardKeyState
import wayland.PointerButtonState
import wayland.server.Display
import wayland.server.Listener
import wlroots.backend.Backend
import wlroots.render.Allocator
import wlroots.render.RectOptions
import wlroots.render.Renderer
import wlroots.types.*
import wlroots.types.Cursor
import wlroots.types.input.InputDevice
import wlroots.types.input.InputDevice.Type.*
import wlroots.types.input.Keyboard
import wlroots.types.input.KeyboardKeyEvent
import wlroots.types.output.Output
import wlroots.types.output.OutputLayout
import wlroots.types.output.OutputState
import wlroots.types.input.PointerAxisEvent
import wlroots.types.input.PointerButtonEvent
import wlroots.types.input.PointerMotionAbsoluteEvent
import wlroots.types.input.PointerMotionEvent
import wlroots.types.input.TabletToolAxisEvent
import wlroots.types.input.TouchCancelEvent
import wlroots.types.input.TouchDownEvent
import wlroots.types.input.TouchMotionEvent
import wlroots.types.input.TouchUpEvent
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

    val backendNewInputListener: Listener
    val backendNewOutputListener: Listener
    val backend = Backend.autocreate(display.eventLoop, null)?.apply {
        backendNewInputListener = events.newInput.add(::onNewInput)
        backendNewOutputListener = events.newOutput.add(::onNewOutput)
    } ?: error("Failed to create wlr_backend")

    val renderer = Renderer.autocreate(backend) ?: error("Failed to create wlr_renderer")
    val allocator = Allocator.autocreate(backend, renderer) ?: error("Failed to create wlr_renderer")
    val outputLayout = OutputLayout.create(display)
    val xcursorManager = XcursorManager.create(null, 24) ?: error("Failed to load default cursor")

    lateinit var output: Output
    lateinit var outputFrameListener: Listener
    lateinit var outputDestroyListener: Listener

    lateinit var keyboard: Keyboard
    lateinit var keyboardKeyListener: Listener
    lateinit var inputDeviceDestroyListener: Listener


    val cursorMotionListener: Listener
    val cursorMotionAbsoluteListener: Listener
    val cursorButtonListener: Listener
    val cursorAxisListener: Listener
    val cursorTouchUpListener: Listener
    val cursorTouchDownListener: Listener
    val cursorTouchMotionListener: Listener
    val cursorTouchCancelListener: Listener
    val cursorTabletToolAxisListener: Listener
    val cursor = Cursor.create().apply {
        with(events) {
            cursorMotionListener = motion.add(::cursorMotionHandler)
            cursorMotionAbsoluteListener = motionAbsolute.add(::onCursorMotionAbsolute)
            cursorButtonListener = button.add(::onCursorButton)
            cursorAxisListener = axis.add(::onCursorAxis)

            cursorTouchUpListener = touchUp.add(::cursorTouchUpHandler)
            cursorTouchDownListener = touchDown.add(::cursorTouchDownHandler)
            cursorTouchMotionListener = touchMotion.add(::cursorTouchMotionHandler)
            cursorTouchCancelListener = touchCancel.add(::cursorTouchCancelHandler)

            cursorTabletToolAxisListener = tabletToolAxis.add(::cursorTabletToolAxisHandler)
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

        // Listeners cleanup
        backendNewInputListener.remove()
        backendNewOutputListener.remove()
        cursorMotionListener.remove()
        cursorMotionAbsoluteListener.remove()
        cursorButtonListener.remove()
        cursorAxisListener.remove()
        cursorTouchUpListener.remove()
        cursorTouchDownListener.remove()
        cursorTouchMotionListener.remove()
        cursorTouchCancelListener.remove()
        cursorTabletToolAxisListener.remove()

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

        outputFrameListener = output.events.frame.add(::onOutputFrame)
        outputDestroyListener = output.events.destroy.add(::onOutputDestroy)

        outputLayout.addAuto(output) ?: error("Can't add wlr_output to wlr_output_layout")

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


    fun onOutputFrame(output: Output) {
        Arena.ofConfined().use { arena ->
            val state = OutputState.allocate(arena)
            state.init()
            val pass = output.beginRenderPass(state, null) ?: error("Failed to create wlr_render_pass")
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


    fun onOutputDestroy(output: Output) {
        outputLayout.remove(output)
        outputFrameListener.remove()
        outputDestroyListener.remove()
    }

    //
    // Keyboard: signal listeners
    //

    fun onNewInput(device: InputDevice) {
        when (device.type) {
            POINTER, TOUCH, TABLET -> cursor.attachInputDevice(device)
            KEYBOARD -> {
//                keyboard = device.keyboardFromInputDevice()
                keyboard = Keyboard.fromInputDevice(device)
                keyboardKeyListener = keyboard.events.key.add(::onKeyboardKey)
                inputDeviceDestroyListener = device.events.destroy.add(::onKeyboardDestroy)

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
        val keycode = key.keycode + 8 // Convert from libinput/evdev raw hardware code to xkbcommon ones.
        val keysym = keyboard.xkbState().keyGetOneSym(keycode)
        check(keysym != XkbKey.NoSymbol)

        println(">> hwkeycode=${key.keycode} keycode=$keycode, keysym=$keysym, state=${key.state}")

        if (keysym == XkbKey.Escape && key.state == KeyboardKeyState.RELEASED) {
            Log.logDebug(">> Terminating display...")
            display.terminate()
            Log.logDebug(">> ...terminated display!")
        }
    }


    fun onKeyboardDestroy(device: InputDevice) {
        keyboardKeyListener.remove()
        inputDeviceDestroyListener.remove()
    }

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

    //
    // Touch input: unimplemented because of lack of hardware to test on.
    //

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