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
import wlroots.types.scene.SceneOutputLayout
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

    lateinit var display: Display
    lateinit var backend: Backend
    lateinit var renderer: Renderer
    lateinit var allocator: Allocator
    lateinit var output: Output

    lateinit var keyboard: Keyboard

    lateinit var cursor: Cursor
    lateinit var xcursorManager: XcursorManager
    lateinit var cursorMode: CursorMode

    lateinit var scene: Scene
    lateinit var outputLayout: OutputLayout
    lateinit var sceneOutputLayout: SceneOutputLayout

    lateinit var xdgShell: XdgShell
    lateinit var seat: Seat


    fun main(args: Array<String>) {
        display = Display.create()
        backend = Backend.autocreate(display.eventLoop, null) ?: error("Failed to create wlr_backend")
        renderer = Renderer.autocreate(backend)?: error("Failed to create wlr_renderer")

        renderer.initWlDisplay(display)

        allocator = Allocator.autocreate(backend, renderer) ?: error("Failed to create wlr_allocator")

        backend.events.newOutput.add(::onNewOutput)
        backend.events.newInput.add(::onNewInput)

        Compositor.create(display, 5, renderer)
        Subcompositor.create(display)
        DataDeviceManager.create(display)

        scene = Scene.create()
        outputLayout = OutputLayout.create(display)
        sceneOutputLayout = scene.attachOutputLayout(outputLayout)

        xdgShell = XdgShell.create(display, 3)
        xdgShell.events.newToplevel.add(::onNewToplevel)
        xdgShell.events.newPopup.add(::onNewPopup)

        cursor = Cursor.create().apply {
            attachOutputLayout(outputLayout)
            events.motion.add(::onCursorMotion)
            events.motionAbsolute.add(::onCursorMotionAbsolute)
            events.button.add(::onCursorButton)
            events.axis.add(::onCursorAxis)
            events.frame.add(::onCursorFrame)
        }
        xcursorManager = XcursorManager.create(null, 24) ?: error("Failed to create wlr_xcursor_manager")
        cursorMode = CursorMode.Passthrough

        seat = Seat.create(display, "seat0")
        seat.events.requestSetCursor.add(::onSeatRequestSetCursor)
        seat.events.requestSetSelection.add(::onSeatRequestSetSelection)

        val socket = display.addSocketAuto() ?: error {
            backend.destroy()
            exitProcess(1)
        }

        // Start the backend: enumerate outputs, inputs, become DRM master, events, ...
        if (!backend.start()) {
            backend.destroy()
            display.destroy()
            exitProcess(1)
        }

        // Run startup command or the default terminal
//        ProcessBuilder().apply {
//            if (args.isEmpty()) command("/usr/bin/foot")
//            else command(*args)
//            environment().put("WAYLAND_DISPLAY", socket)
//            start()
//        }

        // Run the Wayland event loop, it does not return until you exit the compositor.
        Log.logInfo("Running Wayland compositor on WAYLAND_DISPLAY=$socket")
        display.run()

        // Cleanup after the wl_display_run() returns
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
        return



        when (inputDevice.type()) {
            // Typing device
            InputDevice.Type.KEYBOARD -> {
                println(">>> onNewInput: KEYBOARD")
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
