package wlroots.types.scene;

import jextract.wlroots.types.wlr_scene;
import org.jspecify.annotations.NullMarked;
import wlroots.types.output.OutputLayout;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_scene_h.wlr_scene_attach_output_layout;
import static jextract.wlroots.types.wlr_scene_h.wlr_scene_create;


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
}