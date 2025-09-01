package wlroots.types.scene;

import wlroots.types.output.OutputLayoutOutput;

import java.lang.foreign.MemorySegment;

import static jextract.wlroots.types.wlr_scene_h.wlr_scene_output_layout_add_output;


/// `struct wlr_scene_output_layout {}`
public class SceneOutputLayout {
    public final MemorySegment sceneOutputLayoutPtr;


    public SceneOutputLayout(MemorySegment sceneOutputLayoutPtr) {
        this.sceneOutputLayoutPtr = sceneOutputLayoutPtr;
    }


    /// Add an output to the scene output layout.
    ///
    /// When the layout output is repositioned, the scene output will be repositioned accordingly.
    public void addOutput(OutputLayoutOutput outputLayoutOutput,  SceneOutput sceneOutput) {
        wlr_scene_output_layout_add_output(
            sceneOutputLayoutPtr,
            outputLayoutOutput.outputLayoutOutputPtr,
            sceneOutput.sceneOutputPtr);
    }
}