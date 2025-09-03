package compositor

import wayland.server.Display
import wayland.util.Edge
import wlroots.backend.Backend
import wlroots.render.Allocator
import wlroots.render.Renderer
import wlroots.types.DataDeviceManager
import wlroots.types.XcursorManager
import wlroots.types.compositor.Subcompositor
import wlroots.types.output.OutputLayout
import wlroots.types.scene.Scene
import wlroots.types.scene.SceneOutputLayout
import wlroots.types.seat.PointerRequestSetCursorEvent
import wlroots.types.seat.RequestSetSelectionEvent
import wlroots.types.seat.Seat
import wlroots.types.xdgshell.XdgToplevel
import wlroots.util.Box
import wlroots.util.Log
import java.util.EnumSet
import kotlin.system.exitProcess
import wlroots.types.compositor.Compositor as WlrCompositor


class Compositor(val terminalPath: String? = null) {
    val display: Display
    val backend: Backend
    val renderer: Renderer
    val allocator: Allocator

    val scene: Scene
    val outputLayout: OutputLayout
    val sceneOutputLayout: SceneOutputLayout

    val xcursorManager: XcursorManager
    var cursorMode: CursorMode

    val seat: Seat

    val outputSystem: OutputSystem
    val inputSystem: InputSystem
    val windowSystem: WindowSystem

    lateinit var socket: String

    // TODO: Move to separate state machine
    // Grab mode: move, resize
    var grabbedToplevel: XdgToplevel? = null
    lateinit var grabGeobox: Box
    lateinit var resizeEdges: EnumSet<Edge>
    var grabX: Double = 0.0
    var grabY: Double = 0.0


    init {
        display = Display.create()
        backend = Backend.autocreate(display.eventLoop, null) ?: error("Failed to create wlr_backend")
        renderer = Renderer.autocreate(backend) ?: error("Failed to create wlr_renderer")
        renderer.initWlDisplay(display)
        allocator = Allocator.autocreate(backend, renderer) ?: error("Failed to create wlr_allocator")

        // TODO: Why not keep instances around?
        WlrCompositor.create(display, 5, renderer)
        Subcompositor.create(display)
        DataDeviceManager.create(display)


        scene = Scene.create()
        outputLayout = OutputLayout.create(display)
        sceneOutputLayout = scene.attachOutputLayout(outputLayout)

        xcursorManager = XcursorManager.create(null, 24) ?: error("Failed to create wlr_xcursor_manager")

        inputSystem = InputSystem(this)
        outputSystem = OutputSystem(this)
        windowSystem = WindowSystem(this)

        seat = Seat.create(display, "seat0").apply {
            events.requestSetCursor.add(::onSeatRequestSetCursor) // TODO: Maybe move to window system? It's a Pointer event
            events.requestSetSelection.add(::onSeatRequestSetSelection)
        }

        cursorMode = CursorMode.Passthrough
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

        // TODO: Delete, take from command line
        startProcess("/usr/bin/foot")

        Log.logInfo("Running Wayland compositor on WAYLAND_DISPLAY=$socket")
        display.run()
        cleanup()
    }


    fun stop() {
        Log.logInfo("Stopping Wayland compositor on WAYLAND_DISPLAY=$socket")
        display.terminate()
    }


    fun cleanup() {
        // Cleanup resources, must run after the wl_display_run() returns
        display.destroyClients()
        scene.tree().node.destroy()
        xcursorManager.destroy()
        inputSystem.cursor.destroy()
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
        // TODO: Move to input system?
        if (seat.pointerState().focusedClient() == event.seatClient)
            inputSystem.cursor.setSurface(event.surface, event.hotspotX, event.hotspotY)
    }


    fun onSeatRequestSetSelection(event: RequestSetSelectionEvent) {
        // TODO: This should go to some future 'clipboard' system
        seat.setSelection(event.source, event.serial)
    }

}


lateinit var COMPOSITOR: Compositor


fun main(args: Array<String>) {
    Log.init(Log.Importance.DEBUG)
    COMPOSITOR = Compositor(terminalPath = "/usr/bin/foot")
    COMPOSITOR.start()
}