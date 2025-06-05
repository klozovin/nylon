package compositor

import wayland.server.Display
import wlroots.backend.Backend
import wlroots.render.Allocator
import wlroots.render.Renderer
import wlroots.types.Cursor
import wlroots.types.DataDeviceManager
import wlroots.types.XcursorManager
import wlroots.types.compositor.Subcompositor
import wlroots.types.output.OutputLayout
import wlroots.types.scene.Scene
import wlroots.types.scene.SceneOutputLayout
import wlroots.types.seat.PointerRequestSetCursorEvent
import wlroots.types.seat.RequestSetSelectionEvent
import wlroots.types.seat.Seat
import wlroots.types.xdgshell.XdgShell
import wlroots.types.xdgshell.XdgToplevel
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
    var cursorMode: CursorMode

    val xdgShell: XdgShell

    val seat: Seat

    val outputSystem: OutputSystem
    val inputSystem: InputSystem
    val windowSystem: WindowSystem

    lateinit var socket: String

    // TODO: Move to separate state machine
    // Grab mode: move, resize
    var grabbedToplevel: XdgToplevel? = null
    var grabX: Double = 0.0
    var grabY: Double = 0.0


    init {
        display = Display.create()
        backend = Backend.autocreate(display.eventLoop, null) ?: error("Failed to create wlr_backend")
        renderer = Renderer.autocreate(backend) ?: error("Failed to create wlr_renderer")

        renderer.initWlDisplay(display) // TODO: Maybe move down, create up top

        allocator = Allocator.autocreate(backend, renderer) ?: error("Failed to create wlr_allocator")


        // TODO: systems should init handlers themselves
        inputSystem = InputSystem(this)
        outputSystem = OutputSystem(this)
        windowSystem = WindowSystem(this)


        // TODO: Definitely move down
        // This starts everything
        backend.events.newOutput.add(outputSystem::onNewOutput)
        backend.events.newInput.add(inputSystem::onNewInput)

        // TODO: Why not keep instances around?
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
                motionAbsolute.add(inputSystem::onCursorMotionAbsolute)
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

        // TODO: DELETE
        ProcessBuilder().apply {
            command("/usr/bin/foot")
            environment().put("WAYLAND_DISPLAY", socket)
            start()
        }



        Log.logInfo("Running Wayland compositor on WAYLAND_DISPLAY=$socket")
        display.run()
        cleanup()
    }


    fun stop() {
        Log.logInfo("Stopping Wayland compositor on WAYLAND_DISPLAY=$socket")
        display.terminate()
    }


    fun cleanup() {
        // Cleanup after the wl_display_run() returns
        // TODO: Anything missing? check with tinywl.c
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


    // *** Seat signals *** //

    fun onSeatRequestSetCursor(event: PointerRequestSetCursorEvent) {
        println("tusmo")
        val focusedClient = seat.pointerState().focusedClient()
        if (focusedClient?.seatClientPtr == event.seatClient.seatClientPtr) // TODO: Implement equals() for SeatClient
            cursor.setSurface(event.surface, event.hotspotX, event.hotspotY)
    }


    fun onSeatRequestSetSelection(event: RequestSetSelectionEvent) {
        seat.setSelection(event.source, event.serial)
    }

}


enum class CursorMode {
    Move,
    Resize,
    Passthrough
}