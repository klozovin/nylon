import wayland.server.Display
import wlroots.util.Log
import wlroots.backend.Backend
import wlroots.render.Allocator
import wlroots.render.Renderer
import wlroots.types.compositor.Compositor
import wlroots.types.compositor.Surface
import wlroots.types.output.Output
import wlroots.types.output.OutputState
import wlroots.types.scene.Scene
import wlroots.types.scene.SceneOutput
import wlroots.types.scene.SceneRect
import wlroots.types.scene.SceneSurface
import wlroots.types.xdgshell.XdgShell
import wlroots.types.xdgshell.XdgToplevel
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.exitProcess


object SceneGraph {
    var surfaceOffset = 0
    val borderWidth = 3

    lateinit var display: Display
    lateinit var backend: Backend
    lateinit var renderer: Renderer
    lateinit var allocator: Allocator
    lateinit var compositor: Compositor

    lateinit var scene: Scene
    lateinit var sceneOutput: SceneOutput

    lateinit var surface: Surface
    lateinit var surfaceBorder: SceneRect
    lateinit var sceneSurface: SceneSurface


    fun main() {
        Log.init(Log.Importance.DEBUG)
        display = Display.create()
        backend = Backend.autocreate(display.eventLoop, null) ?: error("Failed to create wlr_backend")
        renderer = Renderer.autocreate(backend) ?: error("Failed to create wlr_renderer")
        renderer.initWlDisplay(display)
        allocator = Allocator.autocreate(backend, renderer)
        compositor = Compositor.create(display, 5, renderer)


        scene = Scene.create()
        XdgShell.create(display, 2)

        backend.events.newOutput.add(::newOutputHandler)
        compositor.events.newSurface.add(::newSurfaceHandler)

        val socket = display.addSocketAuto() ?: error {
            display.destroy()
            exitProcess(1)
        }
        if (!backend.start()) {
            display.destroy()
            exitProcess(1)
        }

        ProcessBuilder().apply {
            command("/usr/bin/gthumb", "/home/karlo")
            environment().put("WAYLAND_DISPLAY", socket)
            start()
        }

        Log.logInfo("Running Wayland compositor on WAYLAND_DISPLAY=$socket")

        Timer().schedule(10000) {
            display.terminate()
        }

        display.run()
        display.destroyClients()
        display.destroy()
        exitProcess(0)
    }

    fun newOutputHandler(output: Output) {
        output.initRender(allocator, renderer)
        output.events.frame.add(::outputFrameHandler)
        sceneOutput = scene.outputCreate(output)

        OutputState.allocateConfined { outputState ->
            outputState.init()
            outputState.setEnabled(true)
            output.preferredMode()?.let { outputState.setMode(it) }
            output.commitState(outputState)
            outputState.finish()
            output.createGlobal(display)
        }
    }

    fun outputFrameHandler() {
        if (!sceneOutput.commit())
            return
        sceneOutput.sendFrameDone()
    }

    fun newSurfaceHandler(surface: Surface) {
        println("New surface")

        surface.events.commit.add(::surfaceCommitHandler)
        surface.events.destroy.add(::surfaceDestroyHandler)

        SceneGraph.surface = surface
        surfaceBorder = SceneRect.create(scene.tree(), 0, 0, floatArrayOf(0.5f, 0.5f, 0.5f, 1.0f))
        surfaceBorder.node().setPosition(surfaceOffset, surfaceOffset)
        sceneSurface = SceneSurface.create(scene.tree(), surface)
        sceneSurface.buffer().node().setPosition(surfaceOffset + borderWidth, surfaceOffset + borderWidth)

        surfaceOffset += 50
    }

    fun surfaceCommitHandler() {
        println("Surface commit")
        surfaceBorder.setSize(surface.current().width() + 2 * borderWidth, surface.current().height() + 2 * borderWidth)
        val xdgToplevel = XdgToplevel.tryFromSurface(surface)
        if (xdgToplevel != null && xdgToplevel.base().initialCommit())
            xdgToplevel.setSize(0, 0)

    }

    fun surfaceDestroyHandler() {
        println("Surface destroy")

    }
}


fun main() {
    SceneGraph.main()
}


inline fun error(block: () -> Any): Nothing = error(block())