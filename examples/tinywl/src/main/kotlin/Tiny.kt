import wayland.KeyboardKeyState
import wayland.SeatCapability
import wayland.server.Display
import wayland.server.Listener
import wlroots.backend.Backend
import wlroots.render.Allocator
import wlroots.render.Renderer
import wlroots.types.*
import wlroots.types.compositor.Compositor
import wlroots.types.compositor.Subcompositor
import wlroots.types.compositor.Surface
import wlroots.types.output.EventRequestState
import wlroots.types.output.Output
import wlroots.types.output.OutputLayout
import wlroots.types.output.OutputState
import wlroots.types.pointer.PointerAxisEvent
import wlroots.types.pointer.PointerButtonEvent
import wlroots.types.pointer.PointerMotionAbsoluteEvent
import wlroots.types.pointer.PointerMotionEvent
import wlroots.types.scene.Scene
import wlroots.types.scene.SceneOutput
import wlroots.types.scene.SceneOutputLayout
import wlroots.types.scene.SceneTree
import wlroots.types.seat.PointerRequestSetCursorEvent
import wlroots.types.seat.RequestSetSelectionEvent
import wlroots.types.seat.Seat
import wlroots.types.xdgshell.XdgPopup
import wlroots.types.xdgshell.XdgShell
import wlroots.types.xdgshell.XdgSurface
import wlroots.types.xdgshell.XdgToplevel
import wlroots.util.Log
import xkbcommon.Keymap
import xkbcommon.XkbContext
import xkbcommon.XkbKey
import java.lang.foreign.MemorySegment
import java.util.*
import kotlin.system.exitProcess


