package compositor.output

import compositor.Compositor
import wayland.server.Listener
import wlroots.types.output.EventRequestState
import wlroots.types.output.OutputState
import wlroots.types.scene.SceneOutput
import wlroots.types.output.Output as WlrOutput


class Output(val compositor: Compositor, val wlrOutput: WlrOutput) {

    val frameListener: Listener
    val requestStateListener: Listener
    val destroyListener: Listener
    val outputs = compositor.outputSystem


    init {
        wlrOutput.initRender(compositor.allocator, compositor.renderer)

        // Configure monitor default settings
        OutputState.allocateConfined { outputState ->
            outputState.init()
            outputState.setEnabled(true)
            wlrOutput.preferredMode()?.let { outputState.setMode(it) }
            wlrOutput.commitState(outputState)
            outputState.finish()
        }

        // Let the scene graph know that we have a new output attached
        val outputLayoutOutput = outputs.outputLayout.addAuto(wlrOutput)
        val sceneOutput = SceneOutput.create(compositor.windowSystem.scene, wlrOutput)
        outputs.sceneOutputLayout.addOutput(outputLayoutOutput, sceneOutput)

        with(wlrOutput.events) {
            frameListener = frame.add(::onOutputFrame)
            requestStateListener = requestState.add(::onOutputRequestState)
            destroyListener = destroy.add(::onOutputDestroy)
        }
    }


    fun onOutputFrame(output: WlrOutput) {
        compositor.windowSystem.scene.getSceneOutput(output)!!.apply {
            commit()
            sendFrameDone()
        }
    }


    fun onOutputRequestState(event: EventRequestState) {
        event.output.commitState(event.state)
    }


    fun onOutputDestroy(output: WlrOutput) {
        frameListener.remove()
        requestStateListener.remove()
        destroyListener.remove()
        compositor.outputSystem.remove(this)

        // TODO: Memory management: Close the arena used for listeners
    }
}