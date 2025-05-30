package compositor

import wayland.server.Listener
import wlroots.types.output.EventRequestState
import wlroots.types.output.Output
import wlroots.types.output.OutputState
import wlroots.types.scene.SceneOutput


class OutputSystem(val compositor: Compositor) {
    val outputs: MutableMap<Listener, Output> = HashMap()


    fun onNewOutput(output: Output) {
        output.initRender(compositor.allocator, compositor.renderer)

        OutputState.allocateConfined { outputState ->
            outputState.init()
            outputState.setEnabled(true)
            output.preferredMode()?.let { outputState.setMode(it) }
            output.commitState(outputState)
            outputState.finish()
        }

        // Let the scene graph know that we have a new output attached
        val outputLayoutOutput = compositor.outputLayout.addAuto(output)
        val sceneOutput = SceneOutput.create(compositor.scene, output)
        compositor.sceneOutputLayout.addOutput(outputLayoutOutput, sceneOutput)

        // Add listeners to Output's signals, associate listeners with this output.
        with(outputs) {
            put(output.events.frame.add(::onOutputFrame), output)
            put(output.events.requestState.add(::onOutputRequestState), output)
            put(output.events.destroy.add(::onOutputDestroy), output)
        }
        require(outputs.size == 3)
    }


    fun onOutputFrame(output: Output) {
        compositor.scene.getSceneOutput(output)!!.apply {
            commit()
            sendFrameDone()
        }
    }


    fun onOutputRequestState(event: EventRequestState) {
        event.output.commitState(event.state)
    }


    fun onOutputDestroy(output: Output) {
        // When destroying an output: remove all of its listeners, close the confined arena, delete the
        // Output from the object cache.

        // TODO: Use .keys instead of map {it.key}
        val listeners = outputs.filterValues(output::equals).map { it.key }
        require(listeners.size == 3) { "Number of listeners: ${listeners.size}"}

        // TODO: Add this back in
//        listeners.forEach { it.remove() } // TODO: Memory management: Close the arena used for listeners


        // TODO: Delete from Output cache also
    }
}