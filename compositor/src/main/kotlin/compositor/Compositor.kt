package compositor

import wayland.server.Display
import wlroots.backend.Backend
import wlroots.render.Allocator
import wlroots.render.Renderer
import wlroots.types.Cursor
import wlroots.types.DataDeviceManager
import wlroots.types.XcursorManager
import wlroots.types.compositor.Subcompositor
import wlroots.types.output.Output
import wlroots.types.output.OutputLayout
import wlroots.types.scene.Scene
import wlroots.types.scene.SceneOutputLayout
import wlroots.types.seat.PointerRequestSetCursorEvent
import wlroots.types.seat.RequestSetSelectionEvent
import wlroots.types.seat.Seat
import wlroots.types.xdgshell.XdgShell
import wlroots.util.Log
import kotlin.system.exitProcess
import wlroots.types.compositor.Compositor as WlrCompositor


class Compositor {
    val display: Display
    val backend: Backend
    val renderer: Renderer
    val allocator: Allocator

    val scene: Scene
    val outputLayout: OutputLayout
    val sceneOutputLayout: SceneOutputLayout

    val cursor: Cursor
    val xcursorManager: XcursorManager
    val cursorMode: CursorMode

    val xdgShell: XdgShell

    val seat: Seat

    val inputSystem: InputSystem
    val windowSystem: WindowSystem

    lateinit var socket: String


    init {
        display = Display.create()
        backend = Backend.autocreate(display.eventLoop, null) ?: error("Failed to create wlr_backend")
        renderer = Renderer.autocreate(backend) ?: error("Failed to create wlr_renderer")

        renderer.initWlDisplay(display) // TODO: Maybe move down, create up top

        allocator = Allocator.autocreate(backend, renderer) ?: error("Failed to create wlr_allocator")


        // TODO: systems should init handlers themselves
        inputSystem = InputSystem(this)
        windowSystem = WindowSystem(this)


        // TODO: Definitely move down
        backend.events.newOutput.add(::onNewOutput)
        backend.events.newInput.add(inputSystem::onNewInput)

        // TODO: Why now keep instances around?
        WlrCompositor.create(display, 5, renderer)
        Subcompositor.create(display)
        DataDeviceManager.create(display)

        outputLayout = OutputLayout.create(display) // TODO: order, down

        scene = Scene.create()
        sceneOutputLayout = scene.attachOutputLayout(outputLayout)

        xdgShell = XdgShell.create(display, 3)
        xdgShell.events.newToplevel.add(windowSystem::onNewToplevel) // TODO: Maybe move to window system
        xdgShell.events.newPopup.add(windowSystem::onNewPopup)

        // TODO: move between, or to input system
        cursor = Cursor.create().apply {
            attachOutputLayout(outputLayout)
            with(events) {
                motion.add(inputSystem::onCursorMotion)
                motionAbsolute.add(inputSystem::onCursorMotionAbsoulute)
                button.add(inputSystem::onCursorButton)
                axis.add(inputSystem::onCursorAxis)
                frame.add(inputSystem::onCursorFrame)
            }
        }
        xcursorManager = XcursorManager.create(null, 24) ?: error("Failed to create wlr_xcursor_manager")
        cursorMode = CursorMode.Passthrough

        seat = Seat.create(display, "seat0").apply {
            events.requestSetCursor.add(::onSeatRequestSetCursor) // TODO: Maybe move to window system? It's a Pointer event
            events.requestSetSelection.add(::onSeatRequestSetSelection)
        }
    }


    fun start() {
        socket = display.addSocketAuto() ?: error {
            backend.destroy()
            exitProcess(1)
        }

        // Start the backend: enumerate outputs, inputs, become DRM master, events, ...
        if (!backend.start()) {
            backend.destroy()
            display.destroy()
            exitProcess(1)
        }

        Log.logInfo("Running Wayland compositor on WAYLAND_DISPLAY=$socket")
        display.run()
    }


    fun stop() {
        display.terminate()

        // Cleanup after the wl_display_run() returns
        // TODO: Anything missing?
        display.destroyClients()
        scene.tree().node().destroy()
        xcursorManager.destroy()
        cursor.destroy()
        allocator.destroy()
        renderer.destroy()
        backend.destroy()
        display.destroy()
    }


    fun startProcess(processPath: String) {
        ProcessBuilder().apply {
            command(processPath)
            environment().put("WAYLAND_DISPLAY", socket)
            start()
        }
    }


    // *** Events *** //

    fun onNewAnything(x: Any) {
        TODO()
    }

    fun onNewOutput(output: Output) {

    }

    fun onSeatRequestSetCursor(event: PointerRequestSetCursorEvent) {
        TODO()
    }

    fun onSeatRequestSetSelection(event: RequestSetSelectionEvent) {
        TODO()
    }

}


enum class CursorMode {
    Passthrough,
    Move,
    Resize
}
