import jextract.drm.drm_fourcc_h.C_INT
import jextract.drm.drm_fourcc_h.C_POINTER
import jextract.wlroots.types.wlr_buffer_h.WLR_BUFFER_DATA_PTR_ACCESS_WRITE
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
import java.lang.foreign.MemorySegment
import java.util.*
import kotlin.system.exitProcess


object CairoBuffer {
    lateinit var display: Display
    lateinit var backend: Backend
    lateinit var scene: Scene
    lateinit var sceneOutput: SceneOutput
    lateinit var renderer: Renderer
    lateinit var allocator: Allocator
    lateinit var output: Output

//    lateinit var buffer: Buffer
//    lateinit var cairoBuffer: BufferImpl

    lateinit var cairoSurface: ImageSurface

    lateinit var cisBuffer: CairoImageSurfaceBuffer


    class CairoImageSurfaceBuffer(arena: Arena) : Buffer(arena) {
        val surface = ImageSurface.create(Format.ARGB32, 256, 256)

        override fun implDestroy() {
            finish()
            surface.destroy()
            println("Seek & destroy")
        }

        override fun implBeginDataAccess(flags: EnumSet<AccessFlag>): BufferDataFormat =
            BufferDataFormat(
                flags.contains(AccessFlag.READ),
                jextract.drm.drm_fourcc_h.DRM_FORMAT_ARGB8888(), // TODO: turn into enum
                surface.data,
                surface.stride
            )

        override fun implEndDataAccess() {
            println("Ending data access, nothing to do right now")
        }
    }


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

        cisBuffer = CairoImageSurfaceBuffer(Arena.global())
        cisBuffer.init(256, 256)


//        cairoBuffer = BufferImpl.allocate(Arena.global()).apply {
//            destroy(::cairoBufferDestroy)
//            begin_data_ptr_access(::beginDataPtrAccess)
//            end_data_ptr_access(::endDataPtrAccess)
//        }
//        buffer = Buffer.allocate(Arena.global())
//        buffer.init(cairoBuffer, 256, 256)


        // Cairo: Initialize surface and context
        val cairoContext = Context.create(cisBuffer.surface)

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
        val sceneBuffer = SceneBuffer.create(scene.tree(), cisBuffer)
        sceneBuffer.node().setPosition(50, 50)
        cisBuffer.drop()
        display.run()

        // Cleanup
        display.destroy()
//        surface.close()
    }

    // static void cairo_buffer_destroy(struct wlr_buffer *wlr_buffer) {
    // TODO: create object automatically
    fun cairoBufferDestroy(bufferPtr: MemorySegment) {
//        struct cairo_buffer *buffer = wl_container_of(wlr_buffer, buffer, base);
//        cairo_surface_destroy(buffer->surface);
//        free(buffer);
        cairoSurface.destroy()
    }

    fun beginDataPtrAccess(
        bufferPtr: MemorySegment,
        flags: Int,
        data: MemorySegment,
        format: MemorySegment,
        stride: MemorySegment
    ): Boolean {
        if (flags and WLR_BUFFER_DATA_PTR_ACCESS_WRITE() != 0)
            return false

        format.set(C_INT, 0, jextract.drm.drm_fourcc_h.DRM_FORMAT_ARGB8888())
        data.set(C_POINTER, 0, cairoSurface.data)
        stride.set(C_INT, 0, cairoSurface.stride)

        return true
    }

    fun endDataPtrAccess(bufferPtr: MemorySegment) {}

    fun outputHandleFrame() {
        sceneOutput.commit()
    }

    fun handleNewOutput(output: Output) {
        output.initRender(allocator, renderer)
        CairoBuffer.output = output
        output.events.frame.add(::outputHandleFrame)
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