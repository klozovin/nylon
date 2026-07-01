import wayland.server.Display
import wayland.server.Listener
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
import wlroots.util.Log
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.exitProcess


data class SurfaceExtra(
    val sceneSurface: SceneSurface,
    val border: SceneRect,
    val commitListener: Listener,
    val destroyListener: Listener,
)


object SceneGraph {
    const val borderWidth = 4
    var surfaceOffset = 20

    lateinit var display: Display
    lateinit var backend: Backend
    lateinit var renderer: Renderer
    lateinit var allocator: Allocator
    lateinit var compositor: Compositor

    lateinit var scene: Scene
    lateinit var sceneOutput: SceneOutput

    val surfaceExtras = mutableMapOf<Surface, SurfaceExtra>()

    lateinit var backendNewOutputListener: Listener
    lateinit var compositorNewSurfaceListener: Listener
    lateinit var outputFrameListener: Listener


    fun main(args: Array<String>) {
        Log.init(Log.Importance.DEBUG)

        display = Display.create()
        backend = Backend.autocreate(display.eventLoop, null) ?: error("Failed to create wlr_backend")
        scene = Scene.create()

        renderer = Renderer.autocreate(backend) ?: error("Failed to create wlr_renderer")
        renderer.initWlDisplay(display)

        allocator = Allocator.autocreate(backend, renderer) ?: error("Failed to create wlr_allocator")

        compositor = Compositor.create(display, 5, renderer)

        XdgShell.create(display, 2)

        backendNewOutputListener = backend.events.newOutput.add(::newOutputHandler)
        compositorNewSurfaceListener = compositor.events.newSurface.add(::newSurfaceHandler)

        val socket = display.addSocketAuto() ?: error {
            display.destroy()
            exitProcess(1)
        }

        if (!backend.start()) {
            display.destroy()
            exitProcess(1)
        }

        Timer(true).schedule(4000) {
            display.terminate()
        }

        ProcessBuilder().apply {
            if (args.isEmpty()) command("/usr/bin/gthumb")
            else command(*args)
            environment().put("WAYLAND_DISPLAY", socket)
            start()
        }

        Log.logInfo("Running Wayland compositor on WAYLAND_DISPLAY=$socket")
        display.run()

        display.destroyClients()

        compositorNewSurfaceListener.remove()
        backendNewOutputListener.remove()
        outputFrameListener.remove()

        display.destroy()
    }


    fun newOutputHandler(output: Output) {
        output.initRender(allocator, renderer)
        outputFrameListener = output.events.frame.add(::outputFrameHandler)
        sceneOutput = SceneOutput.create(scene, output)

        OutputState.allocateConfined { state ->
            state.init()
            state.setEnabled(true)
            output.preferredMode()?.let { state.setMode(it) }
            output.commitState(state)
            state.finish()
            output.createGlobal(display)
        }
    }


    // wlr_output.events.frame
    fun outputFrameHandler(output: Output) {
        if (!sceneOutput.commit())
            return
        sceneOutput.sendFrameDone()
    }


    fun newSurfaceHandler(surface: Surface) {
        val commitListener = surface.events.commit.add(::surfaceCommitHandler)
        val destroyListener = surface.events.destroy.add(::surfaceDestroyHandler)

        val surfaceBorder = SceneRect.create(scene.tree(), 0, 0, floatArrayOf(0.8f, 0.2f, 0.2f, 1.0f))
        surfaceBorder.node().setPosition(surfaceOffset, surfaceOffset)

        val sceneSurface = SceneSurface.create(scene.tree(), surface)
        sceneSurface.buffer().node().setPosition(surfaceOffset + borderWidth, surfaceOffset + borderWidth)

        surfaceExtras[surface] = SurfaceExtra(sceneSurface, surfaceBorder, commitListener, destroyListener)
        surfaceOffset += 50
    }


    // wlr_surface.events.commit
    fun surfaceCommitHandler(surface: Surface) {
        val surfaceBorder = surfaceExtras[surface]!!.border

        surfaceBorder.setSize(surface.current().width() + 2 * borderWidth, surface.current().height() + 2 * borderWidth)

        XdgToplevel.tryFromSurface(surface)?.let { topLevel ->
            if (topLevel.base.getInitialCommit())
                topLevel.setSize(0, 0)
        }
    }


    // wlr_surface.events.destroy
    fun surfaceDestroyHandler(surface: Surface) {
        val sceneSurface = surfaceExtras[surface]!!.sceneSurface
        val surfaceBorder = surfaceExtras[surface]!!.border

        surfaceExtras[surface]!!.commitListener.remove()
        surfaceExtras[surface]!!.destroyListener.remove()

        sceneSurface.buffer().node().destroy()
        surfaceBorder.node().destroy()
    }
}


fun main(args: Array<String>) {
    SceneGraph.main(args)
}


inline fun error(block: () -> Any): Nothing = error(block())