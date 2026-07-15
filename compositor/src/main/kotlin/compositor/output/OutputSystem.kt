package compositor.output

import compositor.Compositor
import wlroots.types.output.OutputLayout
import wlroots.types.scene.SceneOutputLayout
import wlroots.types.output.Output as WlrOutput


class OutputSystem(val compositor: Compositor) {

    val outputs: MutableList<Output> = mutableListOf()
    val outputLayout: OutputLayout
    val sceneOutputLayout: SceneOutputLayout


    init {
        outputLayout = OutputLayout.create(compositor.display)
        sceneOutputLayout = compositor.windowSystem.scene.attachOutputLayout(outputLayout)
    }

    fun onNewOutput(wlrOutput: WlrOutput) {
        require(this@OutputSystem.outputs.none { it.wlrOutput == wlrOutput }) { "Output with that wlr_output already exists" }
        val output = Output(compositor, wlrOutput)
        this@OutputSystem.outputs.add(output)
    }


    fun remove(output: Output) {
        require(this@OutputSystem.outputs.contains(output))
        this@OutputSystem.outputs.remove(output)
    }
}