data class TinyXdgToplevel(
    val xdgToplevel: XdgToplevel,
    val sceneTree: SceneTree,

    // TODO: Maybe add listeners here
)

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

    val xdgToplevels = mutableMapOf<MemorySegment, TinyXdgToplevel>()

    // TODO: Better way to handle this? (what if multiple outputs, output gets reconnected...?)
    val listeners = mutableMapOf<String, Listener>()


    fun main(args: Array<String>) {
        display = Display.create()
        backend = Backend.autocreate(display.eventLoop, null) ?: error("Failed to create wlr_backend")
        renderer = Renderer.autocreate(backend) ?: error("Failed to create wlr_renderer")

        renderer.initWlDisplay(display)

        allocator = Allocator.autocreate(backend, renderer) ?: error("Failed to create wlr_allocator")

        backend.events.newOutput.add(::onNewOutput)
        backend.events.newInput.add(::onNewInput)

        Compositor.create(display, 5, renderer)
        Subcompositor.create(display)
        DataDeviceManager.create(display)

        outputLayout = OutputLayout.create(display)

        scene = Scene.create()
        sceneOutputLayout = scene.attachOutputLayout(outputLayout)

        xdgShell = XdgShell.create(display, 3)
        xdgShell.events.newToplevel.add(TopLevel::onNew)
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
        ProcessBuilder().apply {
            if (args.isEmpty()) command("/usr/bin/foot")
            else command(*args)
            environment().put("WAYLAND_DISPLAY", socket)
            start()
        }

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


    fun processCursorMotion(timeMsec: Int) {
        TODO()
        // Process the event in the compositor
        when (cursorMode) {
            CursorMode.Move -> {
                processCursorMove(timeMsec)
                return
            }

            CursorMode.Resize -> {
                processCursorResize(timeMsec)
                return
            }

            else -> Unit
        }

        // Forward events to the client under the pointer
        TODO()
    }

    private fun processCursorMove(timeMsec: Int) {
        TODO()
    }

    private fun processCursorResize(timeMsec: Int) {
        TODO()
    }

    //
    // *** Event handlers ***
    //

    //
    // Backend events: newOutput, newInput
    //

    fun onNewOutput(newOutput: Output) {
        require(!::output.isInitialized) { "Only one output supported." }

        output = newOutput
        output.initRender(allocator, renderer)

        OutputState.allocateConfined { outputState ->
            outputState.init()
            outputState.setEnabled(true)
            output.preferredMode()?.let { outputState.setMode(it) }
            output.commitState(outputState)
            outputState.finish()
        }

        listeners["output.frame"] = output.events.frame.add(::onOutputFrame)
        listeners["output.request_state"] = output.events.requestState.add(::onOutputRequestState)
        listeners["output.destroy"] = output.events.destroy.add(::onOutputDestroy)

        val outputLayoutOutput = outputLayout.addAuto(output)
        val sceneOutput = SceneOutput.create(scene, output)
        sceneOutputLayout.addOutput(outputLayoutOutput, sceneOutput)
    }

    fun onNewInput(inputDevice: InputDevice) {
        when (inputDevice.type()) {

            // Typing device
            InputDevice.Type.KEYBOARD -> {
                require(!::keyboard.isInitialized) { "Only one keyboard input device supported" }
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
                inputDevice.events.destroy.add(::onKeyboardDestroy)
                seat.setKeyboard(keyboard)

                keymap.unref()
                context.unref()
            }

            // Pointing device
            InputDevice.Type.POINTER -> cursor.attachInputDevice(inputDevice)

            else -> println("Unknown device type: ${inputDevice.type()}")
        }

        val capabilities = EnumSet.of(SeatCapability.POINTER)
        if (::keyboard.isInitialized)
            capabilities.add(SeatCapability.KEYBOARD)
        seat.setCapabilities(capabilities)
    }

    //
    // Output events
    //

    fun onOutputFrame(output: Output) {
        val sceneOutput = scene.getSceneOutput(output)!!
        sceneOutput.commit()
        sceneOutput.sendFrameDone()
    }

    fun onOutputRequestState(event: EventRequestState) {
        output.commitState(event.state)
    }

    fun onOutputDestroy(output: Output) {
        TODO()
        listeners["output.frame"]!!.remove()
        listeners["output.request_state"]!!.remove()
        listeners["output.destroy"]!!.remove()
    }


    //
    // Keyboard events
    //

    fun onKeyboardKey(event: KeyboardKeyEvent) {

        return

        println("Handling key")
        val keycode = event.keycode() + 8
        val keysym = keyboard.xkbState().keyGetOneSym(keycode)

        var handledInCompositor = false

        if (keyboard.modifiers.isAltDown && event.state() == KeyboardKeyState.PRESSED) {
            when (keysym) {
                XkbKey.Escape -> {
                    display.terminate()
                    handledInCompositor = true
                }

                XkbKey.F1 -> {
                    // TODO: 	case XKB_KEY_F1
                    // cycle
                    handledInCompositor = true
                }

                XkbKey.F12 -> {
                    Log.logInfo("Heeelllooouuu from TinyWL")
                    handledInCompositor = true
                }
            }
        }

        if (!handledInCompositor) {
            seat.setKeyboard(keyboard) // TODO: What if multiple keyboards
            seat.keyboardNotifyKey(event.timeMsec(), event.keycode(), event.state())
        }
    }

    fun onKeyboardModifiers() {
        TODO()
        println("Sending modifiers")
        seat.setKeyboard(keyboard) // TODO: What if multiple keyboards
        seat.keyboardNotifyModifiers(keyboard.modifiers())
    }

    fun onKeyboardDestroy(device: InputDevice) {
        TODO()
    }


    //
    // Cursor events
    //

    fun onCursorMotion(event: PointerMotionEvent) {
        TODO()
        cursor.move(event.pointer.base(), event.deltaY, event.deltaY)
        processCursorMotion(event.timeMsec)
    }

    fun onCursorMotionAbsolute(event: PointerMotionAbsoluteEvent) {
        TODO()
    }

    fun onCursorButton(event: PointerButtonEvent) {
        TODO()
    }

    fun onCursorAxis(event: PointerAxisEvent) {
        TODO()
    }

    fun onCursorFrame() {
        TODO()
    }

    //
    // XDG Shell: Top level
    //
    object TopLevel {

        fun onNew(toplevel: XdgToplevel) {
            val sceneTree = scene.tree().xdgSurfaceCreate(toplevel.base())




            // Event handlers for the base Surface
            with(toplevel.base().surface().events) {

//                map.add(::onMap)

                map.add {
                    println("here i am")
                }

                unmap.add(::onXdgToplevelUnmap)
                commit.add(::onCommit)
                destroy.add(::onXdgToplevelDestroy)
            }

            // Event handlers for the XDG Toplevel surface
            with(toplevel.events) {
                requestMove.add(::onXdgToplevelRequestMove)
                requestResize.add(::onXdgToplevelRequestResize)
                requestMaximize.add(::onXdgToplevelRequestMaximize)
                requestFullscreen.add(::onXdgToplevelRequestFullscreen)
            }

            xdgToplevels[toplevel.xdgToplevelPtr] = TinyXdgToplevel(
                xdgToplevel = toplevel,
                sceneTree = sceneTree,
            )
        }


        fun onCommit(surface: Surface) {
            println("##############")
//            TODO()
            val topLevel = xdgToplevels
                .values
                .find { it.xdgToplevel.base().surface().surfacePtr == surface.surfacePtr }!!
                .xdgToplevel

            // When an xdg_surface performs an initial commit, the compositor must reply with a configure so the
            // client can map the surface. tinywl configures the xdg_toplevel with 0,0 size to let the client pick
            // the dimensions itself.
            if (topLevel.base().initialCommit())
                topLevel.setSize(0, 0)
        }

        fun onMap(surface: Surface) {
            TODO() // check if surface is surface
        }

        fun onXdgToplevelUnmap(surface: Surface) {
            TODO()
        }

        fun onXdgToplevelDestroy(surface: Surface) {
            TODO()
        }
    }

    //

    fun onNewPopup(popup: XdgPopup) {
        TODO()

        val parent = XdgSurface.tryFromSurface(popup.parent()) ?: error("Popup parent can't be null")

        // TODO: Is this way ok???
        val parentSceneTree = xdgToplevels
            .values
            .find { it.xdgToplevel.base().xdgSurfacePtr == parent.xdgSurfacePtr }!!
            .sceneTree
        parentSceneTree.xdgSurfaceCreate(popup.base())

        popup.base().surface().events.commit.add(::onXdgPopupCommit)
        popup.events.destroy.add(::onXdgPopupDestroy)
    }

    fun onXdgToplevelRequestMove() {
        TODO()
    }

    fun onXdgToplevelRequestResize() {
        TODO()
    }

    fun onXdgToplevelRequestMaximize() {
        TODO()
    }

    fun onXdgToplevelRequestFullscreen() {
        TODO()
    }

    fun onXdgPopupCommit(surface: Surface) {
        TODO()
    }

    fun onXdgPopupDestroy() {
        TODO()
    }

    //
    // Seat events
    //

    fun onSeatRequestSetCursor(event: PointerRequestSetCursorEvent) {
        TODO()
    }

    fun onSeatRequestSetSelection(selection: RequestSetSelectionEvent) {
        TODO()

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
