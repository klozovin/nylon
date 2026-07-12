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
        with(output.events) {
            outputs[frame.add(::onOutputFrame)] = output
            outputs[requestState.add(::onOutputRequestState)] = output
            outputs[destroy.add(::onOutputDestroy)] = output
        }
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
        // All listeners added to this output's signals
        val listeners = outputs.entries.filter { it.value == output }
        require(listeners.size == 3)

        // Remove each listener from the signal
        listeners.forEach { (listener, _) -> listener.remove() }

        // Remove the listener->output entry from the hash map
        outputs.entries.removeAll(listeners)

        // TODO: Memory management: Close the arena used for listeners
    }
}