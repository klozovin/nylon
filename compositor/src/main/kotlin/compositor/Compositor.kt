package compositor

import compositor.input.CursorInputMode
import compositor.input.InputSystem
import compositor.inspector.Inspector
import compositor.output.OutputSystem
import compositor.windows.WindowSystem
import wayland.server.Display
import wayland.server.Listener
import wlroots.backend.Backend
import wlroots.render.Allocator
import wlroots.render.Renderer
import wlroots.types.compositor.Subcompositor
import wlroots.types.data_device.DataDeviceManager
import wlroots.types.seat.Seat
import wlroots.types.xcursor_manager.XcursorManager
import wlroots.util.Log
import kotlin.system.exitProcess
import wlroots.types.compositor.Compositor as WlrCompositor


class Compositor(val terminalPath: String? = null) {
    val display: Display
    val backend: Backend
    val renderer: Renderer
    val allocator: Allocator

    val xcursorManager: XcursorManager

    val outputSystem: OutputSystem
    val inputSystem: InputSystem
    val windowSystem: WindowSystem

    val onBackendNewInputListener: Listener
    val onBackendNewOutputListener: Listener
    val onBackendDestroyListener: Listener

    lateinit var socket: String

    // TODO: Move to inputSystem
    val captureMode: CursorInputMode


    // TODO: Delete
    val seat: Seat


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

        xcursorManager = XcursorManager.create(null, 24) ?: error("Failed to create wlr_xcursor_manager")

        COMPOSITOR = this

        windowSystem = WindowSystem(this)
        outputSystem = OutputSystem(this)
        inputSystem = InputSystem(this)

        with(backend.events) {
            onBackendNewInputListener = newInput.add(inputSystem::onNewInput)
            onBackendNewOutputListener = newOutput.add(outputSystem::onNewOutput)
            onBackendDestroyListener = destroy.add(::onBackendDestroy)
        }

        // TODO Remove later, reference it directly from the input system
        seat = inputSystem.seat


        captureMode = CursorInputMode(this)
    }


    fun onBackendDestroy(backend: Backend) {
        onBackendNewInputListener.remove()
        onBackendNewOutputListener.remove()
        onBackendDestroyListener.remove()
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

        // Run the dummy shell thingy
        val dummy = Inspector(socket)
        dummy.run()

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
        windowSystem.scene.destroy()
        xcursorManager.destroy()
        inputSystem.cursor.destroy()
        allocator.destroy()
        renderer.destroy()
        backend.destroy()

        // Probably best to call after destroying the backend
        seat.destroy()

        display.destroy()
    }


    fun startProcess(processPath: String) {
        ProcessBuilder().apply {
            command(processPath)
            environment().put("WAYLAND_DISPLAY", socket)
            start()
        }
    }
}


lateinit var COMPOSITOR: Compositor


fun main(args: Array<String>) {
    Log.init(Log.Importance.Debug)
    COMPOSITOR = Compositor(terminalPath = "/usr/bin/foot")
    COMPOSITOR.start()
}