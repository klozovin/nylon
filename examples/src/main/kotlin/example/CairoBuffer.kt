package example

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
import kotlin.system.exitProcess


object CairoBuffer {

    lateinit var display: Display
    lateinit var backend: Backend
    lateinit var scene: Scene
    lateinit var sceneOutput: SceneOutput
    lateinit var renderer: Renderer
    lateinit var allocator: Allocator
    lateinit var output: Output

    lateinit var buffer: Buffer
    lateinit var cairoBuffer: BufferImpl
    lateinit var surface: ImageSurface


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

        buffer = Buffer.allocate(Arena.global())
        cairoBuffer = BufferImpl.allocate(Arena.global()).apply {
            destroy(::cairoBufferDestroy)
            begin_data_ptr_access(::beginDataPtrAccess)
            end_data_ptr_access(::endDataPtrAccess)
        }
        buffer.init(cairoBuffer, 256, 256)


        // Cairo: Initialize context and surface
        surface = ImageSurface.create(Format.ARGB32, 256, 256)
        val cr = Context.create(surface)

        // Cairo: Start drawing
        cr.setSourceRGB(1.0, 1.0, 1.0)
        cr.paint()
        cr.setSourceRGB(0.0, 0.0, 0.0)
        val x = 25.6
        val y = 128.0
        val x1 = 102.4
        val y1 = 230.4
        val x2 = 153.6
        val y2 = 25.6
        val x3 = 230.4
        val y3 = 128.0
        cr.moveTo(x, y)
        cr.curveTo(x1, y1, x2, y2, x3, y3)
        cr.lineWidth = 10.0
        cr.stroke()
        cr.destroy()

        // Scene buffer
        val sceneBuffer = SceneBuffer.create(scene.tree(), buffer)
        sceneBuffer.node().setPosition(50, 50)
        buffer.drop()
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
        surface.destroy()
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
        data.set(C_POINTER, 0, surface.data)
        stride.set(C_INT, 0, surface.stride)
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