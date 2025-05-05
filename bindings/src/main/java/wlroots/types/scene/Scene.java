package wlroots.types.scene;

import jextract.wlroots.types.wlr_scene;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wlroots.types.output.Output;
import wlroots.types.output.OutputLayout;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_scene_h.*;


/// The root scene-graph node.
///
/// `struct wlr_scene {}`
@NullMarked
public class Scene {
    MemorySegment scenePtr;


    private Scene(MemorySegment scenePtr) {
        assert !scenePtr.equals(NULL);
        this.scenePtr = scenePtr;
    }


    /// Create a new scene-graph.
    static public Scene create() {
        return new Scene(wlr_scene_create());
    }


    public SceneTree tree() {
        return new SceneTree(wlr_scene.tree(scenePtr));
    }


    public SceneOutputLayout attachOutputLayout(OutputLayout outputLayout) {
        return new SceneOutputLayout(wlr_scene_attach_output_layout(scenePtr, outputLayout.outputLayoutPtr));
    }


    /// @return Scene-graph output from a struct wlr_output, or NULL if the output hasn't been added to the
    ///         scene graph
    public @Nullable SceneOutput getSceneOutput(Output output) {
        return SceneOutput.ofPtrOrNull(wlr_scene_get_scene_output(scenePtr, output.outputPtr));
    }
}