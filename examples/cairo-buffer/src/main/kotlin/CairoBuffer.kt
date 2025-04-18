import jextract.drm.drm_fourcc_h
import org.freedesktop.cairo.Context
import org.freedesktop.cairo.Format
import org.freedesktop.cairo.ImageSurface
import wayland.server.Display
import wlroots.Log
import wlroots.wlr.Backend
import wlroots.wlr.render.Allocator
import wlroots.wlr.render.Renderer
import wlroots.wlr.types.*
import java.lang.foreign.Arena
import java.util.*
import kotlin.system.exitProcess


class CairoImageSurfaceBuffer(val width: Int, val height: Int) : Buffer.Impl(Arena.global()), Buffer.DataSource.Memory {
    val surface = ImageSurface.create(Format.ARGB32, width, height)

    fun init() {
        super.init(width, height)
    }

    override fun destroy() {
        finish()
        surface.destroy()
    }

    override fun beginDataAccess(flags: EnumSet<AccessFlag>): MemoryBufferFormat =
        MemoryBufferFormat(
            flags.contains(AccessFlag.READ),
            drm_fourcc_h.DRM_FORMAT_ARGB8888(), // TODO: turn into enum
            surface.data,
            surface.stride
        )

    override fun endDataAccess() {
        println("Ending data access, nothing to do right now")
    }
}


object CairoBuffer {
    lateinit var output: Output
    lateinit var display: Display
    lateinit var backend: Backend
    lateinit var allocator: Allocator
    lateinit var renderer: Renderer

    lateinit var scene: Scene
    lateinit var sceneOutput: SceneOutput

    lateinit var cairoSurfaceBuffer: CairoImageSurfaceBuffer


    fun run() {
        Log.init(Log.Importance.INFO)

        display = Display.create()
        backend = Backend.autocreate(display.eventLoop, null) ?: error("Can't proceed without Backend")
        scene = Scene.create()
        renderer = Renderer.autocreate(backend) ?: error("Can't proceed without Renderer")
        renderer.initWlDisplay(display)
        allocator = Allocator.autocreate(backend, renderer)

        backend.events.newOutput.add(::handleNewOutput)
        if (!backend.start()) {
            display.destroy()
            exitProcess(1)
        }

        // Cairo: Initialize everything
        cairoSurfaceBuffer = CairoImageSurfaceBuffer(256, 256)
        cairoSurfaceBuffer.init()
        val cairoContext = Context.create(cairoSurfaceBuffer.surface)

        // Cairo: Start drawing
        cairoContext.setSourceRGB(1.0, 1.0, 1.0)
        cairoContext.paint()
        cairoContext.setSourceRGB(0.0, 0.0, 0.0)
        cairoContext.moveTo(25.6, 128.0)
        cairoContext.curveTo(102.4, 230.4, 153.6, 25.6, 230.4, 128.0)
        cairoContext.lineWidth = 10.0
        cairoContext.stroke()
        cairoContext.destroy()

        // Scene buffer
        val sceneBuffer = SceneBuffer.create(scene.tree(), cairoSurfaceBuffer)
        sceneBuffer.node().setPosition(50, 50)
        cairoSurfaceBuffer.drop()
        display.run()
        display.destroy()
    }


    fun handleFrame() {
        sceneOutput.commit()
    }


    fun handleNewOutput(output: Output) {
        output.initRender(allocator, renderer)
        CairoBuffer.output = output
        output.events.frame.add(::handleFrame)
        sceneOutput = scene.outputCreate(CairoBuffer.output)

        Arena.ofConfined().use { arena ->
            val outputState = OutputState.allocate(arena)
            outputState.init()
            outputState.setEnabled(true)
            output.preferredMode()?.let { mode ->
                outputState.setMode(mode)
            }
            output.commitState(outputState)
            outputState.finish()
        }
    }
}


fun main() {
    CairoBuffer.run()
}