package compositor.output

import compositor.Compositor
import wlroots.types.output.Output as WlrOutput


class OutputSystem(val compositor: Compositor) {

    val outputs: MutableList<Output> = mutableListOf()


    fun onNewOutput(wlrOutput: WlrOutput) {
        require(outputs.none { it.wlrOutput == wlrOutput }) { "Output with that wlr_output already exists" }
        val output = Output(compositor, wlrOutput)
        outputs.add(output)
    }


    fun remove(output: Output) {
        require(outputs.contains(output))
        outputs.remove(output)
    }
}