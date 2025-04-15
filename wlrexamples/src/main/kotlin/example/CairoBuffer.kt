package example

import org.freedesktop.cairo.Context
import org.freedesktop.cairo.Format
import org.freedesktop.cairo.ImageSurface
import wayland.server.Display
import wlroots.Log
import wlroots.wlr.Backend
import wlroots.wlr.render.Allocator
import wlroots.wlr.render.Renderer
import wlroots.wlr.types.Output
import wlroots.wlr.types.Scene
import kotlin.system.exitProcess


object CairoBuffer {

    lateinit var display: Display
    lateinit var backend: Backend
    lateinit var scene: Scene
    lateinit var renderer: Renderer
    lateinit var allocator: Allocator
    lateinit var surface: ImageSurface



    fun run() {
        Log.init(Log.Importance.INFO)

        display = Display.create()
        backend = Backend.autocreate(display.getEventLoop(), null) ?: error("Can't proceed without Backend")
        scene = Scene.create()
        renderer = Renderer.autocreate(backend)
        renderer.initWlDisplay(display)
        allocator = Allocator.autocreate(backend, renderer)


        backend.events.newOutput.add(::handleNewOutput)
        if(!backend.start()) {
            display.destroy()
            exitProcess(1)
        }

        // Cairo: Initialize context and surface
        surface = ImageSurface.create(Format.ARGB32, 256, 256)
        val context = Context.create(surface)

        // Cairo: Start drawing
        context.setSourceRGB(1.0, 1.0, 1.0)
        context.destroy()

        context.stroke()



        println(surface)
        println(context)


        // Cleanup
        surface.close()
        display.destroy()
    }

    fun handleNewOutput(output: Output) {
        println(output)
        println("\n\n")

    }
}


fun main() {
    CairoBuffer.run()
}