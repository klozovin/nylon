import wayland.SeatCapability
import wayland.server.Display
import wlroots.backend.Backend
import wlroots.render.Allocator
import wlroots.render.Renderer
import wlroots.types.*
import wlroots.types.compositor.Compositor
import wlroots.types.compositor.Subcompositor
import wlroots.types.output.Output
import wlroots.types.output.OutputLayout
import wlroots.types.pointer.PointerAxisEvent
import wlroots.types.pointer.PointerButtonEvent
import wlroots.types.pointer.PointerMotionAbsoluteEvent
import wlroots.types.pointer.PointerMotionEvent
import wlroots.types.scene.Scene
import wlroots.types.seat.PointerRequestSetCursorEvent
import wlroots.types.seat.RequestSetSelectionEvent
import wlroots.types.seat.Seat
import wlroots.types.xdgshell.XdgPopup
import wlroots.types.xdgshell.XdgShell
import wlroots.types.xdgshell.XdgToplevel
import wlroots.util.Log
import xkbcommon.Keymap
import xkbcommon.XkbContext
import java.util.*
import kotlin.system.exitProcess

object Tiny {

    val display = Display.create()

    val backend = Backend.autocreate(display.eventLoop, null)?.apply {
        events.newOutput.add(::onNewOutput)
        events.newInput.add(::onNewInput)
    } ?: error("Failed to create wlr_backend")

    val renderer = Renderer.autocreate(backend)?.apply {
        initWlDisplay(display)
    } ?: error("Failed to create wlr_renderer")
    val allocator = Allocator.autocreate(backend, renderer) ?: error("Failed to create wlr_allocator")

    val compositor = Compositor.create(display, 5, renderer)
    val subCompositor = Subcompositor.create(display)

    val dataDeviceManager = DataDeviceManager.create(display)

    val scene = Scene.create()
    val outputLayout = OutputLayout.create(display)
    val sceneOutputLayout = scene.attachOutputLayout(outputLayout)

    val xdgShell = XdgShell.create(display, 3).apply {
        events.newToplevel.add(::onNewToplevel)
        events.newPopup.add(::onNewPopup)
    }

    lateinit var keyboard: Keyboard

    val cursor = Cursor.create().apply {
        attachOutputLayout(outputLayout)
        with(events) {
            motion.add(::onCursorMotion)
            motionAbsolute.add(::onCursorMotionAbsolute)
            button.add(::onCursorButton)
            axis.add(::onCursorAxis)
            frame.add(::onCursorFrame)
        }
    }
    val xcursorManager = XcursorManager.create(null, 24) ?: error("Failed to create wlr_xcursor_manager")
    var cursorMode = CursorMode.Passthrough

    val seat = Seat.create(display, "seat0").apply {
        events.requestSetCursor.add(::onSeatRequestSetCursor)
        events.requestSetSelection.add(::onSeatRequestSetSelection)
    }


    fun main(args: Array<String>) {

        val socket = display.addSocketAuto() ?: error {
            backend.destroy()
            exitProcess(1)
        }

        if (!backend.start()) {
            backend.destroy()
            display.destroy()
            exitProcess(1)
        }

        // Run startup command or the default terminal
        ProcessBuilder().apply {
            if (args.isEmpty()) command("/usr/bin/foot")
            else command(*args)
            environment().put("WAYLAND_DISPLAY", socket)
            start()
        }

        Log.logInfo("Running Wayland compositor on WAYLAND_DISPLAY=$socket")
        display.run()

        // Cleanup everything
        display.destroyClients()
        scene.tree().node().destroy()
        xcursorManager.destroy()
        cursor.destroy()
        allocator.destroy()
        renderer.destroy()
        backend.destroy()
        display.destroy()
    }


    //
    // Backend events
    //

    fun onNewOutput(output: Output) {

    }

    fun onNewInput(inputDevice: InputDevice) {
        when (inputDevice.type()) {

            // Typing device
            InputDevice.Type.KEYBOARD -> {
                println(">>> onNewInput: KEYBOARD")
                check(!::keyboard.isLateinit)
                // TODO: Multiple keyboards

                keyboard = inputDevice.keyboardFromInputDevice()
                val context = XkbContext.of(XkbContext.Flags.NO_FLAGS) ?: error {
                    Log.logError("Failed to create XKB context")
                    exitProcess(1)
                }
                val keymap = context.keymapNewFromNames(null, Keymap.CompileFlags.NO_FLAGS) ?: error {
                    Log.logError("Failed to create XKB context")
                    exitProcess(1)
                }

                with(keyboard) {
                    setKeymap(keymap)
                    setRepeatInfo(25, 600)
                    events.modifiers.add(::onKeyboardModifiers)
                    events.key.add(::onKeyboardKey)
                }
                inputDevice.events.destroy.add(::onInputDeviceDestroy)

                seat.setKeyboard(keyboard)

                keymap.unref()
                context.unref()
            }

            // Pointing device
            InputDevice.Type.POINTER -> cursor.attachInputDevice(inputDevice)

            else -> println("Unknown device type: ${inputDevice.type()}")
        }

        seat.setCapabilities(EnumSet.noneOf(SeatCapability::class.java).apply {
            add(SeatCapability.POINTER)
            if (::keyboard.isLateinit) add(SeatCapability.KEYBOARD)
        })
    }

    //
    // Keyboard events
    //

    fun onKeyboardModifiers() {
        println("MODDDDDDDDDDSSSSSSSSS")
    }

    fun onKeyboardKey(event: KeyboardKeyEvent) {

    }

    fun onInputDeviceDestroy() {

    }


    //
    // Cursor events
    //

    fun onCursorMotion(event: PointerMotionEvent) {

    }

    fun onCursorMotionAbsolute(event: PointerMotionAbsoluteEvent) {

    }

    fun onCursorButton(event: PointerButtonEvent) {

    }

    fun onCursorAxis(event: PointerAxisEvent) {

    }

    fun onCursorFrame() {

    }

    //
    // XDG Shell events
    //

    fun onNewToplevel(toplevel: XdgToplevel) {

    }

    fun onNewPopup(popup: XdgPopup) {

    }

    //
    // Seat events
    //

    fun onSeatRequestSetCursor(event: PointerRequestSetCursorEvent) {

    }

    fun onSeatRequestSetSelection(selection: RequestSetSelectionEvent) {

    }

    enum class CursorMode {
        Passthrough,
        Move,
        Resize
    }

}


fun main(args: Array<String>) {
    Log.init(Log.Importance.DEBUG)
    Tiny.main(args)

    // TODO: take startup command from input
}


inline fun error(block: () -> Any): Nothing = error(block())
